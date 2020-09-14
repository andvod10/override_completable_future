package com.completablefuture.logging;

import org.slf4j.MDC;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

public class MdcWrapperHelper {
    public static <T> Callable<T> wrapWithMdcContext(Callable<T> task) {
        //save the current MDC context
        var ctx = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(ctx);
            try {
                return task.call();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static Runnable wrapWithMdcContext(Runnable task) {
        //save the current MDC context
        var ctx = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(ctx);
            try {
                task.run();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static <T> Collection<Callable<T>> wrapWithMdcContext(Collection<? extends Callable<T>> tasks) {
        var ctx = MDC.getCopyOfContextMap();
        return tasks.stream()
                .<Callable<T>>map(task -> () -> {
                    setMDCContext(ctx);
                    try {
                        return task.call();
                    } finally {
                        MDC.clear();
                    }
                })
                .collect(Collectors.toList());
    }

    public static <T> ForkJoinTask<T> wrapWithMdcContext(ForkJoinTask<T> task) {
        var ctx = MDC.getCopyOfContextMap();
        // TODO: not clear how to implement this, and even not sure whether it makes sense at all since ForkJoin task
        //  is also a Future. Just throwing exception here to empirically identify if this is used at all.
//        new ForkJoinTask.AdaptedCallable<T>(() -> {
//            setMDCContext(ctx)
//            task.exec()
//            task.getResult()
//            throw new UnsupportedOperationException('not implemented')
//        })
        throw new UnsupportedOperationException("not implemented");
    }

    static void setMDCContext(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}
