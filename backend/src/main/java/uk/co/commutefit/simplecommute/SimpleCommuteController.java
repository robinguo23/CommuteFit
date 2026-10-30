package uk.co.commutefit.simplecommute;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import uk.co.commutefit.common.ApiError;

@RestController
@RequestMapping("/api/commute-checks")
public class SimpleCommuteController {
    private final CommuteCheckJobs jobs;
    public SimpleCommuteController(CommuteCheckJobs jobs){this.jobs=jobs;}
    @PostMapping public ResponseEntity<?> create(@RequestBody SimpleCommuteService.Request request){
        try{return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobs.start(request));}
        catch(CommuteCheckJobs.Busy e){return ResponseEntity.status(429).body(ApiError.of("check_busy","Other checks are still running. Retry shortly.","已有检查正在进行，请稍后重试。"));}
    }
    @GetMapping("/{id}") public ResponseEntity<?> get(@PathVariable String id){
        var job=jobs.find(id);
        return job.isPresent()?ResponseEntity.ok(job.get()):ResponseEntity.status(404).body(ApiError.of("check_expired","This check expired. Start a new check.","本次检查已过期，请重新检查。"));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> cancel(@PathVariable String id){jobs.cancel(id);return ResponseEntity.noContent().build();}
}
