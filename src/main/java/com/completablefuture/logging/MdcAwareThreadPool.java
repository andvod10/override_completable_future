package com.completablefuture.logging;

import java.util.concurrent.*;

import static com.completablefuture.logging.MdcWrapperHelper.wrapWithMdcContext;

public class MdcAwareThreadPool extends ThreadPoolExecutor {
    MdcAwareThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                       BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    MdcAwareThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                       BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    MdcAwareThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                       BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    MdcAwareThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                       BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrapWithMdcContext(command));
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new MdcAwareThreadPool(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }
}
