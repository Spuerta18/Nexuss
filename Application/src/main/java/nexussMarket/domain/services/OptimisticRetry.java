package nexussMarket.domain.services;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;

/**
 * Re-runs a read-check-change-save step that lost an optimistic-locking race.
 * Each attempt must read the entity again, so the business checks (enough
 * stock, a valid transition) are evaluated on fresh data. After a conflict the
 * step waits a short random pause, so racing callers spread out, and is tried
 * again up to {@link #MAX_RETRIES} times; the last conflict is then rethrown
 * (409).
 *
 * <p>Ten retries are enough for a reservation race on one item: an attempt
 * only fails when another save succeeded in between, so once every competing
 * save has landed the next read sees the final stock and either reserves or
 * fails with {@code InsufficientStockException}.</p>
 */
final class OptimisticRetry {

    static final int MAX_RETRIES = 10;

    private static final int MIN_PAUSE_MILLIS = 1;
    private static final int MAX_PAUSE_MILLIS = 20;

    private OptimisticRetry() {
    }

    /** Runs {@code attempt}, retrying it on {@link ConcurrentUpdateException}. */
    static <T> T run(Supplier<T> attempt) {
        for (int retry = 0; ; retry++) {
            try {
                return attempt.get();
            } catch (ConcurrentUpdateException e) {
                if (retry >= MAX_RETRIES) {
                    throw e;
                }
                pause(e);
            }
        }
    }

    private static void pause(ConcurrentUpdateException conflict) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(MIN_PAUSE_MILLIS, MAX_PAUSE_MILLIS + 1));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw conflict;
        }
    }
}
