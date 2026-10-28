// SPDX-License-Identifier: GPL-3.0-or-later
// Public provider procedures adapted from WorldLiveTV, checked against official players.
package it.gianrolando.listentv;

import androidx.media3.common.MimeTypes;
import java.util.Map;
import org.json.JSONObject;

final class OtherChannelResolver {
  private final StreamResolver http;

  OtherChannelResolver(StreamResolver http) {
    this.http = http;
  }

  StreamResolver.Source resolve(String provider) throws Exception {
    String address =
        switch (provider) {
          case "sportitalia" ->
              OtherChannelSources.sportitalia(
                  http.get("https://sportitalialive.it/sihd/", Map.of()));
          case "sole24" -> sole24();
          case "rtvslo" ->
              OtherChannelSources.koper(
                  new JSONObject(
                      http.get(
                          "https://api.rtvslo.si/ava/getLiveStream/tv.kp1?client_id=82013fb3a531d5414f478747c1aca622",
                          Map.of())));
          default -> throw new java.io.IOException("Unknown broadcaster.");
        };
    return new StreamResolver.Source(address, null, Map.of(), MimeTypes.APPLICATION_M3U8);
  }

  private String sole24() throws Exception {
    String page = http.get("https://s24ore.it/flussotv", Map.of());
    String embed = OtherChannelSources.soleEmbed(page);
    var player = OtherChannelSources.brightcove(http.get(embed, Map.of()));
    String javascript =
        http.get(
            "https://players.brightcove.net/"
                + player.account()
                + "/"
                + player.player()
                + "_default/index.min.js",
            Map.of());
    String policy = OtherChannelSources.policy(javascript);
    String response =
        http.get(
            "https://edge.api.brightcove.com/playback/v1/accounts/"
                + player.account()
                + "/videos/"
                + player.video(),
            Map.of("Accept", "application/json;pk=" + policy));
    return OtherChannelSources.soleStream(new JSONObject(response));
  }
}
