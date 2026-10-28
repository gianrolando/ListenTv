// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded public player-data parsing for the additional broadcasters. */
final class OtherChannelSources {
  private static final Pattern SPORT =
      Pattern.compile("(?:\\bSTREAM_URL\\s*=|\\bfile\\s*:)\\s*[\"']([^\"']+)[\"']");
  private static final Pattern IFRAME =
      Pattern.compile(
          "<iframe\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
  private static final Pattern POLICY =
      Pattern.compile("policyKey\\s*:\\s*[\"']([A-Za-z0-9._~+/=-]{16,16384})[\"']");

  record Brightcove(String account, String player, String video) {}

  static String sportitalia(String page) throws Exception {
    Matcher match = SPORT.matcher(page);
    while (match.find()) {
      String address = match.group(1).replace("\\/", "/").replace("&amp;", "&");
      try {
        return hls(address);
      } catch (IOException ignored) {
      }
    }
    throw new IOException("Sportitalia live source not found.");
  }

  static String soleEmbed(String page) throws Exception {
    Matcher match = IFRAME.matcher(page);
    while (match.find()) {
      URI uri = new URI(match.group(1).replace("&amp;", "&"));
      if ("stream24.ilsole24ore.com".equalsIgnoreCase(uri.getHost())
          && uri.getPath().matches("/embed_by_id/[0-9]+")) {
        StreamResolver.requireHttps(uri.toString());
        return uri.toString();
      }
    }
    throw new IOException("Il Sole 24 Ore TV player not found.");
  }

  static Brightcove brightcove(String page) throws IOException {
    String account = attribute(page, "data-account");
    String player = attribute(page, "data-player");
    String video = attribute(page, "data-video-id");
    if (!account.matches("[0-9]{1,24}")
        || !player.matches("[A-Za-z0-9_-]{1,80}")
        || !video.matches("[0-9]{1,24}")) throw new IOException("Invalid TV player identifiers.");
    return new Brightcove(account, player, video);
  }

  private static String attribute(String page, String name) throws IOException {
    Matcher match = Pattern.compile("\\b" + name + "\\s*=\\s*[\"']([^\"']+)[\"']").matcher(page);
    if (!match.find()) throw new IOException("TV player metadata missing.");
    return match.group(1);
  }

  static String policy(String player) throws IOException {
    Matcher match = POLICY.matcher(player);
    if (!match.find()) throw new IOException("TV playback policy unavailable.");
    return match.group(1);
  }

  static String soleStream(JSONObject response) throws Exception {
    JSONArray sources = response.getJSONArray("sources");
    for (int i = 0; i < sources.length(); i++) {
      JSONObject source = sources.getJSONObject(i);
      String type = source.optString("type");
      if (!type.equalsIgnoreCase("application/vnd.apple.mpegurl")
          && !type.equalsIgnoreCase("application/x-mpegurl")) continue;
      if (source.has("key_systems") && !source.isNull("key_systems")) continue;
      String address = source.optString("src");
      if (!address.isEmpty()) return hls(address);
    }
    throw new IOException("No supported Il Sole 24 Ore TV stream is available.");
  }

  static String koper(JSONObject json) throws Exception {
    JSONObject response = json.getJSONObject("response");
    if (!"tv.kp1".equals(response.getString("id")))
      throw new IOException("Unexpected RTV channel.");
    JSONArray files = response.getJSONArray("mediaFiles");
    for (int i = 0; i < files.length(); i++) {
      JSONObject file = files.getJSONObject(i);
      String base = file.optString("streamer"), path = file.optString("file");
      if (base.isEmpty() || !path.startsWith("/") || path.startsWith("//")) continue;
      // The file path is relative to the streamer's full prefix, not the host root.
      String address = base.replaceAll("/+$", "") + path;
      try {
        return hls(address);
      } catch (IOException ignored) {
      }
    }
    throw new IOException("Telecapodistria live source unavailable.");
  }

  private static String hls(String address) throws Exception {
    StreamResolver.requireHttps(address);
    URI uri = new URI(address);
    if (uri.getPath() == null || !uri.getPath().endsWith(".m3u8") || uri.getFragment() != null)
      throw new IOException("Invalid HTTPS live playlist.");
    return address;
  }
}
