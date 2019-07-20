package com.z.timer;

import java.util.concurrent.Callable;

public class FixedRateRegistration<T> extends OneShotRegistration<T> {

  private int rescheduleRounds;
  private int scheduleOffset;

  public FixedRateRegistration(Callable<T> callable, int fireRounds, long delay,
      int rescheduleRounds,
      int scheduleOffset) {
    super(callable, fireRounds, delay);
    this.rescheduleRounds = rescheduleRounds;
    this.scheduleOffset = scheduleOffset;
  }

  @Override
  public int getOffset() {
    return this.scheduleOffset;
  }

  @Override
  public void reset() {
    this.status = Status.READY;
    this.rounds = rescheduleRounds;
  }

  @Override
  public boolean isCancelAfterUse() {
    return false;
  }
}
