package uk.co.commutefit.journey.tfl;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.client.RestClientResponseException;
import uk.co.commutefit.common.ProviderUnavailableException;
import uk.co.commutefit.common.RateLimitedException;

@Component
public class TflClient {
    private final RestClient client;
    private final String appKey;
    public TflClient(RestClient tflRestClient, @Value("${commute-fit.tfl.app-key:}") String appKey) {
        this.client = tflRestClient;
        this.appKey = appKey;
    }

    public JsonNode journey(String from, String to, LocalDate date, LocalTime time, String timeIs) {
        try {
            return client.get().uri(uri -> {
                var builder = uri.path("/Journey").pathSegment("JourneyResults", from, "to", to)
                    .queryParam("date", date.format(DateTimeFormatter.BASIC_ISO_DATE))
                    .queryParam("time", time.format(DateTimeFormatter.ofPattern("HHmm")))
                    .queryParam("timeIs", timeIs)
                    .queryParam("calcOneDirection", true)
                    .queryParam("mode", "bus,overground,national-rail,elizabeth-line,tube,dlr,tram,river-bus,cable-car,walking");
                if (!appKey.isBlank()) builder.queryParam("app_key", appKey);
                return builder.build();
            }).retrieve().onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                if (response.getStatusCode().value() == 429) throw new RateLimitedException();
                throw new ProviderUnavailableException(null);
            }).body(JsonNode.class);
        } catch (RateLimitedException | ProviderUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) throw new RateLimitedException();
            throw new ProviderUnavailableException(ex);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ProviderUnavailableException(ex);
        }
    }

    public JsonNode stopPointSearch(String query) {
        return locationSearch(uri -> {
            UriBuilder builder = uri.pathSegment("StopPoint", "Search", query).queryParam("maxResults", 8);
            return withKey(builder).build();
        });
    }

    public JsonNode placeSearch(String query) {
        return locationSearch(uri -> withKey(uri.pathSegment("Place", "Search").queryParam("name", query)).build());
    }

    private UriBuilder withKey(UriBuilder builder) {
        return appKey.isBlank() ? builder : builder.queryParam("app_key", appKey);
    }

    private JsonNode locationSearch(Function<UriBuilder, URI> uriFunction) {
        try {
            return client.get().uri(uriFunction).retrieve().onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                if (response.getStatusCode().value() == 429) throw new RateLimitedException();
                throw new ProviderUnavailableException(null);
            }).body(JsonNode.class);
        } catch (RateLimitedException | ProviderUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) throw new RateLimitedException();
            throw new ProviderUnavailableException(ex);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ProviderUnavailableException(ex);
        }
    }
}
