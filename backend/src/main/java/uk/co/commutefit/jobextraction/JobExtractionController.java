package uk.co.commutefit.jobextraction;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-extractions")
public class JobExtractionController {
    private final JobExtractionService service;
    public JobExtractionController(JobExtractionService service){this.service=service;}
    @PostMapping public JobExtractionService.Result extract(@Valid @RequestBody JobExtractionService.Request request){return service.extract(request);}
}
