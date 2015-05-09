package io.tiler.collectors.jenkins;

import io.tiler.collectors.jenkins.config.Config;
import io.tiler.collectors.jenkins.config.ConfigFactory;
import io.tiler.collectors.jenkins.config.Server;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.List;
import java.util.stream.Collectors;

public class JenkinsCollectorVerticle extends Verticle {
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
    getJobs(config.jobLimit(), instances -> {
      transformMetrics(instances, metrics -> {
        publishNewMetrics(metrics, aVoid -> {
          logger.info("Collection finished");
          handler.handle(null);
        });
      });
    });
  }

  private void getJobs(int jobLimit, Handler<JsonArray> handler) {
    getJobs(jobLimit, 0, new JsonArray(), handler);
  }

  private void getJobs(int jobLimit, int serverIndex, JsonArray servers, Handler<JsonArray> handler) {
    if (serverIndex >= config.servers().size()) {
      handler.handle(servers);
    }

    httpClients.get(serverIndex).getNow("/api/json?pretty=true", response -> {
      response.bodyHandler(body -> {
        logger.info("Received jobs " + body);
        JsonArray jobs = new JsonObject(body.toString()).getArray("jobs");

        if (jobs == null) {
          logger.error("Could not retrieve jobs");
          jobs = new JsonArray();
        }

        logger.info("Received " + jobs.size() + " jobs");
        logger.info("Jobs limit set to " + jobLimit);

        List jobList = jobs.toList().subList(0, Math.min(jobs.size(), jobLimit));
        jobs = new JsonArray(jobList);
        logger.info("There are " + jobs.size() + " jobs after limiting");

        Server serverConfig = config.servers().get(serverIndex);

        JsonObject server = new JsonObject();
        server.putString("name", serverConfig.name());
        server.putArray("jobs", jobs);

        servers.addObject(server);
        getJobs(jobLimit, serverIndex + 1, servers, handler);
      });
    });
  }

  private void transformMetrics(JsonArray servers, Handler<JsonArray> handler) {
    logger.info("Transforming metrics");
    long time = getCurrentTimestampInMicroseconds();

    JsonArray newPoints = new JsonArray();
    JsonObject newMetric = new JsonObject()
      .putString("name", "ci.jenkins.job_color")
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

  private long getCurrentTimestampInMicroseconds() {
    return System.currentTimeMillis() * 1000;
  }

  private void publishNewMetrics(JsonArray metrics, Handler<Void> handler) {
    logger.info("Publishing metrics to event bus");
    logger.info("New metrics " + metrics);
    JsonObject message = new JsonObject()
      .putArray("metrics", metrics);
    eventBus.publish("io.squarely.vertxspike.metrics", message);
    handler.handle(null);
  }
}
