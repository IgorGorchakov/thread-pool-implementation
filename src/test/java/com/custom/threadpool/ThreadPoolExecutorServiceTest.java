package com.custom.threadpool;

import com.custom.threadpool.core.ThreadPoolExecutorService;
import com.custom.threadpool.future.Future;
import com.custom.threadpool.rejection.RejectionPolicies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolExecutorServiceTest {

    private ThreadPoolExecutorService pool;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            pool.awaitTermination(2000);
        }
    }

    // ── Basic submit ────────────────────────────────────────────────────

    @Test
    void submitRunnableExecutes() throws InterruptedException {
        pool = new ThreadPoolExecutorService(2);
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] executed = {false};

        pool.submit(() -> {
            executed[0] = true;
            latch.countDown();
        });

        latch.await();
        assertTrue(executed[0]);
    }

    @Test
    void submitCallableReturnsResult() throws Exception {
        pool = new ThreadPoolExecutorService(2);

        Future<Integer> future = pool.submit(() -> 42);

        assertEquals(42, future.get());
    }

    @Test
    void submitCallableWithException() {
        pool = new ThreadPoolExecutorService(2);

        Future<String> future = pool.submit(() -> {
            throw new RuntimeException("boom");
        });

        Exception ex = assertThrows(RuntimeException.class, future::get);
        assertEquals("boom", ex.getMessage());
    }

    // ── Concurrency ─────────────────────────────────────────────────────

    @Test
    void multipleTasksExecuted() throws InterruptedException {
        pool = new ThreadPoolExecutorService(4);
        int taskCount = 20;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await();
        assertEquals(taskCount, counter.get());
    }

    @Test
    void tasksRunOnDifferentThreads() throws InterruptedException {
        pool = new ThreadPoolExecutorService(4);
        List<String> threadNames = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                threadNames.add(Thread.currentThread().getName());
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                latch.countDown();
            });
        }

        latch.await();
        long distinct = threadNames.stream().distinct().count();
        assertTrue(distinct >= 2, "Expected multiple threads, got: " + threadNames);
    }

    // ── Shutdown ────────────────────────────────────────────────────────

    @Test
    void shutdownRejectsNewTasks() {
        pool = new ThreadPoolExecutorService(2);
        pool.shutdown();

        assertTrue(pool.isShutdown());
        assertThrows(IllegalStateException.class, () -> pool.submit(() -> {}));
    }

    @Test
    void shutdownCompletesQueuedTasks() throws Exception {
        pool = new ThreadPoolExecutorService(1);
        AtomicInteger counter = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit(() -> counter.incrementAndGet()));
        }

        pool.shutdown();

        for (Future<Integer> f : futures) {
            f.get();
        }
        assertEquals(5, counter.get());
    }

    @Test
    void awaitTermination() throws InterruptedException {
        pool = new ThreadPoolExecutorService(2);

        pool.submit(() -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        });

        Thread.sleep(100); // let task finish
        pool.shutdown();
        boolean terminated = pool.awaitTermination(5000);

        assertTrue(terminated);
        assertTrue(pool.isTerminated());
    }

    // ── Resilience ──────────────────────────────────────────────────────

    @Test
    void taskExceptionDoesNotKillWorker() throws Exception {
        pool = new ThreadPoolExecutorService(1);

        pool.submit(() -> { throw new RuntimeException("bad"); });
        Thread.sleep(50);

        Future<String> future = pool.submit(() -> "alive");
        assertEquals("alive", future.get());
    }

    @Test
    void singleThreadPoolRunsTasksInOrder() throws InterruptedException {
        pool = new ThreadPoolExecutorService(1);
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            int val = i;
            pool.submit(() -> {
                order.add(val);
                latch.countDown();
            });
        }

        latch.await();
        assertEquals(List.of(1, 2, 3), order);
    }

    // ── Builder & Rejection Policies ────────────────────────────────────

    @Test
    void builderCreatesPoolWithCustomPolicy() {
        pool = ThreadPoolExecutorService.builder(2)
                .rejectionPolicy(RejectionPolicies.discard())
                .build();

        pool.shutdown();
        assertDoesNotThrow(() -> pool.submit(() -> {}));
    }

    @Test
    void callerRunsPolicyExecutesOnCallerThread() {
        pool = ThreadPoolExecutorService.builder(2)
                .rejectionPolicy(RejectionPolicies.callerRuns())
                .build();

        pool.shutdown();
        boolean[] ran = {false};
        pool.submit(() -> ran[0] = true);
        assertTrue(ran[0], "Task should have run on the caller thread");
    }
}
