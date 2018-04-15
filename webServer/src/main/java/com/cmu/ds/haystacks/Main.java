package com.cmu.ds.haystacks;

import com.cmu.ds.haystacks.config.HSConfig;
import com.cmu.ds.haystacks.config.HSConfigParser;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.ByteBuffer;
import org.rapidoid.config.Conf;
import org.rapidoid.data.JSON;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.io.Upload;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import redis.clients.jedis.Jedis;


public class Main {
    private static HSConfig config;
    private static Cluster dirCluster;
    private static Cluster storeCluster;
    private static Session dirSession;
    private static Session storeSession;
    private static Jedis jedisClient;


    public static void main(String[] args) throws Exception {
        // parse haystack configuration
        config = HSConfigParser.parse("../config/config.yaml");

        App.run(args);
        Conf.HTTP.set("maxPipeline", 128);
        Conf.HTTP.set("timeout", 0);

        // connect to the local redis: currently we define the redis only as our cache of directory
        // TODO: decide how to use redis to handle the recovery
        jedisClient = new Jedis("127.0.0.1", config.getDirectoryCacheRedisPort());

        // connnect to the cassandra - dir
        // TODO: hard code or not? it is a question
        dirCluster = Cluster.builder()
            .withClusterName("directory")
            .addContactPoint(config.getDirectroyAddress())
            .withPort(config.getDirectoryPort())
            .build();
        dirSession = dirCluster.connect();
        initCassandraDirectory(dirSession, 1);

        // connect to cassandra - stores
        storeCluster = Cluster.builder()
            .withClusterName("haystack_stores")
            .addContactPoints(config.getObjectStoreAddresses())
            .withPort(config.getObjectStorePort())
            .build();
        storeSession = storeCluster.connect("store");

        // First: parse the uploaded image: http://<dns>/upload/<multipart-file>
        On.post("/upload").managed(false).cacheTTL(6000).plain(Main::onPostImage);

        // Second: get: http://<dns>/get?pid=<pid>
        On.get("/get").managed(false).cacheTTL(6000).plain(Main::onGetImage);

        // Third: delete: http://<dns>/delete?pid=<pid>
        On.delete("/delete").managed(false).cacheTTL(6000).plain(Main::onDelete);
    }

    private static Resp onGetImage(Req req) {
        Resp res = req.response();
        // get the pid
        String pid = req.param("pid", "");
        if (pid.equals("")) {
            return res.code(400).result("Bad request: empty pid.");
        }

        // query to the redis to get the info
        String tmpUrl = jedisClient.get(pid);
        if (tmpUrl != null && tmpUrl.trim().length() != 0) {
            // if hit cache, return the pid + photo path
            return res.redirect(tmpUrl).done();
        } else {
            // if not hit, first query to the directory
            String resPid;
            String resCacheUrl;
            int resLvid;

            String query = "SELECT * FROM photo_info WHERE pid='" + pid + "'";
            ResultSet results = dirSession.execute(query);

            Row row = results.one();

            if (row == null) {
                return res.code(404).result("PID does not exist.");
            }

            resPid = row.getString("pid");
            resCacheUrl = row.getString("cache_url");
            resLvid = row.getInt("lvid");

            String resPath = "http://" + resCacheUrl + "/get?" + "lvid=" + resLvid + "&pid=" + resPid;
            // update the redis
            jedisClient.set(pid, resPath);
            System.out.println("Cache updated.");
            System.out.println("URL: " + resPath);
            return res.redirect(resPath).done();
        }
    }

    private static Resp onPostImage(Req req) {
        Resp res = req.response();
        String[] cacheServers = config.getObjectCacheAddresses();
        int numLogicalVolume = config.getNumLogicalVolumes();
        int objectCachePort = config.getObjectCachePort();

        StringBuilder sb = new StringBuilder();

        // read a/a list of files from the request
        Upload upfile = req.file("img");

        if (upfile != null) {
            // generate a random pid for the current image
            long tmpPid = System.currentTimeMillis();
            sb.append(String.valueOf(tmpPid));

            // query for writable logical volumes
            // status 1 = writable
            String lvidQuery = "SELECT * FROM store_info WHERE status = 1 ALLOW FILTERING;";
            ResultSet results = dirSession.execute(lvidQuery);

            // select lvid
            int selectedLvid = 0;
            int proposedLvid = (int) (tmpPid % numLogicalVolume) + 1;
            for (Row row : results) {
                if (row.getInt("lvid") == proposedLvid) {
                    selectedLvid = proposedLvid;
                    break;
                }
            }
            // if the proposed lvid doesn't exist, choose the first one.
            if (selectedLvid == 0) {
                selectedLvid = results.one().getInt("lvid");
            }

            // select cache_url
            int selectedCacheInx = (int) (tmpPid % cacheServers.length);
            String selectedCacheUrl = cacheServers[selectedCacheInx] + ":" + objectCachePort;

            // insert the photo path into directory
            // cache url is hardcode now: should be the ip of cache server
            // should also uplad binary photo object
            String insertQuery = "INSERT INTO photo_info (pid, cache_url, lvid) VALUES (?, ?, ?);";
            ResultSet r = dirSession.execute(insertQuery, String.valueOf(tmpPid), selectedCacheUrl,
                selectedLvid);

            // insert into object store
            byte[] fileBytes = upfile.content();
            PreparedStatement ps = storeSession.prepare("INSERT INTO " + "lv" + selectedLvid
                + " (pid, image) VALUES (:pid, :image);");
            BoundStatement bound = ps.bind()
                .setString("pid", String.valueOf(tmpPid))
                .setBytes("image", ByteBuffer.wrap(fileBytes));
            storeSession.execute(bound);
        }
        ObjectNode node = JSON.newMapper().createObjectNode();
        node.put("pid", sb.toString());
        return res.code(201).json(node);
    }

    private static Resp onDelete(Req req) {
        Resp res = req.response();
        // get the pid
        String pid = req.param("pid", "");

        // send to the redis to delete the cache
        jedisClient.del(pid);

        // send to the directory to delete the file
        String searchQuery = "SELECT pid, cache_url, lvid FROM photo_info WHERE pid = '" + pid +
            "';";
        ResultSet r = dirSession.execute(searchQuery);
        Row row = r.one();
        if (row == null) {
            // if there is no such photo
            return res.code(404).result("No such image.");
        } else {
            String delQuery = "DELETE FROM photo_info WHERE pid = '" + row.getString("pid") + "';";
            dirSession.execute(delQuery);
            return res.code(200).result("Image deleted.");
        }
    }

    private static void initCassandraDirectory(Session session, int dirReplicationFactor) {
        createKeyspaceIfNotExist(session, "directory", "SimpleStrategy", dirReplicationFactor);
        session.execute("USE directory");
        createStoreInfoIfNotExist(session);
        createPhotoInfoIfNotExist(session);
    }

    private static void createKeyspaceIfNotExist(Session session,
        String keyspaceName, String replicationStrategy, int replicationFactor) {
        StringBuilder sb =
            new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ")
                .append(keyspaceName).append(" WITH replication = {")
                .append("'class':'").append(replicationStrategy)
                .append("','replication_factor':").append(replicationFactor)
                .append("};");

        String query = sb.toString();
        session.execute(query);
    }

    private static void createStoreInfoIfNotExist(Session session) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
            .append("store_info").append("(")
            .append("lvid int PRIMARY KEY, ")
            .append("status int);");

        String query = sb.toString();
        session.execute(query);

        // init table
        int numLogicalVolumes = config.getNumLogicalVolumes();
        for (int i = 1; i <= numLogicalVolumes; i++) {
            PreparedStatement ps = session.prepare(
                "UPDATE store_info "
                    + "SET status=? WHERE lvid=?;");
            BoundStatement bound = ps.bind()
                .setInt(0, 1)
                .setInt(1, i);
            session.execute(bound);
        }
    }

    private static void createPhotoInfoIfNotExist(Session session) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
            .append("photo_info").append("(")
            .append("pid text PRIMARY KEY, ")
            .append("lvid int, ")
            .append("cache_url text);");

        String query = sb.toString();
        session.execute(query);
    }
}

