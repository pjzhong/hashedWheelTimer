package com.z.timer;

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
    int interval = 80;
    int timeout = 130;
    int maxTimeout = timeout + interval * 2;
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
          String.format("Timeout %s delay must be %s < %s < %s", i, timeout, delay, maxTimeout),
          delay >= timeout && delay < maxTimeout);
    }
  }


}
