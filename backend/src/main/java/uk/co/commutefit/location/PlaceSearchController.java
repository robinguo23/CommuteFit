package uk.co.commutefit.location;

import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/place-search")
public class PlaceSearchController {
    private final LocationSearchService service;
    private final AreaConsensusService consensus;
    public PlaceSearchController(LocationSearchService service,AreaConsensusService consensus){this.service=service;this.consensus=consensus;}
    @GetMapping("/consensus") public AreaConsensusService.Assessment consensus(@RequestParam String query){return consensus.assess(query);}
    @GetMapping public LocationSearchService.Result search(@RequestParam String query){return service.search(query);}
    @GetMapping("/provider/{provider}") public LocationSearchService.Result searchProvider(@PathVariable String provider,@RequestParam String query){return service.searchProvider(provider,query);}
}
