// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.io.IOException;
import java.net.URI;

/** Restrict guest authorization to the broadcaster's known public API. */
final class DiscoveryPolicy {
    static final String API="https://public.aurora.enhanced.live";
    record Channel(String id,String host) { }
    static Channel channel(String key) throws IOException {
        return switch(key) {
            case "nove" -> new Channel("3","nove.tv");
            case "realtime" -> new Channel("2","realtime.it");
            case "dmaxit" -> new Channel("4","dmax.it");
            case "giallo" -> new Channel("27","giallotv.it");
            case "foodnetwork" -> new Channel("6","foodnetwork.it");
            case "hgtvit" -> new Channel("13","homegardentv.it");
            case "discoverychannelit" -> new Channel("12","discovery.it");
            case "motortrend" -> new Channel("11","discoveryturbo.it");
            case "warnertvit" -> new Channel("5","warnertv.it");
            case "k2" -> new Channel("24","k2tv.it");
            case "frisbee" -> new Channel("26","frisbeetv.it");
            default -> throw new IOException("Unknown Discovery channel.");
        };
    }
    static String endpoint(String address) throws IOException {
        try {
            URI uri=new URI(address);
            if(!"https".equalsIgnoreCase(uri.getScheme()) || !"public.aurora.enhanced.live".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo()!=null || (uri.getPort()!=-1 && uri.getPort()!=443)
                || uri.getQuery()!=null || uri.getFragment()!=null || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
                throw new IOException("Untrusted Discovery authorization endpoint.");
            return API;
        } catch(java.net.URISyntaxException error) { throw new IOException("Invalid Discovery endpoint.",error); }
    }
}
