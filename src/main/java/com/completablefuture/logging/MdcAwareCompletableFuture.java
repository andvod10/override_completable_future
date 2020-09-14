package com.completablefuture.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;

public class MdcAwareCompletableFuture<T> extends CompletableFuture<T> {
    protected static final Logger log = LoggerFactory.getLogger(MdcAwareCompletableFuture.class);

    static {
        try {
            setFinalStatic(CompletableFuture.class.getDeclaredField("ASYNC_POOL"), new MdcAwareForkJoinPool());
        } catch (Exception e) {
            log.error("failed to configure MDC aware thread pool for CompletableFuture", e);
            throw new RuntimeException("failed to configure MDC aware thread pool for CompletableFuture", e);
        }
    }

    @Override
    public CompletableFuture newIncompleteFuture() {
        return new MdcAwareCompletableFuture();
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        var modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}