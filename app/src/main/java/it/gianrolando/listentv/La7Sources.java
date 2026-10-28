// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Reads public player settings from the site's Next.js data, without executing JavaScript. */
final class La7Sources {
  private static final int PAGE_LIMIT = 2 * 1024 * 1024;
  private static final Pattern FLIGHT =
      Pattern.compile(
          "self\\.__next_f\\.push\\(\\s*(\\[.*?\\])\\s*\\)\\s*;?\\s*</script\\s*>", Pattern.DOTALL);
  private static final Pattern SETTINGS = Pattern.compile("\"configurazionePlayer\"\\s*:\\s*");

  record Configuration(
      String dash, String backupDash, String tokenEndpoint, String contentId, String license) {}

  static Configuration read(String page, String expectedContentId) throws IOException {
    if (!expectedContentId.equals("1") && !expectedContentId.equals("2"))
      throw new IOException("Unknown La7 channel.");
    if (page == null || page.length() > PAGE_LIMIT) throw new IOException("Invalid La7 page size.");
    StringBuilder flight = new StringBuilder();
    Matcher scripts = FLIGHT.matcher(page);
    int chunks = 0;
    while (scripts.find()) {
      if (++chunks > 128) throw new IOException("Too many La7 page chunks.");
      try {
        JSONArray chunk = new JSONArray(scripts.group(1));
        if (chunk.length() == 2
            && chunk.optInt(0, -1) == 1
            && chunk.opt(1) instanceof String text) {
          if (flight.length() + text.length() > PAGE_LIMIT)
            throw new IOException("La7 player data too large.");
          flight.append(text);
        }
      } catch (JSONException invalid) {
        throw new IOException("Invalid La7 player data.", invalid);
      }
    }
    Matcher settings = SETTINGS.matcher(flight);
    int configurations = 0;
    while (settings.find()) {
      if (++configurations > 64) throw new IOException("Too many La7 player configurations.");
      try {
        Object value = new JSONTokener(flight.substring(settings.end())).nextValue();
        if (!(value instanceof JSONObject config)
            || !expectedContentId.equals(config.optString("drmContentId"))) continue;
        String tokenEndpoint = config.getString("drmTokenEndpoint");
        requireEndpoint(tokenEndpoint, "svc-01.la7play.it", "/DRM/tk.php");
        String license = null;
        for (String entry : config.getString("drmLicenseServer").split(";")) {
          String[] fields = entry.split("\\|", 2);
          if (fields.length == 2 && fields[0].trim().equalsIgnoreCase("com.widevine.alpha")) {
            license = fields[1].trim();
            break;
          }
        }
        if (license == null) throw new IOException("La7 Widevine configuration missing.");
        requireEndpoint(license, "la7.prod.conax.cloud", "/widevine/license");
        String backup = config.optString("backupLiveUrl", "");
        return new Configuration(
            dash(config.getString("mainLiveUrl")),
            backup.isBlank() ? null : dash(backup),
            tokenEndpoint,
            expectedContentId,
            license);
      } catch (JSONException invalid) {
        throw new IOException("Incomplete La7 player configuration.", invalid);
      }
    }
    throw new IOException("La7 live player configuration not found.");
  }

  static String token(String body) throws IOException {
    try {
      JSONObject response = new JSONObject(body);
      for (String name : new String[] {"preAuthToken", "token", "auth"}) {
        Object value = response.opt(name);
        if (!(value instanceof String token) || token.isBlank()) continue;
        if (token.length() > 16384) throw new IOException("Invalid La7 authorization token.");
        for (int i = 0; i < token.length(); i++)
          if (Character.isISOControl(token.charAt(i)))
            throw new IOException("Invalid La7 authorization token.");
        return token;
      }
    } catch (JSONException invalid) {
      throw new IOException("Invalid La7 authorization response.", invalid);
    }
    throw new IOException("La7 authorization unavailable.");
  }

  private static String dash(String pair) throws IOException {
    String address = pair.split("\\|", -1)[0].trim();
    URI uri = https(address);
    if (uri.getPath() == null || !uri.getPath().endsWith(".mpd"))
      throw new IOException("La7 DASH source missing.");
    return address;
  }

  private static void requireEndpoint(String address, String host, String path) throws IOException {
    URI uri = https(address);
    if (!host.equalsIgnoreCase(uri.getHost())
        || !path.equals(uri.getPath())
        || uri.getQuery() != null) throw new IOException("Untrusted La7 authorization endpoint.");
  }

  private static URI https(String address) throws IOException {
    try {
      URI uri = new URI(address);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getFragment() != null
          || (uri.getPort() != -1 && uri.getPort() != 443))
        throw new IOException("Invalid La7 HTTPS endpoint.");
      return uri;
    } catch (URISyntaxException invalid) {
      throw new IOException("Invalid La7 endpoint.", invalid);
    }
  }
}
