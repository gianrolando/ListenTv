package it.gianrolando.listentv;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OtherChannelSourcesTest {
  @Test
  public void readsSportitaliaCurrentAndLegacyPlayerAssignments() throws Exception {
    assertEquals(
        "https://cdn.example/live.m3u8",
        OtherChannelSources.sportitalia("const STREAM_URL = \"https://cdn.example/live.m3u8\";"));
    assertEquals(
        "https://cdn.example/live.m3u8?a=1&b=2",
        OtherChannelSources.sportitalia("file: 'https:\\/\\/cdn.example/live.m3u8?a=1&amp;b=2'"));
  }

  @Test
  public void followsOnlyTheOfficialSoleEmbedAndValidatesPlayerIdentifiers() throws Exception {
    String expected =
        "https://stream24.ilsole24ore.com/embed_by_id/6374358424112?autoplay=1&mute=1";
    assertEquals(
        expected,
        OtherChannelSources.soleEmbed(
            "<iframe src='https://ads.example/player'></iframe><iframe src='"
                + expected.replace("&", "&amp;")
                + "'></iframe>"));
    var player =
        OtherChannelSources.brightcove(
            "<video data-video-id='6374358424112' data-account='18140046001'"
                + " data-player='zlHGHZ4oU'>");
    assertEquals("18140046001", player.account());
    assertEquals("zlHGHZ4oU", player.player());
    try {
      OtherChannelSources.brightcove("data-account='1/evil' data-player='abc' data-video-id='1'");
      fail();
    } catch (java.io.IOException expectedFailure) {
    }
    try {
      OtherChannelSources.soleEmbed(
          "<iframe src='https://stream24.ilsole24ore.com.evil.example/embed_by_id/123'>");
      fail();
    } catch (java.io.IOException expectedFailure) {
    }
  }

  @Test
  public void selectsUnprotectedHlsFromBrightcoveAndReadsPublicPolicy() throws Exception {
    String key = "public-policy-key-123";
    assertEquals(key, OtherChannelSources.policy("config={policyKey:\"" + key + "\"}"));
    JSONObject protectedSource =
        new JSONObject()
            .put("type", "application/vnd.apple.mpegurl")
            .put("src", "https://cdn.example/protected.m3u8")
            .put("key_systems", new JSONObject().put("widevine", true));
    JSONObject clear =
        new JSONObject()
            .put("type", "application/vnd.apple.mpegurl")
            .put("src", "https://cdn.example/live.m3u8?token=example");
    assertEquals(
        clear.getString("src"),
        OtherChannelSources.soleStream(
            new JSONObject().put("sources", new JSONArray().put(protectedSource).put(clear))));
    try {
      OtherChannelSources.soleStream(
          new JSONObject().put("sources", new JSONArray().put(protectedSource)));
      fail();
    } catch (java.io.IOException expectedFailure) {
    }
  }

  @Test
  public void retainsRtvStreamPrefixAndExpiringQuery() throws Exception {
    JSONObject response =
        new JSONObject()
            .put("id", "tv.kp1")
            .put(
                "mediaFiles",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("streamer", "https://cdn.example/hls/live/tvkp/")
                            .put("file", "/playlist.m3u8?hdnts=exp=123~acl=*~hmac=test")));
    assertEquals(
        "https://cdn.example/hls/live/tvkp/playlist.m3u8?hdnts=exp=123~acl=*~hmac=test",
        OtherChannelSources.koper(new JSONObject().put("response", response)));
    response.put("id", "radio.kp1");
    try {
      OtherChannelSources.koper(new JSONObject().put("response", response));
      fail();
    } catch (java.io.IOException expectedFailure) {
    }
  }

  @Test
  public void rejectsInsecureAndCredentialBearingSportStreams() throws Exception {
    for (String value :
        new String[] {
          "http://cdn.example/live.m3u8",
          "https://user:secret@cdn.example/live.m3u8",
          "https://cdn.example/player.js"
        }) {
      try {
        OtherChannelSources.sportitalia("STREAM_URL = '" + value + "';");
        fail();
      } catch (java.io.IOException expectedFailure) {
      }
    }
  }
}
