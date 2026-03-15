package com.custom.threadpool.rejection;

import com.custom.threadpool.core.ThreadPoolBuilder;

/**
 * Strategy for handling tasks that are submitted to a thread pool after it has been shut down.
 *
 * <p>This follows the <b>Strategy design pattern</b> — different rejection behaviours
 * can be plugged into the pool without modifying its implementation.</p>
 *
 * <p>Being a {@link FunctionalInterface}, it can be used with lambda expressions:</p>
 * <pre>{@code
 * RejectionPolicy logging = task -> System.err.println("Rejected: " + task);
 * }</pre>
 *
 * @see RejectionPolicies
 * @see ThreadPoolBuilder#rejectionPolicy(RejectionPolicy)
 */
@FunctionalInterface
public interface RejectionPolicy {

    /**
     * Called when a task is rejected by the thread pool.
     *
     * @param task the rejected task that was not accepted for execution
     */
    void reject(Runnable task);
}
