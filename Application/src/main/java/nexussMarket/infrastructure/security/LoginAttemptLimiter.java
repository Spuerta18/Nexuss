package nexussMarket.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Counts failed logins per key (the client address and the attempted email)
 * in a sliding window. When a key reaches the limit it is blocked for a fixed
 * time and its count starts over. A successful login clears the email's
 * count. Thread-safe; state lives in memory, per application instance.
 *
 * <p>Unknown and existing emails are counted the same way, so a block never
 * reveals whether an account exists.</p>
 */
public class LoginAttemptLimiter {

    /** Above this many tracked keys, keys with nothing left to remember are dropped. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Duration blockDuration;
    private final Clock clock;
    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(int maxFailures, Duration window, Duration blockDuration, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be at least 1");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    /** How much longer the most blocked of {@code keys} stays blocked; empty when none is blocked. */
    public Optional<Duration> blockedFor(Collection<String> keys) {
        Instant now = clock.instant();
        Duration longest = Duration.ZERO;
        for (String key : keys) {
            Attempts entry = attempts.get(key);
            if (entry != null) {
                Duration remaining = entry.remainingBlock(now);
                if (remaining.compareTo(longest) > 0) {
                    longest = remaining;
                }
            }
        }
        return longest.isZero() ? Optional.empty() : Optional.of(longest);
    }

    /** Records one failed login for each of {@code keys}. */
    public void recordFailure(Collection<String> keys) {
        Instant now = clock.instant();
        for (String key : keys) {
            attempts.computeIfAbsent(key, ignored -> new Attempts()).fail(now, maxFailures, window, blockDuration);
        }
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.entrySet().removeIf(entry -> entry.getValue().isForgettable(now, window));
        }
    }

    /** Forgets the failures of {@code key} (a successful login for that email). */
    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /** Forgets everything; for tests. */
    public void reset() {
        attempts.clear();
    }

    private static final class Attempts {

        private final Deque<Instant> failures = new ArrayDeque<>();
        private Instant blockedUntil = Instant.MIN;

        synchronized void fail(Instant now, int maxFailures, Duration window, Duration blockDuration) {
            prune(now, window);
            failures.addLast(now);
            if (failures.size() >= maxFailures) {
                blockedUntil = now.plus(blockDuration);
                failures.clear();
            }
        }

        synchronized Duration remainingBlock(Instant now) {
            return now.isBefore(blockedUntil) ? Duration.between(now, blockedUntil) : Duration.ZERO;
        }

        synchronized boolean isForgettable(Instant now, Duration window) {
            prune(now, window);
            return failures.isEmpty() && !now.isBefore(blockedUntil);
        }

        private void prune(Instant now, Duration window) {
            Instant oldest = now.minus(window);
            while (!failures.isEmpty() && !failures.peekFirst().isAfter(oldest)) {
                failures.removeFirst();
            }
        }
    }
}
