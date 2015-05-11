package io.tiler.collectors.jenkins.config;

public class Server {
  private final String name;
  private final String host;
  private final Integer port;
  private final String path;
  private final boolean ssl;
  private final int jobLimit;

  public Server(String name, String host, Integer port, String path, boolean ssl, int jobLimit) {
    if (name == null) {
      name = host;
    }

    if (path == null) {
      path = "";
    }
    else if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    this.name = name;
    this.host = host;
    this.port = port;
    this.path = path;
    this.ssl = ssl;
    this.jobLimit = jobLimit;
  }

  public String name() {
    return name;
  }

  public String host() {
    return host;
  }

  public Integer port() {
    return port;
  }

  public String path() {
    return path;
  }

  public boolean ssl() {
    return ssl;
  }

  public int jobLimit() {
    return jobLimit;
  }
}
