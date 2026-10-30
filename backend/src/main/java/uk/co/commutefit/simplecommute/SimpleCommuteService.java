package uk.co.commutefit.simplecommute;

import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.CancellationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.assessment.AssessmentTimePolicy;
import uk.co.commutefit.assessment.AssessmentTimeBoundaryException;
import uk.co.commutefit.common.*;
import uk.co.commutefit.journey.lookup.JourneyLookupRequest;
import uk.co.commutefit.journey.lookup.JourneyLookupService;

@Service
public class SimpleCommuteService {
    public record Shift(String label, String start, String end, List<DayOfWeek> days, Integer cycleWeek) {}
    public record Window(String from, String until, List<DayOfWeek> days) {}
    public record Request(String originPostcode, String workPostcode, Integer maxOneWayMinutes,
                          List<String> referenceTypes, List<Shift> shifts, Window availability, LocalDate rotaStartDate, Integer rotaWeeks) {
        public Request(String originPostcode,String workPostcode,Integer maxOneWayMinutes,List<String> referenceTypes,List<Shift> shifts,Window availability,LocalDate rotaStartDate){
            this(originPostcode,workPostcode,maxOneWayMinutes,referenceTypes,shifts,availability,rotaStartDate,null);
        }
    }
    public record Route(LocalDateTime departure, LocalDateTime arrival, Integer durationMinutes,
                        int transfers, List<JourneyLeg> legs) {}
    public record Attempt(LocalDateTime targetTime, String status, String reason) {}
    public record Direction(String status, String reason, LocalDateTime targetTime, Route route, List<Attempt> attempts) {}
    public record Check(String type, String label, String status, String availability, Direction outbound, Direction returning) {}
    public record Day(LocalDate date, List<Check> checks) {}
    public record Scope(LocalDate date, String type, String label) {}
    public record Issue(LocalDate date, String type, String label, String direction, String status, String reason) {}
    public record Result(String status, String mode, String originPostcode, String workPostcode, int maxOneWayMinutes,
                         LocalDate scopeStart, LocalDate scopeEnd, List<Day> days, List<Scope> positiveScopes,
                         List<Issue> issues, int queriedPoints, int reusedPoints, String timezone, Instant queriedAt) {}
    private final JourneyLookupService journeys;
    private final Clock clock;
    private final long budgetNanos;
    @Autowired public SimpleCommuteService(JourneyLookupService journeys) {
        this(journeys, Clock.systemUTC());
    }
    SimpleCommuteService(JourneyLookupService journeys, Clock clock) { this(journeys, clock, 180_000_000_000L); }
    SimpleCommuteService(JourneyLookupService journeys, Clock clock, long budgetNanos) {
        this.journeys=journeys;this.clock=clock;this.budgetNanos=budgetNanos;
    }
    public Request validate(Request input) {
        if(input==null)throw new IllegalArgumentException("Enter both postcodes.");
        String origin=postcode(input.originPostcode()),work=postcode(input.workPostcode());
        if(input.maxOneWayMinutes()==null||input.maxOneWayMinutes()<1||input.maxOneWayMinutes()>1440)
            throw new IllegalArgumentException("Use a limit of 1–1440 minutes.");
        List<String> types=input.referenceTypes()==null?List.of():input.referenceTypes();
        if(types.stream().anyMatch(t->t==null||!List.of("early","day","night").contains(t)))throw new IllegalArgumentException("Unknown reference type.");
        types=List.of("early","day","night").stream().filter(types::contains).toList();
        List<Shift> shifts=new ArrayList<>();
        if(input.shifts()!=null)for(Shift s:input.shifts()){
            if(s==null)throw new IllegalArgumentException("Invalid shift.");
            time(s.start());time(s.end());
            if(s.start().equals(s.end()))throw new IllegalArgumentException("Shift start and end must differ.");
            if(s.cycleWeek()!=null&&(s.cycleWeek()<1||s.cycleWeek()>8))throw new IllegalArgumentException("Invalid rota week.");
            if(s.label()!=null&&s.label().length()>100)throw new IllegalArgumentException("Shift label is too long.");
            shifts.add(new Shift(s.label()==null||s.label().isBlank()?"Shift "+(shifts.size()+1):s.label(),s.start(),s.end(),days(s.days()),s.cycleWeek()));
        }
        if(shifts.size()>32||shifts.isEmpty()&&types.isEmpty())throw new IllegalArgumentException("Choose references or add shift times.");
        if(shifts.stream().anyMatch(s->s.cycleWeek()!=null)&&(input.rotaStartDate()==null||input.rotaStartDate().getDayOfWeek()!=DayOfWeek.MONDAY))
            throw new IllegalArgumentException("Confirm Monday of rota Week 1.");
        int maxWeek=shifts.stream().map(Shift::cycleWeek).filter(Objects::nonNull).max(Integer::compareTo).orElse(1);
        int rotaWeeks=input.rotaWeeks()==null?maxWeek:input.rotaWeeks();
        if(rotaWeeks<maxWeek||rotaWeeks>8||rotaWeeks<1)throw new IllegalArgumentException("Invalid rota length.");
        Window window=input.availability();
        if(window!=null){time(window.from());time(window.until());if(window.from().equals(window.until()))throw new IllegalArgumentException("Availability boundaries must differ.");
            window=new Window(window.from(),window.until(),days(window.days()));}
        return new Request(origin,work,input.maxOneWayMinutes(),types,List.copyOf(shifts),window,input.rotaStartDate(),rotaWeeks);
    }
    public Result assess(Request input) { return assess(input,(done,total)->{},()->false); }
    public Result assess(Request input, BiConsumer<Integer,Integer> progress, BooleanSupplier cancelled) {
        Request request=validate(input);
        Context ctx=new Context(request,cancelled);
        LocalDate first=ctx.now.toLocalDate();
        boolean exact=!request.shifts().isEmpty();
        List<LocalDate> dates=java.util.stream.IntStream.range(0,7).mapToObj(first::plusDays).toList();
        int cycle=request.rotaWeeks();
        int total=exact?dates.stream().mapToInt(d->(int)request.shifts().stream().filter(s->applies(s,d,request.rotaStartDate(),cycle)).count()).sum():7*request.referenceTypes().size();
        int done=0;progress.accept(0,total);
        List<Day> resultDays=new ArrayList<>();List<Scope> positives=new ArrayList<>();List<Issue> issues=new ArrayList<>();
        Map<Integer,List<Check>> exactGroups=new LinkedHashMap<>();
        for(LocalDate date:dates){
            ctx.guard();List<Check> checks=new ArrayList<>();
            if(exact){
                for(int index=0;index<request.shifts().size();index++){
                    Shift shift=request.shifts().get(index);if(!applies(shift,date,request.rotaStartDate(),cycle))continue;
                    ctx.guard();LocalDate endDate=time(shift.end()).isBefore(time(shift.start()))?date.plusDays(1):date;
                    Direction out=ctx.search(date,List.of(shift.start()),false,false);
                    Direction back=ctx.search(endDate,List.of(shift.end()),true,false);
                    String availability=availableShift(request.availability(),date.atTime(time(shift.start())),endDate.atTime(time(shift.end())));
                    Check c=check("exact",shift.label(),availability,out,back);checks.add(c);
                    exactGroups.computeIfAbsent(index,k->new ArrayList<>()).add(c);progress.accept(++done,total);
                }
            }else for(String type:request.referenceTypes()){
                ctx.guard();
                List<String> times=switch(type){case "early"->List.of("02:30","03:30","04:30","05:30","06:30");case "night"->List.of("22:30","21:30","20:30");default->List.of("06:30","07:30");};
                Direction out=ctx.search(date,times,false,true);
                Direction back=type.equals("day")?ctx.search(date,List.of("17:10"),true,true):ctx.morning(type.equals("night")?date.plusDays(1):date);
                checks.add(check(type,type,"not-set",out,back));progress.accept(++done,total);
            }
            for(Check c:checks){
                if(c.status().equals("suitable"))positives.add(new Scope(date,c.type(),c.label()));
                if(c.availability().equals("outside"))issues.add(new Issue(date,c.type(),c.label(),"schedule","outside-availability","outside-window"));
                if(!c.outbound().status().equals("within-limit"))issues.add(new Issue(date,c.type(),c.label(),"outbound",c.outbound().status(),c.outbound().reason()));
                if(!c.returning().status().equals("within-limit"))issues.add(new Issue(date,c.type(),c.label(),"return",c.returning().status(),c.returning().reason()));
            }
            resultDays.add(new Day(date,List.copyOf(checks)));
        }
        var all=resultDays.stream().flatMap(d->d.checks().stream()).toList();
        boolean failed=all.stream().anyMatch(c->c.status().equals("unsuitable"));
        boolean unknown=all.isEmpty()||all.stream().anyMatch(c->c.status().equals("needs-review"));
        if(exact){
            failed=false;unknown=all.isEmpty();
            for(var entry:exactGroups.entrySet()){
                var group=entry.getValue();
                if(request.shifts().get(entry.getKey()).days().isEmpty()){
                    boolean possible=group.stream().anyMatch(c->c.status().equals("suitable"));
                    if(!possible){if(group.stream().anyMatch(c->c.status().equals("needs-review")))unknown=true;else failed=true;}
                }else{
                    if(group.stream().anyMatch(c->c.status().equals("unsuitable")))failed=true;
                    if(group.stream().anyMatch(c->c.status().equals("needs-review")))unknown=true;
                }
            }
        }
        String status=exact?(failed?"unsuitable":unknown?"needs-review":"suitable"):!positives.isEmpty()?"suitable":unknown?"needs-review":"unsuitable";
        return new Result(status,exact?"exact":"reference",request.originPostcode(),request.workPostcode(),request.maxOneWayMinutes(),
            first,first.plusDays(6),List.copyOf(resultDays),List.copyOf(positives),List.copyOf(issues),ctx.queried,ctx.reused,
            AssessmentTimePolicy.LONDON.getId(),ctx.queriedAt);
    }
    private static Check check(String type,String label,String availability,Direction out,Direction back){
        boolean failed=availability.equals("outside")||List.of("over-limit","no-route").contains(out.status())||List.of("over-limit","no-route").contains(back.status());
        boolean fits=out.status().equals("within-limit")&&back.status().equals("within-limit")&&!availability.equals("outside");
        return new Check(type,label,fits?"suitable":failed?"unsuitable":"needs-review",availability,out,back);
    }
    private static boolean applies(Shift shift,LocalDate date,LocalDate anchor,int cycle){
        if(!shift.days().isEmpty()&&!shift.days().contains(date.getDayOfWeek()))return false;
        if(shift.cycleWeek()==null)return true;
        if(date.isBefore(anchor))return false;
        long week=java.time.temporal.ChronoUnit.WEEKS.between(anchor,date);
        return Math.floorMod(week,cycle)+1==shift.cycleWeek();
    }
    private record Node(LocalDateTime target,boolean returning) {}
    private record Point(String status,String reason,Route route) {}
    private final class Context {
        final Request request;final BooleanSupplier cancelled;
        final Instant queriedAt=clock.instant();
        final LocalDateTime now=LocalDateTime.ofInstant(queriedAt,AssessmentTimePolicy.LONDON);
        final long started=System.nanoTime();
        final Map<Node,Point> points=new HashMap<>();final Map<LocalDate,Direction> mornings=new HashMap<>();
        int queried=0,reused=0,consecutiveErrors=0;
        String halted=null;
        Context(Request request,BooleanSupplier cancelled){this.request=request;this.cancelled=cancelled;}
        void guard(){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new CancellationException();}
        Direction morning(LocalDate date){
            guard();if(mornings.containsKey(date)){reused++;return mornings.get(date);}
            Direction result=search(date,List.of("06:10","07:10","08:10","09:10","10:10"),true,true);
            mornings.put(date,result);return result;
        }
        Direction search(LocalDate date,List<String> times,boolean returning,boolean filterWindow){
            List<Attempt> attempts=new ArrayList<>();String unknown=null;Point bestOver=null;LocalDateTime overTarget=null;
            for(String value:times){
                guard();LocalDateTime target=date.atTime(time(value));
                Point p=filterWindow&&!availablePoint(request.availability(),target)?new Point("unknown","outside-window",null):point(new Node(target,returning));
                attempts.add(new Attempt(target,p.status(),p.reason()));
                if(p.status().equals("within-limit"))return new Direction(p.status(),p.reason(),target,p.route(),List.copyOf(attempts));
                if(p.status().equals("unknown")){
                    if(unknown==null)unknown=p.reason();
                    if(List.of("service-unavailable","rate-limited","time-budget","location-unresolved").contains(p.reason()))break;
                }
                if(p.status().equals("over-limit")&&(bestOver==null||p.route().durationMinutes()<bestOver.route().durationMinutes())){bestOver=p;overTarget=target;}
            }
            if(unknown!=null)return new Direction("unknown",unknown,null,null,List.copyOf(attempts));
            if(bestOver!=null)return new Direction("over-limit","over-limit",overTarget,bestOver.route(),List.copyOf(attempts));
            return new Direction("no-route","no-route",null,null,List.copyOf(attempts));
        }
        Point point(Node node){
            guard();if(points.containsKey(node)){reused++;return points.get(node);}
            Point p;
            if(System.nanoTime()-started>=budgetNanos)p=new Point("unknown","time-budget",null);
            else if(halted!=null)p=new Point("unknown",halted,null);
            else if(node.target().isBefore(now))p=new Point("unknown","past-time",null);
            else {
                String from=node.returning()?request.workPostcode():request.originPostcode();
                String to=node.returning()?request.originPostcode():request.workPostcode();
                try {
                    queried++;
                    var result=journeys.lookup(new JourneyLookupRequest(from,to,node.returning()?"leave":"arrive",node.target().toLocalDate(),node.target().toLocalTime().toString(),null,null));
                    consecutiveErrors=0;p=select(result,node,request.maxOneWayMinutes(),now);
                }catch(RateLimitedException e){halted="rate-limited";p=new Point("unknown",halted,null);}
                catch(ProviderUnavailableException e){if(++consecutiveErrors>=3)halted="service-unavailable";p=new Point("unknown","service-unavailable",null);}
                catch(LocationDisambiguationException|CandidateTokenExpiredException e){halted="location-unresolved";p=new Point("unknown",halted,null);}
                catch(AssessmentTimeBoundaryException e){p=new Point("unknown","time-unavailable",null);}
            }
            points.put(node,p);return p;
        }
    }
    private static Point select(JourneyLookupService.Result result,Node node,int limit,LocalDateTime now){
        if(result==null||result.routes()==null)return new Point("unknown","missing-data",null);
        Route best=null;boolean missing=false;
        for(var r:result.routes()){
            if(r.durationMinutes()==null||r.durationMinutes()<0||r.departure()==null||r.arrival()==null||r.arrival().isBefore(r.departure())){missing=true;continue;}
            if(r.departure().isBefore(now)){missing=true;continue;}
            if(node.returning()?r.departure().isBefore(node.target()):r.arrival().isAfter(node.target()))continue;
            Route candidate=new Route(r.departure(),r.arrival(),r.durationMinutes(),r.transfers(),r.legs()==null?List.of():r.legs());
            if(best==null||candidate.durationMinutes()<best.durationMinutes())best=candidate;
        }
        if(best!=null&&best.durationMinutes()<=limit)return new Point("within-limit","matched",best);
        if(missing)return new Point("unknown","missing-data",null);
        if(best!=null)return new Point("over-limit","over-limit",best);
        return new Point("no-route","no-route",null);
    }
    private static boolean availablePoint(Window window,LocalDateTime target){
        if(window==null)return true;
        return intervals(window,target.toLocalDate()).stream().anyMatch(i->!target.isBefore(i[0])&&!target.isAfter(i[1]));
    }
    private static String availableShift(Window window,LocalDateTime start,LocalDateTime end){
        if(window==null)return "not-set";
        return intervals(window,start.toLocalDate()).stream().anyMatch(i->!start.isBefore(i[0])&&!end.isAfter(i[1]))?"fits":"outside";
    }
    private static List<LocalDateTime[]> intervals(Window window,LocalDate date){
        List<LocalDateTime[]> intervals=new ArrayList<>();
        for(LocalDate d:List.of(date.minusDays(1),date))if(window.days().isEmpty()||window.days().contains(d.getDayOfWeek())){
            LocalDateTime from=d.atTime(time(window.from()));LocalDateTime until=d.atTime(time(window.until()));
            if(!until.isAfter(from))until=until.plusDays(1);intervals.add(new LocalDateTime[]{from,until});
        }
        return intervals;
    }
    static String postcode(String value){
        String compact=value==null?"":value.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        if(!compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}"))throw new IllegalArgumentException("Enter a complete UK postcode.");
        return compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3);
    }
    private static LocalTime time(String value){
        if(value==null||!value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d"))throw new IllegalArgumentException("Use HH:mm times.");
        return LocalTime.parse(value);
    }
    private static List<DayOfWeek> days(List<DayOfWeek> value){
        if(value==null)return List.of();if(value.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Invalid weekday.");
        return value.stream().distinct().toList();
    }
}
