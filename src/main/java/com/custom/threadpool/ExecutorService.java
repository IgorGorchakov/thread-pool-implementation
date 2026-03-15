package com.custom.threadpool;

import com.custom.threadpool.core.ThreadPoolExecutorService;
import com.custom.threadpool.future.Future;

/**
 * An executor service that manages a pool of worker threads for asynchronous task execution.
 *
 * <p>Simplified version of {@link java.util.concurrent.ExecutorService}.
 * Supports submitting tasks (both {@link Runnable} and {@link Callable}),
 * orderly shutdown, and waiting for termination.</p>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   RUNNING  →  SHUTDOWN  →  TERMINATED
 * </pre>
 *
 * @see ThreadPoolExecutorService
 */
public interface ExecutorService {

    /**
     * Submits a fire-and-forget task for execution.
     *
     * @param task the task to execute
     * @throws IllegalStateException if the executor has been shut down (default behaviour)
     */
    void submit(Runnable task);

    /**
     * Submits a value-returning task for execution.
     *
     * @param task the task to execute
     * @param <V>  the type of the task's result
     * @return a {@link Future} representing the pending result of the task
     * @throws IllegalStateException if the executor has been shut down (default behaviour)
     */
    <V> Future<V> submit(Callable<V> task);

    /**
     * Initiates an orderly shutdown. Previously submitted tasks are executed,
     * but no new tasks will be accepted.
     */
    void shutdown();

    /**
     * Blocks until all workers have exited after a {@link #shutdown()} request,
     * or the timeout elapses — whichever happens first.
     *
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @return {@code true} if the executor terminated, {@code false} if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeoutMillis) throws InterruptedException;

    /**
     * Returns {@code true} if {@link #shutdown()} has been called.
     *
     * @return {@code true} if shut down
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if all workers have exited after shutdown.
     *
     * @return {@code true} if terminated
     */
    boolean isTerminated();
}
