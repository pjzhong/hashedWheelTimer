package com.z.timer;

import java.util.concurrent.TimeUnit;

public interface WaitStrategy {

  /**
   * Wait until the given deadline, deadlineNanoseconds
   *
   * @param deadlineNanoseconds deadline to wait for, in Nanoseconds
   */
  void waitUntil(long deadlineNanoseconds) throws InterruptedException;


  class SleepWait implements WaitStrategy {

    @Override
    public void waitUntil(long deadline) throws InterruptedException {
      long sleepTimeNanos = deadline - System.nanoTime();
      TimeUnit.NANOSECONDS.sleep(sleepTimeNanos);
    }
  }
}
