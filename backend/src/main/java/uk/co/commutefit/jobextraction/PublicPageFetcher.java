package uk.co.commutefit.jobextraction;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;

@Component
public class PublicPageFetcher {
    public record Page(String html, String finalUrl) {}
    static final int MAX_BYTES = 2_000_000;
    public Page fetch(String raw) throws IOException {
        URI uri = validate(raw);
        long deadline=System.nanoTime()+20_000_000_000L;
        for(int hop=0;hop<=3;hop++) {
            if(System.nanoTime()>deadline)throw new IOException("fetch_timeout");
            final String host=uri.getHost();
            InetAddress[] pinned=InetAddress.getAllByName(host);
            for(InetAddress address:pinned)if(!isPublic(address))throw new IOException("private_address");
            DnsResolver resolver=new DnsResolver() {
                public InetAddress[] resolve(String name) throws UnknownHostException {
                    if(!name.equalsIgnoreCase(host))throw new UnknownHostException("Unexpected host");
                    return pinned.clone();
                }
                public String resolveCanonicalHostname(String name) { return name; }
            };
            var manager=PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver).build();
            var config=RequestConfig.custom().setRedirectsEnabled(false).setConnectTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(5)).setConnectionRequestTimeout(Timeout.ofSeconds(2)).build();
            try(var client=HttpClients.custom().setConnectionManager(manager).setDefaultRequestConfig(config)
                    .disableAutomaticRetries().disableCookieManagement().disableContentCompression().build()) {
                var get=new HttpGet(uri);
                get.setHeader("User-Agent","CommuteFit/1.0 (single job page extraction)");
                get.setHeader("Accept","text/html,application/xhtml+xml,text/plain;q=0.8");
                record Reply(int status,String location,String type,byte[] bytes) {}
                Reply reply=client.execute(get,response->{
                    int status=response.getCode();
                    String location=response.getFirstHeader("Location")==null?null:response.getFirstHeader("Location").getValue();
                    var entity=response.getEntity();
                    String type=entity==null?"":Objects.requireNonNullElse(entity.getContentType(),"");
                    if(status>=300&&status<400)return new Reply(status,location,type,new byte[0]);
                    if(entity==null)return new Reply(status,null,type,new byte[0]);
                    if(entity.getContentLength()>MAX_BYTES)throw new IOException("page_too_large");
                    try(InputStream in=entity.getContent()){
                        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                        byte[] buffer=new byte[8192]; int n;
                        while((n=in.read(buffer))!=-1){
                            if(System.nanoTime()>deadline)throw new IOException("fetch_timeout");
                            if(bytes.size()+n>MAX_BYTES)throw new IOException("page_too_large");
                            bytes.write(buffer,0,n);
                        }
                        return new Reply(status,null,type,bytes.toByteArray());
                    }
                });
                if(reply.status()>=300&&reply.status()<400) {
                    if(hop==3||reply.location()==null)throw new IOException("redirect_limit");
                    uri=validate(uri.resolve(reply.location()).toString()); continue;
                }
                if(reply.status()!=200)throw new IOException("http_"+reply.status());
                String type=reply.type().toLowerCase(Locale.ROOT);
                if(!(type.startsWith("text/html")||type.startsWith("application/xhtml+xml")||type.startsWith("text/plain")))
                    throw new IOException("unsupported_content");
                Charset charset=StandardCharsets.UTF_8;
                var match=java.util.regex.Pattern.compile("charset=[\"']?([^;\\s\"']+)").matcher(type);
                if(match.find())try{charset=Charset.forName(match.group(1));}catch(Exception ignored){}
                return new Page(new String(reply.bytes(),charset),uri.toString());
            }
        }
        throw new IOException("redirect_limit");
    }
    static URI validate(String raw) throws IOException {
        try {
            URI uri=URI.create(raw);
            if(!Set.of("http","https").contains(Objects.requireNonNullElse(uri.getScheme(),"").toLowerCase(Locale.ROOT))
                || uri.getHost()==null || uri.getUserInfo()!=null || (uri.getPort()!=-1&&uri.getPort()!=80&&uri.getPort()!=443))
                throw new IOException("invalid_url");
            return uri;
        } catch(IllegalArgumentException ex){throw new IOException("invalid_url");}
    }
    static boolean isPublic(InetAddress a) {
        if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())return false;
        byte[] b=a.getAddress();
        if(b.length==4){int x=b[0]&255,y=b[1]&255;
            int z=b[2]&255;
            return !(x==0||x==10||x==127||x>=224||(x==169&&y==254)||(x==172&&y>=16&&y<=31)||(x==192&&(y==168||y==0))||(x==100&&y>=64&&y<=127)||(x==198&&(y==18||y==19||y==51&&z==100))||(x==203&&y==0&&z==113));
        }
        // Only globally routed IPv6 unicast. Reject ULA, link local and transition forms.
        boolean special2001=(b[0]&255)==0x20&&(b[1]&255)==0x01&&((b[2]==0&&b[3]==0)||((b[2]&255)==0x0d&&(b[3]&255)==0xb8));
        return (b[0]&0xe0)==0x20 && !((b[0]&255)==0x20&&(b[1]&255)==0x02) && !special2001;
    }
}
