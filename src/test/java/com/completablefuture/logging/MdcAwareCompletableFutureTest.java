package com.completablefuture.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MdcAwareCompletableFutureTest {
    private static Logger log = (Logger) LogManager.getLogger(MdcAwareCompletableFutureTest.class);
    private static CustomAppenderImpl appender;

    private static final String txid = "NO-TRANSACTION-1234";

    @BeforeAll
    static void setUp() {
        //invoke static block in MdcAwareCompletableFuture
        new MdcAwareCompletableFuture<>();
    }

    @BeforeEach
    void before() {
        PatternLayout customLayout = createCustomLayout("%d::%X{gtid} %-5p [%t] %c - %m%n");
        appender = new CustomAppenderImpl("MockAppender", null, customLayout, false);
        appender.start();
        log.addAppender(appender);
    }

    @AfterEach
    void after() {
        log.removeAppender(appender);
    }

    @Test
    public void testWrappedMethods() throws ExecutionException, InterruptedException {
        MDC.put("gtid", txid);
        AtomicBoolean assertResult = new AtomicBoolean(false);

        var exec = MdcAwareThreadPool.newFixedThreadPool(10);

        CompletableFuture<Void> result = MdcAwareCompletableFuture
                .supplyAsync(() -> {
                    return assertTransactionId("supplyAsync");
                })
                .thenApply(s -> {
                    return assertTransactionId("thenApply");
                })
                .thenCompose(s -> {
                    // a pretty reasonable use of CF
                    // (e.g. could happen in an external library or framework)
                    // breaks the transaction logging for future steps:
                    var rv = new CompletableFuture<String>();
                    for (int i = 0; i < 10; i++) {
                        exec.submit(() -> {
                            log.info("stage 3 : {}", txid);
                            rv.complete("stage 3 : " + txid);
                        });
                    }
                    return rv;
                })
                .thenCompose(s -> MdcAwareCompletableFuture.supplyAsync(() -> {
                    return assertTransactionId("thenCompose");
                }))
                .handle((s, err) -> {
                    return assertTransactionId("handle");
                })
                .thenAccept(s -> {
                    assertTransactionId("thenAccept");
                    assertResult.set(true);
                });

        //wait to complete all operations
        result.get();
        assertTrue(assertResult.get());

        List<String> logEvents = appender.getMessages();
        logEvents.stream().forEach(logEvent -> assertTrue(logEvent.contains(txid)));
    }

    static PatternLayout createCustomLayout(final String pattern) {
        PatternLayout.Builder builder = PatternLayout.newBuilder();
        builder.withPattern(pattern)
                .withCharset(Charset.forName("GBK"))
                .withAlwaysWriteExceptions(false)
                .withNoConsoleNoAnsi(true);
        return builder.build();
    }

    private String assertTransactionId(String method) {
        String gtid = MDC.get("gtid");
        log.info("in {}: {}", method, gtid);
        assertEquals(txid, gtid);
        return gtid;
    }
}

