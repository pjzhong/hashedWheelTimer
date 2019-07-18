package com.z.timer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class HashedWheelTimer implements ScheduledExecutorService {

  private static final String DEFAULT_TIMER_NAME = "hashed-wheel-timer";

  private WaitStrategy waitStrategy;
  private Set<Registration<?>>[] wheel;
  private final int wheelSize;
  private final long resolution;

  private final ExecutorService loop;
  private final ExecutorService executor;

  private volatile int cursor = 0;

  public HashedWheelTimer(long res, int wheelSize, WaitStrategy strategy) {
    this(DEFAULT_TIMER_NAME, res, wheelSize, strategy, ForkJoinPool.commonPool());
  }

  public HashedWheelTimer(String name, long res, int wheelSize, WaitStrategy strategy,
      ExecutorService exec) {
    this.waitStrategy = strategy;

    this.wheel = new Set[wheelSize];
    for (int i = 0; i < wheelSize; i++) {
      wheel[i] = new ConcurrentSkipListSet<>();
    }
    this.wheelSize = wheelSize;

    this.resolution = res;
    this.executor = exec;
    this.loop = Executors.newSingleThreadExecutor(new ThreadFactory() {
      AtomicInteger i = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, name + "-" + i.getAndIncrement());
        thread.setDaemon(true);
        return thread;
      }
    });
    this.loop.execute(() -> {
      long deadline = System.nanoTime();

      while (true) {
        Set<Registration<?>> registrations = wheel[cursor];
        for (Registration r : registrations) {
          if (r.isCancelled()) {
            registrations.remove(r);
          } else if (r.ready()) {
            registrations.remove(r);
            executor.execute(r);
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
    });
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return null;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return null;
  }

  private <V> Registration<V> scheduleOneShot(long delay, Callable<V> callable) {
    assertRunning();

    int fireOffset = (int) (delay / resolution);
    int fireRounds = fireOffset / wheelSize;

    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
      TimeUnit unit) {
    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
      TimeUnit unit) {
    return null;
  }

  @Override
  public void shutdown() {
    this.loop.shutdown();
    this.executor.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    this.loop.shutdownNow();
    return this.executor.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return this.loop.isShutdown() && this.executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return this.loop.isTerminated() && this.executor.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return this.loop.awaitTermination(timeout, unit) && this.executor
        .awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return this.executor.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return this.executor.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return this.executor.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return this.executor.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) throws InterruptedException {
    return this.executor.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return this.executor.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return this.executor.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    this.executor.execute(command);
  }

  private int idx(int cursor) {
    return cursor % wheelSize;
  }

  private void assertRunning() {
    if (this.loop.isTerminated()) {
      throw new IllegalStateException("Timer is not running");
    }
  }

}
