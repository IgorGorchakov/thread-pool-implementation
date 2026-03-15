package com.custom.threadpool.future;

import com.custom.threadpool.ExecutorService;

/**
 * Represents the result of an asynchronous computation.
 *
 * <p>Simplified version of {@link java.util.concurrent.Future}.
 * A {@code Future} is returned when a task is submitted to a
 * {@link ExecutorService}. The caller
 * can then block on {@link #get()} until the result becomes available.</p>
 *
 * <pre>{@code
 * Future<Integer> future = pool.submit(() -> 42);
 * int result = future.get(); // blocks until ready
 * }</pre>
 *
 * @param <V> the type of the result
 * @see FutureImpl
 */
public interface Future<V> {

    /**
     * Waits if necessary for the computation to complete, then retrieves the result.
     *
     * @return the computed result
     * @throws Exception if the computation threw an exception
     */
    V get() throws Exception;

    /**
     * Returns {@code true} if the computation has completed (either normally or exceptionally).
     *
     * @return {@code true} if complete
     */
    boolean isDone();
}
