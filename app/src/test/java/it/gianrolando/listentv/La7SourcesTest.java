package it.gianrolando.listentv;

import static org.junit.Assert.*;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class La7SourcesTest {
  private JSONObject config(String id) throws Exception {
    String channel = id.equals("1") ? "LA7" : "LA7D";
    String base = "https://d15umi5iaezxgx.cloudfront.net/" + channel + "/DRM/";
    return new JSONObject()
        .put("drmContentId", id)
        .put("mainLiveUrl", base + "DASH/Live.mpd|" + base + "HLS/Live.m3u8")
        .put("backupLiveUrl", base + "DASH-B/Live.mpd|" + base + "HLS-B/Live.m3u8")
        .put("drmTokenEndpoint", "https://svc-01.la7play.it/DRM/tk.php")
        .put(
            "drmLicenseServer",
            "fairplay|https://la7.prod.conax.cloud/fairplay/license;com.widevine.alpha|https://la7.prod.conax.cloud/widevine/license");
  }

  private String script(String data) {
    return "<script>self.__next_f.push(" + new JSONArray().put(1).put(data) + ");</script>";
  }

  private String page(JSONObject configuration) throws Exception {
    return script(
        "e:"
            + new JSONObject()
                .put(
                    "videoData",
                    new JSONObject()
                        .put("__typename", "NodeLiveStreaming")
                        .put("configurazionePlayer", configuration))
            + "\n");
  }

  @Test
  public void readsCurrentNextPlayerAndSelectsDashAndWidevine() throws Exception {
    for (String id : new String[] {"1", "2"}) {
      La7Sources.Configuration result = La7Sources.read(page(config(id)), id);
      assertEquals(id, result.contentId());
      assertTrue(result.dash().endsWith("/DRM/DASH/Live.mpd"));
      assertTrue(result.backupDash().endsWith("/DRM/DASH-B/Live.mpd"));
      assertEquals("https://la7.prod.conax.cloud/widevine/license", result.license());
      assertEquals("https://svc-01.la7play.it/DRM/tk.php", result.tokenEndpoint());
    }
  }

  @Test
  public void joinsSplitFlightChunksAndDoesNotSelectTheOtherChannel() throws Exception {
    String text =
        "e:"
            + new JSONObject().put("configurazionePlayer", config("1"))
            + "\n"
            + "f:"
            + new JSONObject().put("configurazionePlayer", config("2"))
            + "\n";
    int split = text.lastIndexOf("configurazionePlayer") + 8;
    La7Sources.Configuration result =
        La7Sources.read(script(text.substring(0, split)) + script(text.substring(split)), "2");
    assertTrue(result.dash().contains("/LA7D/"));
  }

  @Test
  public void preservesSignedManifestQueries() throws Exception {
    JSONObject value =
        config("1")
            .put(
                "mainLiveUrl",
                "https://cdn.example/Live.mpd?a=1&b=%2Flive|https://cdn.example/Live.m3u8");
    assertEquals(
        "https://cdn.example/Live.mpd?a=1&b=%2Flive", La7Sources.read(page(value), "1").dash());
  }

  @Test
  public void rejectsUntrustedAuthorizationAndNonDashSources() throws Exception {
    for (String address :
        new String[] {
          "http://svc-01.la7play.it/DRM/tk.php",
          "https://svc-01.la7play.it.evil.example/DRM/tk.php",
          "https://user:secret@svc-01.la7play.it/DRM/tk.php",
          "https://svc-01.la7play.it:444/DRM/tk.php"
        }) rejected(config("1").put("drmTokenEndpoint", address));
    rejected(
        config("1")
            .put("drmLicenseServer", "com.widevine.alpha|https://evil.example/widevine/license"));
    rejected(
        config("1")
            .put("drmLicenseServer", "fairplay|https://la7.prod.conax.cloud/fairplay/license"));
    rejected(config("1").put("mainLiveUrl", "http://cdn.example/Live.mpd"));
    rejected(config("1").put("mainLiveUrl", "https://cdn.example/Live.m3u8"));
  }

  private void rejected(JSONObject value) throws Exception {
    try {
      La7Sources.read(page(value), "1");
      fail("Expected invalid source rejection");
    } catch (IOException expected) {
    }
  }

  @Test
  public void rejectsMissingConfigurationWrongChannelAndOversizedPages() throws Exception {
    for (String value :
        new String[] {
          "<html>No player</html>", page(config("2")), "x".repeat(2 * 1024 * 1024 + 1)
        }) {
      try {
        La7Sources.read(value, "1");
        fail("Expected missing configuration rejection");
      } catch (IOException expected) {
      }
    }
  }

  @Test
  public void readsOnlyValidTokenValuesAndRejectsHeaderInjection() throws Exception {
    for (String key : new String[] {"preAuthToken", "token", "auth"})
      assertEquals(
          "guest.token", La7Sources.token(new JSONObject().put(key, "guest.token").toString()));
    for (String response :
        new String[] {
          "{}",
          "{\"preAuthToken\":123}",
          new JSONObject().put("preAuthToken", "bad\r\nheader").toString()
        }) {
      try {
        La7Sources.token(response);
        fail("Expected invalid token rejection");
      } catch (IOException expected) {
      }
    }
  }
}
