package uk.co.commutefit.location;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class PhotonClient {
    private record Cached(Instant expires,JsonNode body) {}
    private final RestClient client;
    private final Map<String,Cached> cache=new LinkedHashMap<>();
    private Instant nextRequest=Instant.EPOCH;
    @Autowired
    public PhotonClient(Environment env) {
        String baseUrl=env.getProperty("commute-fit.geocoding.photon-url","https://photon.komoot.io");
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(6));
        client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).defaultHeader("User-Agent","CommuteFit/1.0 (user-initiated workplace search)").build();
    }
    PhotonClient(RestClient client){this.client=client;}
    public synchronized JsonNode search(String query) {
        String key=query.trim().toLowerCase(Locale.ROOT);
        Instant now=Instant.now();
        cache.entrySet().removeIf(e->!e.getValue().expires().isAfter(now));
        if(cache.containsKey(key))return cache.get(key).body();
        if(now.isBefore(nextRequest))throw new IllegalStateException("Please wait before another location search.");
        nextRequest=now.plusSeconds(1);
        JsonNode body=client.get().uri(b->b.path("/api/").queryParam("q",query).queryParam("limit",8)
            .queryParam("lang","en").queryParam("lat",51.5074).queryParam("lon",-0.1278).build()).retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("Empty geocoder response");
        if(cache.size()>=100)cache.remove(cache.keySet().iterator().next());
        cache.put(key,new Cached(now.plusSeconds(300),body));
        return body;
    }
}
