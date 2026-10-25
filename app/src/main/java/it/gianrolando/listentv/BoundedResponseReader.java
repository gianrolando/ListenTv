package it.gianrolando.listentv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Reads complete API responses or only the HTML head, with an explicit byte limit. */
final class BoundedResponseReader {
    static String read(InputStream in, int limit, boolean headOnly) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (out.size() < limit) {
            int count = in.read(buffer, 0, Math.min(buffer.length, limit - out.size()));
            if (count == -1) {
                if (headOnly) throw new IOException("HTML head not found.");
                return out.toString(StandardCharsets.UTF_8.name());
            }
            out.write(buffer, 0, count);
            if (headOnly) {
                String text = out.toString(StandardCharsets.UTF_8.name());
                int end = text.toLowerCase(Locale.ROOT).indexOf("</head>");
                if (end >= 0) return text.substring(0, end + 7);
            }
        }
        if (!headOnly && in.read() == -1) return out.toString(StandardCharsets.UTF_8.name());
        throw new IOException(headOnly ? "HTML head exceeds size limit." : "Response too large.");
    }
}
