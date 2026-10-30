package uk.co.commutefit.location;

import java.util.*;
import java.util.concurrent.*;
import java.text.Normalizer;
import org.springframework.stereotype.Service;
import uk.co.commutefit.location.LocationSearchService.Candidate;
import uk.co.commutefit.location.LocationSearchService.Result;

@Service
public class AreaConsensusService {
    public static final double LIMIT_METERS=0.6*1609.344;
    // Independent data families: Geoapify (OSM-family, strong brand entity), ArcGIS (Esri), TomTom (proprietary).
    // LocationIQ is also OSM/Nominatim and is kept for direct/postcode lookups but excluded to avoid a correlated vote.
    private static final List<String> PROVIDERS=List.of("geoapify","arcgis","tomtom");
    private final LocationSearchService search;
    public AreaConsensusService(LocationSearchService search){this.search=search;}
    public record ProviderResult(String provider,Result result){}
    public record Excluded(Candidate candidate,String reason){}
    public record Area(int support,List<String> sources,List<Candidate> members,Candidate representative,double maxDistanceMeters){}
    public record Assessment(String status,Candidate selectedCandidate,double maxDistanceMeters,List<ProviderResult> providers,List<Area> areas,List<Excluded> excluded,List<Excluded> corroborated,String message){}
    private static final Set<String> FALLBACK_IGNORED=Set.of("simply","food","foodhall","superstore","express","extra","local","store","stores","shop","station","cafe","coffee","restaurant","the","in","at","uk","gb","united","kingdom","england","london","greater");
    public Assessment assess(String raw) {
        String query=raw==null?"":raw.trim().replaceAll("\\s+"," ");
        if(query.length()<3||query.length()>120)throw new IllegalArgumentException("Enter 3–120 characters.");
        List<ProviderResult> results=new ArrayList<>();
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=PROVIDERS.stream().map(p->executor.submit(()->new ProviderResult(p,safeSearch(p,query)))).toList();
            for(var future:futures)try {results.add(future.get());}
            catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException("Search interrupted",ex);}
            catch(ExecutionException ex){throw new IllegalStateException("Search failed",ex);}
        }
        List<Candidate> accepted=new ArrayList<>();List<Excluded> excluded=new ArrayList<>();
        for(var provider:results)for(var c:provider.result().candidates().stream().limit(8).toList()){
            String reason=rejection(query,c);
            if(reason==null)accepted.add(c);else excluded.add(new Excluded(c,reason));
        }
        // Only original text-qualified anchors may corroborate incomplete addresses.
        var anchors=List.copyOf(accepted);
        List<Excluded> corroborated=new ArrayList<>();
        for(var item:List.copyOf(excluded)) {
            var c=item.candidate();
            if(item.reason().startsWith("名称或区域")&&brandMatches(query,c)
                &&anchors.stream().anyMatch(a->!a.source().equals(c.source())&&brandMatches(query,a)&&withinLimit(distance(a,c)))) {
                accepted.add(c);excluded.remove(item);
                corroborated.add(new Excluded(c,"名称吻合，且与另一来源通过文本核对的候选相距不超过 0.6 英里；以坐标补充不完整地址"));
            }
        }
        var areas=group(accepted);
        var textMatches=results.stream().flatMap(p->p.result().candidates().stream().limit(8))
            .filter(c->textFallbackRejection(query,c)==null).distinct().toList();
        Candidate textFallback=singleTextAreaRepresentative(textMatches);
        var ambiguousCandidates=new ArrayList<Candidate>();
        if(!areas.isEmpty()) {
            var leading=areas.getFirst();
            // A lone distant result is an outlier, not a competing location consensus.
            // Only another cross-source cluster makes the location genuinely ambiguous.
            for(var area:areas.stream().skip(1).toList()) {
                if(area.support()>=2&&!withinLimit(distance(leading.representative(),area.representative()))) {
                    ambiguousCandidates.addAll(area.members());
                    for(var c:area.members())if(excluded.stream().noneMatch(e->e.candidate().equals(c)))
                        excluded.add(new Excluded(c,"另有多个来源支持相距超过 0.6 英里的门店区域；需要更具体地址来区分"));
                }
            }
            for(var c:accepted.stream().distinct().toList())if(!leading.members().contains(c)
                &&!withinLimit(distance(leading.representative(),c))
                &&areas.stream().skip(1).noneMatch(a->a.members().contains(c)))
                excluded.add(new Excluded(c,"单个远处候选未获其他来源支持，不参与主要区域判断"));
        }
        boolean ambiguous=!ambiguousCandidates.isEmpty();
        String status;Candidate selected;String message;
        if(!areas.isEmpty()&&!ambiguous) {
            status="supported";selected=areas.getFirst().representative();
            message=excluded.stream().anyMatch(e->e.reason().contains("单个远处候选"))
                ?"已采用来源支持最多且坐标集中的区域，用于附近通勤初筛。另有未获交叉来源支持的远处候选，已列出但不否决此区域。"
                :"已自动采用来源支持最多的区域；支持数相同时采用坐标最集中的区域，用于附近通勤初筛。";
        } else if(textFallback!=null) {
            status="text-match";selected=textFallback;
            message="没有形成多源坐标共识，但门店品牌与地址中的街道/区域文字匹配，且匹配候选集中在同一 0.6 英里范围；这是低置信度候选，请检查名称、地址与 postcode。";
        } else if(ambiguous||textMatches.size()>1) {
            status="ambiguous";selected=null;
            message="搜索文字匹配到相距较远的同品牌门店，或同一街道的多个分店；没有唯一安全位置，请补充 postcode、门牌或更具体区域。";
            for(var c:textMatches)if(excluded.stream().noneMatch(e->e.candidate().equals(c)))
                excluded.add(new Excluded(c,"地址文字有重合，但匹配候选分布在不同区域；不能据此自动选择分店"));
        } else {
            status="needs-confirmation";selected=null;
            message="没有形成多源位置共识，也没有唯一的地址文字匹配候选；请补充 postcode 或更具体地址。";
        }
        return new Assessment(status,selected,LIMIT_METERS,List.copyOf(results),areas,List.copyOf(excluded),List.copyOf(corroborated),message);
    }
    private Result safeSearch(String provider,String query){
        try{return search.searchProvider(provider,query);}
        catch(RuntimeException ex){return new Result("unavailable",List.of(),"该来源暂不可用，请稍后重试。",provider);}
    }
    private static String normalize(String text){
        return Normalizer.normalize(text,Normalizer.Form.NFKD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("(?<=[a-z0-9])['’](?=[a-z0-9])","")
            .replaceAll("\\bm\\s*&\\s*s\\b|\\bmarks\\s*(?:&|and)\\s*spencer\\b","marks spencer")
            .replaceAll("\\brd\\b","road").replaceAll("\\bave\\b","avenue")
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static boolean brandMatches(String query,Candidate candidate) {
        var generic=Set.of("simply","food","foodhall","superstore","express","extra","local","store","stores","shop","station","coffee","restaurant","cafe");
        var name=Arrays.stream(normalize(candidate.name()).split(" +")).filter(t->!generic.contains(t)).distinct().toList();
        String normalized=" "+normalize(query)+" ";
        return !name.isEmpty()&&name.stream().allMatch(t->normalized.contains(" "+t+" "));
    }
    static String rejection(String query,Candidate c){
        if(c.lat()==null||c.lon()==null||!LocationSearchService.validPoint(c.lat(),c.lon()))return "没有有效坐标";
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        // A full UK postcode is itself the location key. Trust the exact postcode match and do not
        // require venue-level precision/selectable: providers return postcode centroids as "locality"
        // or non-selectable, yet their coordinates are the correct work area. This gate applies only
        // when the input is itself a postcode; brand/address queries keep the venue gate below.
        if(compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")) {
            String candidatePostcode=c.postcode()==null?"":c.postcode().replaceAll("\\s","").toUpperCase(Locale.ROOT);
            return compact.equals(candidatePostcode)?null:"输入的是完整 postcode，但候选 postcode 不一致";
        }
        if(!Set.of("venue","address","postcode").contains(c.precision())||!c.selectable())return "仅区域／街道结果，或候选不可选择，不能用作门店共识票";
        // Generic connector/stop words only — no brand-specific rules. normalize() already collapses
        // "M&S" and "marks &/and spencer" to "marks spencer", so the literal "and" must not be required.
        Set<String> ignored=Set.of("and","or","of","simply","food","foodhall","store","stores","shop","the","in","at","uk","gb","united","kingdom");
        var terms=Arrays.stream(normalize(query).split(" +")).filter(t->!ignored.contains(t)).toList();
        if(terms.isEmpty())return "查询词不足以核对品牌或区域";
        var text=" "+normalize(c.name()+" "+c.address())+" ";
        return terms.stream().allMatch(t->text.contains(" "+t+" "))?null:"名称或区域未覆盖查询关键词，保留在原始结果中供确认";
    }
    private static String textFallbackRejection(String query,Candidate c) {
        if(c.lat()==null||c.lon()==null||!LocationSearchService.validPoint(c.lat(),c.lon()))return "没有有效坐标";
        if(!Set.of("venue","address","postcode").contains(c.precision())||!c.selectable())return "候选精度不足";
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        if(compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")) {
            String pc=c.postcode()==null?"":c.postcode().replaceAll("\\s","").toUpperCase(Locale.ROOT);
            return compact.equals(pc)?null:"完整 postcode 不一致";
        }
        if(!brandMatches(query,c))return "候选名称没有匹配查询品牌";
        var queryTerms=new LinkedHashSet<>(Arrays.asList(normalize(query).split(" +")));
        var brandTerms=Arrays.stream(normalize(c.name()).split(" +")).filter(t->!FALLBACK_IGNORED.contains(t)).collect(java.util.stream.Collectors.toSet());
        var terms=queryTerms.stream().filter(t->!brandTerms.contains(t)&&!FALLBACK_IGNORED.contains(t)).toList();
        if(terms.isEmpty())return "品牌匹配但查询没有可区分的街道/区域词";
        String text=" "+normalize(c.name()+" "+c.address())+" ";
        return terms.stream().allMatch(t->text.contains(" "+t+" "))?null:"品牌或街道/区域文字未覆盖";
    }
    private static Candidate singleTextAreaRepresentative(List<Candidate> candidates) {
        if(candidates.isEmpty()||!compact(candidates))return null;
        var sources=candidates.stream().map(Candidate::source).distinct().toList();
        return candidates.stream().min(Comparator.comparingDouble(c->sources.stream()
            .mapToDouble(s->candidates.stream().filter(p->p.source().equals(s)).mapToDouble(p->distance(c,p)).average().orElse(0)).sum())).orElse(null);
    }
    static boolean withinLimit(double meters){return Double.isFinite(meters)&&meters<=LIMIT_METERS;}
    static double distance(Candidate a,Candidate b){
        double p1=Math.toRadians(a.lat()),p2=Math.toRadians(b.lat()),dp=p2-p1,dl=Math.toRadians(b.lon()-a.lon());
        double h=Math.pow(Math.sin(dp/2),2)+Math.cos(p1)*Math.cos(p2)*Math.pow(Math.sin(dl/2),2);
        return 6371008.8*2*Math.asin(Math.sqrt(Math.min(1,h)));
    }
    private static boolean linked(Candidate a,Candidate b){return !a.source().equals(b.source())&&withinLimit(distance(a,b));}
    private static boolean compact(List<Candidate> points){
        for(int i=0;i<points.size();i++)for(int j=i+1;j<points.size();j++)if(!withinLimit(distance(points.get(i),points.get(j))))return false;
        return true;
    }
    static List<Area> group(List<Candidate> input){
        var points=input.stream().filter(c->c.lat()!=null&&c.lon()!=null&&LocationSearchService.validPoint(c.lat(),c.lon())).distinct().toList();
        List<List<Candidate>> triples=new ArrayList<>(),pairs=new ArrayList<>();
        for(int i=0;i<points.size();i++)for(int j=i+1;j<points.size();j++){
            var a=points.get(i);var b=points.get(j);if(!linked(a,b))continue;
            pairs.add(List.of(a,b));
            for(int k=j+1;k<points.size();k++){var c=points.get(k);if(linked(a,c)&&linked(b,c))triples.add(List.of(a,b,c));}
        }
        List<List<Candidate>> groups=new ArrayList<>(triples);
        for(var pair:pairs)if(triples.stream().noneMatch(t->t.containsAll(pair)))groups.add(pair);
        // Merge duplicate combinations only while the ENTIRE union stays within the diameter.
        for(int i=0;i<groups.size();i++)for(int j=i+1;j<groups.size();){
            var union=new ArrayList<>(new LinkedHashSet<>(groups.get(i)));for(var c:groups.get(j))if(!union.contains(c))union.add(c);
            if(compact(union)){groups.set(i,union);groups.remove(j);}else j++;
        }
        return groups.stream().map(g->{
            var sources=g.stream().map(Candidate::source).distinct().sorted().toList();
            // Equal provider weight: duplicates from one source cannot pull the representative.
            Candidate representative=g.stream().min(Comparator.comparingDouble(c->sources.stream().mapToDouble(s->g.stream().filter(p->p.source().equals(s)).mapToDouble(p->distance(c,p)).average().orElse(0)).sum())).orElseThrow();
            double max=0;for(var a:g)for(var b:g)max=Math.max(max,distance(a,b));
            return new Area(sources.size(),sources,List.copyOf(g),representative,max);
        }).sorted(Comparator.comparingInt(Area::support).reversed().thenComparingDouble(Area::maxDistanceMeters)).toList();
    }
}
