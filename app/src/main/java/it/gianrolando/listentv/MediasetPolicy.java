package it.gianrolando.listentv;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class MediasetPolicy {
    private static final Set<String> SELECTORS = Set.of(
        "link.api.eu.theplatform.com", "link.theplatform.eu", "link-ott-prod.mediaset.net");

    static boolean trustedSelector(String address) {
        try {
            URI uri = new URI(address);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                && uri.getHost() != null && (uri.getPort() == -1 || uri.getPort() == 443)
                && SELECTORS.contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (Exception ignored) { return false; }
    }

    /** Preserve the provider's quality/territory filters while choosing Android DRM. */
    static String widevineAssets(String assets) throws IOException {
        if (assets == null || assets.isBlank()) throw new IOException("Mediaset asset filters missing.");
        String[] alternatives = assets.split(":", -1);
        for (int i = 0; i < alternatives.length; i++) {
            if (alternatives[i].isBlank()) throw new IOException("Invalid Mediaset asset filter.");
            if (!Pattern.compile("(?:^|,)widevine(?:,|$)", Pattern.CASE_INSENSITIVE).matcher(alternatives[i]).find())
                alternatives[i] += ",widevine";
        }
        return String.join(":", alternatives);
    }

    static void requireWidevineManifest(String manifest) throws IOException {
        if (!Pattern.compile("schemeIdUri\\s*=\\s*[\"']urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed[\"']", Pattern.CASE_INSENSITIVE)
                .matcher(manifest).find())
            throw new IOException("The authorized manifest does not advertise Widevine.");
    }
}
