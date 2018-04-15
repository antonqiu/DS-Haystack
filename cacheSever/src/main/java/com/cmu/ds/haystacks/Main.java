package com.cmu.ds.haystacks;

import com.cmu.ds.haystacks.config.HSConfig;
import com.cmu.ds.haystacks.config.HSConfigParser;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import java.nio.ByteBuffer;
import org.rapidoid.config.Conf;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
import redis.clients.jedis.BinaryJedis;


public class Main {
    private static HSConfig config;
    private static Cluster cluster;
    private static Session session;


    public static void main(String[] args) throws Exception {
        // parse haystack configuration
        config = HSConfigParser.parse("../config/config.yaml");

        App.run(args);
        Conf.HTTP.set("maxPipeline", 128);
        Conf.HTTP.set("timeout", 0);

        // connect to the local redis: currently we define the redis only as our cache of directory
        // TODO: decide how to use redis to handle the recovery
        BinaryJedis jedisClient = new BinaryJedis("127.0.0.1");
        System.out.println("Connection to redis server");

        // connnect to the cassandra
        String[] contactPoints = config.getObjectStoreAddresses();
        int storePort = config.getObjectStorePort();
        int storeNumberLogicalVolumes = config.getNumLogicalVolumes();
        int storeReplicationFactor = config.getStoreReplicationFactor();

        cluster = Cluster.builder()
            .withClusterName("haystack_stores")
            .addContactPoints(contactPoints)
            .withPort(storePort)
            .build(); // just for test, it should be changed to the ip of directory server
        session = cluster.connect();

        // init cassandra with store schema
        initCassandraStores(session, storeReplicationFactor, storeNumberLogicalVolumes);

        // Second: get: http://<dns>/get?mid=<mid>&lvid=<lvid>?pid=<pid>
        On.get("/get").managed(false).cacheTTL(6000).plain((req) -> {
            Resp res = req.response();
            // get the pid
            String mid = req.param("mid", "");
            String lvid = req.param("lvid", "");
            String pid = req.param("pid", "");
            if (mid.equals("") || lvid.equals("") || pid.equals("")) {
                return "error";
            }

            // first ask redis to get the info
            // redis store the image as string
            byte[] imageString = jedisClient.get(pid.getBytes());
            if (imageString != null) {
                // if hit cache, return the image object
                return res.contentType(MediaType.IMAGE_ANY).body(imageString);
            } else {
                // if not hit, first query to the directory
                PreparedStatement ps = session.prepare("SELECT * FROM :table WHERE pid= :pid");
                BoundStatement bound = ps.bind()
                    .setString("table", "lv" + lvid)
                    .setString("pid", pid);
                ByteBuffer imageBuffer;
                ResultSet results = session.execute(bound);
                Row row = results.one();
                if (row == null) {
                  return res.code(404);
                }

                imageBuffer = row.getBytes("image");

                // update the redis
                jedisClient.set(pid.getBytes(), imageBuffer.array());
                System.out.println("Cache updated");
                return res.contentType(MediaType.IMAGE_ANY).body(imageBuffer);
            }
        });


        // Third: delete: http://<dns>/delete?pid=<pid>

        On.get("/delete").managed(false).cacheTTL(6000).plain((req) -> {
            // get the pid
            String pid = req.param("pid", "");

            // send to the redis to delete the cache
            jedisClient.del(pid.getBytes());

            // send to the directory to delete the file
            String searchQuery = "SELECT pid, cache_url, mid, lvid FROM photo WHERE pid = '" + pid + "'";
            ResultSet r = session.execute(searchQuery);

            if (!r.iterator().hasNext() || r.iterator().next() == null) {
                // if there is no such photo
                return "no such photo!";
            } else {
                Row row = r.iterator().next();
                String delQuery = "DELETE FROM photo WHERE pid = '" + row.getString("pid") + "'";
                session.execute(delQuery);
                return "delete the photo";
            }
        });

    }

    private static void initCassandraStores(Session session, int storeReplicationFactor,
        int numLogicalVolumes) {
        createKeyspaceIfNotExist(session, "store", "SimpleStrategy", storeReplicationFactor);
        session.execute("USE store");
        for (int i = numLogicalVolumes; i > 0; i--) {
            createLogicalVolumeIfNotExist(session, "lv" + i);
        }
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

    private static void createLogicalVolumeIfNotExist(Session session, String name) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
            .append(name).append("(")
            .append("pid text PRIMARY KEY, ")
            .append("image blob);");

        String query = sb.toString();
        session.execute(query);
    }
}

