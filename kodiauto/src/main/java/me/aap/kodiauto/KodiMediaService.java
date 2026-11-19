package me.aap.kodiauto;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servizio media per Android Auto che espone la libreria di Kodi:
 *   Kodi
 *    |- Film               -> riproduzione locale (ExoPlayer, solo audio)
 *    |- Serie TV -> episodi -> riproduzione locale
 *    |- Canali TV -> gruppo -> canali -> avviati dentro Kodi (controllo remoto)
 *    |- Preferiti          -> avviati dentro Kodi (controllo remoto)
 *
 * Differenza chiave rispetto al problema "va subito al lettore": ogni elemento finale
 * avvia davvero la riproduzione e gli errori vengono mostrati nello stato del player.
 */
@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public class KodiMediaService extends MediaBrowserServiceCompat {
    private static final String TAG = "KodiMediaService";



    private static final String CHANNEL_ID = "kodi_playback";
    private static final int NOTIF_ID = 1001;
    private static final int B64 = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    private KodiClient kodi;
    private ExecutorService io;
    private Handler main;
    private MediaSessionCompat session;
    private ExoPlayer player;
    private boolean remoteMode = false;     // true = si sta riproducendo dentro Kodi
    private boolean remotePlaying = false;
    private final Map<String, String> titles = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------- ciclo di vita

    @Override
    public void onCreate() {
        super.onCreate();
        kodi = KodiSettings.createClient(this);
        io = Executors.newSingleThreadExecutor();
        main = new Handler(Looper.getMainLooper());

        DefaultHttpDataSource.Factory ds = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(15000)
                .setDefaultRequestProperties(kodi.authHeaders());

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(ds))
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(), /* handleAudioFocus= */ true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        // Niente superficie video in auto: decodifichiamo solo l'audio
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build());
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) { syncFromPlayer(); }
            @Override public void onIsPlayingChanged(boolean isPlaying) { syncFromPlayer(); }
            @Override public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Errore player", error);
                updateState(PlaybackStateCompat.STATE_ERROR,
                        "Impossibile riprodurre: " + error.getErrorCodeName());
            }
        });

        session = new MediaSessionCompat(this, "KodiAuto");
        session.setCallback(new SessionCallback());
        setSessionToken(session.getSessionToken());
        session.setActive(true);
        updateState(PlaybackStateCompat.STATE_NONE, null);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Riproduzione Kodi", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        session.setActive(false);
        session.release();
        player.release();
        io.shutdownNow();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- albero di navigazione

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach(); // risposta asincrona (obbligatoria se si chiama la rete)
        io.execute(() -> {
            List<MediaBrowserCompat.MediaItem> out = new ArrayList<>();
            try {
                if (parentId.equals("root")) {
                    out.add(item("movies", "Film", null, true));
                    out.add(item("shows", "Serie TV", null, true));
                    out.add(item("tv", "Canali TV", null, true));
                    out.add(item("favs", "Preferiti", null, true));
                } else if (parentId.equals("movies")) {
                    for (KodiClient.Movie m : kodi.getMovies()) {
                        String id = "movie:" + enc(m.file);
                        String t = m.year > 0 ? m.title + " (" + m.year + ")" : m.title;
                        titles.put(id, t);
                        out.add(item(id, t, null, false));
                    }
                } else if (parentId.equals("shows")) {
                    for (KodiClient.Entry s : kodi.getTvShows())
                        out.add(item("show:" + s.num, s.title, null, true));
                } else if (parentId.startsWith("show:")) {
                    int showId = Integer.parseInt(parentId.substring(5));
                    for (KodiClient.Entry e : kodi.getEpisodes(showId)) {
                        String id = "ep:" + enc(e.file);
                        titles.put(id, e.title);
                        out.add(item(id, e.title, null, false));
                    }
                } else if (parentId.equals("tv")) {
                    for (KodiClient.Entry g : kodi.getChannelGroups())
                        out.add(item("group:" + g.num, g.title, null, true));
                } else if (parentId.startsWith("group:")) {
                    int groupId = Integer.parseInt(parentId.substring(6));
                    for (KodiClient.Channel c : kodi.getChannels(groupId)) {
                        String id = "ch:" + c.id;
                        titles.put(id, c.name);
                        out.add(item(id, c.name, null, false));
                    }
                } else if (parentId.equals("favs")) {
                    for (KodiClient.Entry f : kodi.getFavourites()) {
                        String id = "fav:" + enc(f.file);
                        titles.put(id, f.title);
                        out.add(item(id, f.title, null, false));
                    }
                }
                result.sendResult(out);
            } catch (Exception e) {
                Log.e(TAG, "Errore caricamento " + parentId, e);
                result.sendError(null); // Android Auto mostra un errore invece di una lista vuota
            }
        });
    }

    private static MediaBrowserCompat.MediaItem item(String id, String title,
                                                     @Nullable String subtitle, boolean browsable) {
        MediaDescriptionCompat d = new MediaDescriptionCompat.Builder()
                .setMediaId(id).setTitle(title).setSubtitle(subtitle).build();
        // BROWSABLE = apre una lista, PLAYABLE = avvia la riproduzione. Mai entrambi.
        return new MediaBrowserCompat.MediaItem(d, browsable
                ? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                : MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    // ---------------------------------------------------------------- riproduzione

    private class SessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            final String title = titles.containsKey(mediaId) ? titles.get(mediaId) : "Kodi";
            io.execute(() -> {
                try {
                    if (mediaId.startsWith("ch:")) {
                        kodi.openChannelInKodi(Integer.parseInt(mediaId.substring(3)));
                        main.post(() -> startRemote(title));
                    } else if (mediaId.startsWith("fav:")) {
                        kodi.openFileInKodi(dec(mediaId.substring(4)));
                        main.post(() -> startRemote(title));
                    } else if (mediaId.startsWith("movie:") || mediaId.startsWith("ep:")) {
                        String file = dec(mediaId.substring(mediaId.indexOf(':') + 1));
                        String url = kodi.getPlayableUrl(file);
                        Log.i(TAG, "Riproduco " + url);
                        main.post(() -> startLocal(url, title));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Errore avvio " + mediaId, e);
                    main.post(() -> updateState(PlaybackStateCompat.STATE_ERROR, "Kodi: " + e.getMessage()));
                }
            });
        }

        @Override public void onPlay() {
            if (remoteMode) { if (!remotePlaying) toggleRemote(); }
            else player.play();
        }

        @Override public void onPause() {
            if (remoteMode) { if (remotePlaying) toggleRemote(); }
            else player.pause();
        }

        @Override public void onStop() {
            if (remoteMode) io.execute(() -> { try { kodi.stopPlayer(); } catch (Exception ignored) { } });
            remoteMode = false;
            remotePlaying = false;
            player.stop();
            updateState(PlaybackStateCompat.STATE_STOPPED, null);
            stopForeground(false);
        }

        @Override public void onSeekTo(long pos) { if (!remoteMode) player.seekTo(pos); }
    }

    /** Riproduzione locale: il file viene letto da Kodi via HTTP e suonato da ExoPlayer. */
    private void startLocal(String url, String title) {
        remoteMode = false;
        remotePlaying = false;
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        setMetadata(title);
        ensureForeground(title);
    }

    /** Riproduzione dentro Kodi: Fermata/Android Auto fanno solo da telecomando. */
    private void startRemote(String title) {
        player.stop();
        remoteMode = true;
        remotePlaying = true;
        setMetadata(title);
        updateState(PlaybackStateCompat.STATE_PLAYING, null);
        ensureForeground(title);
    }

    private void toggleRemote() {
        remotePlaying = !remotePlaying;
        updateState(remotePlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, null);
        io.execute(() -> { try { kodi.togglePlayPause(); } catch (Exception e) { Log.e(TAG, "toggle", e); } });
    }

    // ---------------------------------------------------------------- stato e notifica

    private void syncFromPlayer() {
        if (remoteMode) return;
        int st = player.getPlaybackState();
        if (st == Player.STATE_BUFFERING) {
            updateState(PlaybackStateCompat.STATE_BUFFERING, null);
        } else if (st == Player.STATE_READY) {
            updateState(player.getPlayWhenReady()
                    ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, null);
        } else if (st == Player.STATE_ENDED) {
            updateState(PlaybackStateCompat.STATE_STOPPED, null);
        }
        // STATE_IDLE: ignorato, cosi' non cancelliamo un eventuale errore appena mostrato
    }

    private void updateState(int state, @Nullable String error) {
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        long pos = remoteMode ? PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN : player.getCurrentPosition();
        float speed = state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f;
        PlaybackStateCompat.Builder b = new PlaybackStateCompat.Builder()
                .setActions(actions).setState(state, pos, speed);
        if (error != null) b.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, error);
        session.setPlaybackState(b.build());
    }

    private void setMetadata(String title) {
        session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Kodi")
                .build());
    }

    private void ensureForeground(String title) {
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText("Kodi")
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(session.getSessionToken()))
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    // ---------------------------------------------------------------- utilita'

    private static String enc(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), B64);
    }

    private static String dec(String s) {
        return new String(Base64.decode(s, B64), StandardCharsets.UTF_8);
    }
}