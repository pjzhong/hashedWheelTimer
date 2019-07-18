package com.z.timer;

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
    Assert.assertNull(timer.schedule(i::decrementAndGet, 100, TimeUnit.MILLISECONDS)
        .get(10, TimeUnit.SECONDS));
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
    }, 100, TimeUnit.SECONDS);
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
    }, 100, TimeUnit.SECONDS);
    Assert.assertThat(future.get(10, TimeUnit.SECONDS), Is.is("Hello"));
    long end = System.currentTimeMillis();
    Assert.assertThat(i.get(), Is.is(0));
    Assert.assertTrue(end - start >= 100);
  }


}
