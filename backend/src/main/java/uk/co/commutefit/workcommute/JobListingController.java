package uk.co.commutefit.workcommute;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-commute")
public class JobListingController {
    private final JobListingService listings;
    private final WorkCommuteService workCommute;
    public JobListingController(JobListingService listings, WorkCommuteService workCommute) { this.listings = listings; this.workCommute = workCommute; }

    @PostMapping("/extract")
    public uk.co.commutefit.jobextraction.JobExtractionService.Result extract(@Valid @RequestBody JobListingService.ExtractionRequest request) {
        return listings.extract(request);
    }

    @PostMapping("/assess")
    public WorkCommuteService.Result assess(@Valid @RequestBody WorkCommuteService.Request request) {
        return workCommute.assess(request);
    }

    @PostMapping("/scenario")
    public WorkCommuteService.DayResult scenario(@Valid @RequestBody WorkCommuteService.ScenarioRequest request) {
        return workCommute.assessScenario(request);
    }
}
