package com.custom.threadpool.rejection;

/**
 * Factory methods for built-in {@link RejectionPolicy} implementations.
 *
 * <p>Provides three common strategies for handling rejected tasks:</p>
 * <ul>
 *   <li>{@link #abort()}      — throws an exception (default)</li>
 *   <li>{@link #discard()}    — silently drops the task</li>
 *   <li>{@link #callerRuns()} — runs the task on the submitting thread</li>
 * </ul>
 *
 * @see RejectionPolicy
 */
public final class RejectionPolicies {

    private RejectionPolicies() {}

    /**
     * Returns a policy that throws {@link IllegalStateException}.
     * This is the default rejection policy.
     *
     * @return the abort policy
     */
    public static RejectionPolicy abort() {
        return task -> {
            throw new IllegalStateException("ThreadPool is shut down — task rejected");
        };
    }

    /**
     * Returns a policy that silently discards the rejected task.
     *
     * @return the discard policy
     */
    public static RejectionPolicy discard() {
        return task -> { /* do nothing */ };
    }

    /**
     * Returns a policy that runs the rejected task directly on the caller's thread.
     * This provides a simple feedback mechanism that will slow down the submitting thread.
     *
     * @return the caller-runs policy
     */
    public static RejectionPolicy callerRuns() {
        return Runnable::run;
    }
}
