package uk.co.commutefit.jobextraction;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;
import tools.jackson.databind.*;
import org.springframework.stereotype.Component;
import uk.co.commutefit.jobextraction.JobExtractionService.*;

@Component
public class JobDocumentParser {
    public record Parsed(String title,String jobStatus,List<Location> locations,List<Schedule> schedules,List<String> warnings) {}
    private static final Pattern POSTCODE=Pattern.compile("(?i)\\b(GIR\\s*0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2})\\b");
    private static final Pattern RANGE=Pattern.compile("(?i)(?<![\\d:])(\\d{1,2}(?::[0-5]\\d(?::00)?)?\\s*(?:am|pm)?)\\s*(?:-|–|—|to|until)\\s*(\\d{1,2}(?::[0-5]\\d(?::00)?)?\\s*(?:am|pm)?)(?![\\d:])");
    private static final String[] DAY_NAMES={"mon(?:day)?","tue(?:sday)?","wed(?:nesday)?","thu(?:rsday)?","fri(?:day)?","sat(?:urday)?","sun(?:day)?"};
    private final ObjectMapper mapper=new ObjectMapper();

    public Parsed parse(String content,boolean html) {
        List<Location> locations=new ArrayList<>();
        List<Schedule> schedules=new ArrayList<>();
        List<String> warnings=new ArrayList<>();
        String title=null, jobStatus="unknown", text=content;
        if(html){
            List<JsonNode> jobs=new ArrayList<>();
            Matcher scripts=Pattern.compile("(?is)<script\\b[^>]*type\\s*=\\s*['\"]application/ld\\+json['\"][^>]*>(.*?)</script\\s*>").matcher(content);
            while(scripts.find())try{collectJobs(mapper.readTree(scripts.group(1)),jobs);}catch(Exception ex){warnings.add("Malformed structured data was ignored.");}
            if(jobs.size()>1)return new Parsed(null,"unknown",List.of(),List.of(),List.of("Multiple job postings were found. Paste the intended job description."));
            String description="";
            if(jobs.size()==1){
                JsonNode job=jobs.getFirst(); title=value(job,"title");
                JsonNode jl=job.path("jobLocation");
                if(jl.isArray()&&jl.size()>1)warnings.add("Multiple workplace locations were found; confirm the intended site.");
                if(jl.isArray()){for(JsonNode loc:jl)structuredLocation(loc,locations);}else structuredLocation(jl,locations);
                description=Objects.requireNonNullElse(value(job,"description"),"");
                String hours=value(job,"workHours");
                if(hours!=null)parseSchedules("Working hours: "+hours,"JobPosting.workHours",schedules);
                String through=value(job,"validThrough");
                if(through!=null)try{if(LocalDate.parse(through.substring(0,10)).isBefore(LocalDate.now(ZoneId.of("Europe/London"))))jobStatus="expired";}catch(Exception ignored){}
                if("TELECOMMUTE".equalsIgnoreCase(value(job,"jobLocationType")))warnings.add("This job mentions remote work; confirm whether a physical commute is required.");
            }
            Matcher main=Pattern.compile("(?is)<(?:main|article)\\b[^>]*>(.*?)</(?:main|article)\\s*>").matcher(content);
            String body=main.find()?main.group(1):content;
            text=plain(body);
            if(!description.isBlank())text=plain(description)+"\n"+text;
            if(title==null){Matcher h=Pattern.compile("(?is)<h1\\b[^>]*>(.*?)</h1>").matcher(body);if(h.find())title=plain(h.group(1)).trim();}
        }
        String lower=text.toLowerCase(Locale.ROOT);
        if(lower.contains("job has expired")||lower.contains("job has been closed")||lower.contains("no longer accepting applications")){jobStatus="expired";warnings.add("This job may have expired. Confirm that it is still open.");}
        if(html && (lower.contains("verify you are human")||lower.contains("access denied")||lower.contains("enable javascript to continue")||lower.contains("sign in to continue")||text.trim().length()<30)) {
            warnings.add("The page may be restricted or require JavaScript. Paste the job description.");
            if(jobsAbsent(locations,schedules) && title==null)return new Parsed(null,jobStatus,List.of(),List.of(),warnings);
        }
        if(locations.isEmpty())textLocations(text,locations);
        if(locations.isEmpty())namedLocations(text,locations);
        parseSchedules(text,"description",schedules);
        return new Parsed(title,jobStatus,dedupLocations(locations),dedupSchedules(schedules),List.copyOf(new LinkedHashSet<>(warnings)));
    }
    private static boolean jobsAbsent(List<Location> locations,List<Schedule> schedules){return locations.isEmpty()&&schedules.isEmpty();}

    private static String value(JsonNode n,String key){JsonNode v=n.path(key);return v.isTextual()?v.asText():null;}
    private static void collectJobs(JsonNode n,List<JsonNode> out){
        if(n==null)return;
        if(n.isArray()){for(JsonNode item:n)collectJobs(item,out);return;}
        if(!n.isObject())return;
        JsonNode type=n.path("@type");
        boolean job=type.isTextual()&&type.asText().equals("JobPosting");
        if(type.isArray())for(JsonNode t:type)job|=t.asText().equals("JobPosting");
        if(job){out.add(n);return;}
        if(n.has("@graph"))collectJobs(n.get("@graph"),out);
    }
    private static void structuredLocation(JsonNode n,List<Location> locations){
        if(n==null||!n.isObject())return;
        JsonNode a=n.path("address");
        if(a.isTextual()){textLocations(a.asText(),locations);return;}
        String pc=value(a,"postalCode"),street=value(a,"streetAddress"),city=value(a,"addressLocality");
        if(pc!=null&&POSTCODE.matcher(pc).matches())locations.add(new Location(normalizePostcode(pc),pc,"JobPosting.jobLocation.address.postalCode"));
        else if(street!=null&&city!=null)locations.add(new Location(street+", "+city,street+", "+city,"JobPosting.jobLocation.address"));
    }
    private static List<Location> dedupLocations(List<Location> values){
        Map<String,Location> out=new LinkedHashMap<>();for(Location l:values)out.putIfAbsent(l.value().toLowerCase(Locale.ROOT),l);return List.copyOf(out.values());
    }
    private static void textLocations(String text,List<Location> out){
        for(String line:text.split("\\R")){
            if(line.toLowerCase(Locale.ROOT).matches(".*(registered office|head office|headquarters|privacy policy).*"))continue;
            Matcher m=POSTCODE.matcher(line);
            while(m.find())out.add(new Location(normalizePostcode(m.group(1)),clip(line),"description"));
        }
    }
    static String normalizePostcode(String s){String v=s.toUpperCase(Locale.ROOT).replaceAll("\\s","");return v.substring(0,v.length()-3)+" "+v.substring(v.length()-3);}
    private static void namedLocations(String text,List<Location> out){
        String[] lines=text.split("\\R");String englishStore=null,englishCity=null,localStore=null,localCity=null;
        for(int i=0;i<lines.length;i++){
            String line=lines[i].trim();
            Matcher label=Pattern.compile("(?i)^(store|shop|work location|location|店铺|門店|门店|工作地点|地点)(?:\\s*[:：]\\s*(.+)|\\s*)$").matcher(line);
            if(!label.matches())continue;
            String key=label.group(1).toLowerCase(Locale.ROOT),value=Objects.requireNonNullElse(label.group(2),"").trim();
            if(value.isEmpty()&&i+1<lines.length)value=lines[i+1].trim();
            if(value.isBlank()||value.length()>150)continue;
            if(key.equals("store")||key.equals("shop"))englishStore=value;
            else if(key.equals("location")||key.equals("work location"))englishCity=value;
            else if(Set.of("店铺","門店","门店").contains(key))localStore=value;
            else localCity=value;
        }
        String store=englishStore!=null?englishStore:localStore,city=englishCity!=null?englishCity:localCity;
        if(store!=null){String candidate=city==null?store:store+", "+city;out.add(new Location(candidate,candidate,"description.store"));}
    }
    private static String clip(String s){String v=s.trim();return v.length()>400?v.substring(0,400):v;}

    static List<DayOfWeek> days(String s){
        s=s.toLowerCase(Locale.ROOT);
        LinkedHashSet<DayOfWeek> result=new LinkedHashSet<>();
        for(int i=0;i<7;i++)for(int j=0;j<7;j++)
            if(Pattern.compile("\\b"+DAY_NAMES[i]+"\\s*(?:-|–|to)\\s*"+DAY_NAMES[j]+"\\b").matcher(s).find())
                for(int k=i;;k=(k+1)%7){result.add(DayOfWeek.of(k+1));if(k==j)break;}
        for(int i=0;i<7;i++)if(Pattern.compile("\\b"+DAY_NAMES[i]+"\\b").matcher(s).find())result.add(DayOfWeek.of(i+1));
        String[] chinese={"一","二","三","四","五","六","日天"};
        for(int i=0;i<7;i++)if(Pattern.compile("(?:周|星期|礼拜)["+chinese[i]+"]").matcher(s).find())result.add(DayOfWeek.of(i+1));
        Matcher cnRange=Pattern.compile("(?:周|星期|礼拜)([一二三四五六日天])\\s*(?:至|到|[-–])\\s*(?:周|星期|礼拜)?([一二三四五六日天])").matcher(s);
        while(cnRange.find()){int a="一二三四五六日".indexOf(cnRange.group(1).replace('天','日')),b="一二三四五六日".indexOf(cnRange.group(2).replace('天','日'));for(int k=a;;k=(k+1)%7){result.add(DayOfWeek.of(k+1));if(k==b)break;}}
        if(s.contains("weekdays"))for(int i=1;i<=5;i++)result.add(DayOfWeek.of(i));
        if(s.contains("weekends")){result.add(DayOfWeek.SATURDAY);result.add(DayOfWeek.SUNDAY);}
        if(s.contains("周末")){result.add(DayOfWeek.SATURDAY);result.add(DayOfWeek.SUNDAY);}
        return result.stream().sorted().toList();
    }
    private static String kind(String s){
        String v=s.toLowerCase(Locale.ROOT);
        if(v.contains("availability window")||v.contains("available to work")||v.contains("availability:")||v.contains("可排班")||v.contains("可用时间"))return "availability-window";
        if(v.contains("opening hours")||v.contains("store opens")||v.contains("business hours")||v.contains("营业时间"))return "opening-hours";
        if(v.contains("shift")||v.contains("working hours")||v.contains("you will work")||v.contains("hours of work")||v.contains("工作模式")||v.contains("工作时间")||v.contains("班次"))return "exact-shift";
        return null;
    }
    private static void parseSchedules(String text,String path,List<Schedule> out){
        String context=null;Integer cycleWeek=null;
        for(String raw:text.split("\\R")){
            String line=raw.trim();if(line.isEmpty())continue;
            // Week headings are a rota label, not a calendar-date assertion.
            if(line.matches("(?i)^week\\s*[12]\\s*[:：]?$")||line.matches("^第[一二12]周\\s*[:：]?$")){
                cycleWeek=line.matches("(?i).*1.*")||line.contains("一")?1:2;context="exact-shift";continue;
            }
            line=line.replaceAll("(\\d{1,2}:[0-5]\\d(?::00)?)\\s*\\|\\s*(\\d{1,2}:[0-5]\\d(?::00)?)","$1-$2");
            String specific=kind(line);
            if(specific!=null)context=specific;
            // New heading terminates a preceding table/window context.
            if(line.length()<80&&line.toLowerCase(Locale.ROOT).matches(".*(responsibilities|benefits|qualifications|about you|what you.need).*"))context=null;
            Matcher m=RANGE.matcher(line);boolean found=false;
            while(m.find()){
                String a=m.group(1).trim(),b=m.group(2).trim();
                if(!(a.contains(":")||b.contains(":")||a.matches("(?i).*([ap]m).*")||b.matches("(?i).*([ap]m).*")))continue;
                if(a.matches("(?i).*([ap]m).*")&&!b.matches("(?i).*([ap]m).*")&&!b.contains(":"))continue;
                if(b.matches("(?i).*([ap]m).*")&&!a.matches("(?i).*([ap]m).*")&&!a.contains(":"))continue;
                String start=time(a,false),end=time(b,true);if(start==null||end==null||start.equals(end))continue;
                List<DayOfWeek> d=days(line);
                if(line.toLowerCase(Locale.ROOT).matches(".*(?:\\d+|five|four|three)\\s+days?\\s+(?:from|out of|of)\\s+(?:7|seven).*"))d=List.of();
                if(line.matches(".*(?:每周|一周).*?(?:[一二三四五六七0-7])天.*")||line.matches(".*[七7]天.*选.*"))d=List.of();
                if(line.toLowerCase(Locale.ROOT).contains("including weekends")||line.contains("包括周末")||line.contains("含周末"))d=List.of();
                String k=specific!=null?specific:context!=null?context:!d.isEmpty()?"exact-shift":"unknown";
                if(line.toLowerCase(Locale.ROOT).contains("premium")||line.toLowerCase(Locale.ROOT).contains("pay rate"))k="unknown";
                if(line.toLowerCase(Locale.ROOT).matches(".*(optional|occasionally|as required|may vary).*"))k="unknown";
                out.add(new Schedule(k,d,start,end,b.startsWith("24:")||end.compareTo(start)<0,clip(line),path,cycleWeek));found=true;
            }
            if(!found&&line.toLowerCase(Locale.ROOT).matches(".*\\b\\d+(?:\\.\\d+)?\\s*(?:hours|hrs)\\s*(?:per|a|/)\\s*week.*"))
                out.add(new Schedule("weekly-hours",List.of(),null,null,false,clip(line),path,cycleWeek));
            if(!found&&line.toLowerCase(Locale.ROOT).matches(".*\\b(early|night|late|rotating)\\s+shifts?\\b.*"))
                out.add(new Schedule("vague-shift",days(line),null,null,false,clip(line),path,cycleWeek));
        }
    }
    private static List<Schedule> dedupSchedules(List<Schedule> values){
        Map<String,Schedule> out=new LinkedHashMap<>();
        for(Schedule s:values)out.putIfAbsent(s.kind()+s.days()+s.start()+s.end()+s.endsNextDay()+s.cycleWeek(),s);
        return List.copyOf(out.values());
    }
    static String time(String value,boolean end){
        Matcher m=Pattern.compile("(?i)(\\d{1,2})(?::([0-5]\\d)(?::00)?)?\\s*(am|pm)?").matcher(value.trim());
        if(!m.matches())return null;
        int h=Integer.parseInt(m.group(1)),min=m.group(2)==null?0:Integer.parseInt(m.group(2));
        if(m.group(3)!=null){if(h<1||h>12)return null;h=h%12+(m.group(3).equalsIgnoreCase("pm")?12:0);}
        if(h==24)return end&&min==0?"00:00":null;
        if(h>23)return null;
        return String.format(Locale.ROOT,"%02d:%02d",h,min);
    }
    static String plain(String html){
        String clean=html.replaceAll("(?is)<(script|style|noscript)\\b[^>]*>.*?</\\1\\s*>"," ");
        StringBuilder out=new StringBuilder();
        try{new ParserDelegator().parse(new StringReader(clean),new HTMLEditorKit.ParserCallback(){
            public void handleText(char[] data,int pos){out.append(data).append(' ');}
            public void handleEndTag(HTML.Tag tag,int pos){
                if(tag==HTML.Tag.TD||tag==HTML.Tag.TH)out.append(" | ");
                if(Set.of("p","div","tr","h1","h2","h3","h4","li","table").contains(tag.toString()))out.append('\n');
            }
            public void handleSimpleTag(HTML.Tag tag,MutableAttributeSet attrs,int pos){if(tag==HTML.Tag.BR)out.append('\n');}
        },true);}catch(IOException ignored){}
        return out.toString().replaceAll("[\\t\\x0B\\f\\r ]+"," ").replaceAll("\\n\\s*\\n","\n");
    }
}
