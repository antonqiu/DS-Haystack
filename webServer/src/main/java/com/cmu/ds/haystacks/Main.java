package com.cmu.ds.haystacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rapidoid.config.Conf;
import org.rapidoid.io.Upload;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import redis.clients.jedis.Jedis;


public class Main {
    private static Cluster cluster;
    private static Session session;


    public static void main(String[] args) throws Exception {
        App.run(args);
        Conf.HTTP.set("maxPipeline", 128);
        Conf.HTTP.set("timeout", 0);

        // connect to the local redis: currently we define the redis only as our cache of directory
        // TODO: decide how to use redis to handle the recovery
        Jedis jedisClient = new Jedis("127.0.0.1");
        System.out.println("Connection to redis server");

        // connnect to the cassandra
        // TODO: hard code or not? it is a question
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build(); // just for test, it should be changed to the ip of directory server
        session = cluster.connect("directory"); // connect to the directory keyspace

        // First: parse the uploaded image: http://<dns>/upload/<multipart-file>
        On.post("/upload").managed(false).cacheTTL(6000).plain((req) -> {
            StringBuilder sb = new StringBuilder();

            // read a/a list of files from the request
            Map<String, List<Upload>> upFiles = req.files();

            for (Map.Entry<String, List<Upload>> entry : upFiles.entrySet()) {
                // generate a random pid for the current image
                String tmpPid = String.valueOf(System.currentTimeMillis());

                //sb.append("sucessful upload! Pid: ").append(tmpPid);

                // query for writable logical volumes
                // return 1 for developing, it should be larger than 1, status 1 = writable
                String lvidQuery = "SELECT lvid, mid FROM store WHERE status = 1 LIMIT 1 ALLOW FILTERING";
                ResultSet results = session.execute(lvidQuery);
                List<String[]> storeList = new ArrayList<String[]>();
                for (Row row : results) {
                    storeList.add(new String[]{row.getString("lvid"), row.getString("mid")});
                }

                // insert the photo path into directory
                // cache url is hardcode now: should be the ip of cache server
                // should also uplad binary photo object
                String insertQuery = "INSERT INTO photo (pid, cache_url, mid, lvid) VALUES (?, '127.0.0.1:8080', ?, ?);";
                for (String[] s : storeList) {
                    ResultSet r = session.execute(insertQuery, tmpPid, s[1], s[0]);
                    // send the image to the store
                    //FileInputStream fis = new FileInputStream();
                }
            }

            return sb.toString();
        });

        // Second: get: http://<dns>/get?pid=<pid>
        On.get("/get").managed(false).cacheTTL(6000).plain((req) -> {
            // get the pid
            String pid = req.param("pid", "");
            if (pid.equals("")) {
                return "error";
            }

            // query to the redis to get the info
            String tmpUrl = jedisClient.get(pid);
            if (tmpUrl != null && tmpUrl.trim().length() != 0) {
                // if hit cache, return the pid + photo path
                return pid + tmpUrl;
            } else {
                // if not hit, first query to the directory
                String resPid = "";
                String resCacheUrl = "";
                String resMid = "";
                String resLvid = "";

                String query = "SELECT * FROM photo WHERE pid='" + pid + "'";
                ResultSet results = session.execute(query);
                if (results == null) return "no results";
                for (Row row : results) {
                    // get the result
                    resPid = row.getString("photoPath");
                    resCacheUrl = row.getString("cache_url");
                    resMid = row.getString("mid");
                    resLvid = row.getString("lvid");
                }

                if (resPid.equals("") || resCacheUrl.equals("") || resMid.equals("") || resLvid.equals("")) {
                    return "no results";
                }

                String resPath = "http://" + resCacheUrl + "/get?" + "mid=" + resMid + "&lvid=" + resLvid + "&pid=" + resPid;
                // update the redis
                jedisClient.set(pid, resPath);
                System.out.println("Cache updated");
                return resPath;
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
}

