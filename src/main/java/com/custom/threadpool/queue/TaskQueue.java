package com.custom.threadpool.queue;

/**
 * A thread-safe blocking queue for passing tasks from producer threads to worker threads.
 *
 * <p>Simplified version of {@link java.util.concurrent.BlockingQueue}.
 * Workers call {@link #take()} which blocks when the queue is empty.
 * Producers call {@link #put(Runnable)} to enqueue tasks.</p>
 *
 * <p>Implementations must be fully thread-safe.</p>
 *
 * @see BlockingTaskQueue
 */
public interface TaskQueue {

    /**
     * Adds a task to the queue. May wake up one waiting consumer.
     *
     * @param task the task to enqueue
     */
    void put(Runnable task);

    /**
     * Removes and returns the next task. Blocks if the queue is empty
     * until a task becomes available.
     *
     * @return the next task
     * @throws InterruptedException if interrupted while waiting
     */
    Runnable take() throws InterruptedException;

    /**
     * Returns {@code true} if the queue contains no tasks.
     *
     * @return {@code true} if empty
     */
    boolean isEmpty();
}
