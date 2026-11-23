package it.gianrolando.listentv;

import static org.junit.Assert.*;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class StreamResolverTest {
  private MediaItem item() {
    return new MediaItem.Builder().setMediaId("channel").build();
  }

  @Test
  public void mediasetKeepsAuthorizationAndAllowsRotatingKeys() throws Exception {
    Map<String, String> headers = Map.of("Origin", "https://mediasetinfinity.mediaset.it");
    MediaItem result =
        StreamResolver.configureSource(
            item(),
            new StreamResolver.Source(
                "https://example.test/live.mpd", "https://example.test/license", headers),
            true);
    assertEquals("channel", result.mediaId);
    assertEquals(MimeTypes.APPLICATION_MPD, result.localConfiguration.mimeType);
    MediaItem.DrmConfiguration drm = result.localConfiguration.drmConfiguration;
    assertNotNull(drm);
    assertEquals(C.WIDEVINE_UUID, drm.scheme);
    assertTrue(drm.multiSession);
    assertEquals("https://example.test/license", drm.licenseUri.toString());
    assertEquals(headers, drm.licenseRequestHeaders);
  }

  @Test
  public void unencryptedMediasetDoesNotAcquireADrmSession() throws Exception {
    MediaItem result =
        StreamResolver.configureSource(
            item(),
            new StreamResolver.Source("https://example.test/live.mpd", null, Map.of()),
            true);
    assertEquals(MimeTypes.APPLICATION_MPD, result.localConfiguration.mimeType);
    assertNull(result.localConfiguration.drmConfiguration);
  }

  @Test
  public void otherBroadcastersRetainTheirDrmAndMediaType() throws Exception {
    MediaItem result =
        StreamResolver.configureSource(
            item(),
            new StreamResolver.Source(
                "https://example.test/live.m3u8",
                "https://example.test/license",
                Map.of(),
                MimeTypes.APPLICATION_M3U8),
            false);
    assertEquals(MimeTypes.APPLICATION_M3U8, result.localConfiguration.mimeType);
    assertFalse(result.localConfiguration.drmConfiguration.multiSession);
  }

  @Test
  public void rejectsInsecureLicenseEndpoints() throws Exception {
    assertThrows(
        java.io.IOException.class,
        () ->
            StreamResolver.configureSource(
                item(),
                new StreamResolver.Source(
                    "https://example.test/live.mpd", "http://example.test/license", Map.of()),
                true));
  }
}
