package uk.co.commutefit.location;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AreaConsensusServiceTest {
    LocationSearchService.Candidate point(String name,String source,double lon) {
        return new LocationSearchService.Candidate(name+source,name,"Woolwich London","SE18 6FL",51.49,lon,"venue",source,name,true);
    }
    @Test void threeSourcesSupportOneAreaAndRepresentativeIsAnActualPoint() {
        var a=point("Marks & Spencer","LocationIQ",0.07);
        var b=point("M&S","ArcGIS",0.071);
        var c=point("M&S Foodhall","TomTom",0.072);
        var areas=AreaConsensusService.group(List.of(a,b,c));
        assertThat(areas).hasSize(1);
        assertThat(areas.getFirst().support()).isEqualTo(3);
        assertThat(areas.getFirst().representative()).isEqualTo(b);
        assertThat(areas.getFirst().maxDistanceMeters()).isLessThan(966);
    }
    @Test void sameSourceCannotCreateSupportAndChainCannotCreateTriple() {
        assertThat(AreaConsensusService.group(List.of(point("a","LocationIQ",0.07),point("b","LocationIQ",0.071)))).isEmpty();
        var areas=AreaConsensusService.group(List.of(point("a","LocationIQ",0.07),point("b","ArcGIS",0.08),point("c","TomTom",0.09)));
        assertThat(areas).hasSize(2).allMatch(a->a.support()==2);
    }
    @Test void separatedAreasRemainSeparateAndDuplicateProviderGetsOneVote() {
        var areas=AreaConsensusService.group(List.of(point("a","LocationIQ",0.07),point("b","ArcGIS",0.071),point("c","LocationIQ",0.072),point("d","LocationIQ",0.2),point("e","TomTom",0.201)));
        assertThat(areas).hasSize(2).allMatch(a->a.support()==2);
    }
    @Test void nameFilterHandlesBrandAliasAndRejectsWrongAreaAndNonfinitePoint() {
        assertThat(AreaConsensusService.rejection("M&S Woolwich Simply Food",point("Marks & Spencer","LocationIQ",0.07))).isNull();
        assertThat(AreaConsensusService.rejection("Marks and Spencer Birstall",point("M&S","ArcGIS",0.07))).isNotNull();
        assertThat(AreaConsensusService.rejection("M&S Woolwich",point("M&S","ArcGIS",Double.NaN))).isNotNull();
    }
    @Test void possessiveAndAbbreviatedStreetNamesMatchCanonicalLocationText() {
        var kingRoad=observed("Marks & Spencer","ArcGIS","85 King's Road, Chelsea, London","SW3 4NX",51.49,-0.165);
        assertThat(AreaConsensusService.rejection("Marks & Spencer Kings Rd London",kingRoad)).isNull();
    }
    @Test void fullPostcodeOnlyAcceptsCandidatesWithTheSamePostcode() {
        var exact=observed("Marks & Spencer","LocationIQ","Woolwich, London","SE18 6FL",51.49,0.07);
        var nearbyButDifferent=observed("Marks & Spencer","ArcGIS","Woolwich, London","SE18 6FJ",51.4901,0.0701);
        assertThat(AreaConsensusService.rejection("SE18 6FL",exact)).isNull();
        assertThat(AreaConsensusService.rejection("SE18 6FL",nearbyButDifferent)).contains("postcode 不一致");
    }
    @Test void searchesRunConcurrentlyAndOneFailedProviderStillAllowsTwoSourceSupport() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(3);
        var calls=java.util.Collections.synchronizedList(new ArrayList<String>());
        var stub=new LocationSearchService(null,null,null,null,null,null,null,null,null,null,null){
            @Override public Result searchProvider(String provider,String query){
                calls.add(provider);entered.countDown();
                try {if(!entered.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Not concurrent");}
                catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
                if(provider.equals("tomtom"))throw new IllegalStateException("Provider failed");
                return new Result("matches",List.of(point("M&S",provider,provider.equals("locationiq")?0.07:0.071)),"ok",provider);
            }
        };
        var result=new AreaConsensusService(stub).assess("M&S Woolwich");
        assertThat(calls).containsExactlyInAnyOrder("locationiq","arcgis","tomtom");
        assertThat(result.status()).isEqualTo("supported");
        assertThat(result.areas().getFirst().support()).isEqualTo(2);
        assertThat(result.providers().getLast().result().status()).isEqualTo("unavailable");
    }
    @Test void consensusHttpContractIncludesThresholdSourcesAndExclusions() throws Exception {
        var stub=new LocationSearchService(null,null,null,null,null,null,null,null,null,null,null){
            @Override public Result searchProvider(String provider,String query){
                return new Result("matches",List.of(point("M&S",provider,0.07)),"ok",provider);
            }
        };
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            new PlaceSearchController(stub,new AreaConsensusService(stub))).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/place-search/consensus").param("query","M&S Woolwich"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("supported"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.areas[0].support").value(3))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.maxDistanceMeters").value(965.6064));
    }
    LocationSearchService.Candidate observed(String name,String source,String address,String pc,double lat,double lon){
        return new LocationSearchService.Candidate(name+source+lon,name,address,pc,lat,lon,"venue",source,name,true);
    }
    AreaConsensusService.Assessment fixture(String query,List<LocationSearchService.Candidate> points){
        var stub=new LocationSearchService(null,null,null,null,null,null,null,null,null,null,null){
            @Override public Result searchProvider(String provider,String q){return new Result("matches",points.stream().filter(c->c.source().equals(provider)).toList(),"fixture",provider);}
        };
        return new AreaConsensusService(stub).assess(query);
    }
    @Test void observedNandosIncompleteAddressesCanVoteByCrossSourceCoordinates(){
        var result=fixture("Nando's Woolwich",List.of(
            observed("Nando's","locationiq","50 Powis Street, Callis Yard, London","SE18 6LQ",51.49143,0.06679),
            observed("Nando's","arcgis","50 Powis Street, Woolwich, London","SE18 6LQ",51.49144,0.06678),
            observed("Nando's","tomtom","50 Powis Street, London","SE18 6LQ",51.49144,0.06678)));
        assertThat(result.status()).isEqualTo("supported");
        assertThat(result.areas().getFirst().support()).isEqualTo(3);
        assertThat(result.corroborated()).hasSize(2);
    }
    @Test void observedBirstallDifferentPostcodesStillFormAreaWithoutLocationIq(){
        var result=fixture("marks and spencer birstall",List.of(
            observed("Marks & Spencer","arcgis","Holden Ing Way, Birstall, Batley","WF17 9AD",53.74474,-1.64436),
            observed("Marks & Spencer","arcgis","Holden Ing Way, Birstall, Batley","WF17 9DT",53.74610,-1.64321),
            observed("Marks & Spencer M&S Simply Food","tomtom","Holden Ing Way, Batley","WF17 9DT",53.74480,-1.64443)));
        assertThat(result.areas()).hasSize(1);
        assertThat(result.areas().getFirst().support()).isEqualTo(2);
        assertThat(result.areas().getFirst().maxDistanceMeters()).isLessThan(200);
    }
    @Test void nearbyWrongBrandAndDistantSameBrandCannotBeCorroborated(){
        var result=fixture("Costa Woolwich",List.of(
            observed("Costa","arcgis","Woolwich New Road","SE18 6EU",51.49030,0.06940),
            observed("Starbucks","locationiq","London","SE18",51.49030,0.06940),
            observed("Costa","tomtom","London","SE7",51.49,0.02)));
        assertThat(result.areas()).isEmpty();assertThat(result.corroborated()).isEmpty();
    }
    @Test void multipleDistantRegionsRequireMoreSpecificLocationEvenWhenOneHasMoreSources(){
        var result=fixture("Costa Woolwich",List.of(
            observed("Costa","locationiq","Woolwich","SE18",51.49,0.07),
            observed("Costa","arcgis","Woolwich","SE18",51.49,0.071),
            observed("Costa","tomtom","Woolwich","SE18",51.49,0.072),
            observed("Costa","locationiq","Woolwich Road","SE7",51.49,0.02),
            observed("Costa","tomtom","Woolwich Road","SE7",51.49,0.02)));
        assertThat(result.status()).isEqualTo("ambiguous");
        assertThat(result.areas()).hasSize(2);
        assertThat(result.areas().getFirst().support()).isEqualTo(3);
        assertThat(result.selectedCandidate()).isNull();
    }
    @Test void twoDistantThreeSourceRegionsBothRetainRepresentativeCoordinates(){
        var result=fixture("Costa Woolwich",List.of(
            observed("Costa","locationiq","Woolwich","SE18",51.49,0.07),
            observed("Costa","arcgis","Woolwich","SE18",51.49,0.071),
            observed("Costa","tomtom","Woolwich","SE18",51.49,0.072),
            observed("Costa","locationiq","Woolwich Road","SE7",51.49,0.02),
            observed("Costa","arcgis","Woolwich Road","SE7",51.49,0.021),
            observed("Costa","tomtom","Woolwich Road","SE7",51.49,0.022)));
        assertThat(result.status()).isEqualTo("ambiguous");
        assertThat(result.areas()).hasSize(2).allSatisfy(area->{
            assertThat(area.support()).isEqualTo(3);
            assertThat(area.representative().lat()).isNotNull();
            assertThat(area.representative().lon()).isNotNull();
        });
    }
    @Test void distantSingleCandidateDoesNotVetoThreeSourceConsensus(){
        var result=fixture("Sainsbury's Wandsworth Road London",List.of(
            observed("Sainsbury's","locationiq","Wandsworth Road, Vauxhall, London","SW8 2LF",51.480777,-0.1281354),
            observed("Sainsbury's","arcgis","62 Wandsworth Road, Vauxhall, London","SW8 2ET",51.4807371,-0.1281523),
            observed("Sainsbury's","tomtom","62 Wandsworth Road, London","SW8 2ET",51.480737,-0.128152),
            observed("Sainsbury's Local","tomtom","646-650 Wandsworth Road, London","SW8 3SB",51.46638,-0.15031)));
        assertThat(result.areas().getFirst().support()).isEqualTo(3);
        assertThat(result.status()).isEqualTo("supported");
        assertThat(result.selectedCandidate()).isEqualTo(result.areas().getFirst().representative());
        assertThat(result.excluded()).anyMatch(e->e.candidate().postcode().equals("SW8 3SB")&&e.reason().contains("单个远处候选"));
    }
    @Test void uniqueStreetTextCandidateCanBeShownWithoutCallingItMultiSourceConsensus(){
        var result=fixture("Marks & Spencer Oxford Street",List.of(
            observed("Marks & Spencer","locationiq","173 Oxford Street, London","W1D 2JR",51.515,-0.138)));
        assertThat(result.areas()).isEmpty();
        assertThat(result.status()).isEqualTo("text-match");
        assertThat(result.selectedCandidate()).isNotNull();
        assertThat(result.selectedCandidate().postcode()).isEqualTo("W1D 2JR");
    }
    @Test void sameStreetTextAtDistantBranchesStaysAmbiguousWithoutConsensus(){
        var result=fixture("Sainsbury's Wandsworth Road London",List.of(
            observed("Sainsbury's","locationiq","Wandsworth Road, Vauxhall, London","SW8 2LF",51.480777,-0.1281354),
            observed("Sainsbury's Local","tomtom","646-650 Wandsworth Road, London","SW8 3SB",51.46638,-0.15031)));
        assertThat(result.areas()).isEmpty();
        assertThat(result.status()).isEqualTo("ambiguous");
        assertThat(result.selectedCandidate()).isNull();
        assertThat(result.excluded()).anyMatch(e->e.candidate().postcode().equals("SW8 3SB"));
    }
    @Test void brandOnlyQueryDoesNotSelectAStoreByTextAlone(){
        var result=fixture("Marks & Spencer",List.of(
            observed("Marks & Spencer","locationiq","173 Oxford Street, London","W1D 2JR",51.515,-0.138)));
        assertThat(result.status()).isEqualTo("needs-confirmation");
        assertThat(result.selectedCandidate()).isNull();
    }
    @Test void textFallbackRequiresTheQueriedStreetOrAreaToAppearInCandidateAddress(){
        var result=fixture("Marks & Spencer Oxford Street",List.of(
            observed("Marks & Spencer","locationiq","173 Regent Street, London","W1B 4JQ",51.51,-0.14)));
        assertThat(result.status()).isEqualTo("needs-confirmation");
        assertThat(result.selectedCandidate()).isNull();
    }
    @Test void exactDistanceThresholdHasInclusiveBoundary() {
        assertThat(AreaConsensusService.withinLimit(965.6064)).isTrue();
        assertThat(AreaConsensusService.withinLimit(965.6065)).isFalse();
        assertThat(AreaConsensusService.withinLimit(Double.NaN)).isFalse();
    }
}
