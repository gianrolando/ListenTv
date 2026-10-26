// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

record EpgProgramme(String channel,String title,long start,long end) {
    boolean onAir(long now) { return start<=now && now<end; }
    int progress(long now) { return (int)Math.max(0,Math.min(100,(now-start)*100/Math.max(1,end-start))); }
    static EpgProgramme current(List<EpgProgramme> programmes,long now) {
        EpgProgramme current=null;
        if(programmes!=null) for(EpgProgramme p:programmes)
            if(p.onAir(now) && (current==null || p.start>=current.start)) current=p;
        return current;
    }
    static long xmlTime(String value) {
        Matcher m=Pattern.compile("^(\\d{12}(?:\\d{2})?)(?:\\s*([+-]\\d{4}|Z))?$").matcher(value==null?"":value.trim());
        if(!m.matches()) throw new IllegalArgumentException("Invalid XMLTV time.");
        String digits=m.group(1),offset=m.group(2)==null?"+0000":m.group(2);
        return LocalDateTime.parse(digits,DateTimeFormatter.ofPattern(digits.length()==14?"uuuuMMddHHmmss":"uuuuMMddHHmm")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)).toInstant(ZoneOffset.of(offset)).toEpochMilli();
    }
    static String channelId(String source,Set<String> known) {
        String id=source==null?"":source.toLowerCase(Locale.ROOT);
        if(id.endsWith(".it")) id=id.substring(0,id.length()-3);
        id=switch(id) {case "rainews24" -> "rainews";case "motortrend" -> "discoveryturbo";
            case "smtv" -> "sanmarinortv";case "smtvsport" -> "sanmarinortvsport";default -> id;};
        return known.contains(id)?id:null;
    }
    static boolean validTitle(String title) {
        String t=title==null?"":title.trim().toLowerCase(Locale.ROOT);
        return !t.isEmpty() && !t.equals("epg non disponibile") && !t.equals("programmazione non disponibile")
            && !t.equals("no information") && !t.equals("no programming information");
    }
}
