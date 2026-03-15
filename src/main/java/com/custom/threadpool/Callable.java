package com.custom.threadpool;

/**
 * A task that returns a result and may throw a checked exception.
 *
 * <p>Simplified version of {@link java.util.concurrent.Callable}.
 * Being a {@link FunctionalInterface}, it can be used with lambda expressions:</p>
 *
 * <pre>{@code
 * Callable<Integer> task = () -> 2 + 2;
 * }</pre>
 *
 * @param <V> the type of the computed result
 * @see ExecutorService#submit(Callable)
 */
@FunctionalInterface
public interface Callable<V> {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}
