package io.tiler.collectors.jenkins.config;

import java.util.List;

public class Config {
  private final long collectionIntervalInMilliseconds;
  private final Integer jobLimit;
  private final List<Server> servers;

  public Config(long collectionIntervalInMilliseconds, Integer jobLimit, List<Server> servers) {
    this.collectionIntervalInMilliseconds = collectionIntervalInMilliseconds;
    this.jobLimit = jobLimit;
    this.servers = servers;
  }

  public long collectionIntervalInMilliseconds() {
    return collectionIntervalInMilliseconds;
  }

  public Integer jobLimit() {
    return jobLimit;
  }

  public List<Server> servers() {
    return servers;
  }
}
