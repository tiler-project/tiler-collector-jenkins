package io.tiler.collectors.jenkins.config;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ConfigFactory {
  private static final long ONE_HOUR_IN_MILLISECONDS = 60 * 60 * 1000l;

  public Config load(JsonObject config) {
    return new Config(
      getCollectionIntervalInMilliseconds(config),
      getJobLimit(config),
      getServers(config));
  }

  private long getCollectionIntervalInMilliseconds(JsonObject config) {
    return config.getLong("collectionIntervalInMilliseconds", ONE_HOUR_IN_MILLISECONDS);
  }

  private Integer getJobLimit(JsonObject config) {
    return config.getInteger("jobLimit", 10);
  }

  private List<Server> getServers(JsonObject config) {
    JsonArray servers = config.getArray("servers");
    ArrayList<Server> loadedServers = new ArrayList<>();

    if (servers == null) {
      return loadedServers;
    }

    servers.forEach(serverObject -> {
      JsonObject server = (JsonObject) serverObject;
      loadedServers.add(getServer(server));
    });

    return loadedServers;
  }

  private Server getServer(JsonObject server) {
    String serverName = getServerName(server);
    String serverHost = getServerHost(server);

    if (serverName == null) {
      serverName = serverHost;
    }

    return new Server(
      serverName,
      serverHost,
      getServerPort(server),
      getServerSsl(server));
  }

  private String getServerName(JsonObject server) {
    return server.getString("name");
  }

  private boolean getServerSsl(JsonObject server) {
    return server.getBoolean("ssl", false);
  }

  private Integer getServerPort(JsonObject server) {
    return server.getInteger("port", 8080);
  }

  private String getServerHost(JsonObject server) {
    return server.getString("host", "localhost");
  }
}
