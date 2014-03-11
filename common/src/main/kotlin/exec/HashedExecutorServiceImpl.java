/*
 * Copyright (c) 2009-2013 Cazcade Limited  - All Rights Reserved
 */

package exec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Neil Ellis
 */

public class HashedExecutorServiceImpl extends AbstractServiceStateMachine implements HashedExecutorService {

    private final String name;
    private final int maxRetry;

    private final int buckets;

    private List<ThreadPoolExecutor> executors;
    private final int queueSize;
    private final int requeueDelay;

    private final int threadsPerBucket;


    private final AtomicLong count = new AtomicLong();
    private int threadCount;

    public HashedExecutorServiceImpl(final String name, final int maxRetry, final int buckets, final int queueSize, final int requeueDelay, final int threadsPerBucket) {
        super();
        this.name = name;
        this.maxRetry = maxRetry;
        this.buckets = buckets;
        this.queueSize = queueSize;
        this.requeueDelay = requeueDelay;
        this.threadsPerBucket = threadsPerBucket;
    }

    public void execute(final boolean retry, final Object key, final HashedExecutable executable) throws InterruptedException {
        begin();
        try {
            executeInternal(retry, false, false, executable, getThreadPoolExecutor(key));
        } finally {
            end();
        }
    }

    private ThreadPoolExecutor getThreadPoolExecutor(Object key) {
        int index = hashToBucket(key);
        ThreadPoolExecutor threadPoolExecutor = executors.get(index);
        if (threadPoolExecutor == null) {
            threadPoolExecutor = createExecutor(key, index);
            executors.set(index, threadPoolExecutor);
        }
        return threadPoolExecutor;
    }

    @Override
    public void submit(final boolean retry, boolean skipIfFUll, final Object key, final HashedExecutable executable) throws InterruptedException {
        begin();
        try {
            executeInternal(retry, true, skipIfFUll, executable, getThreadPoolExecutor(key));
        } finally {
            end();
        }
    }

    private int hashToBucket(Object key) {
        byte[] bytes = key.toString().getBytes();
        long hashedValue = 1;
        for (byte aByte : bytes) {
            hashedValue += Math.abs(aByte) * 39;
        }
        for (byte aByte : bytes) {
            hashedValue *= Math.abs(aByte) / 7 + 1;
        }
        for (byte aByte : bytes) {
            hashedValue += Math.abs(aByte) * 13 - hashedValue / 3;
        }
//        System.out.println("Hashed value " + hashedValue);
        int hashInt = (int) (Math.abs(hashedValue) % buckets);
//        System.out.println("Hash bucket is " + hashInt);
        return hashInt;
    }

    private void executeInternal(final boolean retry, boolean submit, boolean skipIfFull, final HashedExecutable executable, final ThreadPoolExecutor threadPoolExecutor) throws InterruptedException {
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
                                    executable.run();
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
                if (submit) {
//                    System.out.println(name + ": Submitted request, queue is " + threadPoolExecutor.getQueue().size() + " long");

                    threadPoolExecutor.submit(command);
                } else {
//                    System.out.println(name + ": Executing request, queue is " + threadPoolExecutor.getQueue().size() + " long");
                    threadPoolExecutor.execute(command);
                }
                cont = false;
                count.incrementAndGet();
            } catch (RejectedExecutionException e) {
                if (skipIfFull) {
                    System.out.println(name + ": Queue size at limit " + threadPoolExecutor.getQueue().size() + " skipping");
                    return;
                } else {
                    System.out.println(name + ": Queue size at limit " + threadPoolExecutor.getQueue().size() + " requeing");
                    Thread.sleep(requeueDelay);
                }
            }
        }
    }

    public void execute(final boolean retry, final HashedExecutable executable) throws InterruptedException {
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
            executeInternal(retry, false, false, executable, executor);
        } finally {
            end();
        }
    }

    public void execute(final HashedExecutable executable) throws InterruptedException {
        final ThreadPoolExecutor threadPoolExecutor = executors.get((int) (buckets * Math.random()));
        executeInternal(false, false, false, executable, threadPoolExecutor);
    }

    @Override
    public void start() throws Exception {
        super.start();
        executors = new ArrayList<ThreadPoolExecutor>(buckets);
        for (int i = 0; i < buckets; i++) {
            executors.add(null);
        }
        unlock();
    }

    private ThreadPoolExecutor createExecutor(final Object key, final int index) {
        return new ThreadPoolExecutor(threadsPerBucket, threadsPerBucket, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queueSize), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, name + "[" + index + "," + threadCount++ + "] (" + key.toString() + ")");
            }
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void stop() {
        System.out.println("Stopping Fountain Executor.");
        super.stop();
        try {
            for (int i = 0; i < buckets; i++) {
                if (executors.get(i) != null) {
                    executors.get(i).shutdownNow();
                }
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
