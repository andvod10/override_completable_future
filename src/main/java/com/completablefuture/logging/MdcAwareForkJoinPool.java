package com.completablefuture.logging;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.completablefuture.logging.MdcWrapperHelper.wrapWithMdcContext;

public class MdcAwareForkJoinPool extends ForkJoinPool {
    @Override
    public void execute(ForkJoinTask<?> task) {
        super.execute(wrapWithMdcContext(task));
    }

    @Override
    public void execute(Runnable task) {
        super.execute(wrapWithMdcContext(task));
    }

    @Override
    public <T> T invoke(ForkJoinTask<T> task) {
        return super.invoke(wrapWithMdcContext(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        return super.invokeAll(wrapWithMdcContext(tasks.stream().map(it -> (Callable<T>)it).collect(Collectors.toList())));
    }

    @Override
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        return super.submit(wrapWithMdcContext(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return super.submit(wrapWithMdcContext(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return super.submit(wrapWithMdcContext(task), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        return super.submit(wrapWithMdcContext(task));
    }
}