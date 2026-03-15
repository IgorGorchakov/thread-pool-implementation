package com.custom.threadpool.core;

import com.custom.threadpool.queue.BlockingTaskQueue;
import com.custom.threadpool.queue.TaskQueue;
import com.custom.threadpool.rejection.RejectionPolicies;
import com.custom.threadpool.rejection.RejectionPolicy;

/**
 * Fluent builder for constructing a {@link ThreadPool} with custom configuration.
 *
 * <pre>{@code
 * ThreadPoolExecutorService pool = ThreadPoolExecutorService.builder(4)
 *     .rejectionPolicy(RejectionPolicies.callerRuns())
 *     .taskQueue(new BlockingTaskQueue())
 *     .build();
 * }</pre>
 */
public class ThreadPoolBuilder {
    private final int poolSize;
    private TaskQueue taskQueue = new BlockingTaskQueue();
    private RejectionPolicy rejectionPolicy = RejectionPolicies.abort();

    ThreadPoolBuilder(int poolSize) {
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
    public ThreadPoolBuilder taskQueue(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
        return this;
    }

    /**
     * Sets the rejection policy. Defaults to {@link RejectionPolicies#abort()}.
     *
     * @param rejectionPolicy the policy to use when tasks are rejected
     * @return this builder
     */
    public ThreadPoolBuilder rejectionPolicy(RejectionPolicy rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
        return this;
    }

    /**
     * Builds and starts the thread pool.
     *
     * @return a new, running {@link ThreadPool}
     */
    public ThreadPool build() {
        return new ThreadPool(poolSize, taskQueue, rejectionPolicy);
    }
}
