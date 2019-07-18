package com.z.timer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TimerTest {

  HashedWheelTimer timer;

  @Before
  public void before() {
    timer = new HashedWheelTimer(TimeUnit.MICROSECONDS.toNanos(10), 8,
        new WaitStrategy.SleepWait());
  }

  @After
  public void after() throws InterruptedException {
    timer.shutdownNow();
    Assert.assertTrue(timer.awaitTermination(10, TimeUnit.SECONDS));
  }

  @Test
  public void oneShotRunnableTest() throws InterruptedException {
    AtomicInteger i = new AtomicInteger(1);
    timer.schedule(i::decrementAndGet, 100, TimeUnit.MILLISECONDS);
    Thread.sleep(300);
    Assert.assertThat(i.get(), Is.is(0));
  }

  @Test
  public void oneShotRunnableFuture() {
    AtomicInteger i = new AtomicInteger(1);
    long start = System.currentTimeMillis();
    Assert.assertNull(timer.schedule(i::decrementAndGet, 100, TimeUnit.MILLISECONDS));
    long end = System.currentTimeMillis();
    Assert.assertThat(i.get(), Is.is(0));
    Assert.assertTrue(end - start >= 100);
  }



}
