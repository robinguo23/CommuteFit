package uk.co.commutefit.journey.tfl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import uk.co.commutefit.common.ProviderUnavailableException;
import uk.co.commutefit.common.RateLimitedException;

@Service
public class LocationSuggestionService {
    private static final int MAX_RESULTS = 8;
    private static final int STOP_POINT_RESULTS = 5;
    private final TflClient client;
    private final CandidateTokenStore tokens;

    public LocationSuggestionService(TflClient client, CandidateTokenStore tokens) {
        this.client = client;
        this.tokens = tokens;
    }

    public record Suggestion(String id, String name, String locality) {}
    private record Match(String routeValue, String name, String locality) {}

    public List<Suggestion> search(String query) {
        String input = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        if (input.length() < 3) return List.of();

        List<Match> matches = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        RuntimeException providerFailure = null;

        try {
            JsonNode stops = client.stopPointSearch(input);
            JsonNode stopMatches = stops.path("matches");
            if (stopMatches.isArray()) {
                for (JsonNode stop : stopMatches) {
                    String id = stop.path("id").asText("");
                    // TfL search may return an interchange HUB (for example HUBKGX).
                    // Journey Planner accepts the associated ICS stop ID, not that HUB key.
                    String routeId = id.startsWith("HUB") ? stop.path("icsId").asText("") : id;
                    String name = stop.path("name").asText("").trim();
                    if (routeId.isBlank() || name.isBlank()) continue;
                    String key = matchKey(name, stop.path("lat").asText(""), stop.path("lon").asText(""));
                    if (seen.add(key)) matches.add(new Match(routeId, name, "StopPoint"));
                    if (matches.size() >= STOP_POINT_RESULTS) break;
                }
            }
        } catch (RateLimitedException | ProviderUnavailableException ex) {
            providerFailure = ex;
        }

        if (matches.size() < STOP_POINT_RESULTS) {
            try {
                JsonNode places = client.placeSearch(input);
                if (places.isArray()) {
                    for (JsonNode place : places) {
                        String name = place.path("commonName").asText("").trim();
                        String lat = place.path("lat").asText("");
                        String lon = place.path("lon").asText("");
                        if (name.isBlank() || lat.isBlank() || lon.isBlank()) continue;
                        String key = matchKey(name, lat, lon);
                        if (seen.add(key)) {
                            String type = place.path("placeType").asText("Place");
                            matches.add(new Match(lat + "," + lon, name, type));
                        }
                        if (matches.size() >= MAX_RESULTS) break;
                    }
                }
            } catch (RateLimitedException | ProviderUnavailableException ex) {
                if (providerFailure == null || ex instanceof RateLimitedException) providerFailure = ex;
            }
        }

        if (matches.isEmpty() && providerFailure != null) throw providerFailure;

        List<Suggestion> result = new ArrayList<>();
        for (Match match : matches) {
            // Bind the short-lived selection token to the exact display text placed in the input.
            String token = tokens.add(match.name(), match.routeValue(), match.name(), match.locality());
            result.add(new Suggestion(token, match.name(), match.locality()));
        }
        return List.copyOf(result);
    }

    private static String matchKey(String name, String lat, String lon) {
        return name.toLowerCase(Locale.ROOT) + "|" + lat + "|" + lon;
    }
}
