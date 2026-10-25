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
        public MediaItem item(Context context) {
            return new MediaItem.Builder().setMediaId(id).setMediaMetadata(
                new MediaMetadata.Builder().setTitle(name).setArtist(context.getString(R.string.live_audio))
                    .setIsBrowsable(false).setIsPlayable(true).build()).build();
        }
    }
    private final SharedPreferences prefs;
    private final Context context;
    public Catalog(Context context) { this.context = context.getApplicationContext(); prefs = context.getSharedPreferences("catalog", Context.MODE_PRIVATE); }
    public List<Channel> all() {
        List<Channel> list = new ArrayList<>(List.of(
            new Channel("rai1", "Rai 1", "rai", "Rai 1"),
            new Channel("rai2", "Rai 2", "rai", "Rai 2"),
            new Channel("rai3", "Rai 3", "rai", "Rai 3"),
            new Channel("rainews", "Rai News 24", "rai", "Rai News 24"),
            new Channel("skytg24", "Sky TG24", "sky", "1"),
            new Channel("cielo", "Cielo", "sky", "2"),
            new Channel("tv8", "TV8", "sky", "7"),
            new Channel("la7", context.getString(R.string.la7_experimental), "la7", ""),
            new Channel("tv2000", "TV2000", "tv2000", "4Y3ZMmvcvo")
        ));
        try {
            JSONArray a = new JSONArray(prefs.getString("custom", "[]"));
            for (int i=0; i<a.length(); i++) {
                JSONObject o=a.getJSONObject(i);
                list.add(new Channel(o.getString("id"),o.getString("name"),"direct",o.getString("url")));
            }
        } catch(JSONException ignored) { }
        return list;
    }
    public Channel find(String id) { return all().stream().filter(c->c.id().equals(id)).findFirst().orElse(null); }
    public boolean favorite(String id) { return prefs.getStringSet("favorites", Set.of()).contains(id); }
    public void toggle(String id) {
        Set<String> s=new HashSet<>(prefs.getStringSet("favorites", Set.of()));
        if (!s.remove(id)) s.add(id);
        prefs.edit().putStringSet("favorites",s).apply();
    }
    public void add(String name, String url) throws Exception {
        java.net.URI uri=new java.net.URI(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
            throw new IllegalArgumentException(context.getString(R.string.invalid_url));
        if (name.isBlank()) throw new IllegalArgumentException(context.getString(R.string.missing_name));
        JSONArray a=new JSONArray(prefs.getString("custom","[]"));
        a.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("url",url));
        prefs.edit().putString("custom",a.toString()).apply();
    }
    public void remove(String id) {
        try {
            JSONArray old=new JSONArray(prefs.getString("custom","[]")), out=new JSONArray();
            for(int i=0;i<old.length();i++) if(!old.getJSONObject(i).getString("id").equals(id)) out.put(old.get(i));
            prefs.edit().putString("custom",out.toString()).apply();
        } catch(JSONException ignored) { }
    }
}
