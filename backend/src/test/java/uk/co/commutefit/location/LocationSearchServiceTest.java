package uk.co.commutefit.location;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.*;
import uk.co.commutefit.journey.tfl.*;
import uk.co.commutefit.journey.lookup.*;

class LocationSearchServiceTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final CandidateTokenStore tokens=new CandidateTokenStore();
    private final GeoapifyClient geoapify=new GeoapifyClient("https://api.geoapify.com","");
    private final MapTilerClient maptiler=new MapTilerClient("https://api.maptiler.com","");
    private final LocationIqClient locationiq=new LocationIqClient("https://eu1.locationiq.com","");
    private final OpenCageClient opencage=new OpenCageClient("https://api.opencagedata.com","");
    private final TomTomPlacesClient tomtom=new TomTomPlacesClient("https://api.tomtom.com","");
    private final PostcodesIoClient postcodesIo=new PostcodesIoClient();
    JsonNode json(String s){return mapper.readTree(s);}
    LocationSearchService service(JsonNode data){
        return new LocationSearchService(new PhotonClient(RestClient.create()){public JsonNode search(String q){return data;}},
            new TflClient(RestClient.create(),""){public JsonNode stopPointSearch(String q){return json("{\"matches\":[]}");} public JsonNode placeSearch(String q){return json("[]");}},geoapify,maptiler,locationiq,opencage,tomtom,postcodesIo,new ArcGisClient(),new FoursquareClient("https://places-api.foursquare.com",""),tokens);
    }
    @Test void arcGisKeepsProviderOrderAndDistinguishesAreasFromStores() {
        var candidates=service(json("{}")).arcGisCandidates(json("""
            {"candidates":[
              {"address":"Woolwich","location":{"x":0.063,"y":51.49},"attributes":{"Addr_type":"Locality","Postal":"SE18"}},
              {"address":"Marks & Spencer","location":{"x":0.070707,"y":51.4916055},"attributes":{"Addr_type":"POI","Postal":"se186fl","Place_addr":"4 Plumstead Road"}},
              {"address":"Invalid","location":{"x":190,"y":51},"attributes":{"Addr_type":"POI"}}
            ]}
            """));
        assertThat(candidates).hasSize(2);
        assertThat(candidates.getFirst().name()).isEqualTo("Woolwich");
        assertThat(candidates.getFirst().selectable()).isFalse();
        assertThat(candidates.getFirst().postcode()).isNull();
        var store=candidates.get(1);
        assertThat(store.postcode()).isEqualTo("SE18 6FL");
        assertThat(store.precision()).isEqualTo("venue");
        assertThat(tokens.resolve(store.id(),store.name()).providerId()).isEqualTo("51.4916055,0.070707");
    }
    @Test void foursquareUsesNewResponseCoordinatesAndRejectsForeignOrClosedPlaces() {
        var svc=service(json("{}"));
        var result=svc.foursquareCandidates(json("""
            {"results":[
              {"fsq_place_id":"a","name":"M&S","latitude":51.49,"longitude":0.07,"location":{"country":"GB","postcode":"SE18 6FL"}},
              {"fsq_place_id":"b","name":"Closed","latitude":51.49,"longitude":0.07,"date_closed":"2025-01-01","location":{"country":"GB"}},
              {"name":"Foreign","latitude":51,"longitude":0,"location":{"country":"FR"}}
            ]}
            """));
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().postcode()).isEqualTo("SE18 6FL");
        assertThat(tokens.resolve(result.getFirst().id(),"M&S").providerId()).isEqualTo("51.49,0.07");
        assertThat(result.get(1).selectable()).isFalse();
        assertThat(svc.searchProvider("foursquare","M&S Woolwich").status()).isEqualTo("unavailable");
        assertThatThrownBy(()->svc.foursquareCandidates(json("{}"))).isInstanceOf(RuntimeException.class);
    }
    @Test void foursquareDefaultEndpointMatchesOfficialPlacesApi() throws Exception {
        var ctor=FoursquareClient.class.getConstructor(String.class,String.class);
        var config=ctor.getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Value.class);
        assertThat(config.value()).isEqualTo("${commute-fit.geocoding.foursquare-url:https://places-api.foursquare.com}");
    }
    String feature(String coordinates,String key){
        return "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":"+coordinates+"},\"properties\":{\"name\":\"M&S Woolwich\",\"street\":\"Example Street\",\"housenumber\":\"1\",\"city\":\"London\",\"countrycode\":\"GB\",\"osm_key\":\""+key+"\",\"osm_type\":\"N\",\"osm_id\":123}}";
    }
    @Test void externalGeoJsonIsConvertedToTflLatLonAndConfirmedToken(){
        var svc=service(json("{\"features\":["+feature("[-0.067,51.49]","shop")+"]}"));
        var r=svc.search("M&S Woolwich");assertThat(r.candidates()).hasSize(1);
        var c=r.candidates().getFirst();
        assertThat(c.lat()).isEqualTo(51.49);assertThat(c.lon()).isEqualTo(-0.067);
        assertThat(c.sourceId()).isEqualTo("N123");
        assertThat(tokens.resolve(c.id(),c.name()).providerId()).isEqualTo("51.49,-0.067");
    }
    @Test void choosingSecondIdenticallyNamedStoreUsesItsCoordinatesForBothDirections(){
        var svc=service(json("{\"features\":["+feature("[-0.067,51.49]","shop")+","+feature("[-0.068,51.491]","shop")+"]}"));
        var candidates=svc.search("M&S Woolwich").candidates();assertThat(candidates).hasSize(2);
        var chosen=candidates.get(1);List<String> endpoints=new ArrayList<>();
        var tfl=new TflClient(RestClient.create(),""){
            public JsonNode journey(String from,String to,LocalDate d,LocalTime t,String mode){endpoints.add(from+" → "+to);return json("{\"journeys\":[]}");}
        };
        var lookup=new JourneyLookupService(new TflJourneyService(tfl,tokens));
        lookup.lookup(new JourneyLookupRequest("DA18 4DE",chosen.name(),"arrive",LocalDate.of(2026,10,30),"09:00",null,chosen.id()));
        lookup.lookup(new JourneyLookupRequest(chosen.name(),"DA18 4DE","leave",LocalDate.of(2026,10,30),"17:00",chosen.id(),null));
        assertThat(endpoints).containsExactly("DA18 4DE → 51.491,-0.068","51.491,-0.068 → DA18 4DE");
    }
    @Test void invalidCoordinatesNeverBecomeSelectableTokens(){
        var svc=service(json("{}"));
        var data=json("{\"features\":["+feature("[190,51]","shop")+","+feature("[0,91]","shop")+","+feature("[0]","shop")+","+feature("[\"-0.1\",51]","shop")+"]}");
        assertThat(svc.photonCandidates(data)).isEmpty();
        assertThat(LocationSearchService.validPoint(Double.NaN,0)).isFalse();
    }
    @Test void localityIsNotASelectableExactStore(){
        var svc=service(json("{}"));
        var result=svc.photonCandidates(json("{\"features\":[{\"geometry\":{\"type\":\"Point\",\"coordinates\":[-0.1,51.5]},\"properties\":{\"name\":\"London\",\"type\":\"city\",\"osm_key\":\"place\"}}]}"));
        assertThat(result.getFirst().precision()).isEqualTo("locality");
        assertThat(result.getFirst().selectable()).isFalse();assertThat(result.getFirst().id()).isEmpty();
    }
    @Test void directPostcodeCoordinateAndIdsDoNotNeedGeocoder(){
        var svc=service(null);
        for(String q:new String[]{"da184de","51.5178,-0.0823","940GZZLULVT","9400ZZLULVT4","490000138K","910GABWD","1000138"}){
            var r=svc.search(q);assertThat(r.candidates()).hasSize(1);
            var c=r.candidates().getFirst();assertThat(tokens.resolve(c.id(),c.name()).providerId()).isNotBlank();
        }
        assertThat(svc.search("da184de").candidates().getFirst().name()).isEqualTo("DA18 4DE");
        assertThatThrownBy(()->svc.search("91,0")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void observedWoolwichResponseSeparatesStationsFromFoodhallAndKeepsEastLongitude()throws Exception{
        try(var in=getClass().getResourceAsStream("/locations/photon-woolwich.json")){
            var candidates=service(mapper.readTree(in)).search("M&S Woolwich").candidates();
            assertThat(candidates).hasSize(3);
            assertThat(candidates.getLast().precision()).isEqualTo("station");
            var shop=candidates.getFirst();assertThat(shop.precision()).isEqualTo("venue");
            assertThat(shop.name()).contains("M&S Foodhall","Victory Parade","SE18 6FL");
            assertThat(shop.address()).contains("4 Victory Parade");
            assertThat(tokens.resolve(shop.id(),shop.name()).providerId()).isEqualTo("51.4915794,0.070647");
        }
    }
    @Test void providerFailureIsNotMisreportedAsAValidLocation(){
        var svc=new LocationSearchService(new PhotonClient(RestClient.create()){public JsonNode search(String q){throw new IllegalStateException();}},
            new TflClient(RestClient.create(),""){public JsonNode stopPointSearch(String q){throw new IllegalStateException();} public JsonNode placeSearch(String q){throw new IllegalStateException();}},geoapify,maptiler,locationiq,opencage,tomtom,postcodesIo,new ArcGisClient(),new FoursquareClient("https://places-api.foursquare.com",""),tokens);
        assertThat(svc.search("M&S Woolwich").status()).isEqualTo("unavailable");
    }
    @Test void tflPlaceLocalityOrUnknownTypeIsNotPresentedAsAnExactVenue(){
        var svc=new LocationSearchService(new PhotonClient(RestClient.create()){public JsonNode search(String q){return json("{\"features\":[]}");}},
            new TflClient(RestClient.create(),""){
                public JsonNode stopPointSearch(String q){return json("{\"matches\":[]}");}
                public JsonNode placeSearch(String q){return json("[{\"commonName\":\"Woolwich\",\"placeType\":\"Locality\",\"lat\":51.49,\"lon\":0.067},{\"commonName\":\"Woolwich shop\",\"placeType\":\"PointOfInterest\",\"lat\":51.49,\"lon\":0.067}]");}
            },geoapify,maptiler,locationiq,opencage,tomtom,postcodesIo,new ArcGisClient(),new FoursquareClient("https://places-api.foursquare.com",""),tokens);
        var candidates=svc.search("Woolwich").candidates();
        assertThat(candidates).hasSize(2);
        assertThat(candidates.getLast().selectable()).isFalse();assertThat(candidates.getLast().id()).isEmpty();
        assertThat(candidates.getFirst().selectable()).isTrue();assertThat(candidates.getFirst().precision()).isEqualTo("venue");
    }
    @Test void addressFailureWithTflResultsIsExplicitlyPartialAndKeepsStationIdentity(){
        var svc=new LocationSearchService(new PhotonClient(RestClient.create()){public JsonNode search(String q){throw new IllegalStateException();}},
            new TflClient(RestClient.create(),""){
                public JsonNode stopPointSearch(String q){return json("{\"matches\":[{\"id\":\"HUBLST\",\"icsId\":\"1000138\",\"name\":\"Liverpool Street\"}]}");}
            },geoapify,maptiler,locationiq,opencage,tomtom,postcodesIo,new ArcGisClient(),new FoursquareClient("https://places-api.foursquare.com",""),tokens);
        var result=svc.search("Liverpool Street");
        assertThat(result.status()).isEqualTo("matches");assertThat(result.message()).contains("Address search unavailable");
        var station=result.candidates().getFirst();assertThat(station.precision()).isEqualTo("station");
        assertThat(tokens.resolve(station.id(),station.name()).providerId()).isEqualTo("1000138");
    }
    @Test void explicitPhotonQueriesAreCachedAndRequestUsesLocationBias(){
        var builder=RestClient.builder().baseUrl("https://photon.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request->{assertThat(request.getURI().getQuery()).contains("q=M&S Woolwich","limit=8","lang=en","lat=51.5074");})
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"features\":[]}",MediaType.APPLICATION_JSON));
        var client=new PhotonClient(builder.build());
        client.search("M&S Woolwich");client.search("M&S Woolwich");server.verify();
        assertThatThrownBy(()->client.search("a different place")).isInstanceOf(IllegalStateException.class);
    }
    @Test void geoapifyResultsPreserveAddressPostcodeAndUseCoordinatesForJourneyToken(){
        var svc=service(json("{\"features\":[]}"));
        var results=svc.geoapifyCandidates(json("{\"results\":[{\"name\":\"M&S Woolwich Foodhall\",\"formatted\":\"4 Victory Parade, Woolwich, London, SE18 6FL, United Kingdom\",\"postcode\":\"SE18 6FL\",\"result_type\":\"building\",\"lat\":51.4915,\"lon\":0.0706,\"place_id\":\"test-id\"}]}"));
        assertThat(results).hasSize(1);
        var candidate=results.getFirst();
        assertThat(candidate.name()).isEqualTo("M&S Woolwich Foodhall");
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.selectable()).isTrue();
        assertThat(tokens.resolve(candidate.id(),candidate.name()).providerId()).isEqualTo("51.4915,0.0706");
    }
    @Test void geoapifyWithoutKeyReturnsItsOwnUnavailableState(){
        var result=service(json("{\"features\":[]}")).searchProvider("geoapify","M&S Woolwich");
        assertThat(result.status()).isEqualTo("unavailable");
        assertThat(result.message()).contains("Geoapify");
        assertThat(result.candidates()).isEmpty();
    }
    @Test void maptilerPoiAndPostcodeAreNormalizedWithoutPassingProviderIdToTfl(){
        var svc=service(json("{\"features\":[]}"));
        var results=svc.maptilerCandidates(json("{\"features\":[{\"id\":\"poi.123\",\"text\":\"M&S Foodhall\",\"place_name\":\"4 Plumstead Road, Woolwich, SE18 6FL, United Kingdom\",\"place_type\":[\"poi\"],\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.0706,51.4915]},\"properties\":{\"ref\":\"osm:r123\"}}]}"));
        assertThat(results).hasSize(1);
        var candidate=results.getFirst();
        assertThat(candidate.precision()).isEqualTo("venue");
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.sourceId()).isEqualTo("poi.123");
        assertThat(tokens.resolve(candidate.id(),candidate.name()).providerId()).isEqualTo("51.4915,0.0706");
    }
    @Test void maptilerWithoutKeyReturnsItsOwnUnavailableState(){
        var result=service(json("{\"features\":[]}")).searchProvider("maptiler","M&S Woolwich");
        assertThat(result.status()).isEqualTo("unavailable");
        assertThat(result.message()).contains("MapTiler");
    }
    @Test void locationIqPostcodeIsSelectableAndUsesPostalAreaCoordinates(){
        var svc=service(json("{\"features\":[]}"));
        var candidates=svc.locationIqCandidates(json("[{\"place_id\":123,\"lat\":\"51.4915\",\"lon\":\"0.0706\",\"display_name\":\"SE18 6FL, Greenwich, London, United Kingdom\",\"addresstype\":\"postcode\",\"address\":{\"postcode\":\"SE18 6FL\"}}]"),true);
        assertThat(candidates).hasSize(1);
        var candidate=candidates.getFirst();
        assertThat(candidate.precision()).isEqualTo("postcode");
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.selectable()).isTrue();
        assertThat(tokens.resolve(candidate.id(),candidate.name()).providerId()).isEqualTo("51.4915,0.0706");
    }
    @Test void locationIqWithoutKeyReturnsItsOwnUnavailableState(){
        var result=service(json("{\"features\":[]}")).searchProvider("locationiq","M&S Woolwich");
        assertThat(result.status()).isEqualTo("unavailable");
        assertThat(result.message()).contains("LocationIQ");
    }
    @Test void locationIqHttp404IsReportedAsNoMatchRatherThanProviderOutage(){
        var noMatchClient=new LocationIqClient("https://eu1.locationiq.com","test-key"){
            @Override public JsonNode search(String query,boolean postcode){
                throw org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,"Not Found",org.springframework.http.HttpHeaders.EMPTY,
                        "{\"error\":\"Unable to geocode\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.UTF_8);
            }
        };
        var svc=new LocationSearchService(new PhotonClient(RestClient.create()),new TflClient(RestClient.create(),""),geoapify,maptiler,noMatchClient,opencage,tomtom,postcodesIo,new ArcGisClient(),new FoursquareClient("https://places-api.foursquare.com",""),tokens);
        var result=svc.searchProvider("locationiq","M&S Woolwich Simply Food");
        assertThat(result.status()).isEqualTo("no-matches");
        assertThat(result.message()).contains("HTTP 404", "无地址匹配");
    }
    @Test void openCageNamedShopAndPostcodeAreNormalizedWithCoordinateToken(){
        var svc=service(json("{\"features\":[]}"));
        var candidates=svc.openCageCandidates(json("{\"results\":[{\"formatted\":\"Marks & Spencer, 4 Victory Parade, Woolwich, London SE18 6FL, United Kingdom\",\"components\":{\"name\":\"Marks & Spencer\",\"shop\":\"supermarket\",\"house_number\":\"4\",\"postcode\":\"SE18 6FL\"},\"geometry\":{\"lat\":51.49162,\"lng\":0.07068},\"annotations\":{\"geohash\":\"test\"}}]}"),"Marks and Spencer Woolwich");
        assertThat(candidates).hasSize(1);
        var candidate=candidates.getFirst();
        assertThat(candidate.name()).isEqualTo("Marks & Spencer");
        assertThat(candidate.precision()).isEqualTo("address");
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.selectable()).isTrue();
        assertThat(tokens.resolve(candidate.id(),candidate.name()).providerId()).isEqualTo("51.49162,0.07068");
    }
    @Test void openCageWithoutKeyReturnsItsOwnUnavailableState(){
        var result=service(json("{\"features\":[]}")).searchProvider("opencage","M&S Woolwich");
        assertThat(result.status()).isEqualTo("unavailable");
        assertThat(result.message()).contains("OpenCage");
    }
    @Test void tomTomFuzzyPoiAndExtendedPostcodeAreNormalized(){
        var svc=service(json("{\"features\":[]}"));
        var candidates=svc.tomTomFuzzyCandidates(json("{\"results\":[{\"type\":\"POI\",\"id\":\"poi-1\",\"poi\":{\"name\":\"Marks & Spencer M&S Foodhall\",\"brands\":[{\"name\":\"Marks & Spencer\"}]},\"address\":{\"freeformAddress\":\"4 Victory Parade, Woolwich, SE18 6FL\",\"postalCode\":\"SE18\",\"extendedPostalCode\":\"SE18 6FL\"},\"position\":{\"lat\":51.49162,\"lon\":0.07068}}] }"));
        assertThat(candidates).hasSize(1);
        var candidate=candidates.getFirst();
        assertThat(candidate.name()).isEqualTo("Marks & Spencer M&S Foodhall");
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.precision()).isEqualTo("venue");
        assertThat(candidate.selectable()).isTrue();
    }
    @Test void tomTomPlacesDiscoverPrefersExtendedPostcodeOverOutwardCode(){
        var svc=service(json("{\"features\":[]}"));
        var candidates=svc.tomTomCandidates(json("{\"results\":[{\"type\":\"poi\",\"id\":\"place-1\",\"title\":\"Marks & Spencer M&S Foodhall\",\"position\":{\"type\":\"Point\",\"coordinates\":[0.07058,51.49156]},\"address\":{\"street\":\"Victory Parade\",\"houseNumber\":\"6\",\"municipality\":\"London\",\"postalCode\":\"SE18\",\"extendedPostalCode\":\"SE18 6FL\"}}] }"),"M&S Woolwich");
        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().postcode()).isEqualTo("SE18 6FL");
        assertThat(candidates.getFirst().address()).contains("SE18 6FL");
    }
    @Test void postcodesIoCandidatesPreserveFullPostcodeAndCoordinatePrecision(){
        var svc=service(json("{\"features\":[]}"));
        var candidates=svc.postcodesIoCandidates(json("{\"status\":200,\"result\":[{\"postcode\":\"SE18 6FL\",\"quality\":1,\"latitude\":51.49162,\"longitude\":0.07068,\"admin_district\":\"Greenwich\",\"region\":\"London\"}]}"));
        assertThat(candidates).hasSize(1);
        var candidate=candidates.getFirst();
        assertThat(candidate.postcode()).isEqualTo("SE18 6FL");
        assertThat(candidate.precision()).isEqualTo("postcode");
        assertThat(candidate.lat()).isEqualTo(51.49162);
        assertThat(candidate.lon()).isEqualTo(0.07068);
        assertThat(candidate.selectable()).isTrue();
    }
    @Test void postcodesIoClearlyRejectsBrandSearchWithoutCallingItAnOutage(){
        var result=service(json("{\"features\":[]}")).searchProvider("postcodes-io","Marks and Spencer Woolwich");
        assertThat(result.status()).isEqualTo("no-matches");
        assertThat(result.message()).contains("不搜索品牌");
    }
    @Test void httpContractReturnsSelectableMetadata()throws Exception{
        var svc=service(json("{\"features\":["+feature("[-0.067,51.49]","shop")+"]}"));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new PlaceSearchController(svc,new AreaConsensusService(svc)))
            .setControllerAdvice(new uk.co.commutefit.common.ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/place-search").param("query","M&S Woolwich"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.candidates[0].lat").value(51.49))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.candidates[0].sourceId").value("N123"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.candidates[0].id").isNotEmpty());
    }
}
