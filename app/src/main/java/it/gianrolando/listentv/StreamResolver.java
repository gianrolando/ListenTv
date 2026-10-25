// SPDX-License-Identifier: GPL-3.0-or-later
// Provider procedures adapted from WLTV Group, WorldLiveTV 1.2.14 hotfix.
package it.gianrolando.listentv;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import org.xmlpull.v1.*;
import android.util.Xml;

public final class StreamResolver {
    private final android.content.Context context;
    public StreamResolver(android.content.Context context) { this.context=context.getApplicationContext(); }
    static final String USER_AGENT="listentv/0.2.1 (Android; audio player)";
    record Source(String url, String license, Map<String,String> licenseHeaders) { }

    public MediaItem resolve(Catalog.Channel channel) throws Exception {
        Source s=switch(channel.provider()) {
            case "mediaset" -> new MediasetResolver(this).resolve(channel.key());
            case "rai" -> rai(channel.key());
            case "sky" -> plain(json("https://video.sky.it/api/v1/getLivestream?id="+channel.key()+"&isMobile=false").getString("streaming_url"));
            case "tv2000" -> tv2000(channel.key());
            case "la7" -> la7();
            default -> plain(channel.key());
        };
        requireHttps(s.url());
        MediaItem.Builder b=channel.item(context).buildUpon().setUri(s.url());
        if (s.license()!=null) {
            requireHttps(s.license());
            b.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                .setLicenseUri(s.license()).setLicenseRequestHeaders(s.licenseHeaders()).build());
        }
        return b.build();
    }
    private Source plain(String url) { return new Source(url,null,Map.of()); }
    private Source rai(String name) throws Exception {
        JSONArray contents=json("https://www.raiplay.it/dirette.json").getJSONArray("contents");
        String relinker=null;
        for(int i=0;i<contents.length();i++) {
            JSONObject c=contents.getJSONObject(i);
            if(c.optString("channel").replace(" ","").equalsIgnoreCase(name.replace(" ",""))) {
                relinker=c.getJSONObject("video").getString("content_url"); break;
            }
        }
        if(relinker==null) throw new IOException("Channel not found in the Rai catalog.");
        String query=(relinker.contains("?") ? "&" : "?")+"output=63&forceUserAgent=raiplayappletv";
        JSONObject r=json(relinker+query);
        String url=r.getJSONArray("video").getString(0);
        JSONObject map=r.optJSONObject("licence_server_map");
        JSONArray licenses=map==null ? null : map.optJSONArray("drmLicenseUrlValues");
        if(licenses!=null && licenses.length()>0) {
            for(int i=0;i<licenses.length();i++) {
                JSONObject l=licenses.getJSONObject(i);
                if("WIDEVINE".equalsIgnoreCase(l.optString("drm"))) {
                    Uri uri=Uri.parse(l.getString("licenceUrl"));
                    String token=uri.getQueryParameter("Authorization");
                    if(token==null) throw new IOException("Rai authorization unavailable.");
                    return new Source(url,uri.buildUpon().clearQuery().build().toString(),
                        Map.of("nv-authorizations",token));
                }
            }
            throw new IOException("This Rai stream requires an unsupported DRM system.");
        }
        return plain(url);
    }
    private Source tv2000(String id) throws Exception {
        String body=get("https://data.meride.tv/proxy/bulkproxynew/embedproxy_bulk.php/"+id+
            "/play2000/desktop/preroll-play2000/f4m/default/aHR0cHM6Ly93d3cucGxheTIwMDAuaXQvKg==",Map.of());
        XmlPullParser parser=Xml.newPullParser();
        parser.setInput(new StringReader(body));
        boolean inVideo=false;
        for(int e=parser.getEventType();e!=XmlPullParser.END_DOCUMENT;e=parser.next()) {
            if(e==XmlPullParser.START_TAG && parser.getName().equals("video")) inVideo=true;
            else if(e==XmlPullParser.END_TAG && parser.getName().equals("video")) inVideo=false;
            else if(inVideo && e==XmlPullParser.START_TAG && parser.getName().equals("default")) return plain(parser.nextText());
        }
        throw new IOException("TV2000 source not found.");
    }
    private Source la7() throws Exception {
        String body=get("https://www.la7.it/dirette-tv",Map.of());
        String dash=match(body,"[\"']?dash[\"']?\\s*:\\s*[\"']([^\"']+)[\"']");
        String tokenUrl=match(body,"preTokenUrl\\s*=\\s*[\"']([^\"']+)[\"']");
        tokenUrl=new URI("https://www.la7.it/dirette-tv").resolve(tokenUrl).toString();
        String token=json(tokenUrl).getString("preAuthToken");
        return new Source(dash.replace("\\/","/"),
            "https://la7.prod.conax.cloud/widevine/license?d="+(System.currentTimeMillis()/1000),
            Map.of("preAuthorization",token,"Origin","https://www.la7.it","Referer","https://www.la7.it/"));
    }
    private String match(String body,String regex) throws IOException {
        Matcher m=Pattern.compile(regex).matcher(body);
        if(!m.find()) throw new IOException("The website format changed: source resolver needs updating.");
        return m.group(1).replace("&amp;","&");
    }
    private JSONObject json(String url) throws Exception { return new JSONObject(get(url,Map.of())); }
    static void requireHttps(String url) throws Exception {
        URI uri=new URI(url);
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
            throw new IOException("The source does not provide a valid HTTPS URL.");
    }
    /** Bounded requests, normal TLS validation, no persisted cookies or access tokens. */
    String get(String address,Map<String,String> headers) throws Exception {
        return request(address,headers,null,false);
    }
    String post(String address,Map<String,String> headers,JSONObject body) throws Exception {
        return request(address,headers,body.toString().getBytes(StandardCharsets.UTF_8),false);
    }
    String getHtmlHead(String address,Map<String,String> headers) throws Exception {
        return request(address,headers,null,true);
    }
    private String request(String address,Map<String,String> headers,byte[] body,boolean headOnly) throws Exception {
        for(int redirect=0;redirect<6;redirect++) {
            requireHttps(address);
            HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(10000); c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent",USER_AGENT);
            for(var h:headers.entrySet()) c.setRequestProperty(h.getKey(),h.getValue());
            try {
                if(body!=null) {
                    c.setRequestMethod("POST"); c.setDoOutput(true);
                    c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                    c.setFixedLengthStreamingMode(body.length);
                    try(OutputStream out=c.getOutputStream()) { out.write(body); }
                }
                int status=c.getResponseCode();
                if(status>=300 && status<400) {
                    if(body!=null) throw new IOException("Unexpected redirect from authorization service.");
                    String next=c.getHeaderField("Location");
                    if(next==null) throw new IOException("Invalid redirect.");
                    address=new URI(address).resolve(next).toString(); continue;
                }
                if(status==401 || status==403) throw new IOException("Access unavailable: authorization or geographic restriction.");
                if(status!=200) throw new IOException("Broadcaster unavailable (HTTP "+status+").");
                try(InputStream in=c.getInputStream()) {
                    return BoundedResponseReader.read(in,headOnly ? 128*1024 : 2*1024*1024,headOnly);
                }
            } finally { c.disconnect(); }
        }
        throw new IOException("Too many redirects.");
    }
}
