package com.z.timer;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class HashedWheelTimer {

    private WaitStrategy waitStrategy;
    private Set<Registration<?>>[] wheel;
    private final int wheelSize;
    private final long resolution;

    private final ExecutorService loop;
    private final ExecutorService executor;

    private volatile int cursor = 0;

    public HashedWheelTimer(String name, long res, int wheelSize, WaitStrategy strategy, ExecutorService exec) {
        this.waitStrategy = strategy;

        this.wheel = new Set[wheelSize];
        for (int i = 0; i < wheelSize; i++) {
            wheel[i] = new ConcurrentSkipListSet<>();
        }
        this.wheelSize = wheelSize;

        this.resolution = res;
        final Runnable loopRun = new Runnable() {
            @Override
            public void run() {
                long deadline = System.nanoTime();

                while (true) {
                    Set<Registration<?>> registrations = wheel[cursor];
                    for (Registration r : registrations) {
                        if (r.isCancelled()) {
                            registrations.remove(r);
                        } else if (r.ready()) {
                            registrations.remove(r);

                            exec.execute(r);
                            if (!r.isCancelAfterUse()) {
                                //TODO reschedule(r);
                            }
                        } else {
                            r.decrement();
                        }
                    }

                    deadline += resolution;

                    try {
                        waitStrategy.waitUntil(deadline);
                    } catch (InterruptedException e) {
                        return;
                    }

                    cursor = (cursor + 1) % wheelSize;
                }
            }
        };

        this.loop = Executors.newSingleThreadExecutor(new ThreadFactory() {
            AtomicInteger i = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, name + "-" + i.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        this.loop.submit(loopRun);
        this.executor = exec;
    }
}
