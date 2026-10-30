package uk.co.commutefit.journey.lookup;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journeys")
public class JourneyLookupController {
    private final JourneyLookupService service;
    public JourneyLookupController(JourneyLookupService service) { this.service = service; }
    @PostMapping public JourneyLookupService.Result lookup(@Valid @RequestBody JourneyLookupRequest request) {
        return service.lookup(request);
    }
}
