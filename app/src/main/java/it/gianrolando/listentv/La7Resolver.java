// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import android.net.Uri;
import androidx.media3.common.MimeTypes;
import java.util.Map;

/** Mirrors the official live player's current configuration and guest DRM authorization. */
final class La7Resolver {
  private final StreamResolver http;

  La7Resolver(StreamResolver http) {
    this.http = http;
  }

  StreamResolver.Source resolve(String key) throws Exception {
    String page =
        switch (key) {
          case "" -> "https://www.la7.it/dirette-tv";
          case "cinema" -> "https://www.la7.it/live-la7cinema";
          default -> throw new java.io.IOException("Unknown La7 channel.");
        };
    La7Sources.Configuration config =
        La7Sources.read(http.get(page, Map.of()), key.isEmpty() ? "1" : "2");
    String address = config.dash();
    // Check the current manifest before requesting a short-lived token. The official player
    // also supplies a backup stream; never substitute an unprotected or third-party source.
    try {
      http.get(address, Map.of());
    } catch (Exception unavailable) {
      if (config.backupDash() == null) throw unavailable;
      address = config.backupDash();
      http.get(address, Map.of());
    }
    String tokenUrl =
        Uri.parse(config.tokenEndpoint())
            .buildUpon()
            .appendQueryParameter("id", config.contentId())
            .appendQueryParameter("contentType", "LIVE")
            .appendQueryParameter("v", String.valueOf(System.currentTimeMillis()))
            .build()
            .toString();
    String token = La7Sources.token(http.get(tokenUrl, Map.of("Referer", page)));
    return new StreamResolver.Source(
        address,
        config.license(),
        Map.of("preAuthorization", token, "Origin", "https://www.la7.it", "Referer", page),
        MimeTypes.APPLICATION_MPD);
  }
}
