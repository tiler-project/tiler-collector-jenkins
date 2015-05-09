package io.tiler.collectors.jenkins.config;

import java.util.List;

public class Config {
  private final long collectionIntervalInMilliseconds;
  private final int jobLimit;
  private final List<Server> servers;
  private final String metricNamePrefix;

  public Config(long collectionIntervalInMilliseconds, int jobLimit, List<Server> servers, String metricNamePrefix) {
    this.collectionIntervalInMilliseconds = collectionIntervalInMilliseconds;
    this.jobLimit = jobLimit;
    this.servers = servers;
    this.metricNamePrefix = metricNamePrefix;
  }

  public long collectionIntervalInMilliseconds() {
    return collectionIntervalInMilliseconds;
  }

  public int jobLimit() {
    return jobLimit;
  }

  public List<Server> servers() {
    return servers;
  }

  public String metricNamePrefix() {
    return metricNamePrefix;
  }
}
