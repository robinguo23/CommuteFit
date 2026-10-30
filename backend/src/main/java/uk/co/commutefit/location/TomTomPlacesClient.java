package uk.co.commutefit.location;

import java.time.Duration;
import java.util.Map;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class TomTomPlacesClient {
    private final RestClient client;
    private final String apiKey;

    public TomTomPlacesClient(@Value("${commute-fit.geocoding.tomtom-url:https://api.tomtom.com}") String baseUrl,
                              @Value("${commute-fit.geocoding.tomtom-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query) {
        if(!configured())throw new IllegalStateException("TomTom API key is not configured.");
        Map<String,Object> body=Map.of(
                "query",query,
                "maxResults",8,
                "filters",Map.of("types",new String[]{"poi","address","street","area"},"countryCodesIso2",new String[]{"GB"}));
        JsonNode response=client.post().uri("/maps/orbis/places/discover")
                .header("TomTom-Api-Key",apiKey)
                .header("TomTom-Api-Version","3")
                .header("Attributes","results(id,type,title,subtitles,position,address(postalCode,extendedPostalCode,street,houseNumber,municipality))")
                .body(body).retrieve().body(JsonNode.class);
        if(response==null)throw new IllegalStateException("TomTom returned an empty response.");
        return response;
    }

    public JsonNode fuzzySearch(String query) {
        if(!configured())throw new IllegalStateException("TomTom API key is not configured.");
        String encoded=UriUtils.encodePathSegment(query, StandardCharsets.UTF_8);
        JsonNode response=client.get().uri(uriBuilder->uriBuilder
                .path("/search/2/search/"+encoded+".json")
                .queryParam("key",apiKey)
                .queryParam("countrySet","GB")
                .queryParam("limit",8)
                .queryParam("idxSet","POI,PAD,Addr,Str,EPP")
                .queryParam("extendedPostalCodesFor","POI,PAD,Addr,EPP")
                .build()).retrieve().body(JsonNode.class);
        if(response==null)throw new IllegalStateException("TomTom returned an empty response.");
        return response;
    }
}
