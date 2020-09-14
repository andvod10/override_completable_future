package com.completablefuture;

import com.completablefuture.logging.MdcAwareCompletableFuture;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CreateCmd {
    Logger logger = LoggerFactory.getLogger(CreateCmd.class);
    int defaultRequestTimeout;

    public CreateCmd(int defaultRequestTimeout) {
        this.defaultRequestTimeout = defaultRequestTimeout;
    }

    public CompletableFuture<String> handle(String cmdRequest, Map headers) {
        try {
            String transactionId = null;
            if (Objects.nonNull(headers))
                transactionId = (String) headers.get("X-Clearleap-TransactionId");
            MDC.put("gtid", transactionId);

            JsonObject body = new JsonObject(cmdRequest);
            logger.info("body reqest: {}", body.encode());

            String phoneNo = body.getString("phone_no");

            return MdcAwareCompletableFuture.supplyAsync(() -> {
                //create user in DB
                String userId = "phone-no-" + phoneNo;
                MDC.put("userId", userId);
                logger.info("Created user in database");

                return userId;
            }).thenApplyAsync(userId -> {
                //generate an OTP for the user
                String otp = UUID.randomUUID().toString();
                logger.info("Generated OTP for user");

                //send the OTP to user
                String messageId = userId + otp;
                MDC.put("messageId", messageId);
                logger.info("OTP sent to user");

                return "Successfully, OK";
            });
        } catch (Exception e) {
            logger.info("BAD REQUEST: Invalid Json: " + e.getMessage() + " : " + cmdRequest);
            return CompletableFuture.completedFuture("Invalid Json Provided");
        } finally {
            MDC.remove("gtid");
        }
    }

    public void handleRequest(RoutingContext ctx) {
        CompletableFuture.runAsync(() -> {
            Map<String, String> hdrs = new HashMap<>();
            ctx.request().headers().forEach(entry -> {
                hdrs.put(entry.getKey(), entry.getValue());
            });
            Map headers = hdrs;
            handle(ctx.getBodyAsString(), headers).thenAccept(cmdResult -> {
                HttpServerResponse response = ctx.response();
                response.setStatusCode(200);
                response.end(cmdResult);
                logger.info("cmdResult: {}", cmdResult);
            });

        });
    }
}