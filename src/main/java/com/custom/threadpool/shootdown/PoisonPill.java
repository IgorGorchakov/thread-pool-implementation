package com.custom.threadpool.shootdown;

import com.custom.threadpool.core.ThreadPool;

/**
 * A sentinel task that signals a {@link com.custom.threadpool.core.Worker} thread to exit.
 *
 * <p>This implements the <b>Poison Pill</b> pattern for graceful shutdown.
 * The task is never actually executed — workers check for it with
 * {@code instanceof} before calling {@code run()}, and break out of their loop.</p>
 *
 * <p>One poison pill is enqueued per worker during
 * {@link ThreadPool#shutdown()}.</p>
 *
 * @see com.custom.threadpool.core.Worker
 * @see ThreadPool#shutdown()
 */
public final class PoisonPill implements Runnable {

    /** Singleton instance — only one pill object is needed. */
    public static final PoisonPill INSTANCE = new PoisonPill();

    private PoisonPill() {}

    /**
     * Always throws {@link UnsupportedOperationException}.
     * A poison pill must never be executed — it is only a marker.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void run() {
        throw new UnsupportedOperationException("PoisonPill must not be run");
    }
}
