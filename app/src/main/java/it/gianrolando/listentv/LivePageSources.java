// SPDX-License-Identifier: GPL-3.0-or-later
// San Marino player procedure adapted from WLTV Group's broadcaster/smtv.py.
package it.gianrolando.listentv;

import java.io.IOException;
import java.net.URI;
import java.util.regex.*;

final class LivePageSources {
    private static final Pattern SOURCE=Pattern.compile("\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']");
    static String hls(String body) throws IOException {
        Matcher match=SOURCE.matcher(body);
        while(match.find()) {
            String url=match.group(1).replace("\\/","/").replace("&amp;","&");
            try {
                URI uri=new URI(url);
                if("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null
                    && uri.getPath()!=null && uri.getPath().endsWith(".m3u8")) return url;
            } catch(java.net.URISyntaxException ignored) { }
        }
        throw new IOException("No HTTPS live playlist found in the broadcaster player.");
    }
}
