package it.gianrolando.listentv;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import java.util.*;
import org.json.*;

/** Persistent IDs are exposed to Android Auto; URLs are resolved only at play time. */
public final class Catalog {
  public record Channel(String id, String name, String provider, String key) {
    /** UI grouping is independent from the broadcaster used for playback. */
    public String group() {
      return id.equals("tv8") ? "discovery" : provider;
    }

    public MediaItem item(Context context) {
      MediaMetadata.Builder metadata =
          new MediaMetadata.Builder()
              .setTitle(name)
              .setArtist(context.getString(R.string.live_audio))
              .setIsBrowsable(false)
              .setIsPlayable(true);
      EpgGuide guide = EpgGuide.get(context);
      EpgProgramme programme = guide.current(id);
      if (programme != null)
        metadata
            .setArtist(programme.title())
            .setSubtitle(programme.title())
            .setDescription(guide.range(programme));
      byte[] artwork = ChannelArtwork.data(context, id);
      if (artwork != null) metadata.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
      return new MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata.build()).build();
    }
  }

  private final SharedPreferences prefs;
  private final Context context;
  private final List<Channel> builtIn;
  private String customJson;
  private List<Channel> snapshot;
  private Map<String, Channel> index = Map.of();

  public Catalog(Context context) {
    this.context = context.getApplicationContext();
    prefs = this.context.getSharedPreferences("catalog", Context.MODE_PRIVATE);
    builtIn =
        List.of(
            new Channel("rai1", "Rai 1", "rai", "Rai 1"),
            new Channel("rai2", "Rai 2", "rai", "Rai 2"),
            new Channel("rai3", "Rai 3", "rai", "Rai 3"),
            new Channel("rainews", "Rai News 24", "rai", "Rai News 24"),
            new Channel("raisport", "Rai Sport", "rai", "Rai Sport"),
            new Channel("rai4k", "Rai 4K", "rai", "Rai 4K"),
            new Channel("rai4", "Rai 4", "rai", "Rai 4"),
            new Channel("rai5", "Rai 5", "rai", "Rai 5"),
            new Channel("raimovie", "Rai Movie", "rai", "Rai Movie"),
            new Channel("raipremium", "Rai Premium", "rai", "Rai Premium"),
            new Channel("raistoria", "Rai Storia", "rai", "Rai Storia"),
            new Channel("italiana", "Italiana", "rai", "Italiana"),
            new Channel("raiyoyo", "Rai Yoyo", "rai", "Rai Yoyo"),
            new Channel("raigulp", "Rai Gulp", "rai", "Rai Gulp"),
            new Channel("rete4", "Rete 4", "mediaset", "R4"),
            new Channel("canale5", "Canale 5", "mediaset", "C5"),
            new Channel("italia1", "Italia 1", "mediaset", "I1"),
            new Channel("tgcom24", "TGcom24", "mediaset", "KF"),
            new Channel("mediaset20", "20 Mediaset", "mediaset", "LB"),
            new Channel("iris", "Iris", "mediaset", "KI"),
            new Channel("la5", "La5", "mediaset", "KA"),
            new Channel("mediasetextra", "Mediaset Extra", "mediaset", "KQ"),
            new Channel("focus", "Focus", "mediaset", "FU"),
            new Channel("topcrime", "Top Crime", "mediaset", "LT"),
            new Channel("italia2", "Italia 2", "mediaset", "I2"),
            new Channel("cine34", "Cine34", "mediaset", "B6"),
            new Channel("twentyseven", "27 Twentyseven", "mediaset", "TS"),
            new Channel("boing", "Boing", "mediaset", "KB"),
            new Channel("cartoonito", "Cartoonito", "mediaset", "LA"),
            new Channel("nove", "NOVE", "discovery", "nove"),
            new Channel("realtime", "Real Time", "discovery", "realtime"),
            new Channel("dmax", "DMAX", "discovery", "dmaxit"),
            new Channel("giallo", "Giallo", "discovery", "giallo"),
            new Channel("foodnetwork", "Food Network", "discovery", "foodnetwork"),
            new Channel("hgtv", "HGTV", "discovery", "hgtvit"),
            new Channel("discovery", "Discovery", "discovery", "discoverychannelit"),
            new Channel("discoveryturbo", "Discovery Turbo", "discovery", "motortrend"),
            new Channel("warnertv", "Warner TV", "discovery", "warnertvit"),
            new Channel("k2", "K2", "discovery", "k2"),
            new Channel("frisbee", "Frisbee", "discovery", "frisbee"),
            new Channel("skytg24", "Sky TG24", "sky", "1"),
            new Channel("cielo", "Cielo", "sky", "2"),
            new Channel("tv8", "TV8", "sky", "7"),
            new Channel("la7", context.getString(R.string.la7_experimental), "la7", ""),
            new Channel("la7cinema", "La7 Cinema", "la7", "cinema"),
            new Channel("sanmarinortv", "San Marino RTV", "sanmarino", "0"),
            new Channel("sanmarinortvsport", "San Marino RTV Sport", "sanmarino", "1"),
            new Channel("tv2000", "TV2000", "tv2000", "4Y3ZMmvcvo"));
  }

  /** Rebuild only when custom sources change; favorites do not invalidate channel data. */
  public synchronized List<Channel> all() {
    String stored = prefs.getString("custom", "[]");
    if (snapshot != null && stored.equals(customJson)) return snapshot;
    List<Channel> list = new ArrayList<>(builtIn);
    try {
      JSONArray a = new JSONArray(stored);
      for (int i = 0; i < a.length(); i++) {
        JSONObject o = a.getJSONObject(i);
        list.add(new Channel(o.getString("id"), o.getString("name"), "direct", o.getString("url")));
      }
    } catch (JSONException ignored) {
    }
    snapshot = List.copyOf(list);
    customJson = stored;
    Map<String, Channel> lookup = new HashMap<>();
    for (Channel channel : snapshot) lookup.put(channel.id(), channel);
    index = Map.copyOf(lookup);
    return snapshot;
  }

  public synchronized Channel find(String id) {
    all();
    return index.get(id);
  }

  public boolean favorite(String id) {
    return prefs.getStringSet("favorites", Set.of()).contains(id);
  }

  public void toggle(String id) {
    Set<String> s = new HashSet<>(prefs.getStringSet("favorites", Set.of()));
    if (!s.remove(id)) s.add(id);
    prefs.edit().putStringSet("favorites", s).apply();
  }

  public void add(String name, String url) throws Exception {
    java.net.URI uri = new java.net.URI(url);
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null)
      throw new IllegalArgumentException(context.getString(R.string.invalid_url));
    if (name.isBlank())
      throw new IllegalArgumentException(context.getString(R.string.missing_name));
    JSONArray a = new JSONArray(prefs.getString("custom", "[]"));
    a.put(
        new JSONObject().put("id", UUID.randomUUID().toString()).put("name", name).put("url", url));
    prefs.edit().putString("custom", a.toString()).apply();
  }

  public void remove(String id) {
    try {
      JSONArray old = new JSONArray(prefs.getString("custom", "[]")), out = new JSONArray();
      for (int i = 0; i < old.length(); i++)
        if (!old.getJSONObject(i).getString("id").equals(id)) out.put(old.get(i));
      Set<String> favorites = new HashSet<>(prefs.getStringSet("favorites", Set.of()));
      favorites.remove(id);
      prefs.edit().putString("custom", out.toString()).putStringSet("favorites", favorites).apply();
    } catch (JSONException ignored) {
    }
  }
}
