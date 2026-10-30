package uk.co.commutefit.location;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class MapTilerClient {
    private final RestClient client;
    private final String apiKey;

    public MapTilerClient(@Value("${commute-fit.geocoding.maptiler-url:https://api.maptiler.com}") String baseUrl,
                          @Value("${commute-fit.geocoding.maptiler-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query) {
        if(!configured())throw new IllegalStateException("MapTiler API key is not configured.");
        JsonNode body=client.get().uri(b->b.path("/geocoding/{query}.json")
                .queryParam("country","gb").queryParam("language","en")
                .queryParam("autocomplete","false").queryParam("limit",8)
                .queryParam("key",apiKey).build(query))
                .retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("MapTiler returned an empty response.");
        return body;
    }
}
