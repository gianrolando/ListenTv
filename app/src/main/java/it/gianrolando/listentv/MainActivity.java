package it.gianrolando.listentv;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.session.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class MainActivity extends Activity {
    private static final int BG=0xff0c121e, SURFACE=0xff151f30, BORDER=0xff29364c;
    private static final int INK=0xfff3f6fc, MUTED=0xff9ba9bf, MINT=0xff74ebc4, ERROR=0xffffaaab;
    private Catalog catalog;
    private MediaBrowser browser;
    private ListenableFuture<MediaBrowser> connection;
    private SharedPreferences preferences;
    private TextView status, playerTitle, resultCount;
    private ImageButton play, stop;
    private ImageView playerLogo;
    private EditText search;
    private LinearLayout rows, chips;
    private ScrollView channelScroll;
    private final Map<String,View> cards=new HashMap<>();
    private final Map<String,TextView> cardSubtitles=new HashMap<>();
    private String filter="all", query="", selectedId="";
    private boolean resolving=false;
    private int columns;
    private final SharedPreferences.OnSharedPreferenceChangeListener errorListener=(prefs,key)-> {
        if("last_error".equals(key)) runOnUiThread(()-> {
            if(!prefs.getString(key,"").isEmpty()) resolving=false;
            update();
        });
    };
    private final Player.Listener listener=new Player.Listener() {
        @Override public void onEvents(Player player,Player.Events events) { update(); }
        @Override public void onPlayerError(PlaybackException error) {
            resolving=false; update();
        }
    };
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setFontFeatureSettings("kern");view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return view;
    }
    private GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(radius));
        if(stroke!=0) drawable.setStroke(dp(1),stroke);return drawable;
    }
    private void touch(View view,int color,int radius) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2274ebc4),shape(color,radius,0),shape(Color.WHITE,radius,0)));
    }
    private LinearLayout vertical() { LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);return view; }
    private ImageButton iconButton(int icon,String description,int color,int fill) {
        ImageButton button=new ImageButton(this);button.setImageDrawable(new UiIcon(icon,color));
        button.setPadding(dp(13),dp(13),dp(13),dp(13));button.setContentDescription(description);
        touch(button,fill,28);return button;
    }
    private void setLogo(ImageView image,String id) {
        int res=ChannelArtwork.resource(id);
        if(res==0) image.setImageDrawable(new UiIcon(UiIcon.HEADPHONES,MINT));else image.setImageResource(res);
        image.setBackground(shape(res==0?SURFACE:ChannelArtwork.background(id),13,0));
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        catalog=new Catalog(this);preferences=getSharedPreferences("catalog",MODE_PRIVATE);
        if(saved!=null) { filter=saved.getString("filter","all");query=saved.getString("query","");selectedId=saved.getString("selected",""); }
        int width=getResources().getConfiguration().screenWidthDp;
        columns=width>=840?4:width>=600?3:2;
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        LinearLayout root=vertical();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(16),dp(20),dp(12));
        root.setOnApplyWindowInsetsListener((view,insets)-> {
            view.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),
                dp(20)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.ic_app);brand.setContentDescription(null);
        header.addView(brand,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout heading=vertical();TextView name=label(getString(R.string.app_name),30,INK,true);name.setLetterSpacing(-0.04f);
        heading.addView(name);TextView subtitle=label(getString(R.string.tagline),12,MUTED,false);
        LinearLayout.LayoutParams subtitleParams=new LinearLayout.LayoutParams(-2,-2);subtitleParams.topMargin=dp(4);heading.addView(subtitle,subtitleParams);
        LinearLayout.LayoutParams headingParams=new LinearLayout.LayoutParams(0,-2,1);headingParams.leftMargin=dp(12);header.addView(heading,headingParams);
        ImageButton menu=iconButton(UiIcon.MORE,getString(R.string.more_options),MUTED,Color.TRANSPARENT);
        menu.setOnClickListener(v->showMenu(menu));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(header);
        LinearLayout searchBox=new LinearLayout(this);searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(shape(SURFACE,16,BORDER));searchBox.setPadding(dp(14),0,dp(10),0);
        ImageView glass=new ImageView(this);glass.setImageDrawable(new UiIcon(UiIcon.SEARCH,MUTED));searchBox.addView(glass,new LinearLayout.LayoutParams(dp(20),dp(20)));
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);search.setHintTextColor(MUTED);
        search.setHint(getString(R.string.search_channels));search.setTextSize(15);search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(12),0,0,0);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setText(query);
        search.setOnEditorActionListener((view,action,event)-> { if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { hideKeyboard();return true; }return false; });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { query=s.toString();renderChannels(); }
            public void afterTextChanged(Editable s) { }
        });
        searchBox.addView(search,new LinearLayout.LayoutParams(0,dp(52),1));
        LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,dp(52));searchParams.topMargin=dp(22);root.addView(searchBox,searchParams);
        HorizontalScrollView filterScroll=new HorizontalScrollView(this);filterScroll.setHorizontalScrollBarEnabled(false);filterScroll.setClipToPadding(false);
        chips=new LinearLayout(this);filterScroll.addView(chips);renderFilters();
        LinearLayout.LayoutParams filterParams=new LinearLayout.LayoutParams(-1,dp(46));filterParams.topMargin=dp(14);root.addView(filterScroll,filterParams);
        LinearLayout section=new LinearLayout(this);section.setGravity(Gravity.CENTER_VERTICAL);
        section.addView(label(getString(R.string.choose_listening),20,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        resultCount=label("",12,MUTED,false);section.addView(resultCount);
        LinearLayout.LayoutParams sectionParams=new LinearLayout.LayoutParams(-1,dp(48));sectionParams.topMargin=dp(2);root.addView(section,sectionParams);
        channelScroll=new ScrollView(this);channelScroll.setVerticalScrollBarEnabled(false);channelScroll.setClipToPadding(false);channelScroll.setFillViewport(true);
        rows=vertical();rows.setPadding(0,0,0,dp(6));channelScroll.addView(rows);
        root.addView(channelScroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(buildPlayer());setContentView(root);
        root.setFocusableInTouchMode(true);root.requestFocus();
        preferences.registerOnSharedPreferenceChangeListener(errorListener);
        renderChannels();update();
    }
    private void showMenu(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,R.string.add_source);menu.getMenu().add(0,2,1,R.string.about_app);menu.getMenu().add(0,3,2,R.string.about_audio);
        menu.setOnMenuItemClickListener(item->{ switch(item.getItemId()) {
            case 1 -> addChannel();case 2 -> showAbout();
            case 3 -> new AlertDialog.Builder(this).setTitle(R.string.about_audio).setMessage(R.string.data_note).setPositiveButton(R.string.close,null).show();
            default -> { return false; }
        }return true; });menu.show();
    }
    private void renderFilters() {
        chips.removeAllViews();
        String[] ids={"all","favorites","rai","mediaset","discovery","other"};int[] titles={R.string.all,R.string.favorites,R.string.rai_group,R.string.mediaset_group,R.string.discovery_group,R.string.other_group};
        for(int i=0;i<ids.length;i++) {
            String id=ids[i];boolean active=filter.equals(id);TextView chip=label(getString(titles[i]),13,active?BG:MUTED,true);
            chip.setGravity(Gravity.CENTER);chip.setPadding(dp(17),0,dp(17),0);touch(chip,active?MINT:SURFACE,22);
            chip.setSelected(active);chip.setContentDescription(getString(titles[i]));
            chip.setOnClickListener(view->{filter=id;renderFilters();renderChannels();channelScroll.smoothScrollTo(0,0);});
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,dp(42));params.rightMargin=dp(8);chips.addView(chip,params);
        }
    }
    private String provider(Catalog.Channel channel) {
        return switch(channel.provider()) { case "rai" -> "Rai";case "mediaset" -> "Mediaset";case "discovery" -> "Discovery";case "sky" -> "Sky";case "la7" -> "La7";case "sanmarino" -> "San Marino RTV";case "tv2000" -> "TV2000";default -> getString(R.string.custom_source); };
    }
    private void renderChannels() {
        if(rows==null) return;
        rows.removeAllViews();cards.clear();cardSubtitles.clear();
        String term=query.trim().toLowerCase(Locale.ROOT);List<Catalog.Channel> visible=new ArrayList<>();
        for(Catalog.Channel channel:catalog.all()) {
            boolean match=switch(filter) {case "favorites" -> catalog.favorite(channel.id());case "rai","mediaset","discovery" -> channel.group().equals(filter);case "other" -> !Set.of("rai","mediaset","discovery").contains(channel.group());default -> true;};
            if(match && (term.isEmpty() || (channel.name()+" "+provider(channel)).toLowerCase(Locale.ROOT).contains(term))) visible.add(channel);
        }
        resultCount.setText(getResources().getQuantityString(R.plurals.channel_count,visible.size(),visible.size()));
        if(visible.isEmpty()) {
            LinearLayout empty=vertical();empty.setGravity(Gravity.CENTER);empty.setPadding(dp(24),dp(36),dp(24),dp(36));
            ImageView icon=new ImageView(this);icon.setImageDrawable(new UiIcon(filter.equals("favorites")?UiIcon.STAR:UiIcon.SEARCH,MUTED));empty.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
            TextView message=label(getString(filter.equals("favorites")&&term.isEmpty()?R.string.empty_favorites:R.string.no_channels),15,MUTED,false);message.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams messageParams=new LinearLayout.LayoutParams(-1,-2);messageParams.topMargin=dp(18);empty.addView(message,messageParams);rows.addView(empty);return;
        }
        for(int offset=0;offset<visible.size();offset+=columns) {
            LinearLayout line=new LinearLayout(this);line.setBaselineAligned(false);
            for(int col=0;col<columns;col++) {
                LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);if(col>0)cell.leftMargin=dp(12);
                if(offset+col<visible.size()) line.addView(channelCard(visible.get(offset+col)),cell);else line.addView(new View(this),cell);
            }
            LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(-1,-2);lineParams.bottomMargin=dp(12);rows.addView(line,lineParams);
        }
        highlightCards();
    }
    private View channelCard(Catalog.Channel channel) {
        LinearLayout card=vertical();card.setPadding(dp(12),dp(12),dp(8),dp(8));card.setMinimumHeight(dp(144));
        card.setBackground(shape(SURFACE,20,BORDER));card.setForeground(new RippleDrawable(ColorStateList.valueOf(0x2274ebc4),null,shape(Color.WHITE,20,0)));
        card.setFocusable(true);card.setClickable(true);card.setContentDescription(getString(R.string.listen_to,channel.name()));
        card.setOnClickListener(view->selectChannel(channel));
        if(channel.provider().equals("direct"))card.setOnLongClickListener(view->{new AlertDialog.Builder(this).setTitle(getString(R.string.delete_channel,channel.name()))
            .setPositiveButton(R.string.delete,(dialog,which)->{catalog.remove(channel.id());renderChannels();}).setNegativeButton(R.string.cancel,null).show();return true;});
        ImageView logo=new ImageView(this);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setPadding(dp(18),dp(12),dp(18),dp(12));setLogo(logo,channel.id());
        LinearLayout.LayoutParams logoParams=new LinearLayout.LayoutParams(-1,dp(72));logoParams.rightMargin=dp(4);card.addView(logo,logoParams);
        LinearLayout caption=new LinearLayout(this);caption.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text=vertical();TextView title=label(channel.name(),15,INK,true);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);text.addView(title);
        TextView subtitle=label(provider(channel),11,MUTED,false);subtitle.setMaxLines(1);subtitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(-1,-2);subParams.topMargin=dp(5);text.addView(subtitle,subParams);cardSubtitles.put(channel.id(),subtitle);
        caption.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        boolean favorite=catalog.favorite(channel.id());ImageButton star=iconButton(UiIcon.STAR,getString(favorite?R.string.remove_favorite:R.string.add_favorite,channel.name()),favorite?MINT:MUTED,Color.TRANSPARENT);
        star.setOnClickListener(view->{catalog.toggle(channel.id());renderChannels();});caption.addView(star,new LinearLayout.LayoutParams(dp(44),dp(48)));
        LinearLayout.LayoutParams captionParams=new LinearLayout.LayoutParams(-1,-2);captionParams.topMargin=dp(5);card.addView(caption,captionParams);cards.put(channel.id(),card);return card;
    }
    private void selectChannel(Catalog.Channel channel) {
        if(browser==null) { status.setText(R.string.wait_player);return; }
        hideKeyboard();selectedId=channel.id();resolving=true;
        preferences.edit().remove("last_error").apply();update();highlightCards();
        browser.setMediaItem(channel.item(this));browser.prepare();browser.play();
    }
    private void highlightCards() {
        for(var entry:cards.entrySet()) {
            boolean selected=entry.getKey().equals(selectedId);entry.getValue().setBackground(shape(selected?0xff192c32:SURFACE,20,selected?MINT:BORDER));
            TextView subtitle=cardSubtitles.get(entry.getKey());Catalog.Channel channel=catalog.find(entry.getKey());
            if(subtitle!=null && channel!=null) { subtitle.setText(selected?getString(R.string.selected_channel):provider(channel));subtitle.setTextColor(selected?MINT:MUTED); }
        }
    }
    private View buildPlayer() {
        LinearLayout panel=vertical();panel.setPadding(dp(16),dp(12),dp(10),dp(12));panel.setBackground(shape(SURFACE,24,BORDER));
        TextView badge=label(getString(R.string.now_listening).toUpperCase(Locale.getDefault()),10,MINT,true);badge.setLetterSpacing(0.16f);panel.addView(badge);
        LinearLayout main=new LinearLayout(this);main.setGravity(Gravity.CENTER_VERTICAL);
        playerLogo=new ImageView(this);playerLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);playerLogo.setPadding(dp(7),dp(7),dp(7),dp(7));setLogo(playerLogo,selectedId);
        main.addView(playerLogo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout info=vertical();playerTitle=label(getString(R.string.player_idle_title),16,INK,true);playerTitle.setMaxLines(1);playerTitle.setEllipsize(TextUtils.TruncateAt.END);info.addView(playerTitle);
        status=label(getString(R.string.connecting),12,MUTED,false);status.setMaxLines(2);status.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(-1,-2);statusParams.topMargin=dp(5);info.addView(status,statusParams);
        LinearLayout.LayoutParams infoParams=new LinearLayout.LayoutParams(0,-2,1);infoParams.leftMargin=dp(12);infoParams.rightMargin=dp(4);main.addView(info,infoParams);
        play=iconButton(UiIcon.PLAY,getString(R.string.resume),BG,MINT);play.setOnClickListener(view->{if(browser!=null){if(browser.isPlaying())browser.pause();else{if(browser.getPlaybackState()==Player.STATE_IDLE)browser.prepare();browser.play();}}});
        main.addView(play,new LinearLayout.LayoutParams(dp(50),dp(50)));
        stop=iconButton(UiIcon.STOP,getString(R.string.stop),MUTED,Color.TRANSPARENT);stop.setOnClickListener(view->{if(browser!=null){resolving=false;browser.stop();}});main.addView(stop,new LinearLayout.LayoutParams(dp(44),dp(48)));
        LinearLayout.LayoutParams mainParams=new LinearLayout.LayoutParams(-1,-2);mainParams.topMargin=dp(10);panel.addView(main,mainParams);return panel;
    }
    private void update() {
        if(status==null) return;
        boolean playing=browser!=null && browser.isPlaying();
        play.setImageDrawable(new UiIcon(playing?UiIcon.PAUSE:UiIcon.PLAY,BG));play.setContentDescription(getString(playing?R.string.pause:R.string.resume));
        MediaItem current=browser==null?null:browser.getCurrentMediaItem();
        if(current!=null && (!resolving || current.mediaId.equals(selectedId))) { selectedId=current.mediaId;resolving=false; }
        Catalog.Channel selected=catalog.find(selectedId);playerTitle.setText(selected==null?getString(R.string.player_idle_title):selected.name());setLogo(playerLogo,selectedId);
        play.setEnabled(browser!=null && (selected!=null || current!=null));play.setAlpha(play.isEnabled()?1f:0.4f);stop.setEnabled(current!=null);
        status.setTextColor(MUTED);
        String sourceError=preferences==null?"":preferences.getString("last_error","");
        if(!sourceError.isEmpty()) { status.setText(sourceError);status.setTextColor(ERROR);resolving=false; }
        else if(browser==null)status.setText(R.string.connecting);
        else if(browser.getPlayerError()!=null) { status.setText(getString(R.string.playback_error,browser.getPlayerError().getErrorCodeName()));status.setTextColor(ERROR); }
        else if(resolving || browser.getPlaybackState()==Player.STATE_BUFFERING)status.setText(R.string.state_loading);
        else if(playing)status.setText(R.string.state_live);
        else if(current==null)status.setText(R.string.choose_channel);
        else status.setText(R.string.state_paused);
        highlightCards();
    }
    private void hideKeyboard() { search.clearFocus();InputMethodManager keyboard=getSystemService(InputMethodManager.class);if(keyboard!=null)keyboard.hideSoftInputFromWindow(search.getWindowToken(),0); }
    @Override protected void onStart() {
        super.onStart();connection=new MediaBrowser.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();
        final ListenableFuture<MediaBrowser> pending=connection;
        pending.addListener(()->{if(connection!=pending || pending.isCancelled())return;try{browser=pending.get();browser.addListener(listener);update();}catch(Exception error){status.setText(R.string.player_unavailable);}},this::runOnUiThread);
    }
    @Override protected void onStop() {if(browser!=null)browser.removeListener(listener);if(connection!=null)androidx.media3.session.MediaController.releaseFuture(connection);connection=null;browser=null;super.onStop();}
    @Override protected void onDestroy() {if(preferences!=null)preferences.unregisterOnSharedPreferenceChangeListener(errorListener);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out) {out.putString("filter",filter);out.putString("query",query);out.putString("selected",selectedId);super.onSaveInstanceState(out);}
    private void showAbout() {
        String version="1.2.0";
        try { version=getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch(Exception ignored) { }
        TextView content=label(getString(R.string.about_body),15,INK,false);
        content.setLineSpacing(dp(5),1f);content.setPadding(dp(24),dp(12),dp(24),dp(16));content.setTextIsSelectable(true);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)+" "+version).setView(scroll)
            .setPositiveButton(R.string.view_license,(dialog,which)->showLegalText("LICENSE.txt",getString(R.string.license_title)))
            .setNeutralButton(R.string.third_party_notices,(dialog,which)->showLegalText("THIRD_PARTY_NOTICES.txt",getString(R.string.third_party_notices)))
            .setNegativeButton(R.string.close,null).show();
    }
    private void showLegalText(String asset,String title) {
        String body;
        try(java.io.InputStream in=getAssets().open(asset)) { body=BoundedResponseReader.read(in,128*1024,false); }
        catch(Exception error) { body=getString(R.string.legal_unavailable); }
        TextView content=label(body,12,INK,false);content.setTextIsSelectable(true);content.setPadding(dp(20),dp(12),dp(20),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton(R.string.close,null).show();
    }
    private void addChannel() {
        LinearLayout form=vertical();form.setPadding(dp(20),dp(8),dp(20),0);
        EditText name=new EditText(this);name.setHint(R.string.channel_name);name.setSingleLine(true);form.addView(name);
        EditText url=new EditText(this);url.setHint(R.string.url_hint);url.setSingleLine(true);url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.custom_source).setView(form).setPositiveButton(R.string.add,null).setNegativeButton(R.string.cancel,null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{try{catalog.add(name.getText().toString().trim(),url.getText().toString().trim());renderChannels();dialog.dismiss();}catch(Exception error){url.setError(error.getMessage());}}));dialog.show();
    }
}
