/*
 * Copyright (c) 2009-2013 Cazcade Limited  - All Rights Reserved
 */

package exec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Neil Ellis
 */

public class FountainExecutorServiceImpl extends AbstractServiceStateMachine implements FountainExecutorService {

    private final int maxRetry;

    private final int buckets;

    private List<ThreadPoolExecutor> executors;
    private final int queueSize;
    private final int requeueDelay;

    private final int threadsPerBucket;


    private final AtomicLong count = new AtomicLong();

    public FountainExecutorServiceImpl(final int maxRetry, final int buckets, final int queueSize, final int requeueDelay, final int threadsPerBucket) {
        super();
        this.maxRetry = maxRetry;
        this.buckets = buckets;
        this.queueSize = queueSize;
        this.requeueDelay = requeueDelay;
        this.threadsPerBucket = threadsPerBucket;
    }

    public void execute(final boolean retry, final Object key, final FountainExecutable executable) throws InterruptedException {
        begin();
        try {
            executeInternal(retry, false, executable, executors.get(Math.abs(key.hashCode() % buckets)));
        } finally {
            end();
        }
    }

    @Override
    public void submit(final boolean retry, final Object key, final FountainExecutable executable) throws InterruptedException {
        begin();
        try {
            System.out.println("Submitted with key " + key);
            executeInternal(retry, true, executable, executors.get(Math.abs(key.hashCode() % buckets)));
        } finally {
            end();
        }
    }

    private void executeInternal(final boolean retry, boolean submit, final FountainExecutable executable, final ThreadPoolExecutor threadPoolExecutor) throws InterruptedException {
        boolean cont = true;
        while (cont) {
            try {
                Runnable command = new Runnable() {
                    public void run() {
                        try {
                            boolean fail = true;
                            int count = 0;
                            do {
                                try {
                                    if (isStopped()) {
                                        return;
                                    }
                                    if (isPaused()) {
                                        Thread.sleep(100);
                                        continue;
                                    }
                                    System.out.println("Executing ...");
                                    executable.run();
                                    System.out.println("Executed ...");
                                    fail = false;
                                } catch (InterruptedException ie) {
                                    Thread.interrupted();
                                    System.out.println("Aborted due to interrupt.");
                                    return;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException ie) {
                                        Thread.interrupted();
                                        System.out.println("Aborted due to interrupt.");
                                        return;
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    System.out.println("Unrecoverable error: " + t);

                                    //                                    System.exit(-1);
                                }
                            } while (retry && fail && count++ < maxRetry);
                        } finally {
                            count.decrementAndGet();
                        }
                    }
                };
                System.out.println("Queue size is " + threadPoolExecutor.getQueue().size());
                if (submit) {
                    threadPoolExecutor.submit(command);
                } else {
                    threadPoolExecutor.execute(command);
                }
                cont = false;
                count.incrementAndGet();
            } catch (RejectedExecutionException e) {
                Thread.sleep(requeueDelay);
            }
        }
    }

    public void execute(final boolean retry, final FountainExecutable executable) throws InterruptedException {
        begin();
        try {
            int minimum = Integer.MAX_VALUE;
            ThreadPoolExecutor executor = null;
            for (final ThreadPoolExecutor threadPoolExecutor : executors) {
                final int i = threadPoolExecutor.getQueue().size();
                if (i < minimum) {
                    executor = threadPoolExecutor;
                    minimum = i;
                }
            }
            assert executor != null;
            executeInternal(retry, false, executable, executor);
        } finally {
            end();
        }
    }

    public void execute(final FountainExecutable executable) throws InterruptedException {
        final ThreadPoolExecutor threadPoolExecutor = executors.get((int) (buckets * Math.random()));
        executeInternal(false, false, executable, threadPoolExecutor);
    }

    @Override
    public void start() throws Exception {
        super.start();
        executors = new ArrayList<ThreadPoolExecutor>(buckets);
        for (int i = 0; i < buckets; i++) {
            executors.add(new ThreadPoolExecutor(threadsPerBucket, threadsPerBucket, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queueSize), new ThreadPoolExecutor.AbortPolicy()));
        }
        unlock();
    }

    @Override
    public void stop() {
        System.out.println("Stopping Fountain Executor.");
        super.stop();
        try {
            for (int i = 0; i < buckets; i++) {
                executors.get(i).shutdownNow();
            }
            executors.clear();
            System.out.println("Fountain Executor STOPPED");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waitForExecutionsToFinish() throws InterruptedException {
        System.out.println("Waiting for executions to finish.");
        do {
            Thread.sleep(100);
            System.out.println("Waiting for executions to finish.");
        } while (!isStopped() && count.get() > 0);
        if (count.get() > 0 && isStopped()) {
            System.out.println("Given up on waiting for executions to finish service is stopped.");
        }
    }
}
