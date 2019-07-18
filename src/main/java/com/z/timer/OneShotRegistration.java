package com.z.timer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OneShotRegistration<T> extends CompletableFuture<T> implements Registration<T> {

  private final Callable<T> callable;
  private volatile int rounds;
  private volatile Status status;

  private final long delay;

  public OneShotRegistration(Callable<T> callable, int rounds, long delay) {
    this.callable = callable;
    this.rounds = rounds;
    this.delay = delay;
    this.status = Status.READY;
  }

  @Override
  public boolean ready() {
    return rounds == 0;
  }

  @Override
  public boolean isCancelAfterUse() {
    return true;
  }

  @Override
  public int rounds() {
    return rounds;
  }

  @Override
  public int getOffset() {
    throw new UnsupportedOperationException("One Shot Registrations can be reschedule");
  }

  @Override
  public void decrement() {
    rounds -= 1;
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("One Shot Registrations can be reset");
  }

  @Override
  public void run() {
    try {
      this.complete(callable.call());
    } catch (Exception e) {
      this.completeExceptionally(e);
    }
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return delay;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }
}
