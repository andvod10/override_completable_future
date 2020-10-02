package com.completablefuture;

import com.completablefuture.logging.MdcAwareCompletableFuture;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AppVerticle extends AbstractVerticle {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AppVerticle.class);

    public AppVerticle() {
        this(30, 0);
    }

    protected AppVerticle(int dnsTtl, int dnsNegTtl) {
        try {
            java.security.Security.setProperty("networkaddress.cache.ttl", String.valueOf(dnsTtl));
        } catch (Exception e) {
            LOGGER.warn("Attempting to set DNS cache: {}", e.getMessage());
        }

        try {
            java.security.Security.setProperty("networkaddress.cache.negative.ttl", String.valueOf(dnsNegTtl));
        } catch (Exception e) {
            LOGGER.warn("Attempting to set DNS negative cache: {}", e.getMessage());
        }
    }

    protected CompletableFuture<Boolean> initialize(JsonObject config) {
        //before starting service completablefuture should be implemented with MDC aware
        new MdcAwareCompletableFuture<>();

        int webserverCount = config.getInteger("httpServer.count", 1);
        LOGGER.warn("Starting {} Vertx Web Servers", webserverCount);

        config.put("useHttps", false);
        config.put("http.port", 8093);

        return deployVerticle(() -> new ServerVerticle(config), webserverCount).thenApply((v) -> true);
    }

    @Override
    public void start(Promise<Void> startPromise) {
        CompletableFuture<JsonObject> futureConfig = ConfigLoader.loadConfig(vertx);

        // Fetch the configuration from /conf/config.json
        CompletableFuture<Boolean> steps = futureConfig.thenComposeAsync(config -> {
            VertxOptions vertxOptions = new VertxOptions();
            int eventLoopSize = config.getInteger("vertx.eventloop.size", 0);

            if (eventLoopSize > vertxOptions.getEventLoopPoolSize()) {
                vertxOptions.setEventLoopPoolSize(eventLoopSize);
                // Overwrite the vertx with the modified one
                vertx = Vertx.vertx(vertxOptions);
            }
            LOGGER.warn("Vertx Event Loop Size: {}", vertxOptions.getEventLoopPoolSize());

            return initialize(config);
        });

        steps.handle((data, ex) -> {
            if (null != ex) {
                startPromise.fail(ex);
            } else {
                if (data) {
                    startPromise.complete();
                } else {
                    startPromise.fail("Initialization Failed");
                }
            }
            return null;
        });
    }

    protected CompletableFuture<Void> deployVerticle(Supplier<AbstractVerticle> fn, int count) {
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setInstances(count);
        return deployVerticle(fn, deploymentOptions);
    }

    protected CompletableFuture<Void> deployVerticle(Supplier<AbstractVerticle> fn, DeploymentOptions options) {
        CompletableFuture<Void> deployComplete = new CompletableFuture<>();
        try {
            // Start the HTTP Server
            vertx.deployVerticle(() -> fn.get(), options, ar -> {
                if (ar.succeeded()) {
                    LOGGER.info("Verticles Deployed");
                    deployComplete.complete(null);
                } else {
                    LOGGER.error("Could not start Verticles", ar.cause());
                    deployComplete.completeExceptionally(ar.cause());
                }
            });
        } catch (Exception e) {
            deployComplete.completeExceptionally(e);
        }
        return deployComplete;
    }
}
