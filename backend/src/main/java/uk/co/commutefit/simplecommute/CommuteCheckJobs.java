package uk.co.commutefit.simplecommute;

import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.commutefit.common.ApiError;

@Service
public class CommuteCheckJobs implements AutoCloseable {
    public static class Busy extends RuntimeException {}
    public record Snapshot(String id,String status,int completed,int total,SimpleCommuteService.Result result,ApiError error) {}
    private static final class Job {
        final String id=UUID.randomUUID().toString();
        volatile String status="running";volatile int completed,total;volatile boolean cancelled;
        volatile SimpleCommuteService.Result result;volatile ApiError error;volatile Instant expires;
        volatile Future<?> future;
        Job(Instant now){expires=now.plusSeconds(600);}
        Snapshot snapshot(){return new Snapshot(id,status,completed,total,result,error);}
    }
    private final SimpleCommuteService service;
    private final Clock clock;
    private final ExecutorService executor;
    private final Map<String,Job> jobs=new LinkedHashMap<>();
    @Autowired public CommuteCheckJobs(SimpleCommuteService service){this(service,Clock.systemUTC(),Executors.newVirtualThreadPerTaskExecutor());}
    CommuteCheckJobs(SimpleCommuteService service,Clock clock,ExecutorService executor){this.service=service;this.clock=clock;this.executor=executor;}
    public synchronized Snapshot start(SimpleCommuteService.Request input){
        var request=service.validate(input);cleanup();
        if(jobs.size()>=16||jobs.values().stream().filter(j->j.status.equals("running")).count()>=2)throw new Busy();
        Job job=new Job(clock.instant());jobs.put(job.id,job);
        try {job.future=executor.submit(()->run(job,request));}
        catch(RejectedExecutionException e){jobs.remove(job.id);throw new Busy();}
        return job.snapshot();
    }
    private void run(Job job,SimpleCommuteService.Request request){
        try {
            var result=service.assess(request,(done,total)->{job.total=total;job.completed=done;},()->job.cancelled);
            if(!job.cancelled){job.result=result;job.expires=clock.instant().plusSeconds(120);job.status="complete";}
        }catch(CancellationException e){job.status="cancelled";}
        catch(RuntimeException e){
            if(!job.cancelled){
                job.error=ApiError.of("check_failed","The check could not finish. Please retry.","本次检查未能完成，请重试。");
                job.expires=clock.instant().plusSeconds(120);job.status="failed";
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Commute check failed ({})",e.getClass().getSimpleName());
            }
        }
    }
    public synchronized Optional<Snapshot> find(String id){cleanup();return Optional.ofNullable(jobs.get(id)).map(Job::snapshot);}
    public synchronized void cancel(String id){Job job=jobs.remove(id);if(job!=null){job.cancelled=true;job.status="cancelled";if(job.future!=null)job.future.cancel(true);}}
    @Scheduled(fixedDelay=30_000)
    public synchronized void cleanup(){
        Instant now=clock.instant();
        for(String id:new ArrayList<>(jobs.keySet()))if(!jobs.get(id).expires.isAfter(now))cancel(id);
    }
    @PreDestroy public synchronized void close(){for(String id:new ArrayList<>(jobs.keySet()))cancel(id);executor.shutdownNow();}
}
