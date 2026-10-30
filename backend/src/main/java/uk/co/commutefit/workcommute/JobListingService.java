package uk.co.commutefit.workcommute;

import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import uk.co.commutefit.jobextraction.*;

@Service
public class JobListingService {
    public record ExtractionRequest(@NotBlank String sourceType,@Size(max=20000) String content) {}
    private final JobExtractionService extraction;
    public JobListingService(){this(new JobExtractionService(new PublicPageFetcher(),new JobDocumentParser()));}
    @org.springframework.beans.factory.annotation.Autowired
    public JobListingService(JobExtractionService extraction){this.extraction=extraction;}
    public JobExtractionService.Result extract(ExtractionRequest request){
        return extraction.extract(new JobExtractionService.Request(request.sourceType(),request.content()));
    }
}
