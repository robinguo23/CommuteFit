package uk.co.commutefit.journey.tfl;

import java.time.Instant;
import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import uk.co.commutefit.common.CandidateTokenExpiredException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CandidateTokenStore {
    private static final long TTL_SECONDS = 600;
    private static final int MAX_CANDIDATES = 2_000;
    public record Candidate(String query, String providerId, String name, String locality, Instant expiresAt) {}
    private final Map<String, Candidate> candidates = new LinkedHashMap<>();
    private final Clock clock;

    public CandidateTokenStore() { this(Clock.systemUTC()); }

    CandidateTokenStore(Clock clock) { this.clock = clock; }

    public synchronized String add(String query, String providerId, String name, String locality) {
        purgeExpired(clock.instant());
        while (candidates.size() >= MAX_CANDIDATES) {
            Iterator<String> oldest = candidates.keySet().iterator();
            if (!oldest.hasNext()) break;
            oldest.next();
            oldest.remove();
        }
        String token = UUID.randomUUID().toString();
        candidates.put(token, new Candidate(normalize(query), providerId, name, locality, clock.instant().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public synchronized Candidate resolve(String token, String query) {
        Instant now = clock.instant();
        purgeExpired(now);
        Candidate candidate = candidates.get(token);
        if (candidate == null || !candidate.expiresAt().isAfter(now)) {
            throw new CandidateTokenExpiredException();
        }
        if (!candidate.query().equals(normalize(query))) {
            throw new IllegalArgumentException("That workplace choice has expired or no longer matches the entered location.");
        }
        return candidate;
    }

    public synchronized void remove(String token) {
        if (token != null && !token.isBlank()) candidates.remove(token);
    }

    @Scheduled(fixedDelay = 60_000)
    public synchronized void removeExpiredCandidates() {
        purgeExpired(clock.instant());
    }

    private void purgeExpired(Instant now) {
        candidates.values().removeIf(candidate -> !candidate.expiresAt().isAfter(now));
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
}
