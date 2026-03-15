package com.custom.threadpool.core;

import com.custom.threadpool.queue.TaskQueue;
import com.custom.threadpool.shotdown.PoisonPill;

/**
 * A daemon thread that continuously takes tasks from a {@link TaskQueue} and executes them.
 *
 * <p>Mirrors {@code ThreadPoolExecutor.Worker} in the real JDK. Each worker runs an
 * infinite loop: {@code take → run → repeat}, and exits when it receives a
 * {@link PoisonPill} or is interrupted.</p>
 *
 * <p>Decoupled from the thread pool — communicates through:</p>
 * <ul>
 *   <li>{@link TaskQueue} — to receive tasks</li>
 *   <li>{@link Callback} — to notify the pool on exit (Observer pattern)</li>
 * </ul>
 *
 * @see ThreadPoolExecutorService
 * @see PoisonPill
 */
public class Worker extends Thread {

    /**
     * Lifecycle callback allowing the thread pool to react when a worker thread exits.
     * Follows the <b>Observer pattern</b>.
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * Invoked when the given worker thread has exited its run loop.
         *
         * @param worker the worker that exited
         */
        void onWorkerExit(Worker worker);
    }

    private final TaskQueue taskQueue;
    private final Callback callback;

    /**
     * Creates a new worker thread.
     *
     * @param name      the thread name (e.g. {@code "pool-thread-0"})
     * @param taskQueue the queue to take tasks from
     * @param callback  called when this worker exits
     */
    public Worker(String name, TaskQueue taskQueue, Callback callback) {
        super(name);
        this.taskQueue = taskQueue;
        this.callback = callback;
        setDaemon(true);
    }

    /**
     * Runs the worker loop: takes a task from the queue, executes it, and repeats.
     * Exits when a {@link PoisonPill} is received or the thread is interrupted.
     * Exceptions thrown by individual tasks are caught so that a single bad task
     * does not kill the worker.
     */
    @Override
    public void run() {
        try {
            while (true) {
                Runnable task = taskQueue.take();

                if (task instanceof PoisonPill) {
                    break;
                }

                try {
                    task.run();
                } catch (Throwable t) {
                    // Don't let a bad task kill the worker
                }
            }
        } catch (InterruptedException e) {
            // Worker interrupted — exit gracefully
        } finally {
            callback.onWorkerExit(this);
        }
    }
}
