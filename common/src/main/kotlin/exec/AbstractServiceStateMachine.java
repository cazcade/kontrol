/*
 * Copyright (c) 2009-2013 Cazcade Limited  - All Rights Reserved
 */

package exec;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Neil Ellis
 */

public abstract class AbstractServiceStateMachine implements ServiceStateMachine {
    private enum State {
        STOPPED, INITIALISATION, STARTED, PAUSED
    }

    public static final int LOCK_TRY_TIMEOUT_IN_SECS = 20;

    private volatile State state = State.STOPPED;


    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicBoolean locked = new AtomicBoolean(false);

    protected AbstractServiceStateMachine() {
    }

    public void begin() throws InterruptedException {
        if (state == State.STOPPED) {
            throw new IllegalStateException("Cannot begin an action as the service is stopped.");
        }
        if (state == State.PAUSED) {
            System.out.println("Waiting on paused service.");
        }
        if (locked.get()) {
            waitUntilUnlocked();
        }
        activeCount.incrementAndGet();
    }

    private void waitUntilUnlocked() throws InterruptedException {
        System.out.println("Waiting for service lock...");
        int count = 0;
        while (locked.get() && count++ < LOCK_TRY_TIMEOUT_IN_SECS * 10) {
            Thread.sleep(100);
            System.err.print(".");
            if (state == State.STOPPED) {
                System.out.println("Service stopped, giving up on lock.");
                throw new IllegalStateException("Cannot begin an action as the service is stopped.");
            }
        }
        if (locked.get()) {
            throw new StateMachineFailure("Failed waiting for unlock with " + count + "attempts.");
        }
    }

    public void end() {
        activeCount.decrementAndGet();
    }

    public synchronized void hardstop() {
        state = State.STOPPED;
        locked.set(false);
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public void pause() throws Exception {
        if (state == State.STOPPED) {
            throw new IllegalStateException("Tried to pause a stopped service.");
        }
        if (state == State.PAUSED) {
            throw new IllegalStateException("Tried to pause a paused service.");
        }
        state = State.PAUSED;
        lock();
    }

    public void resume() throws Exception {
        if (state == State.STOPPED) {
            throw new IllegalStateException("Tried to resume a stopped service.");
        }
        if (state == State.STARTED) {
            throw new IllegalStateException("Tried to resume a started service.");
        }
        state = State.STARTED;
        unlock();
    }

    public final void startIfNotStarted() throws Exception {
        synchronized (locked) {
            if (!isStarted()) {
                start();
            }
        }
    }

    public boolean isStarted() {
        return state == State.STARTED;
    }

    public void start() throws Exception {
        if (state == State.STARTED) {
            throw new IllegalStateException("Tried to start a started service.");
        }
        if (state == State.PAUSED) {
            throw new IllegalStateException("Tried to start a paused service.");
        }
        state = State.STARTED;
    }

    public final void stopIfNotStopped() {
        synchronized (locked) {
            if (!isStopped()) {
                stop();
                unlock();
            }
        }
    }

    public boolean isStopped() {
        return state == State.STOPPED;
    }

    public void stop() {
        synchronized (locked) {
            changeStateToStopped();
            try {
                if (!locked.get()) {
                    lock();
                }
            } catch (InterruptedException e) {
                //Interruptions at this point shouldn't stop a shutdown process.
                Thread.interrupted();
                e.printStackTrace();
            }
        }
    }

    protected void changeStateToStopped() {
        if (state == State.STOPPED) {
            System.out.println("Tried to stop a stopped service.");
        }
        state = State.STOPPED;
    }

    public void lock() throws InterruptedException {
        synchronized (locked) {
            if (locked.get()) {
                System.out.println("Already locked.");
            } else {
                locked.set(true);
            }
            int count = 0;
            while (activeCount.get() > 0 && count++ < LOCK_TRY_TIMEOUT_IN_SECS) {
                //noinspection WaitWithoutCorrespondingNotify
                locked.wait(1000);
                System.out.println("Awaiting active count (" + activeCount.get() + ") on " + getClass());
            }
            if (count >= LOCK_TRY_TIMEOUT_IN_SECS) {
                unlock();
                throw new StateMachineFailure(String.format("Lock failed with %s attempts %s active.", count, activeCount));
            }
        }
    }

    public void unlock() {
        synchronized (locked) {
            locked.set(false);
        }
    }
}
