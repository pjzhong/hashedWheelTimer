package com.z.timer;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FixedDelayRegistration<T> extends OneShotRegistration<T> {

  private final int rescheduleRounds;
  private final int scheduleOffset;
  private final Consumer<Registration<?>> callback;

  public FixedDelayRegistration(Callable<T> callable, int firstRound, long delay,
      int scheduleRounds, int scheduleOffset, Consumer<Registration<?>> callback) {
    super(callable, firstRound, delay);
    this.rescheduleRounds = scheduleRounds;
    this.scheduleOffset = scheduleOffset;
    this.callback = callback;
  }

  @Override
  public int getOffset() {
    return scheduleOffset;
  }

  @Override
  public void reset() {
    this.status = Status.READY;
    this.rounds = rescheduleRounds;
  }

  @Override
  public boolean isCancelAfterUse() {
    return true;
  }

  @Override
  public void run() {
    super.run();
    callback.accept(this);
  }
}
