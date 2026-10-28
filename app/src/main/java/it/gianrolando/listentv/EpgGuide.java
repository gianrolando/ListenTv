// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import android.content.Context;
import android.os.*;
import android.util.AtomicFile;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import org.json.*;
import org.tukaani.xz.XZInputStream;

/** Shared, bounded guide cache. Playback never waits for a schedule download. */
final class EpgGuide {
  private static EpgGuide instance;

  static synchronized EpgGuide get(Context context) {
    if (instance == null) instance = new EpgGuide(context);
    return instance;
  }

  private static final long REFRESH = 6 * 60 * 60 * 1000L, RETRY = 30 * 60 * 1000L;
  private final Context context;
  private final AtomicFile cache;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Set<Runnable> listeners = new HashSet<>();
  private volatile Map<String, List<EpgProgramme>> schedules = Map.of();
  private volatile boolean loaded = false;
  private long updated = 0, lastAttempt = -RETRY;
  private boolean busy = false;

  private EpgGuide(Context context) {
    this.context = context.getApplicationContext();
    cache = new AtomicFile(new File(this.context.getFilesDir(), "programme-guide-v1.json"));
  }

  EpgProgramme current(String id) {
    return EpgProgramme.current(schedules.get(id), System.currentTimeMillis());
  }

  boolean loading() {
    return !loaded;
  }

  String range(EpgProgramme programme) {
    java.text.DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
    return format.format(new Date(programme.start()))
        + " – "
        + format.format(new Date(programme.end()));
  }

  synchronized void addListener(Runnable listener) {
    listeners.add(listener);
    ensureFresh();
  }

  synchronized void removeListener(Runnable listener) {
    listeners.remove(listener);
  }

  synchronized void ensureFresh() {
    long age = System.currentTimeMillis() - updated, elapsed = SystemClock.elapsedRealtime();
    if (busy || (loaded && age >= 0 && age < REFRESH) || (loaded && elapsed - lastAttempt < RETRY))
      return;
    busy = true;
    lastAttempt = elapsed;
    worker.execute(this::refresh);
  }

  private void publish(List<EpgProgramme> programmes) {
    Map<String, List<EpgProgramme>> grouped = new HashMap<>();
    long now = System.currentTimeMillis();
    for (EpgProgramme p : new LinkedHashSet<>(programmes))
      if (p.retain(now)) grouped.computeIfAbsent(p.channel(), key -> new ArrayList<>()).add(p);
    Map<String, List<EpgProgramme>> frozen = new HashMap<>();
    grouped.forEach(
        (id, list) -> {
          list.sort(Comparator.comparingLong(EpgProgramme::start));
          frozen.put(id, List.copyOf(list));
        });
    schedules = Map.copyOf(frozen);
    notifyListeners();
  }

  private void notifyListeners() {
    main.post(
        () -> {
          List<Runnable> copy;
          synchronized (this) {
            copy = List.copyOf(listeners);
          }
          for (Runnable listener : copy) listener.run();
        });
  }

  private void refresh() {
    try {
      if (!loaded) loadCache();
      long now = System.currentTimeMillis(), age = now - updated;
      android.util.Log.i(
          "ListenTvGuide", "Guide refresh at " + java.time.Instant.ofEpochMilli(now));
      if (updated > 0 && age >= 0 && age < REFRESH) return;
      Set<String> known = new HashSet<>();
      for (var channel : new Catalog(context).all())
        if (!channel.provider().equals("direct")) known.add(channel.id());
      List<EpgProgramme> programmes = new ArrayList<>();
      // Keep a snapshot before publishing XMLTV, which may omit Rai fallback channels.
      Map<String, List<EpgProgramme>> previous = schedules;
      boolean downloaded = false;
      try {
        byte[] compressed = download();
        try (InputStream in = new XZInputStream(new ByteArrayInputStream(compressed), 32768)) {
          programmes.addAll(EpgXmlTv.read(in, known, now));
        }
        downloaded = !programmes.isEmpty();
        android.util.Log.i("ListenTvGuide", "Parsed " + programmes.size() + " guide programmes");
        if (downloaded) publish(programmes);
      } catch (Exception unavailable) {
        android.util.Log.w(
            "ListenTvGuide",
            "Guide feed unavailable: "
                + unavailable.getClass().getSimpleName()
                + ": "
                + unavailable.getMessage(),
            unavailable);
        for (var list : schedules.values()) programmes.addAll(list);
      }
      // Use official schedules where XMLTV has an obsolete name or placeholder.
      for (var fallback : Map.of("italiana", "italiana", "rai4k", "rai-4k").entrySet()) {
        List<EpgProgramme> extra;
        try {
          extra = raiSchedule(fallback.getKey(), fallback.getValue(), now);
        } catch (Exception unavailable) {
          android.util.Log.w(
              "ListenTvGuide",
              "Rai schedule unavailable: "
                  + fallback.getKey()
                  + " "
                  + unavailable.getClass().getSimpleName()
                  + ": "
                  + unavailable.getMessage(),
              unavailable);
          extra = List.of();
        }
        extra = EpgProgramme.merge(previous.getOrDefault(fallback.getKey(), List.of()), extra, now);
        if (!extra.isEmpty()) {
          programmes.removeIf(p -> p.channel().equals(fallback.getKey()));
          programmes.addAll(extra);
        }
      }
      if (!programmes.isEmpty()) {
        publish(programmes);
        if (downloaded) updated = now;
        List<EpgProgramme> retained = new ArrayList<>();
        for (var list : schedules.values()) retained.addAll(list);
        saveCache(retained);
      }
    } finally {
      synchronized (this) {
        busy = false;
        loaded = true;
      }
      notifyListeners();
    }
  }

  private byte[] download() throws Exception {
    String address = "https://epg-guide.com/wltv.xz";
    for (int redirect = 0; redirect < 6; redirect++) {
      URI uri = new URI(address);
      String host = uri.getHost(), path = uri.getPath();
      boolean front =
          ("epg-guide.com".equals(host) && "/wltv.xz".equals(path))
              || ("api.epg-guide.com".equals(host) && "/get/wltv.xz".equals(path));
      boolean mirror = "116.202.210.205".equals(host) && "/test/it_wltv_full.xz".equals(path);
      if ((!front && !mirror)
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || uri.getPort() != -1
          || !("https".equals(uri.getScheme()) || (mirror && "http".equals(uri.getScheme()))))
        throw new IOException("Untrusted guide endpoint.");
      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(6000);
      connection.setReadTimeout(10000);
      connection.setRequestProperty("User-Agent", "WLTV ListenTv/1.3.3");
      try {
        int status = connection.getResponseCode();
        android.util.Log.i("ListenTvGuide", "Guide HTTP " + status + " from " + host);
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
          String location = connection.getHeaderField("Location");
          if (location == null) throw new IOException("Invalid guide redirect.");
          URI next = uri.resolve(location);
          // Both public front doors support TLS, even when their redirect says HTTP.
          if ("http".equals(next.getScheme())
              && ("epg-guide.com".equals(next.getHost())
                  || "api.epg-guide.com".equals(next.getHost())))
            next =
                new URI(
                    "https",
                    next.getAuthority(),
                    next.getPath(),
                    next.getQuery(),
                    next.getFragment());
          address = next.toString();
          continue;
        }
        if (status != 200) throw new IOException("Guide service HTTP " + status);
        try (InputStream in =
                new EpgXmlTv.LimitedInput(connection.getInputStream(), 4 * 1024 * 1024);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
          byte[] buffer = new byte[16384];
          for (int n; (n = in.read(buffer)) != -1; ) bytes.write(buffer, 0, n);
          return bytes.toByteArray();
        }
      } finally {
        connection.disconnect();
      }
    }
    throw new IOException("Too many guide redirects.");
  }

  private List<EpgProgramme> raiSchedule(String channel, String slug, long now) throws Exception {
    List<EpgProgramme> out = new ArrayList<>();
    ZoneId zone = ZoneId.of("Europe/Rome");
    LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
    StreamResolver http = new StreamResolver(context);
    Exception lastFailure = null;
    for (int day = -1; day <= 1; day++)
      try {
        String date =
            today.plusDays(day).format(DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT));
        JSONArray events =
            new JSONObject(
                    http.get(
                        "https://www.raiplay.it/palinsesto/app/" + slug + "/" + date + ".json",
                        Map.of()))
                .getJSONArray("events");
        for (int i = 0; i < events.length(); i++)
          try {
            JSONObject event = events.getJSONObject(i);
            String title = event.getString("name").replaceAll("\\s+", " ").trim();
            if (title.length() > 300) title = title.substring(0, 300);
            long start =
                LocalDateTime.parse(
                        event.getString("date") + " " + event.getString("hour"),
                        DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
                            .withResolverStyle(java.time.format.ResolverStyle.STRICT))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli();
            String[] d = event.getString("duration").split(":");
            if (d.length != 3) continue;
            long duration =
                (Long.parseLong(d[0]) * 3600 + Long.parseLong(d[1]) * 60 + Long.parseLong(d[2]))
                    * 1000;
            if (duration > 0 && duration <= 24 * 60 * 60 * 1000L && EpgProgramme.validTitle(title))
              out.add(new EpgProgramme(channel, title, start, start + duration));
          } catch (RuntimeException | JSONException badEvent) {
          }
      } catch (Exception unavailable) {
        lastFailure = unavailable;
      }
    if (out.isEmpty() && lastFailure != null) throw lastFailure;
    return out;
  }

  private void loadCache() {
    try (InputStream in = cache.openRead()) {
      JSONObject json = new JSONObject(BoundedResponseReader.read(in, 4 * 1024 * 1024, false));
      JSONArray rows = json.getJSONArray("programmes");
      if (rows.length() > 10000) throw new IOException("Invalid guide cache.");
      List<EpgProgramme> programmes = new ArrayList<>();
      for (int i = 0; i < rows.length(); i++) {
        JSONObject p = rows.getJSONObject(i);
        programmes.add(
            new EpgProgramme(
                p.getString("channel"),
                p.getString("title"),
                p.getLong("start"),
                p.getLong("end")));
      }
      updated = json.getLong("updated");
      publish(programmes);
    } catch (Exception unavailable) {
    }
  }

  private void saveCache(List<EpgProgramme> programmes) {
    FileOutputStream out = null;
    try {
      JSONArray rows = new JSONArray();
      for (EpgProgramme p : programmes)
        rows.put(
            new JSONObject()
                .put("channel", p.channel())
                .put("title", p.title())
                .put("start", p.start())
                .put("end", p.end()));
      byte[] bytes =
          new JSONObject()
              .put("updated", updated)
              .put("programmes", rows)
              .toString()
              .getBytes(java.nio.charset.StandardCharsets.UTF_8);
      out = cache.startWrite();
      out.write(bytes);
      cache.finishWrite(out);
    } catch (Exception unavailable) {
      if (out != null) cache.failWrite(out);
    }
  }
}
