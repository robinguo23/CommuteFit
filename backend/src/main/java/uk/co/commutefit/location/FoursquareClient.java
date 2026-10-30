package uk.co.commutefit.location;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class FoursquareClient {
    private final RestClient client;
    private final String apiKey;

    public FoursquareClient(@Value("${commute-fit.geocoding.foursquare-url:https://places-api.foursquare.com}") String baseUrl,
                          @Value("${commute-fit.geocoding.foursquare-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query) {
        if(!configured())throw new IllegalStateException("Foursquare API key is not configured.");
        JsonNode body=client.get().uri(b->b.path("/places/search")
                .queryParam("query",query).queryParam("near","London, GB")
                .queryParam("fields","fsq_place_id,name,location,latitude,longitude,date_closed")
                .queryParam("limit",8).build())
                .header("Authorization","Bearer "+apiKey)
                .header("X-Places-Api-Version","2025-06-17")
                .retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("Foursquare returned an empty response.");
        return body;
    }
}
