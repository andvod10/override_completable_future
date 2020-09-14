package com.completablefuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;
import io.vertx.config.*;

public class ConfigLoader {
    private static CompletableFuture<JsonObject> loadVertxConfig(Vertx vertx,
                                                                 ConfigRetrieverOptions options) {
        CompletableFuture<JsonObject> futureConfig = new CompletableFuture<>();

        ConfigRetriever retriever = null;

        if (null != vertx) {
            if (null != options) {
                retriever = ConfigRetriever.create(vertx, options);
            } else {
                retriever = ConfigRetriever.create(vertx);
            }

            if (null != retriever) {
                retriever.getConfig(ar -> {
                    if (ar.failed()) {
                        futureConfig.completeExceptionally(ar.cause());
                    } else {
                        futureConfig.complete(new JsonObject(ar.result().getMap()));
                    }
                });
            } else {
                futureConfig.completeExceptionally(new Exception("Failed to get ConfigRetriever"));
            }
        } else {
            futureConfig.completeExceptionally(new Exception("Vertx not initialized into Holder"));
        }

        return futureConfig;
    }

    public static CompletableFuture<JsonObject> loadConfig(Vertx vertx) {
        return loadConfig(vertx, "conf/config.json");
    }

    /**
     * Load a config from the given file.
     */
    public static CompletableFuture<JsonObject> loadConfig(Vertx vertx, String fileName) {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setOptional(true)
                .setFormat("json")
                .setConfig(new io.vertx.core.json.JsonObject().put("path", fileName));

        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");

        ConfigStoreOptions envPropsStore = new ConfigStoreOptions().setType("env");

        ConfigStoreOptions vertxConfig = new ConfigStoreOptions().setType("json").setConfig(vertx.getOrCreateContext().config());

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(vertxConfig)
                .addStore(fileStore)
                .addStore(sysPropsStore)
                .addStore(envPropsStore);

        return loadVertxConfig(vertx, options);
    }
}
