package com.completablefuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ServerVerticle extends AbstractVerticle {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class);
    private static final int DEFAULT_PORT = 8443;
    protected JsonObject config;

    public ServerVerticle(JsonObject configObj) {
        this.config = configObj;
    }

    protected CompletableFuture<Void> startHttpServer(final JsonObject config) {
        CompletableFuture<Void> future = new CompletableFuture<Void>();

        boolean useHttps = config.getBoolean("useHttps", true);
        final String serverType = useHttps ? "HTTPS" : "HTTP";
        HttpServer server = vertx.createHttpServer();

        Router router = buildRouter();

        int port = config.getInteger("http.port", DEFAULT_PORT);

        server.requestHandler(router)
                .listen(port, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("{} server running on {} ", serverType, port);
                        future.complete(null);
                    } else {
                        LOGGER.error("Could not start a {} server", serverType, ar.cause());
                        future.completeExceptionally(ar.cause());
                    }
                });

        return future;
    }

    protected Router buildRouter() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        CreateCmd createSCmd = new CreateCmd(config.getInteger("request.timeout", 500));
        router.post("/completablefuture/v1/create").handler(createSCmd::handleRequest);

        return router;
    }

    @Override
    public void start(final Future<Void> startFuture) {
        LOGGER.info("Initializing Routing and Server");
        startHttpServer(config).handle((data, ex) -> {
            if (null != ex) {
                LOGGER.error("Failed to start application: " + ex.toString());
                startFuture.fail(ex);
            } else {
                startFuture.complete();
            }
            return null;
        });
    }
}
