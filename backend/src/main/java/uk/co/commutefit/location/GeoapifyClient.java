package uk.co.commutefit.location;

import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class GeoapifyClient {
    private final RestClient client;
    private final String apiKey;

    public GeoapifyClient(@Value("${commute-fit.geocoding.geoapify-url:https://api.geoapify.com}") String baseUrl,
                          @Value("${commute-fit.geocoding.geoapify-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query) {
        if(!configured()) throw new IllegalStateException("Geoapify API key is not configured. Set GEOAPIFY_API_KEY in the backend environment and restart it.");
        // Generic connector stopwords only (no brand-specific rules): Geoapify's upstream recall
        // drops the correct POI when a literal "and" is present, e.g. "marks and spencer woolwich".
        String text=stripConnectorWords(query);
        JsonNode body=client.get().uri(b->b.path("/v1/geocode/search")
                .queryParam("text",text).queryParam("filter","countrycode:gb")
                .queryParam("limit",8).queryParam("format","json").queryParam("apiKey",apiKey).build())
                .retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("Geoapify returned an empty response.");
        return body;
    }

    private static String stripConnectorWords(String query){
        if(query==null)return "";
        String cleaned=query.replaceAll("(?i)\\b(?:and|or|of)\\b"," ").replaceAll("\\s+"," ").trim();
        return cleaned.isBlank()?query.trim():cleaned;
    }
}
