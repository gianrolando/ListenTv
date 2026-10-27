package it.gianrolando.listentv;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

public class DiscoveryPolicyTest {
  @Test
  public void acceptsOfficialGuestApi() throws Exception {
    assertEquals(
        DiscoveryPolicy.API, DiscoveryPolicy.endpoint("https://public.aurora.enhanced.live/"));
  }

  @Test
  public void rejectsAuthorizationRedirectTargets() throws Exception {
    for (String url :
        new String[] {
          "http://public.aurora.enhanced.live",
          "https://public.aurora.enhanced.live.attacker.test",
          "https://attacker.test",
          "https://user@public.aurora.enhanced.live",
          "https://public.aurora.enhanced.live:8443",
          "https://public.aurora.enhanced.live/path",
          "https://public.aurora.enhanced.live?realm=other"
        }) {
      try {
        DiscoveryPolicy.endpoint(url);
        fail(url);
      } catch (IOException expected) {
      }
    }
  }

  @Test(expected = IOException.class)
  public void rejectsUnknownChannelKeys() throws Exception {
    DiscoveryPolicy.channel("https://attacker.test");
  }
}
