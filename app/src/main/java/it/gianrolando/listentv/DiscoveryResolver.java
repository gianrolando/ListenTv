// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from WLTV Group, WorldLiveTV 1.2.14 and its 2026-04-19 hotfix.
package it.gianrolando.listentv;

import java.io.IOException;
import java.util.*;
import org.json.*;
import androidx.media3.common.MimeTypes;

final class DiscoveryResolver {
    private final StreamResolver http;
    DiscoveryResolver(StreamResolver http) { this.http=http; }
    StreamResolver.Source resolve(String key) throws Exception {
        DiscoveryPolicy.Channel channel=DiscoveryPolicy.channel(key);
        String api=DiscoveryPolicy.API,realm="it",id=channel.id();
        Map<String,String> headers=new HashMap<>();
        headers.put("Origin","https://"+channel.host());
        headers.put("Referer","https://"+channel.host()+"/");
        headers.put("Accept","*/*");
        if(!key.equals("k2") && !key.equals("frisbee")) {
            JSONObject site=new JSONObject(http.get(api+"/site/configurations?include=default&filter%5Benvironment%5D="+key+"&v=2",headers))
                .getJSONObject("settings").getJSONObject("site").getJSONObject("simulcast");
            if(!site.optBoolean("enabled",false)) throw new IOException("Discovery live channel is currently unavailable.");
            api=DiscoveryPolicy.endpoint(site.getString("sonicEndpoint"));
            realm=site.getString("sonicRealm");id=site.getString("channelId");
            if(!"it".equals(realm) || !id.matches("[0-9]{1,6}")) throw new IOException("Unsupported Discovery channel configuration.");
        }
        headers.put("X-Device-Info","STONEJS/1 (Unknown/Unknown; Windows/NT 10.0; Unknown)");
        headers.put("X-disco-params","realm="+realm);
        headers.put("X-disco-client","WEB:UNKNOWN:wbdatv:2.1.9");
        String token=new JSONObject(http.get(api+"/token?realm="+realm,headers)).getJSONObject("data").getJSONObject("attributes").getString("token");
        if(token.isEmpty()) throw new IOException("Discovery guest authorization unavailable.");
        headers.put("Authorization","Bearer "+token);
        JSONObject device=new JSONObject().put("adBlocker",false).put("drmSupported",true)
            .put("hdrCapabilities",new JSONArray().put("SDR")).put("hwDecodingCapabilities",new JSONArray())
            .put("soundCapabilities",new JSONArray().put("STEREO"));
        JSONObject browser=new JSONObject().put("name","chrome").put("version","140.0.0.0");
        JSONObject wisteria=new JSONObject().put("device",new JSONObject().put("browser",browser).put("type","desktop")).put("platform","desktop");
        JSONObject body=new JSONObject().put("deviceInfo",device).put("wisteriaProperties",wisteria).put("channelId",id);
        JSONArray streams=new JSONObject(http.post(api+"/playback/v3/channelPlaybackInfo",headers,body))
            .getJSONObject("data").getJSONObject("attributes").getJSONArray("streaming");
        // Some channels provide HLS only. Do not require DASH to be present.
        for(String type:List.of("hls","dash")) for(int i=0;i<streams.length();i++) {
            JSONObject stream=streams.getJSONObject(i);
            if(!type.equalsIgnoreCase(stream.optString("type")) || stream.optString("url").isEmpty()) continue;
            JSONObject protection=stream.optJSONObject("protection");
            if(protection!=null && (protection.optBoolean("drmEnabled",false) || protection.optBoolean("clearkeyEnabled",false)))
                throw new IOException("This Discovery stream requires unsupported protection.");
            String url=stream.getString("url");StreamResolver.requireHttps(url);
            // Authorization is used only for the API, never passed to a stream CDN.
            return new StreamResolver.Source(url,null,Map.of(),type.equals("hls")?MimeTypes.APPLICATION_M3U8:MimeTypes.APPLICATION_MPD);
        }
        throw new IOException("No supported Discovery live stream is available.");
    }
}
