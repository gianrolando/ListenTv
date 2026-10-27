package it.gianrolando.listentv;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.*;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import java.util.*;
import java.util.concurrent.Executors;

@UnstableApi
public final class PlaybackService extends MediaLibraryService {
  private MediaLibrarySession session;
  private ExoPlayer player;
  private Catalog catalog;
  private EpgGuide guide;
  private String guideFingerprint = "";
  private final android.os.Handler guideTimer =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable guideChanged = this::updateGuideMetadata;
  private final Runnable guideTick =
      new Runnable() {
        public void run() {
          guide.ensureFresh();
          updateGuideMetadata();
          guideTimer.postDelayed(this, 60000);
        }
      };
  private final ListeningExecutorService io =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
  // Artwork encoding must not block the session thread or queue behind network resolution.
  private final ListeningExecutorService libraryIo =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  private SharedPreferences preferences;
  private final SharedPreferences.OnSharedPreferenceChangeListener changed =
      (p, key) -> {
        if (session != null && ("favorites".equals(key) || "custom".equals(key))) {
          session.notifyChildrenChanged("all", catalog.all().size(), null);
          session.notifyChildrenChanged(
              "favorites",
              (int) catalog.all().stream().filter(c -> catalog.favorite(c.id())).count(),
              null);
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    catalog = new Catalog(this);
    player =
        new ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                new DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(
                        new DefaultHttpDataSource.Factory()
                            .setUserAgent(StreamResolver.USER_AGENT)
                            .setConnectTimeoutMs(10000)
                            .setReadTimeoutMs(15000)))
            .build();
    player.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build(),
        true);
    player.setHandleAudioBecomingNoisy(true);
    player.setWakeMode(C.WAKE_MODE_NETWORK);
    player.setTrackSelectionParameters(
        player
            .getTrackSelectionParameters()
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .setPreferredAudioLanguage("it")
            .build());
    PendingIntent activity =
        PendingIntent.getActivity(
            this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    session =
        new MediaLibrarySession.Builder(this, player, new Callback())
            .setSessionActivity(activity)
            .build();
    preferences = getSharedPreferences("catalog", MODE_PRIVATE);
    preferences.registerOnSharedPreferenceChangeListener(changed);
    guide = EpgGuide.get(this);
    guide.addListener(guideChanged);
    guideTimer.post(guideTick);
  }

  private void updateGuideMetadata() {
    if (session == null) return;
    StringBuilder fingerprint = new StringBuilder();
    for (var channel : catalog.all()) {
      EpgProgramme programme = guide.current(channel.id());
      if (programme != null)
        fingerprint
            .append(channel.id())
            .append(':')
            .append(programme.start())
            .append(':')
            .append(programme.title())
            .append('\n');
    }
    String next = fingerprint.toString();
    if (next.equals(guideFingerprint)) return;
    guideFingerprint = next;
    session.notifyChildrenChanged("all", catalog.all().size(), null);
    session.notifyChildrenChanged(
        "favorites",
        (int) catalog.all().stream().filter(c -> catalog.favorite(c.id())).count(),
        null);
    MediaItem current = player.getCurrentMediaItem();
    int index = player.getCurrentMediaItemIndex();
    if (current != null && index >= 0) {
      Catalog.Channel channel = catalog.find(current.mediaId);
      if (channel != null) {
        MediaMetadata metadata = channel.item(this).mediaMetadata;
        if (!metadata.equals(current.mediaMetadata))
          player.replaceMediaItem(index, current.buildUpon().setMediaMetadata(metadata).build());
      }
    }
  }

  @Override
  public MediaLibrarySession onGetSession(MediaSession.ControllerInfo info) {
    return session;
  }

  private MediaItem folder(String id, String title) {
    return new MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            new MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build())
        .build();
  }

  private List<MediaItem> channels(String parent, int page, int size) {
    List<Catalog.Channel> selected = new ArrayList<>();
    for (var channel : catalog.all())
      if (!parent.equals("favorites") || catalog.favorite(channel.id())) selected.add(channel);
    List<MediaItem> result = new ArrayList<>();
    for (var channel : page(selected, page, size)) result.add(channel.item(this));
    return result;
  }

  private <T> List<T> page(List<T> list, int page, int size) {
    if (page < 0 || size <= 0) return List.of();
    long start = (long) page * size;
    if (start >= list.size()) return List.of();
    return list.subList((int) start, (int) Math.min(start + size, list.size()));
  }

  private final class Callback implements MediaLibrarySession.Callback {
    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
        MediaLibrarySession s, MediaSession.ControllerInfo c, LibraryParams p) {
      return Futures.immediateFuture(
          LibraryResult.ofItem(folder("root", getString(R.string.app_name)), p));
    }

    @Override
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
        MediaLibrarySession s,
        MediaSession.ControllerInfo c,
        String parent,
        int page,
        int size,
        LibraryParams p) {
      if (!Set.of("root", "all", "favorites").contains(parent))
        return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
      return libraryIo.submit(
          () -> {
            List<MediaItem> items =
                parent.equals("root")
                    ? page(
                        List.of(
                            folder("favorites", getString(R.string.favorites)),
                            folder("all", getString(R.string.all_channels))),
                        page,
                        size)
                    : channels(parent, page, size);
            return LibraryResult.ofItemList(items, p);
          });
    }

    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
        MediaLibrarySession s, MediaSession.ControllerInfo c, String id) {
      if (id.equals("root") || id.equals("all") || id.equals("favorites"))
        return Futures.immediateFuture(
            LibraryResult.ofItem(
                folder(
                    id,
                    id.equals("root")
                        ? getString(R.string.app_name)
                        : id.equals("all")
                            ? getString(R.string.all_channels)
                            : getString(R.string.favorites)),
                null));
      Catalog.Channel ch = catalog.find(id);
      if (ch == null)
        return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
      return libraryIo.submit(() -> LibraryResult.ofItem(ch.item(PlaybackService.this), null));
    }

    @Override
    public ListenableFuture<List<MediaItem>> onAddMediaItems(
        MediaSession s, MediaSession.ControllerInfo controller, List<MediaItem> items) {
      // Controller-supplied URIs are deliberately ignored: resolve trusted catalog IDs.
      return io.submit(
          () -> {
            preferences.edit().remove("last_error").apply();
            List<MediaItem> resolved = new ArrayList<>();
            StreamResolver resolver = new StreamResolver(PlaybackService.this);
            for (MediaItem item : items) {
              var ch = catalog.find(item.mediaId);
              if (ch == null)
                throw new IllegalArgumentException(getString(R.string.unknown_channel));
              try {
                resolved.add(resolver.resolve(ch));
              } catch (Exception e) {
                String stage =
                    e instanceof MediasetResolver.ResolutionException failure
                        ? failure.stage
                        : "RESOLVE";
                String detail = " [" + stage + "]";
                preferences
                    .edit()
                    .putString("last_error", getString(R.string.source_error, ch.name()) + detail)
                    .apply();
                throw e;
              }
            }
            return resolved;
          });
    }
  }

  @Override
  public void onDestroy() {
    if (guide != null) guide.removeListener(guideChanged);
    guideTimer.removeCallbacks(guideTick);
    if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(changed);
    if (session != null) session.release();
    if (player != null) player.release();
    io.shutdownNow();
    libraryIo.shutdownNow();
    super.onDestroy();
  }
}
