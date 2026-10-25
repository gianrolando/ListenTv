// SPDX-License-Identifier: GPL-3.0-or-later
// Guest authorization and SMIL resolution adapted from WLTV Group, WorldLiveTV 1.2.14.
package it.gianrolando.listentv;

import android.net.Uri;
import android.util.Xml;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/** Fresh guest authorization per play request; no account credentials or persisted tokens. */
final class MediasetResolver {
    private static final String HOST = "https://mediasetinfinity.mediaset.it";
    private static final String API = "https://api-ott-prod-fe.mediaset.net/PROD/play/";
    private static final Map<String,String> HEADERS = Map.of(
        "Origin", HOST, "Referer", HOST + "/", "Accept", "application/json, text/plain, */*");
    private final StreamResolver http;

    MediasetResolver(StreamResolver http) { this.http = http; }

    static final class ResolutionException extends IOException {
        final String stage;
        ResolutionException(String stage, Exception cause) { super("Mediaset resolution failed at " + stage, cause); this.stage=stage; }
    }
    private String stage="M01";
    StreamResolver.Source resolve(String channelCode) throws Exception {
        try { return resolveInternal(channelCode); }
        catch(Exception error) { throw new ResolutionException(stage,error); }
    }
    private StreamResolver.Source resolveInternal(String channelCode) throws Exception {
        if (!channelCode.matches("[A-Z0-9]{2}")) throw new IOException("Invalid Mediaset channel code.");
        String home = http.getHtmlHead(HOST, HEADERS);
        Matcher meta = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(home);
        String appName = null;
        while (meta.find()) {
            String tag = meta.group();
            if (Pattern.compile("(?:name|property)\\s*=\\s*[\"']app-name[\"']", Pattern.CASE_INSENSITIVE).matcher(tag).find()) {
                Matcher content = Pattern.compile("content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(tag);
                if (content.find()) { appName = content.group(1).replace("&amp;", "&"); break; }
            }
        }
        if (appName == null) throw new IOException("Mediaset application metadata changed.");
        stage="M02";
        JSONObject login = response(http.post(API + "idm/anonymous/login/v2.0", HEADERS,
            new JSONObject().put("appName", appName).put("client_id", UUID.randomUUID().toString())));
        String token = login.getString("beToken");
        String sid = login.getString("sid");
        if (token.isBlank() || sid.isBlank()) throw new IOException("Mediaset guest authorization unavailable.");
        Map<String,String> authorized = Map.of("Origin", HOST, "Referer", HOST + "/",
            "Accept", "application/json", "Authorization", "Bearer " + token);
        String checkUrl = Uri.parse(API + "playback/check/v2.0").buildUpon().appendQueryParameter("sid", sid).build().toString();
        stage="M03";
        JSONObject check = response(http.post(checkUrl, authorized,
            new JSONObject().put("channelCode", channelCode).put("streamType", "LIVE")
                .put("delivery", "Streaming").put("createDevice", "true").put("overrideAppName", appName)));
        stage="M04";
        JSONObject selector = check.getJSONObject("mediaSelector");
        String selectorUrl = selector.getString("url");
        StreamResolver.requireHttps(selectorUrl);
        // Bearer tokens must only be passed to the broadcaster's known media selector.
        String selectorHost = Uri.parse(selectorUrl).getHost();
        if (!("link.api.eu.theplatform.com".equals(selectorHost) || "link.theplatform.eu".equals(selectorHost)))
            throw new IOException("Unrecognized Mediaset media selector.");
        Uri.Builder uri = Uri.parse(selectorUrl).buildUpon();
        var keys = selector.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.equals("url") && !key.equals("auth") && !key.equals("formats"))
                uri.appendQueryParameter(key, String.valueOf(selector.get(key)));
        }
        // Request the Android-compatible format; HLS can select Apple FairPlay.
        uri.appendQueryParameter("formats", "MPEG-DASH").appendQueryParameter("auth", token);
        String smil=http.get(uri.build().toString(), HEADERS);
        stage="M05";
        return parseSmil(smil, token);
    }

    private JSONObject response(String body) throws Exception {
        JSONObject json = new JSONObject(body);
        if (!json.optBoolean("isOk", false)) throw new IOException("Mediaset did not authorize this live channel.");
        return json.getJSONObject("response");
    }

    private StreamResolver.Source parseSmil(String smil, String token) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(smil));
        String url = null, security = "", pid = null;
        boolean inRef = false;
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG && parser.getName().equals("ref")) {
                url = parser.getAttributeValue(null, "src");
                security = parser.getAttributeValue(null, "security");
                pid = null; inRef = true;
            } else if (event == XmlPullParser.START_TAG && inRef && parser.getName().equals("param")) {
                String value = parser.getAttributeValue(null, "value");
                if (value != null) {
                    Matcher match = Pattern.compile("(?:^|\\|)pid=([^|]+)").matcher(value);
                    if (match.find()) pid = match.group(1);
                }
            } else if (event == XmlPullParser.END_TAG && parser.getName().equals("ref")) {
                inRef = false;
                if (url == null || url.isBlank()) continue;
                StreamResolver.requireHttps(url);
                if ("commonEncryption".equals(security)) {
                    if (pid == null) throw new IOException("Mediaset Widevine release identifier missing.");
                    String license = Uri.parse("https://widevine.entitlement.theplatform.eu/wv/web/ModularDrm/getRawWidevineLicense")
                        .buildUpon().appendQueryParameter("releasePid", pid)
                        .appendQueryParameter("account", "http://access.auth.theplatform.com/data/Account/2702976343")
                        .appendQueryParameter("schema", "1.0").appendQueryParameter("token", token).build().toString();
                    return new StreamResolver.Source(url, license, Map.of("Origin", HOST, "Referer", HOST + "/",
                        "Content-Type", "application/octet-stream"));
                }
                if (security == null || security.isEmpty() || security.equals("none"))
                    return new StreamResolver.Source(url, null, Map.of());
                // Never treat an unsupported DRM scheme as an unprotected stream.
            }
        }
        throw new IOException("No Android-compatible Mediaset stream was authorized.");
    }
}
