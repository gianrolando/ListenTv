package it.gianrolando.listentv;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

public class MediasetPolicyTest {
    @Test public void acceptsBothLiveSelectorGenerations() {
        assertTrue(MediasetPolicy.trustedSelector("https://link-ott-prod.mediaset.net/smil"));
        assertTrue(MediasetPolicy.trustedSelector("https://link.api.eu.theplatform.com/s/PR1GhC/media/example"));
        assertTrue(MediasetPolicy.trustedSelector("https://link.theplatform.eu/s/PR1GhC/example"));
    }

    @Test public void rejectsUntrustedDestinationsForTheAuthorizationToken() {
        assertFalse(MediasetPolicy.trustedSelector("http://link-ott-prod.mediaset.net/smil"));
        assertFalse(MediasetPolicy.trustedSelector("https://link-ott-prod.mediaset.net.attacker.example/smil"));
        assertFalse(MediasetPolicy.trustedSelector("https://attacker.example/link-ott-prod.mediaset.net"));
        assertFalse(MediasetPolicy.trustedSelector("https://user@link-ott-prod.mediaset.net/smil"));
        assertFalse(MediasetPolicy.trustedSelector("https://link-ott-prod.mediaset.net:444/smil"));
    }

    @Test public void keepsEveryQualityAndGeographicConstraint() throws Exception {
        assertEquals("HR,browser,geoIT|geoNo,widevine:SD,browser,geoIT|geoNo,widevine",
            MediasetPolicy.widevineAssets("HR,browser,geoIT|geoNo:SD,browser,geoIT|geoNo"));
        assertEquals("HD,browser,widevine,geoIT", MediasetPolicy.widevineAssets("HD,browser,widevine,geoIT"));
    }

    @Test public void neverTreatsPlayReadyAsWidevine() throws Exception {
        MediasetPolicy.requireWidevineManifest("<ContentProtection schemeIdUri='urn:uuid:EDEF8BA9-79D6-4ACE-A3C8-27DCD51D21ED'/>");
        try {
            MediasetPolicy.requireWidevineManifest("<ContentProtection schemeIdUri='urn:uuid:9A04F079-9840-4286-AB92-E65BE0885F95'/>");
            fail("PlayReady-only manifests must not be sent to a Widevine session");
        } catch (IOException expected) { }
    }
}
