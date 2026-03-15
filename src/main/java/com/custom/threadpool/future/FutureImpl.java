package com.custom.threadpool.future;

/**
 * Default implementation of {@link Future} that holds the result (or exception)
 * of an asynchronous computation.
 *
 * <p>Uses {@code wait/notifyAll} for thread coordination. Calling {@link #get()}
 * blocks the caller until a worker thread calls {@link #complete(Object)} or
 * {@link #completeExceptionally(Exception)}.</p>
 *
 * <p>Simplified version of {@link java.util.concurrent.FutureTask}.</p>
 *
 * @param <V> the type of the result
 */
public class FutureImpl<V> implements Future<V> {

    private V result;
    private Exception exception;
    private volatile boolean done = false;

    /**
     * Waits if necessary for the computation to complete, then retrieves the result.
     *
     * @return the computed result
     * @throws Exception if the computation completed exceptionally
     */
    @Override
    public synchronized V get() throws Exception {
        while (!done) {
            wait();
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * Completes this future with the given value and wakes up any threads
     * blocked on {@link #get()}.
     *
     * @param value the result value
     */
    public synchronized void complete(V value) {
        this.result = value;
        this.done = true;
        notifyAll();
    }

    /**
     * Completes this future exceptionally and wakes up any threads
     * blocked on {@link #get()}.
     *
     * @param ex the exception that caused the computation to fail
     */
    public synchronized void completeExceptionally(Exception ex) {
        this.exception = ex;
        this.done = true;
        notifyAll();
    }
}
