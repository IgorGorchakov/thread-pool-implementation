package com.custom.threadpool.core;

import com.custom.threadpool.Callable;
import com.custom.threadpool.ExecutorService;
import com.custom.threadpool.future.Future;
import com.custom.threadpool.future.FutureImpl;
import com.custom.threadpool.queue.BlockingTaskQueue;
import com.custom.threadpool.queue.TaskQueue;
import com.custom.threadpool.rejection.RejectionPolicies;
import com.custom.threadpool.rejection.RejectionPolicy;
import com.custom.threadpool.shotdown.PoisonPill;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed-size thread pool that executes submitted tasks using a set of worker threads.
 *
 * <p>Simplified version of {@link java.util.concurrent.ThreadPoolExecutor}.
 * Implements {@link ExecutorService} and supports pluggable task queues
 * and rejection policies.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>On creation, {@code N} {@link Worker} threads are started.</li>
 *   <li>Each worker loops: take a task from the {@link TaskQueue} → run it → repeat.</li>
 *   <li>Submitting a task puts it on the queue — a waiting worker picks it up.</li>
 *   <li>On {@link #shutdown()}, a {@link PoisonPill} is enqueued per worker.
 *       Workers drain remaining tasks, then exit.</li>
 * </ol>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   RUNNING  →  SHUTDOWN  →  TERMINATED
 * </pre>
 *
 * <h3>Construction</h3>
 * <p>Use the simple constructor for defaults, or the {@link Builder} for full control:</p>
 * <pre>{@code
 * // Simple
 * ThreadPoolExecutorService pool = new ThreadPoolExecutorService(4);
 *
 * // Builder
 * ThreadPoolExecutorService pool = ThreadPoolExecutorService.builder(4)
 *     .taskQueue(new BlockingTaskQueue())
 *     .rejectionPolicy(RejectionPolicies.callerRuns())
 *     .build();
 * }</pre>
 *
 * <h3>Design patterns</h3>
 * <ul>
 *   <li><b>Strategy</b>    — {@link RejectionPolicy} and {@link TaskQueue} are pluggable</li>
 *   <li><b>Builder</b>     — {@link Builder} for flexible, readable construction</li>
 *   <li><b>Observer</b>    — {@link Worker.Callback} decouples worker lifecycle from the pool</li>
 *   <li><b>Poison Pill</b> — {@link PoisonPill} for graceful shutdown signalling</li>
 * </ul>
 *
 * @see ExecutorService
 * @see Worker
 * @see TaskQueue
 * @see RejectionPolicy
 */
public class ThreadPoolExecutorService implements ExecutorService {

    private final TaskQueue taskQueue;
    private final RejectionPolicy rejectionPolicy;
    private final Worker[] workers;
    private final AtomicInteger activeWorkers;
    private volatile boolean shutdown = false;
    private volatile boolean terminated = false;
    private final Object terminationLock = new Object();

    // ── Builder ─────────────────────────────────────────────────────────

    /**
     * Fluent builder for constructing a {@link ThreadPoolExecutorService} with custom configuration.
     *
     * <pre>{@code
     * CustomThreadPool pool = CustomThreadPool.builder(4)
     *     .rejectionPolicy(RejectionPolicies.callerRuns())
     *     .taskQueue(new BlockingTaskQueue())
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final int poolSize;
        private TaskQueue taskQueue = new BlockingTaskQueue();
        private RejectionPolicy rejectionPolicy = RejectionPolicies.abort();

        private Builder(int poolSize) {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("Pool size must be positive");
            }
            this.poolSize = poolSize;
        }

        /**
         * Sets the task queue implementation. Defaults to {@link BlockingTaskQueue}.
         *
         * @param taskQueue the queue to use
         * @return this builder
         */
        public Builder taskQueue(TaskQueue taskQueue) {
            this.taskQueue = taskQueue;
            return this;
        }

        /**
         * Sets the rejection policy. Defaults to {@link RejectionPolicies#abort()}.
         *
         * @param rejectionPolicy the policy to use when tasks are rejected
         * @return this builder
         */
        public Builder rejectionPolicy(RejectionPolicy rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
            return this;
        }

        /**
         * Builds and starts the thread pool.
         *
         * @return a new, running {@link ThreadPoolExecutorService}
         */
        public ThreadPoolExecutorService build() {
            return new ThreadPoolExecutorService(poolSize, taskQueue, rejectionPolicy);
        }
    }

    /**
     * Returns a new {@link Builder} for constructing a thread pool with the given size.
     *
     * @param poolSize the number of worker threads
     * @return a new builder instance
     */
    public static Builder builder(int poolSize) {
        return new Builder(poolSize);
    }

    // ── Constructors ────────────────────────────────────────────────────

    /**
     * Creates and starts a thread pool with the given number of workers,
     * using a {@link BlockingTaskQueue} and an {@linkplain RejectionPolicies#abort() abort}
     * rejection policy.
     *
     * @param poolSize the number of worker threads (must be positive)
     */
    public ThreadPoolExecutorService(int poolSize) {
        this(poolSize, new BlockingTaskQueue(), RejectionPolicies.abort());
    }

    private ThreadPoolExecutorService(int poolSize, TaskQueue taskQueue, RejectionPolicy rejectionPolicy) {
        this.taskQueue = taskQueue;
        this.rejectionPolicy = rejectionPolicy;
        this.workers = new Worker[poolSize];
        this.activeWorkers = new AtomicInteger(poolSize);

        for (int i = 0; i < poolSize; i++) {
            workers[i] = new Worker("pool-thread-" + i, taskQueue, this::onWorkerExit);
            workers[i].start();
        }
    }

    // ── Submit tasks ────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>If the pool is shut down, the configured {@link RejectionPolicy} handles the task.</p>
     */
    @Override
    public void submit(Runnable task) {
        if (shutdown) {
            rejectionPolicy.reject(task);
            return;
        }
        taskQueue.put(task);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wraps the {@link Callable} in a {@link Runnable} that completes a
     * {@link FutureImpl} on success or failure.</p>
     */
    @Override
    public <V> Future<V> submit(Callable<V> task) {
        FutureImpl<V> future = new FutureImpl<>();
        submit((Runnable) () -> {
            try {
                V result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // ── Shutdown lifecycle ──────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Enqueues one {@link PoisonPill} per worker thread. Each worker will
     * process remaining tasks ahead of the pill, then exit.</p>
     */
    @Override
    public void shutdown() {
        shutdown = true;
        for (int i = 0; i < workers.length; i++) {
            taskQueue.put(PoisonPill.INSTANCE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        synchronized (terminationLock) {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (!terminated) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) return false;
                terminationLock.wait(remaining);
            }
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTerminated() {
        return terminated;
    }

    // ── Worker lifecycle callback ───────────────────────────────────────

    /**
     * Called by each {@link Worker} when it exits. When the last worker exits,
     * the pool transitions to the {@code TERMINATED} state and notifies
     * any threads waiting in {@link #awaitTermination(long)}.
     *
     * @param exitedWorker the worker that just exited
     */
    private void onWorkerExit(Worker exitedWorker) {
        if (activeWorkers.decrementAndGet() == 0) {
            terminated = true;
            synchronized (terminationLock) {
                terminationLock.notifyAll();
            }
        }
    }
}
