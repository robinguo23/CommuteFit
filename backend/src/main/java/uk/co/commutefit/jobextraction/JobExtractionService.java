package uk.co.commutefit.jobextraction;

import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;

@Service
public class JobExtractionService {
    public record Request(@NotBlank String sourceType,@NotBlank @Size(max=20000) String content) {}
    public record Source(String method,String url,Instant fetchedAt) {}
    public record Location(String value,String evidence,String sourcePath) {}
    public record Schedule(String kind,List<DayOfWeek> days,String start,String end,boolean endsNextDay,String evidence,String sourcePath,Integer cycleWeek) {}
    public record Result(String status,String title,String jobStatus,Source source,List<Location> locations,List<Schedule> schedules,
            List<String> warnings,List<String> missing,String workplaceLocation,List<DayOfWeek> workDays,String startTime,String endTime,List<String> notes) {}
    private final PublicPageFetcher fetcher;
    private final JobDocumentParser parser;
    public JobExtractionService(PublicPageFetcher fetcher,JobDocumentParser parser){this.fetcher=fetcher;this.parser=parser;}
    public Result extract(Request request) {
        if(request==null||request.sourceType()==null||request.content()==null||request.content().length()>20000)throw new IllegalArgumentException("Invalid extraction input");
        String type=request.sourceType(),content=request.content().trim();
        if(!Set.of("url","description","work-postcode").contains(type))throw new IllegalArgumentException("Unknown job source type");
        if(content.isBlank())throw new IllegalArgumentException("Provide job content");
        String url=null;
        if(type.equals("url")){
            try {var page=fetcher.fetch(content);content=page.html();url=safeUrl(page.finalUrl());}
            catch(Exception ex) {return result(null,"unknown",new Source("static-http",safeUrl(content),Instant.now()),List.of(),List.of(),
                List.of("Could not read this page. Paste the job description or enter the workplace details manually."));}
        }
        if(type.equals("work-postcode"))return result(null,"unknown",new Source("manual",null,Instant.now()),
            List.of(new Location(content,content,"manual")),List.of(),List.of("Enter the job's days and hours."));
        var parsed=parser.parse(content,type.equals("url"));
        return result(parsed.title(),parsed.jobStatus(),new Source(type.equals("url")?"static-http":"text",url,Instant.now()),parsed.locations(),parsed.schedules(),parsed.warnings());
    }
    static Result result(String title,String jobStatus,Source source,List<Location> locations,List<Schedule> schedules,List<String> warnings) {
        List<String> notes=new ArrayList<>(warnings),missing=new ArrayList<>();
        String location=locations.size()==1&&!warnings.stream().anyMatch(w->w.startsWith("Multiple workplace"))?locations.getFirst().value():null;
        if(location==null){missing.add("workplace");notes.add(locations.size()>1?"More than one postcode was found; confirm the workplace postcode.":"No clear workplace postcode was found; enter the workplace location.");}
        var exact=schedules.stream().filter(s->s.kind().equals("exact-shift")&&s.start()!=null&&s.end()!=null).toList();
        if(!exact.isEmpty())schedules=schedules.stream().filter(s->!s.kind().equals("availability-window")).toList();
        Schedule chosen=exact.size()==1?exact.getFirst():null;
        if(chosen==null){missing.add(exact.isEmpty()?"exact-shift":"shift-selection");notes.add(exact.isEmpty()?"Shift times were not clear; enter the start and end times manually.":"Multiple shifts were found; select the shifts to check.");}
        if(!exact.isEmpty()&&exact.stream().anyMatch(s->s.days().isEmpty()))notes.add("Work days were not specified; check all seven days for reference.");
        notes.add("Extracted details are suggestions. Review them before checking the commute.");
        boolean unavailable=warnings.stream().anyMatch(w->w.startsWith("Could not read"));
        return new Result(unavailable?"unavailable":missing.isEmpty()&&!jobStatus.equals("expired")?"ready":"partial",title,jobStatus,source,List.copyOf(locations),List.copyOf(schedules),List.copyOf(warnings),List.copyOf(missing),
            location,chosen==null?List.of():chosen.days(),chosen==null?null:chosen.start(),chosen==null?null:chosen.end(),List.copyOf(notes));
    }
    private static String safeUrl(String raw) {
        try{var u=java.net.URI.create(raw);return new java.net.URI(u.getScheme(),null,u.getHost(),u.getPort(),u.getPath(),null,null).toString();}
        catch(Exception ex){return null;}
    }
}
