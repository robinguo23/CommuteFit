package uk.co.commutefit.assessment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessment")
@org.springframework.context.annotation.Profile("legacy")
public class AssessmentController {
    private final AssessmentService service;
    public AssessmentController(AssessmentService service) { this.service = service; }
    @PostMapping public AssessmentResponse assess(@Valid @RequestBody AssessmentRequest request) { return service.assess(request); }
}
