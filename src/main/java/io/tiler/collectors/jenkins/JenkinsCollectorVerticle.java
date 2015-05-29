package io.tiler.collectors.jenkins;

import io.tiler.BaseCollectorVerticle;
import io.tiler.collectors.jenkins.config.Config;
import io.tiler.collectors.jenkins.config.ConfigFactory;
import io.tiler.collectors.jenkins.config.Server;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class JenkinsCollectorVerticle extends BaseCollectorVerticle {
  private Logger logger;
  private Config config;
  private EventBus eventBus;
  private List<HttpClient> httpClients;

  public void start() {
    logger = container.logger();
    config = new ConfigFactory().load(container.config());
    eventBus = vertx.eventBus();
    httpClients = createHttpClients();

    final boolean[] isRunning = {true};

    collect(aVoid -> {
      isRunning[0] = false;
    });

    vertx.setPeriodic(config.collectionIntervalInMilliseconds(), aLong -> {
      if (isRunning[0]) {
        logger.info("Collection aborted as previous run still executing");
        return;
      }

      isRunning[0] = true;

      collect(aVoid -> {
        isRunning[0] = false;
      });
    });

    logger.info("JenkinsCollectorVerticle started");
  }

  private List<HttpClient> createHttpClients() {
    return config.servers()
      .stream()
      .map(server -> {
        HttpClient httpClient = vertx.createHttpClient()
          .setHost(server.host())
          .setPort(server.port())
          .setSSL(server.ssl())
          .setTryUseCompression(true);
        // Get the following error without turning keep alive off.  Looks like a vertx bug
        // SEVERE: Exception in Java verticle
        // java.nio.channels.ClosedChannelException
        httpClient.setKeepAlive(false);
        return httpClient;
      })
      .collect(Collectors.toList());
  }

  private void collect(Handler<Void> handler) {
    logger.info("Collection started");
    getJobs(instances -> {
      transformMetrics(instances, metrics -> {
        saveMetrics(metrics);
        logger.info("Collection finished");
        handler.handle(null);
      });
    });
  }

  private void getJobs(Handler<JsonArray> handler) {
    getJobs(0, new JsonArray(), handler);
  }

  private void getJobs(int serverIndex, JsonArray servers, Handler<JsonArray> handler) {
    if (serverIndex >= config.servers().size()) {
      handler.handle(servers);
      return;
    }

    Server serverConfig = config.servers().get(serverIndex);

    httpClients.get(serverIndex).getNow(serverConfig.path() + "/api/json?pretty=true", response -> {
      response.bodyHandler(body -> {
        JsonArray jobs = new JsonObject(body.toString()).getArray("jobs");

        if (jobs == null) {
          logger.error("Could not retrieve jobs");
          jobs = new JsonArray();
        }

        logger.info("Received " + jobs.size() + " jobs");
        int jobLimit = serverConfig.jobLimit();
        logger.info("Jobs limit set to " + jobLimit);
        jobs = new JsonArray(jobs.toList().subList(0, Math.min(jobs.size(), jobLimit)));
        logger.info("There are " + jobs.size() + " jobs after limiting");

        JsonObject server = new JsonObject();
        server.putString("name", serverConfig.name());
        server.putArray("jobs", jobs);

        servers.addObject(server);
        getJobs(serverIndex + 1, servers, handler);
      });
    });
  }

  private void transformMetrics(JsonArray servers, Handler<JsonArray> handler) {
    logger.info("Transforming metrics");
    long time = currentTimeInMicroseconds();

    JsonArray newPoints = new JsonArray();
    JsonObject newMetric = new JsonObject()
      .putString("name", config.metricNamePrefix() + "job-color")
      .putArray("points", newPoints)
      .putNumber("timestamp", time);

    servers.forEach(serverObject -> {
      JsonObject server = (JsonObject) serverObject;
      String serverName = server.getString("name");

      server.getArray("jobs").forEach(jobObject -> {
        JsonObject job = (JsonObject) jobObject;
        newPoints.addObject(new JsonObject()
          .putNumber("time", time)
          .putString("serverName", serverName)
          .putString("jobName", job.getString("name"))
          .putString("value", job.getString("color")));
      });
    });

    JsonArray newMetrics = new JsonArray();
    newMetrics.addObject(newMetric);

    handler.handle(newMetrics);
  }
}
