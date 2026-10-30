package uk.co.commutefit.journey.tfl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import uk.co.commutefit.common.CandidateTokenExpiredException;

class CandidateTokenStoreTest {
    @Test
    void normalizesQueryAndExpiresTokensAfterTenMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-27T10:00:00Z"));
        CandidateTokenStore store = new CandidateTokenStore(clock);
        String token = store.add(" Oxford Circus ", "provider-id", "Oxford Circus", "locality");

        assertEquals("provider-id", store.resolve(token, "oxford circus").providerId());
        clock.advance(Duration.ofMinutes(10));
        assertThrows(CandidateTokenExpiredException.class, () -> store.resolve(token, "Oxford Circus"));
    }

    @Test
    void rejectsTokenWhenSubmittedQueryNoLongerMatches() {
        CandidateTokenStore store = new CandidateTokenStore(Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC));
        String token = store.add("Oxford Circus", "provider-id", "Oxford Circus", "locality");

        assertThrows(IllegalArgumentException.class, () -> store.resolve(token, "Baker Street"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        private MutableClock(Instant initial) { now = new AtomicReference<>(initial); }
        private void advance(Duration amount) { now.updateAndGet(value -> value.plus(amount)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
}
