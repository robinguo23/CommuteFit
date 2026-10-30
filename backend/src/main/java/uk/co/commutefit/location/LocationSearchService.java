package uk.co.commutefit.location;

import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import uk.co.commutefit.journey.tfl.*;

@Service
public class LocationSearchService {
    public record Candidate(String id,String name,String address,String postcode,Double lat,Double lon,
                            String precision,String source,String sourceId,boolean selectable) {}
    public record Result(String status,List<Candidate> candidates,String message,String attribution) {}
    private final PhotonClient photon;
    private final TflClient tfl;
    private final GeoapifyClient geoapify;
    private final MapTilerClient maptiler;
    private final LocationIqClient locationiq;
    private final OpenCageClient opencage;
    private final TomTomPlacesClient tomtom;
    private final PostcodesIoClient postcodesIo;
    private final ArcGisClient arcgis;
    private final FoursquareClient foursquare;
    private final CandidateTokenStore tokens;
    public LocationSearchService(PhotonClient photon,TflClient tfl,GeoapifyClient geoapify,MapTilerClient maptiler,LocationIqClient locationiq,OpenCageClient opencage,TomTomPlacesClient tomtom,PostcodesIoClient postcodesIo,ArcGisClient arcgis,FoursquareClient foursquare,CandidateTokenStore tokens){this.photon=photon;this.tfl=tfl;this.geoapify=geoapify;this.maptiler=maptiler;this.locationiq=locationiq;this.opencage=opencage;this.tomtom=tomtom;this.postcodesIo=postcodesIo;this.arcgis=arcgis;this.foursquare=foursquare;this.tokens=tokens;}
    public Result search(String raw) {
        long started=System.nanoTime();
        String query=raw==null?"":raw.trim().replaceAll("\\s+"," ");
        if(query.length()<3||query.length()>120)throw new IllegalArgumentException("Enter 3–120 characters.");
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        if(compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")){
            String pc=compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3);
            return result(List.of(candidate(pc,pc,pc,null,null,"postcode","User postcode",null,pc,true)),"A postcode identifies a postal area, not necessarily the store entrance.");
        }
        Matcher point=Pattern.compile("^(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)$").matcher(query);
        if(point.matches()){
            double lat=Double.parseDouble(point.group(1)),lon=Double.parseDouble(point.group(2));
            if(!validPoint(lat,lon))throw new IllegalArgumentException("Invalid coordinates");
            String route=coordinateValue(lat,lon);
            return result(List.of(candidate(route,route,null,lat,lon,"coordinate","User coordinates",null,route,true)),"Confirm the map point.");
        }
        if(query.matches("\\d{7}")||query.matches("(?i)(?:940|910|490)[A-Z0-9]{4,12}")){
            return result(List.of(candidate(query,query,null,null,null,"station","User station ID",query,query,true)),"Station ID format accepted; the station name has not been verified.");
        }
        List<Candidate> out=new ArrayList<>();
        boolean externalFailed=false,tflFailed=false;
        try {out.addAll(photonCandidates(photon.search(query)));} catch(RuntimeException ex){externalFailed=true;}
        if(out.size()<8){
            try{
                JsonNode stops=tfl.stopPointSearch(query).path("matches");
                if(stops.isArray())for(JsonNode stop:stops){
                    if(out.size()>=8)break;
                    String name=stop.path("name").asText("").trim(),id=stop.path("id").asText("");
                    if(id.startsWith("HUB"))id=stop.path("icsId").asText("");
                    if(name.isBlank()||id.isBlank())continue;
                    Double lat=number(stop.path("lat")),lon=number(stop.path("lon"));
                    if(lat==null||lon==null||!validPoint(lat,lon)){lat=null;lon=null;}
                    out.add(candidate(name,name,null,lat,lon,"station","TfL",id,id,true));
                }
            }catch(RuntimeException ex){tflFailed=true;}
            if(out.isEmpty()&&!tflFailed&&System.nanoTime()-started<8_000_000_000L)try{
                JsonNode places=tfl.placeSearch(query);
                if(places.isArray())for(JsonNode place:places){
                    if(out.size()>=8)break;
                    String name=place.path("commonName").asText("").trim();
                    Double lat=number(place.path("lat")),lon=number(place.path("lon"));
                    if(!name.isBlank()&&lat!=null&&lon!=null&&validPoint(lat,lon)) {
                        String type=place.path("placeType").asText("").toLowerCase(Locale.ROOT);
                        String precision=switch(type) {
                            case "pointofinterest" -> "venue";
                            case "stoppoint" -> "station";
                            case "street" -> "street";
                            default -> "locality";
                        };
                        boolean selectable=precision.equals("venue")||precision.equals("station");
                        out.add(candidate(name,name,null,lat,lon,precision,"TfL Place",null,coordinateValue(lat,lon),selectable));
                    }
                }
            }catch(RuntimeException ex){tflFailed=true;}
        }
        out.sort(Comparator.comparingInt(c->switch(c.precision()) {
            case "venue","address" -> 0;
            case "station" -> 1;
            default -> 2;
        }));
        String message=externalFailed?"Address search unavailable or temporarily rate limited. Results may only include TfL locations.":out.isEmpty()?"No matching place. Add a street address or postcode.":"Confirm the intended place and its precision.";
        return new Result(out.isEmpty()&&externalFailed&&tflFailed?"unavailable":out.isEmpty()?"no-matches":"matches",List.copyOf(out),message,"Geocoding: Photon / © OpenStreetMap contributors; station/place data: TfL");
    }
    public Result searchProvider(String provider,String raw) {
        String query=raw==null?"":raw.trim().replaceAll("\\s+"," ");
        if(query.length()<3||query.length()>120)throw new IllegalArgumentException("Enter 3–120 characters.");
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        if(provider.equals("direct")&&compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")) {
            String pc=compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3);
            return result(List.of(candidate(pc,pc,pc,null,null,"postcode","User postcode",null,pc,true)),"邮编格式识别成功；此步骤不验证邮编是否真实存在，也不返回坐标。 ");
        }
        return switch(provider) {
            case "geoapify" -> searchGeoapify(query);
            case "maptiler" -> searchMapTiler(query);
            case "locationiq" -> searchLocationIq(query);
            case "opencage" -> searchOpenCage(query);
            case "tomtom" -> searchTomTom(query);
            case "tomtom-search" -> searchTomTomFuzzy(query);
            case "postcodes-io" -> searchPostcodesIo(query);
            case "arcgis" -> searchArcGis(query);
            case "foursquare" -> searchFoursquare(query);
            case "photon" -> new Result("matches",photonCandidates(photon.search(query)),"Photon / OpenStreetMap 地理搜索结果", "Geocoding: Photon / © OpenStreetMap contributors");
            case "tfl-stops" -> {
                List<Candidate> candidates=new ArrayList<>();
                JsonNode stops=tfl.stopPointSearch(query).path("matches");
                if(stops.isArray())for(JsonNode stop:stops){
                    String name=stop.path("name").asText("").trim(),id=stop.path("id").asText("");
                    if(id.startsWith("HUB"))id=stop.path("icsId").asText("");
                    if(name.isBlank()||id.isBlank())continue;
                    Double lat=number(stop.path("lat")),lon=number(stop.path("lon"));
                    if(lat==null||lon==null||!validPoint(lat,lon)){lat=null;lon=null;}
                    candidates.add(candidate(name,name,null,lat,lon,"station","TfL",id,id,true));
                    if(candidates.size()>=8)break;
                }
                yield new Result("matches",List.copyOf(candidates),"TfL StopPoint Search 站点搜索结果", "TfL StopPoint API");
            }
            case "tfl-places" -> {
                List<Candidate> candidates=new ArrayList<>();JsonNode places=tfl.placeSearch(query);
                if(places.isArray())for(JsonNode place:places){
                    String name=place.path("commonName").asText("");Double lat=number(place.path("lat")),lon=number(place.path("lon"));
                    if(name.isBlank()||lat==null||lon==null||!validPoint(lat,lon))continue;
                    String type=place.path("placeType").asText("").toLowerCase(Locale.ROOT);
                    String precision=switch(type){case "pointofinterest"->"venue";case "stoppoint"->"station";case "street"->"street";default->"locality";};
                    boolean selectable=precision.equals("venue")||precision.equals("station");
                    candidates.add(candidate(name,name,null,lat,lon,precision,"TfL Place",null,coordinateValue(lat,lon),selectable));
                    if(candidates.size()>=8)break;
                }
                yield new Result("matches",List.copyOf(candidates),"TfL Place Search 地点搜索结果", "TfL Place API");
            }
            default -> throw new IllegalArgumentException("Unknown place search provider.");
        };
    }
    private Result searchGeoapify(String query) {
        String attribution="Geoapify Geocoding API";
        if(!geoapify.configured())return new Result("unavailable",List.of(),"Geoapify 未配置 API key。请在项目根目录 .env 设置 GEOAPIFY_API_KEY 并重启本地服务。",attribution);
        try {
            List<Candidate> candidates=geoapifyCandidates(geoapify.search(query));
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"Geoapify 没有返回可用的英国候选地点。":"Geoapify 英国地点搜索结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            String message=status==401||status==403
                    ? "Geoapify 拒绝了请求。请检查本机 .env 中的 GEOAPIFY_API_KEY 是否有效。"
                    : status==429
                    ? "Geoapify 请求频率或免费额度已达限制，请稍后再试。"
                    : "Geoapify 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 Geoapify 地点搜索服务，请检查网络后重试。",attribution);
        }
    }
    private Result searchMapTiler(String query) {
        String attribution="MapTiler / © OpenStreetMap contributors";
        if(!maptiler.configured())return new Result("unavailable",List.of(),"MapTiler 未配置 API key。请在项目根目录 .env 设置 MAPTILER_API_KEY 并重启本地服务。",attribution);
        try {
            List<Candidate> candidates=maptilerCandidates(maptiler.search(query));
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"MapTiler 没有返回可用的英国候选地点。":"MapTiler 英国地点搜索结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            String message=status==401||status==403
                    ? "MapTiler 拒绝了请求。请检查本机 .env 中的 MAPTILER_API_KEY 是否有效。"
                    : status==429
                    ? "MapTiler 请求额度或频率已达限制，请稍后再试。"
                    : "MapTiler 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 MapTiler 地点搜索服务，请检查网络后重试。",attribution);
        }
    }
    List<Candidate> maptilerCandidates(JsonNode root){
        List<Candidate> out=new ArrayList<>();JsonNode features=root.path("features");
        if(!features.isArray())return out;
        Pattern postcodePattern=Pattern.compile("(?i)\\b(GIR\\s?0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2})\\b");
        for(JsonNode feature:features){
            if(out.size()>=8)break;
            JsonNode geometry=feature.path("geometry"),coordinates=geometry.path("coordinates"),properties=feature.path("properties");
            if(!geometry.path("type").asText("").equals("Point")||!coordinates.isArray()||coordinates.size()<2)continue;
            Double lon=number(coordinates.get(0)),lat=number(coordinates.get(1));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String name=feature.path("text").asText("").trim();
            String formatted=feature.path("place_name").asText("").trim();
            if(name.isBlank())name=properties.path("name").asText("").trim();
            if(name.isBlank())name=formatted;
            if(name.isBlank())continue;
            String types=feature.path("place_type").isArray()
                    ? java.util.stream.StreamSupport.stream(feature.path("place_type").spliterator(),false).map(JsonNode::asText).collect(java.util.stream.Collectors.joining(" "))
                    : feature.path("place_type").asText("");
            if(types.isBlank())types=properties.path("place_designation").asText("");
            types=types.toLowerCase(Locale.ROOT);
            Matcher postcodeMatch=postcodePattern.matcher(formatted.isBlank()?name:formatted);
            String postcode=postcodeMatch.find()?postcodeMatch.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+"," "):"";
            String precision=types.contains("postcode")?"postcode":types.contains("address")||types.contains("house")?"address":types.contains("poi")||types.contains("venue")?"venue":types.contains("street")||types.contains("road")?"street":"locality";
            if(!postcode.isBlank()&&precision.equals("locality"))precision="postcode";
            boolean selectable=Set.of("postcode","address","venue").contains(precision);
            String sourceId=feature.path("id").asText("");
            String id=sourceId.isBlank()?properties.path("ref").asText(""):sourceId;
            String route=coordinateValue(lat,lon);
            out.add(candidate(name,formatted.isBlank()?name:formatted,postcode.isBlank()?null:postcode,lat,lon,precision,"MapTiler",id.isBlank()?null:id,route,selectable));
        }
        return List.copyOf(out);
    }
    private Result searchFoursquare(String query) {
        String attribution="Powered by Foursquare";
        if(!foursquare.configured())return new Result("unavailable",List.of(),"请在项目根目录 .env 填写 FOURSQUARE_API_KEY 并重启服务。",attribution);
        try {
            var candidates=foursquareCandidates(foursquare.search(query));
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,
                "Foursquare 商业门店搜索结果（near=London, GB）；请核对目标门店。",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            return new Result("unavailable",List.of(),"Foursquare 请求失败（HTTP "+status+"）。"+
                (status==429?"请求频率或额度受限。":status==401||status==403?"请检查 API key 和 Places API 权限。":"请稍后重试。"),attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"Foursquare 服务暂不可用，请稍后重试。",attribution);
        }
    }
    List<Candidate> foursquareCandidates(JsonNode root) {
        if(!root.path("results").isArray())throw new IllegalStateException("Missing Foursquare results");
        List<Candidate> out=new ArrayList<>();
        for(JsonNode item:root.path("results")) {
            if(out.size()>=8)break;
            var location=item.path("location");
            if(!location.path("country").asText("").equalsIgnoreCase("GB"))continue;
            Double lat=number(item.path("latitude")),lon=number(item.path("longitude"));
            String name=item.path("name").asText("").trim();
            if(name.isBlank()||lat==null||lon==null||!validPoint(lat,lon))continue;
            String postcode=location.path("postcode").asText("").trim().toUpperCase(Locale.ROOT);
            String address=String.join(", ",java.util.stream.Stream.of("address","locality","region","postcode")
                .map(k->location.path(k).asText("").trim()).filter(v->!v.isBlank()).distinct().toList());
            boolean selectable=item.path("date_closed").asText("").isBlank();
            out.add(candidate(name,address,postcode.isBlank()?null:postcode,lat,lon,"venue","Foursquare",
                item.path("fsq_place_id").asText(""),coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    private Result searchArcGis(String query) {
        String attribution="ArcGIS / Esri；仅作非持久化接口对比测试";
        try {
            var candidates=arcGisCandidates(arcgis.search(query));
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,
                "ArcGIS 英国原样查询结果；保留上游顺序，首项和高分均不保证为目标门店。",attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"ArcGIS 服务不可用或拒绝请求，请稍后重试。",attribution);
        }
    }
    List<Candidate> arcGisCandidates(JsonNode root) {
        List<Candidate> out=new ArrayList<>();
        if(!root.path("candidates").isArray())return out;
        for(JsonNode item:root.path("candidates")) {
            if(out.size()>=8)break;
            var a=item.path("attributes");var location=item.path("location");
            Double lat=number(location.path("y")),lon=number(location.path("x"));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String name=a.path("PlaceName").asText("").trim();
            if(name.isBlank())name=item.path("address").asText("").trim();
            if(name.isBlank())continue;
            String address=a.path("Place_addr").asText("").trim();
            if(address.isBlank())address=a.path("LongLabel").asText(name);
            String postcode=a.path("Postal").asText("").trim().toUpperCase(Locale.ROOT);
            String compact=postcode.replaceAll("\\s","");
            postcode=compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")
                ?compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3):"";
            String precision=switch(a.path("Addr_type").asText("")) {
                case "POI" -> "venue";
                case "PointAddress","Subaddress","StreetAddress" -> "address";
                case "Postal" -> "postcode";
                case "StreetName" -> "street";
                default -> "locality";
            };
            boolean selectable=Set.of("venue","address","postcode").contains(precision);
            out.add(candidate(name,address,postcode.isBlank()?null:postcode,lat,lon,precision,"ArcGIS",
                a.path("MatchID").asText(""),coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    private Result searchLocationIq(String query) {
        String attribution="LocationIQ / © OpenStreetMap contributors";
        if(!locationiq.configured())return new Result("unavailable",List.of(),"LocationIQ 未配置 API key。请在项目根目录 .env 设置 LOCATIONIQ_API_KEY 并重启本地服务。",attribution);
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        boolean postcode=compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}");
        try {
            List<Candidate> candidates=locationIqCandidates(locationiq.search(query,postcode),postcode);
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"LocationIQ 没有返回可用的英国候选地点。":"LocationIQ 英国"+(postcode?"postcode":"地点")+"搜索结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            if(status==404)return new Result("no-matches",List.of(),"LocationIQ 没有找到匹配地点（该 API 用 HTTP 404 表示无地址匹配）。",attribution);
            String message=status==401||status==403
                    ? "LocationIQ 拒绝了请求。请检查本机 .env 中的 LOCATIONIQ_API_KEY 是否有效。"
                    : status==429
                    ? "LocationIQ 请求频率或免费额度已达限制，请稍后再试。"
                    : "LocationIQ 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 LocationIQ 地点搜索服务，请检查网络后重试。",attribution);
        }
    }
    List<Candidate> locationIqCandidates(JsonNode root,boolean postalQuery){
        List<Candidate> out=new ArrayList<>();if(!root.isArray())return out;
        for(JsonNode item:root){
            if(out.size()>=8)break;
            Double lat=decimal(item.path("lat")),lon=decimal(item.path("lon"));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            JsonNode addressNode=item.path("address");
            String name=item.path("name").asText("").trim();
            if(name.isBlank())name=addressNode.path("name").asText("").trim();
            String formatted=item.path("display_name").asText("").trim();
            if(name.isBlank())name=formatted;
            if(name.isBlank())continue;
            String postcode=addressNode.path("postcode").asText("").trim();
            if(postcode.isBlank()){
                Matcher matcher=Pattern.compile("(?i)\\b(GIR\\s?0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2})\\b").matcher(formatted);
                if(matcher.find())postcode=matcher.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+"," ");
            }
            String category=item.path("class").asText("").toLowerCase(Locale.ROOT);
            String type=item.path("type").asText("").toLowerCase(Locale.ROOT);
            String addresstype=item.path("addresstype").asText("").toLowerCase(Locale.ROOT);
            String precision=postalQuery||addresstype.equals("postcode")||type.equals("postcode")?"postcode":
                    !addressNode.path("house_number").asText("").isBlank()||!addressNode.path("house_name").asText("").isBlank()?"address":
                    Set.of("shop","amenity","tourism","leisure","office","historic","building").contains(category)?"venue":
                    Set.of("shop","attraction","restaurant","cafe","supermarket","mall").contains(type)?"venue":
                    Set.of("road","street").contains(addresstype)||category.equals("highway")?"street":"locality";
            boolean selectable=Set.of("postcode","address","venue").contains(precision);
            String sourceId=item.path("place_id").asText("");
            out.add(candidate(name,formatted.isBlank()?name:formatted,postcode.isBlank()?null:postcode,lat,lon,precision,"LocationIQ",sourceId.isBlank()?null:sourceId,coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    private Result searchOpenCage(String query) {
        String attribution="OpenCage Geocoding API / © OpenStreetMap contributors";
        if(!opencage.configured())return new Result("unavailable",List.of(),"OpenCage 未配置 API key。请在项目根目录 .env 设置 OPENCAGE_API_KEY 并重启本地服务。",attribution);
        try {
            List<Candidate> candidates=openCageCandidates(opencage.search(query),query);
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"OpenCage 没有返回可用的英国候选地点。":"OpenCage 英国地点搜索结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            String message=status==401||status==403
                    ? "OpenCage 拒绝了请求。请检查本机 .env 中的 OPENCAGE_API_KEY 是否有效或仍可用。"
                    : status==402
                    ? "OpenCage 免费测试额度已用完，或账户额度不足。"
                    : status==429
                    ? "OpenCage 请求过快，请稍后再试。"
                    : "OpenCage 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 OpenCage 地点搜索服务，请检查网络后重试。",attribution);
        }
    }
    List<Candidate> openCageCandidates(JsonNode root,String query){
        List<Candidate> out=new ArrayList<>();JsonNode results=root.path("results");
        if(!results.isArray())return out;
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        boolean postalQuery=compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}");
        for(JsonNode item:results){
            if(out.size()>=8)break;
            JsonNode geometry=item.path("geometry"),components=item.path("components");
            Double lat=decimal(geometry.path("lat")),lon=decimal(geometry.path("lng"));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String formatted=item.path("formatted").asText("").trim();
            String name=components.path("name").asText("").trim();
            if(name.isBlank())name=components.path("brand").asText("").trim();
            if(name.isBlank())name=components.path("shop").asText("").trim();
            if(name.isBlank())name=formatted;
            if(name.isBlank())continue;
            String postcode=components.path("postcode").asText("").trim();
            boolean namedVenue=List.of("amenity","shop","tourism","leisure","office","attraction","building","commercial").stream().anyMatch(field->!components.path(field).asText("").isBlank());
            String precision=postalQuery?"postcode":!postcode.isBlank()&&name.equals(formatted)?"address":
                    !components.path("house_number").asText("").isBlank()?"address":namedVenue?"venue":!postcode.isBlank()?"address":"locality";
            boolean selectable=Set.of("postcode","address","venue").contains(precision);
            String sourceId=item.path("annotations").path("geohash").asText("");
            out.add(candidate(name,formatted.isBlank()?name:formatted,postcode.isBlank()?null:postcode,lat,lon,precision,"OpenCage",sourceId.isBlank()?null:sourceId,coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    private Result searchTomTom(String query) {
        String attribution="TomTom Places Search API / TomTom Orbis Maps";
        if(!tomtom.configured())return new Result("unavailable",List.of(),"TomTom 未配置 API key。请在项目根目录 .env 设置 TOMTOM_API_KEY 并重启本地服务。",attribution);
        try {
            List<Candidate> candidates=tomTomCandidates(tomtom.search(query),query);
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"TomTom 没有返回可用的英国地点。":"TomTom Places 英国地点与商业 POI 搜索结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            String message=status==401||status==403
                    ? "TomTom 拒绝了请求。请检查本机 .env 中的 TOMTOM_API_KEY 是否有效或已启用 Places Search API。"
                    : status==429
                    ? "TomTom 请求频率已达限制，请稍后再试。"
                    : "TomTom 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 TomTom Places 搜索服务，请检查网络后重试。",attribution);
        }
    }
    private Result searchTomTomFuzzy(String query) {
        String attribution="TomTom Search API / TomTom Maps";
        if(!tomtom.configured())return new Result("unavailable",List.of(),"TomTom 未配置 API key。请在项目根目录 .env 设置 TOMTOM_API_KEY 并重启本地服务。",attribution);
        try {
            List<Candidate> candidates=tomTomFuzzyCandidates(tomtom.fuzzySearch(query));
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"TomTom Search API 没有返回可用的英国地点。":"TomTom Search API Fuzzy Search 英国地点结果",attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            String message=status==401||status==403?"TomTom 拒绝了请求。请检查本机 .env 中的 TOMTOM_API_KEY 是否有效或已启用 Search API。":status==429?"TomTom 请求频率已达限制，请稍后再试。":"TomTom Search API 暂时无法完成地点搜索（HTTP "+status+"），请稍后重试。";
            return new Result("unavailable",List.of(),message,attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 TomTom Search API，请检查网络后重试。",attribution);
        }
    }
    private Result searchPostcodesIo(String query) {
        String attribution="Postcodes.io / ONS Postcode Directory / Ordnance Survey Open Data";
        try {
            String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
            JsonNode response;
            String message;
            if(compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")) {
                String postcode=compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3);
                response=postcodesIo.lookup(postcode);message="Postcodes.io 完整 postcode 查询结果";
            } else {
                Matcher coords=Pattern.compile("^(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)$").matcher(query);
                if(coords.matches()) {
                    double lat=Double.parseDouble(coords.group(1)),lon=Double.parseDouble(coords.group(2));
                    if(!validPoint(lat,lon))throw new IllegalArgumentException("Invalid coordinates");
                    response=postcodesIo.nearby(lat,lon);message="Postcodes.io 坐标附近邮编结果";
                } else if(compact.matches("[A-Z]{1,2}\\d[A-Z\\d]?")) {
                    response=postcodesIo.search(compact);message="Postcodes.io 邮编前缀搜索结果";
                } else {
                    return new Result("no-matches",List.of(),"Postcodes.io 只搜索英国邮编和坐标，不搜索品牌或商店名称。请试完整邮编、邮编前缀（如 SE18）或纬度,经度。",attribution);
                }
            }
            List<Candidate> candidates=postcodesIoCandidates(response);
            return new Result(candidates.isEmpty()?"no-matches":"matches",candidates,candidates.isEmpty()?"Postcodes.io 没有找到匹配邮编。":message,attribution);
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            return new Result(status==404?"no-matches":"unavailable",List.of(),status==404?"Postcodes.io 没有找到该邮编。":"Postcodes.io 暂时不可用（HTTP "+status+"），请稍后重试。",attribution);
        } catch(RuntimeException ex) {
            return new Result("unavailable",List.of(),"无法连接 Postcodes.io，请检查网络后重试。",attribution);
        }
    }
    List<Candidate> postcodesIoCandidates(JsonNode root){
        List<Candidate> out=new ArrayList<>();JsonNode result=root.path("result");
        if(result.isObject())addPostcodeCandidate(out,result);
        else if(result.isArray())for(JsonNode item:result){if(out.size()>=8)break;addPostcodeCandidate(out,item);}
        return List.copyOf(out);
    }
    private void addPostcodeCandidate(List<Candidate> out,JsonNode item){
        if(out.size()>=8||item==null||item.isNull())return;
        Double lat=number(item.path("latitude")),lon=number(item.path("longitude"));
        String postcode=item.path("postcode").asText("").trim();if(postcode.isBlank())return;
        if(lat==null||lon==null||!validPoint(lat,lon)){lat=null;lon=null;}
        String district=item.path("admin_district").asText("").trim();
        String region=item.path("region").asText("").trim();
        String address=String.join(", ",java.util.stream.Stream.of(district,region).filter(s->!s.isBlank()).distinct().toList());
        String precision=item.path("quality").asInt(9)<=4?"postcode":"locality";
        String route=lat!=null?coordinateValue(lat,lon):postcode;
        out.add(candidate(postcode,address.isBlank()?postcode:address+", "+postcode,postcode,lat,lon,precision,"Postcodes.io",postcode,route,true));
    }
    List<Candidate> tomTomFuzzyCandidates(JsonNode root){
        List<Candidate> out=new ArrayList<>();JsonNode results=root.path("results");
        if(!results.isArray())return out;
        Pattern postcodePattern=Pattern.compile("(?i)\\b(GIR\\s?0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2})\\b");
        for(JsonNode item:results){
            if(out.size()>=8)break;
            JsonNode position=item.path("position"),addressNode=item.path("address"),poi=item.path("poi");
            Double lat=number(position.path("lat")),lon=number(position.path("lon"));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String name=poi.path("name").asText("").trim();if(name.isBlank())name=item.path("address").path("freeformAddress").asText("").trim();
            if(name.isBlank())continue;
            String postcode=addressNode.path("extendedPostalCode").asText("").trim();
            if(postcode.isBlank())postcode=addressNode.path("postalCode").asText("").trim();
            Matcher extendedMatcher=Pattern.compile("(?i)\\b(GIR\\s?0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2})\\b").matcher(postcode);
            if(extendedMatcher.find())postcode=extendedMatcher.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+"," ");
            String formatted=addressNode.path("freeformAddress").asText("").trim();
            if(postcode.isBlank()) {Matcher matcher=postcodePattern.matcher(formatted);if(matcher.find())postcode=matcher.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+"," ");}
            String type=item.path("type").asText("").toLowerCase(Locale.ROOT);
            String precision=type.equals("poi")?"venue":type.equals("point address")||type.equals("extended postal point")?"address":type.equals("street")||type.equals("address range")?"street":"locality";
            if(type.equals("extended postal point"))precision="postcode";
            boolean selectable=Set.of("venue","address","postcode").contains(precision);
            String id=item.path("id").asText("");
            out.add(candidate(name,formatted.isBlank()?name:formatted,postcode.isBlank()?null:postcode,lat,lon,precision,"TomTom Search",id.isBlank()?null:id,coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    List<Candidate> tomTomCandidates(JsonNode root,String query){
        List<Candidate> out=new ArrayList<>();JsonNode results=root.path("results");
        if(!results.isArray())return out;
        String compact=query.replaceAll("\\s","").toUpperCase(Locale.ROOT);
        Matcher postcodeMatcher=Pattern.compile("(?i)^(GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2})$").matcher(compact);
        String exactPostcode=postcodeMatcher.matches()?compact.substring(0,compact.length()-3)+" "+compact.substring(compact.length()-3):"";
        for(JsonNode item:results){
            if(out.size()>=8)break;
            JsonNode coords=item.path("position").path("coordinates"),addressNode=item.path("address");
            Double lon=coords.size()>0?decimal(coords.get(0)):null,lat=coords.size()>1?decimal(coords.get(1)):null;
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String title=item.path("title").asText("").trim();
            if(title.isBlank())continue;
            String street=addressNode.path("street").asText("").trim();
            String house=addressNode.path("houseNumber").asText("").trim();
            String town=addressNode.path("municipality").asText("").trim();
            String postcode=exactPostcode.isBlank()?addressNode.path("extendedPostalCode").asText("").trim():exactPostcode;
            if(postcode.isBlank())postcode=addressNode.path("postalCode").asText("").trim();
            Matcher fullPostcode=Pattern.compile("(?i)\\b(GIR\\s?0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2})\\b").matcher(postcode);
            if(fullPostcode.find())postcode=fullPostcode.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+"," ");
            List<String> parts=new ArrayList<>();
            if(!house.isBlank()&&!street.isBlank())parts.add(house+" "+street);else if(!street.isBlank())parts.add(street);
            if(!town.isBlank())parts.add(town);
            if(!postcode.isBlank())parts.add(postcode);
            if(parts.isEmpty()&&item.path("subtitles").isArray())for(JsonNode subtitle:item.path("subtitles")){String value=subtitle.asText("").trim();if(!value.isBlank())parts.add(value);}
            String formatted=String.join(", ",parts);
            String type=item.path("type").asText("").toLowerCase(Locale.ROOT);
            String precision=!exactPostcode.isBlank()?"postcode":switch(type){case "poi"->"venue";case "address"->"address";case "street"->"street";case "area"->"locality";default->"locality";};
            boolean selectable=Set.of("venue","address").contains(precision);
            String sourceId=item.path("id").asText("");
            out.add(candidate(title,formatted.isBlank()?title+(!postcode.isBlank()?", "+postcode:""):formatted,postcode.isBlank()?null:postcode,lat,lon,precision,"TomTom",sourceId.isBlank()?null:sourceId,coordinateValue(lat,lon),selectable));
        }
        return List.copyOf(out);
    }
    private static Double decimal(JsonNode node){
        if(node==null||node.isNull())return null;
        try{double value=node.isNumber()?node.asDouble():Double.parseDouble(node.asText());return Double.isFinite(value)?value:null;}
        catch(NumberFormatException ex){return null;}
    }
    List<Candidate> geoapifyCandidates(JsonNode root){
        List<Candidate> out=new ArrayList<>();JsonNode results=root.path("results");
        if(!results.isArray())return out;
        for(JsonNode item:results){
            if(out.size()>=8)break;
            String name=item.path("name").asText("").trim();
            String formatted=item.path("formatted").asText("").trim();
            String postcode=item.path("postcode").asText("").trim();
            Double lat=number(item.path("lat")),lon=number(item.path("lon"));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            if(name.isBlank())name=formatted.isBlank()?postcode:formatted;
            if(name.isBlank())continue;
            String category=item.path("result_type").asText("").toLowerCase(Locale.ROOT);
            String precision=!postcode.isBlank()?(category.contains("postcode")?"postcode":"address"):
                    category.contains("building")?"address":
                    category.contains("amenity")||category.contains("commercial")||category.contains("retail")?"venue":
                    category.contains("street")?"street":"locality";
            boolean selectable=Set.of("postcode","address","venue").contains(precision);
            String address=formatted.isBlank()?name:formatted;
            String sourceId=item.path("place_id").asText("");
            String route=coordinateValue(lat,lon);
            out.add(candidate(name,address,postcode.isBlank()?null:postcode,lat,lon,precision,"Geoapify",sourceId.isBlank()?null:sourceId,route,selectable));
        }
        return List.copyOf(out);
    }
    List<Candidate> photonCandidates(JsonNode root){
        List<Candidate> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        JsonNode features=root.path("features");if(!features.isArray())return out;
        for(JsonNode f:features){
            if(out.size()>=8)break;
            JsonNode geometry=f.path("geometry"),xy=geometry.path("coordinates"),p=f.path("properties");
            if(!geometry.path("type").asText("").equals("Point")||!xy.isArray()||xy.size()<2)continue;
            Double lon=number(xy.get(0)),lat=number(xy.get(1));
            if(lat==null||lon==null||!validPoint(lat,lon))continue;
            String country=p.path("countrycode").asText("");
            if(!country.isBlank()&&!country.equalsIgnoreCase("GB")&&!country.equalsIgnoreCase("UK"))continue;
            String name=p.path("name").asText("").trim();
            if(name.isBlank())continue;
            String key=p.path("osm_key").asText(""),type=p.path("type").asText("");
            String street=p.path("street").asText(""),house=p.path("housenumber").asText(""),pc=p.path("postcode").asText("");
            String precision=Set.of("railway","public_transport").contains(key)?"station":Set.of("shop","amenity","office","tourism","leisure","building").contains(key)?"venue":!house.isBlank()?"address":key.equals("highway")||type.equals("street")?"street":"locality";
            String address=join(house.isBlank()?street:house+" "+street,p.path("city").asText(""),p.path("district").asText(""),pc,p.path("country").asText(""));
            String label=address.isBlank()?name:name+" — "+address;
            String unique=label+"|"+lat+"|"+lon;if(!seen.add(unique))continue;
            boolean selectable=Set.of("venue","address","station").contains(precision);
            String sourceId=p.path("osm_type").asText("")+p.path("osm_id").asText("");
            out.add(candidate(label,address,pc.isBlank()?null:pc,lat,lon,precision,"Photon / OpenStreetMap",sourceId,coordinateValue(lat,lon),selectable));
        }
        return out;
    }
    private Candidate candidate(String name,String address,String postcode,Double lat,Double lon,String precision,String source,String sourceId,String route,boolean selectable){
        name=name.trim().replaceAll("\\s+"," ");
        if(name.length()>120)name=name.substring(0,120).trim();
        String id=selectable?tokens.add(name,route,name,address):"";
        return new Candidate(id,name,address,postcode,lat,lon,precision,source,sourceId,selectable);
    }
    private Result result(List<Candidate> candidates,String message){return new Result("matches",candidates,message,"TfL endpoint format; no geocoder lookup");}
    private static String join(String... parts){return String.join(", ",Arrays.stream(parts).filter(s->!s.isBlank()).distinct().toList());}
    static Double number(JsonNode n){return n!=null&&n.isNumber()&&Double.isFinite(n.asDouble())?n.asDouble():null;}
    static boolean validPoint(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    static String coordinateValue(double lat,double lon){return java.math.BigDecimal.valueOf(lat).toPlainString()+","+java.math.BigDecimal.valueOf(lon).toPlainString();}
}
