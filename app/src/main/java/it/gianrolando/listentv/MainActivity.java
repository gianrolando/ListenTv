package it.gianrolando.listentv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.session.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;

public final class MainActivity extends Activity {
    private Catalog catalog;
    private MediaBrowser browser;
    private ListenableFuture<MediaBrowser> connection;
    private TextView status;
    private Button play;
    private LinearLayout rows;
    private boolean onlyFavorites=false;
    private SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener errorListener=(p,key)-> {
        if("last_error".equals(key)) runOnUiThread(()-> {
            String error=p.getString(key,""); if(!error.isEmpty()) status.setText(error);
        });
    };
    private final Player.Listener listener=new Player.Listener() {
        @Override public void onEvents(Player p,Player.Events events) { update(); }
        @Override public void onPlayerError(PlaybackException e) {
            status.setText(getString(R.string.playback_error,e.getErrorCodeName()));
        }
    };
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.rgb(24,59,86));
        v.setPadding(0,dp(6),0,dp(6)); return v;
    }
    private Button button(String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setMinHeight(dp(48));
        b.setOnClickListener(v->action.run()); return b;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); catalog=new Catalog(this);
        onlyFavorites=saved!=null && saved.getBoolean("favorites");
        LinearLayout layout=new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20),dp(28),dp(20),dp(18)); layout.setBackgroundColor(Color.rgb(245,248,250));
        layout.setOnApplyWindowInsetsListener((v,insets)-> {
            v.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(16)+insets.getSystemWindowInsetTop(),
                dp(20)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        layout.addView(text(getString(R.string.app_name),30)); layout.addView(text(getString(R.string.tagline),16));
        status=text(getString(R.string.connecting),16); layout.addView(status);
        LinearLayout controls=new LinearLayout(this);
        play=button(getString(R.string.resume),()-> {
            if(browser!=null) {
                if(browser.isPlaying()) browser.pause();
                else { if(browser.getPlaybackState()==Player.STATE_IDLE) browser.prepare(); browser.play(); }
            }
        });
        controls.addView(play,new LinearLayout.LayoutParams(0,dp(54),1));
        controls.addView(button(getString(R.string.stop),()-> { if(browser!=null) browser.stop(); }),new LinearLayout.LayoutParams(0,dp(54),1));
        layout.addView(controls);
        LinearLayout filters=new LinearLayout(this);
        filters.addView(button(getString(R.string.all),()-> {onlyFavorites=false; renderRows();}),new LinearLayout.LayoutParams(0,dp(54),1));
        filters.addView(button(getString(R.string.favorites),()-> {onlyFavorites=true; renderRows();}),new LinearLayout.LayoutParams(0,dp(54),1));
        layout.addView(filters);
        ScrollView scroll=new ScrollView(this); rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows);
        layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        layout.addView(button(getString(R.string.add_source),this::addChannel));
        layout.addView(text(getString(R.string.data_note),12));
        setContentView(layout); renderRows();
        preferences=getSharedPreferences("catalog",MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(errorListener);
    }
    @Override protected void onStart() {
        super.onStart();
        connection=new MediaBrowser.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();
        final ListenableFuture<MediaBrowser> pending=connection;
        pending.addListener(()-> {
            if(connection!=pending || pending.isCancelled()) return;
            try { browser=pending.get(); browser.addListener(listener); update(); }
            catch(Exception e) { status.setText(getString(R.string.player_unavailable)); }
        },this::runOnUiThread);
    }
    @Override protected void onStop() {
        if(browser!=null) browser.removeListener(listener);
        if(connection!=null) MediaController.releaseFuture(connection);
        connection=null; browser=null; super.onStop();
    }
    @Override protected void onDestroy() {
        if(preferences!=null) preferences.unregisterOnSharedPreferenceChangeListener(errorListener);
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("favorites",onlyFavorites); super.onSaveInstanceState(out); }
    private void update() {
        if(browser==null) return;
        play.setText(browser.isPlaying()?getString(R.string.pause):getString(R.string.resume));
        if(browser.getPlayerError()!=null) return;
        String name=String.valueOf(browser.getMediaMetadata().title);
        if(browser.getCurrentMediaItem()==null) status.setText(getString(R.string.choose_channel));
        else if(browser.getPlaybackState()==Player.STATE_BUFFERING) status.setText(getString(R.string.buffering,name));
        else if(browser.isPlaying()) status.setText(getString(R.string.listening,name));
        else status.setText(getString(R.string.paused,name));
    }
    private void renderRows() {
        rows.removeAllViews();
        rows.addView(text(onlyFavorites?getString(R.string.your_favorites):getString(R.string.channels_status),14));
        int count=0;
        for(var c:catalog.all()) {
            if(onlyFavorites && !catalog.favorite(c.id())) continue;
            count++;
            LinearLayout row=new LinearLayout(this);
            Button channel=button(c.name(),()-> {
                if(browser==null) { status.setText(getString(R.string.wait_player)); return; }
                status.setText(getString(R.string.opening,c.name()));
                browser.setMediaItem(c.item(this)); browser.prepare(); browser.play();
            });
            if(c.provider().equals("direct")) channel.setOnLongClickListener(v-> {
                new AlertDialog.Builder(this).setTitle(getString(R.string.delete_channel,c.name()))
                    .setPositiveButton(getString(R.string.delete),(d,w)-> {catalog.remove(c.id()); renderRows();})
                    .setNegativeButton(getString(R.string.cancel),null).show(); return true;
            });
            row.addView(channel,new LinearLayout.LayoutParams(0,dp(60),1));
            Button star=button(catalog.favorite(c.id())?"★":"☆",()-> {catalog.toggle(c.id()); renderRows();});
            star.setContentDescription(getString(catalog.favorite(c.id())?R.string.remove_favorite:R.string.add_favorite,c.name()));
            row.addView(star,new LinearLayout.LayoutParams(dp(64),dp(60))); rows.addView(row);
        }
        if(count==0) rows.addView(text(getString(R.string.empty_favorites),16));
    }
    private void addChannel() {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),0,dp(20),0);
        EditText name=new EditText(this); name.setHint(getString(R.string.channel_name)); name.setSingleLine(true); form.addView(name);
        EditText url=new EditText(this); url.setHint(getString(R.string.url_hint)); url.setSingleLine(true);
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI); form.addView(url);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.custom_source)).setView(form)
            .setPositiveButton(getString(R.string.add),null).setNegativeButton(getString(R.string.cancel),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v-> {
            try { catalog.add(name.getText().toString().trim(),url.getText().toString().trim()); renderRows(); dialog.dismiss(); }
            catch(Exception e) { url.setError(e.getMessage()); }
        })); dialog.show();
    }
}
