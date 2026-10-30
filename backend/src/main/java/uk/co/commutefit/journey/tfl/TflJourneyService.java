package uk.co.commutefit.journey.tfl;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.commutefit.common.LocationDisambiguationException;

@Service
public class TflJourneyService {
    private final TflClient client;
    private final CandidateTokenStore tokens;
    public TflJourneyService(TflClient client, CandidateTokenStore tokens) { this.client = client; this.tokens = tokens; }

    public String resolveDestination(String destination, String selectedToken) {
        return resolve(destination, selectedToken);
    }

    public String resolveOrigin(String origin, String selectedToken) {
        return resolve(origin, selectedToken);
    }

    public void removeCandidateTokens(String destinationToken, String originToken) {
        tokens.remove(destinationToken);
        tokens.remove(originToken);
    }

    public JsonNode journeys(String from, String to, String fromQuery, String toQuery,
                             String fromCandidateField, String toCandidateField,
                             LocalDate date, LocalTime time, String timeIs) {
        JsonNode response = client.journey(from, to, date, time, timeIs);
        if (response == null) throw new uk.co.commutefit.common.ProviderUnavailableException(null);
        JsonNode fromOptions = response.path("fromLocationDisambiguation").path("disambiguationOptions");
        if (fromOptions.isArray() && !fromOptions.isEmpty()) {
            throw disambiguation(fromOptions, fromQuery, fromCandidateField);
        }
        JsonNode toOptions = response.path("toLocationDisambiguation").path("disambiguationOptions");
        if (toOptions.isArray() && !toOptions.isEmpty()) {
            throw disambiguation(toOptions, toQuery, toCandidateField);
        }
        return response;
    }

    private LocationDisambiguationException disambiguation(JsonNode options, String query, String candidateField) {
        List<Map<String, String>> choices = new ArrayList<>();
        for (JsonNode option : options) {
            JsonNode place = option.path("place");
            String providerId = option.path("parameterValue").asText("");
            if(providerId.isBlank())providerId=place.path("id").asText(place.path("naptanId").asText(""));
            if (providerId.isBlank()) continue;
            String name = place.path("commonName").asText(place.path("name").asText("Location"));
            String locality = place.path("placeType").asText("");
            Map<String, String> candidate = new LinkedHashMap<>();
            candidate.put("id", tokens.add(name, providerId, name, locality));
            candidate.put("name", name);
            candidate.put("locality", locality);
            choices.add(candidate);
        }
        if (choices.isEmpty()) return new LocationDisambiguationException(candidateField, List.of());
        return new LocationDisambiguationException(candidateField, choices);
    }

    private String resolve(String query, String selectedToken) {
        if (selectedToken == null || selectedToken.isBlank()) return query;
        return tokens.resolve(selectedToken, query).providerId();
    }
}
