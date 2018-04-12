import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CassandraInit {

  private static final int PORT = 9043;
  private static final String[] NODES = {
//      "unix4.andrew.cmu.edu",
//      "unix5.andrew.cmu.edu",
      "unix6.andrew.cmu.edu",
      "unix7.andrew.cmu.edu",
      "unix8.andrew.cmu.edu"
  };

  public static void main(String[] args) {
    Cluster cluster = null;

    cluster = Cluster.builder()
        .withClusterName("haystack_stores")
        .addContactPoints(NODES)
        .withPort(PORT)
        .build();

    Session session = cluster.connect();

    createKeyspace(session, "photo", "SimpleStrategy", 2);
    session.execute("USE photo");

    ResultSet rs = session.execute("select release_version from system.local");    // (3)
    Row row = rs.one();
    System.out.println(row.getString("release_version"));                          // (4)

    cluster.close();
  }

  public static void createKeyspace(Session session,
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

  public static void createTable(Session session, String name) {
    StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
        .append(name).append("(")
        .append("id uuid PRIMARY KEY, ")
        .append("title text,")
        .append("subject text);");

    String query = sb.toString();
    session.execute(query);
  }
}
