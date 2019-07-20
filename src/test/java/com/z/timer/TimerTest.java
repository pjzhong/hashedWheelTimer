package com.z.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    timer = new HashedWheelTimer(TimeUnit.MILLISECONDS.toNanos(10), 8,
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
    timer.schedule(() -> {
      i.decrementAndGet();
    }, 100, TimeUnit.MILLISECONDS);
    Thread.sleep(300);
    Assert.assertThat(i.get(), Is.is(0));
  }

  @Test
  public void oneShotRunnableFutureTest()
      throws InterruptedException, ExecutionException, TimeoutException {
    AtomicInteger i = new AtomicInteger(1);
    long start = System.currentTimeMillis();
    Assert.assertThat(timer.schedule(i::decrementAndGet, 100, TimeUnit.MILLISECONDS)
        .get(10, TimeUnit.SECONDS), Is.is(0));
    long end = System.currentTimeMillis();
    Assert.assertThat(i.get(), Is.is(0));
    Assert.assertTrue(end - start >= 100);
  }

  @Test
  public void oneShotCallableTest() throws InterruptedException {
    AtomicInteger i = new AtomicInteger(1);
    timer.schedule(() -> {
      i.decrementAndGet();
      return "Hello";
    }, 100, TimeUnit.MILLISECONDS);
    Thread.sleep(300);
    Assert.assertThat(i.get(), Is.is(0));
  }

  @Test
  public void oneShotCallableFuture()
      throws InterruptedException, TimeoutException, ExecutionException {
    AtomicInteger i = new AtomicInteger(1);
    long start = System.currentTimeMillis();
    Future<String> future = timer.schedule(() -> {
      i.decrementAndGet();
      return "Hello";
    }, 900, TimeUnit.MILLISECONDS);
    Assert.assertThat(future.get(1, TimeUnit.SECONDS), Is.is("Hello"));
    long end = System.currentTimeMillis();
    Assert.assertThat(i.get(), Is.is(0));
    Assert.assertTrue(end - start >= 100);
  }

  @Test
  public void executionOnTime() throws InterruptedException {
    int scheduledTasks = 100000;
    int tickDuration = 10;
    int timeout = 130;

    int minTimeout = timeout - tickDuration, maxTimeout =
        timeout + (tickDuration * 8 /*Wheel size*/);
    long[] delays = new long[scheduledTasks];

    CountDownLatch latch = new CountDownLatch(scheduledTasks);
    for (int i = 0; i < scheduledTasks; i++) {
      long start = System.nanoTime();
      int idx = i;
      timer.schedule(() -> {
        delays[idx] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        latch.countDown();
      }, timeout, TimeUnit.MILLISECONDS);
    }

    latch.await();

    for (int i = 0; i < scheduledTasks; i++) {
      long delay = delays[i];
      Assert.assertTrue(
          String.format("Timeout %s delay must be %s < %s < %s", i, minTimeout, delay, maxTimeout),
          delay >= minTimeout && delay < maxTimeout);
    }
  }

  @Test
  public void fixedRateFirstFireTest() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    long start = System.currentTimeMillis();
    timer.scheduleAtFixedRate(latch::countDown, 100, 100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    Assert.assertTrue(end - start >= 100);
  }

  @Test
  public void fixedRateSubsequentFireTest() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(10);
    long start = System.currentTimeMillis();
    timer.scheduleAtFixedRate(latch::countDown, 100, 100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    Assert.assertTrue(end - start >= 1000);
  }

  @Test
  public void delayBetweenFixedRateEvents() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    List<Long> r = new ArrayList<>();
    timer.scheduleAtFixedRate(() -> {
      r.add(System.currentTimeMillis());
      latch.countDown();

      if (latch.getCount() == 0) {
        return;
      }

      try {
        Thread.sleep(90);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      r.add(System.currentTimeMillis());

    }, 100, 100, TimeUnit.MILLISECONDS);

    // time difference between the beginning of second tick and end of first one
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    Assert.assertTrue(r.get(2) - r.get(1) <= 100);
  }

  @Test
  public void fixedDelayFirstFireTest() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    long start = System.currentTimeMillis();
    timer.scheduleWithFixedDelay(latch::countDown, 100, 100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    Assert.assertTrue(end - start >= 100);
  }

  @Test
  public void fixedDelaySubsequentFireTest() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(10);
    long start = System.currentTimeMillis();
    timer.scheduleWithFixedDelay(latch::countDown, 100, 100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    Assert.assertTrue(end - start >= 1000);
  }

  @Test
  public void delayBetweenFixedDelayEvents() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    List<Long> r = new ArrayList<>();
    timer.scheduleWithFixedDelay(() -> {
      r.add(System.currentTimeMillis());

      latch.countDown();

      if (latch.getCount() == 0) {
        return;
      }

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      r.add(System.currentTimeMillis());
    }, 100, 100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    // time difference between the beginning of second tick and the first one
    Assert.assertTrue(r.get(2) - r.get(1) >= 100);
  }

}
