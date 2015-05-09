package io.tiler.collectors.jenkins.config;

public class Server {
  private final String name;
  private final String host;
  private final Integer port;
  private final boolean ssl;

  public Server(String name, String host, Integer port, boolean ssl) {
    this.name = name;
    this.host = host;
    this.port = port;
    this.ssl = ssl;
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

  public boolean ssl() {
    return ssl;
  }
}
