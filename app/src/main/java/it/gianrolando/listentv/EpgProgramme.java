// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

record EpgProgramme(String channel, String title, long start, long end) {
  private static final Pattern XML_TIME =
      Pattern.compile("^(\\d{12}(?:\\d{2})?)(?:\\s*([+-]\\d{4}|Z))?$");
  private static final DateTimeFormatter FULL_TIME =
      DateTimeFormatter.ofPattern("uuuuMMddHHmmss", Locale.ROOT)
          .withResolverStyle(java.time.format.ResolverStyle.STRICT);
  private static final DateTimeFormatter SHORT_TIME =
      DateTimeFormatter.ofPattern("uuuuMMddHHmm", Locale.ROOT)
          .withResolverStyle(java.time.format.ResolverStyle.STRICT);

  boolean retain(long now) {
    long duration = end - start;
    return channel != null
        && !channel.isBlank()
        && validTitle(title)
        && title.length() <= 300
        && duration > 0
        && duration <= 24 * 60 * 60 * 1000L
        && end > now - 6 * 60 * 60 * 1000L
        && start < now + 48 * 60 * 60 * 1000L;
  }

  boolean onAir(long now) {
    return start <= now && now < end;
  }

  int progress(long now) {
    return (int) Math.max(0, Math.min(100, (now - start) * 100 / Math.max(1, end - start)));
  }

  static EpgProgramme current(List<EpgProgramme> programmes, long now) {
    EpgProgramme current = null;
    if (programmes != null)
      for (EpgProgramme p : programmes)
        if (p.onAir(now) && (current == null || p.start >= current.start)) current = p;
    return current;
  }

  /** Preserve usable cached slots when only part of a daily schedule can be fetched. */
  static List<EpgProgramme> merge(List<EpgProgramme> cached, List<EpgProgramme> fresh, long now) {
    record Slot(String channel, long start) {}
    Map<Slot, EpgProgramme> merged = new LinkedHashMap<>();
    for (List<EpgProgramme> source : List.of(cached, fresh))
      for (EpgProgramme programme : source)
        if (programme.retain(now))
          merged.put(new Slot(programme.channel(), programme.start()), programme);
    return List.copyOf(merged.values());
  }

  static long xmlTime(String value) {
    Matcher m = XML_TIME.matcher(value == null ? "" : value.trim());
    if (!m.matches()) throw new IllegalArgumentException("Invalid XMLTV time.");
    String digits = m.group(1), offset = m.group(2) == null ? "+0000" : m.group(2);
    return LocalDateTime.parse(digits, digits.length() == 14 ? FULL_TIME : SHORT_TIME)
        .toInstant(ZoneOffset.of(offset))
        .toEpochMilli();
  }

  static String channelId(String source, Set<String> known) {
    String id = source == null ? "" : source.toLowerCase(Locale.ROOT);
    if (id.endsWith(".it")) id = id.substring(0, id.length() - 3);
    id =
        switch (id) {
          case "rainews24" -> "rainews";
          case "motortrend" -> "discoveryturbo";
          case "smtv" -> "sanmarinortv";
          case "smtvsport" -> "sanmarinortvsport";
          default -> id;
        };
    return known.contains(id) ? id : null;
  }

  static boolean validTitle(String title) {
    String t = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    return !t.isEmpty()
        && !t.equals("epg non disponibile")
        && !t.equals("programmazione non disponibile")
        && !t.equals("no information")
        && !t.equals("no programming information");
  }
}
