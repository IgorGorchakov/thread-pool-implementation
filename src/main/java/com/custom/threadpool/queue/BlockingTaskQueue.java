package com.custom.threadpool.queue;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple linked-list-backed implementation of {@link TaskQueue}.
 *
 * <p>Uses {@code synchronized} with {@code wait/notify} for thread coordination.
 * Simplified version of {@link java.util.concurrent.LinkedBlockingQueue}.</p>
 *
 * <ul>
 *   <li>{@link #put(Runnable)} — adds a task and wakes one waiting worker</li>
 *   <li>{@link #take()} — blocks until a task is available, then removes and returns it</li>
 * </ul>
 */
public class BlockingTaskQueue implements TaskQueue {

    private final Queue<Runnable> queue = new LinkedList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void put(Runnable task) {
        queue.add(task);
        notify();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Runnable take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
