package com.cmu.ds.haystacks;

import com.cmu.ds.haystacks.config.HSConfig;
import com.cmu.ds.haystacks.config.HSConfigParser;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.rapidoid.config.Conf;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
import redis.clients.jedis.Jedis;


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
        Jedis jedisClient = new Jedis("127.0.0.1");
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
            // get the pid
            String mid = req.param("mid", "");
            String lvid = req.param("lvid", "");
            String pid = req.param("pid", "");
            if (mid.equals("") || lvid.equals("") || pid.equals("")) {
                return "error";
            }

            // first ask redis to get the info
            // redis store the image as string
            String imageString = jedisClient.get(pid);
            if (imageString != null) {
                // if hit cache, return the image object
                return ImageUtils.decodeToImage(imageString);
            } else {
                // if not hit, first query to the directory
                // TODO: how cassandra store? different lvid as different table?
                String resImageString = "";
                String query = "SELECT * FROM photo WHERE pid='" + pid + "'";
                ResultSet results = session.execute(query);
                if (results == null) {
                    return "error";
                }
                for (Row row : results) {
                    resImageString = row.getString("image_string");
                }

                if (resImageString.equals("")) {
                    return "no results";
                }

                // update the redis
                jedisClient.set(pid, resImageString);
                System.out.println("Cache updated");
                return ImageUtils.decodeToImage(resImageString);
            }
        });


        // Third: delete: http://<dns>/delete?pid=<pid>

        On.get("/delete").managed(false).cacheTTL(6000).plain((req) -> {
            // get the pid
            String pid = req.param("pid", "");

            // send to the redis to delete the cache
            jedisClient.del(pid);

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
        for (int i = numLogicalVolumes; i > 0; i--) {
            createLogicalVolumeIfNotExist(session, "lv" + i);
        }
        session.execute("USE store");
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
            .append("pid uuid PRIMARY KEY, ")
            .append("photo blob);");

        String query = sb.toString();
        session.execute(query);
    }
}

