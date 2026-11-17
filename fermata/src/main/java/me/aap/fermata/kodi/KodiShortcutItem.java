package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;

/** Browsable Kodi library categories. Movie and TV show lists come from Kodi JSON-RPC. */
public final class KodiShortcutItem extends BrowsableItemBase {
    public static final String SCHEME = "kodi";
    public static final String TV = "kodi:tv";
    public static final String MOVIES = "kodi:movies";
    public static final String TV_SHOWS = "kodi:tvshows";
    public static final String FAVORITES = "kodi:favorites";
    public static final String PLAYLISTS = "kodi:playlists";

    private final String title;

    KodiShortcutItem(BrowsableItemBase parent, String id) {
        super(id, parent, null);
        this.title = getTitleForId(id);
    }

    public static FutureSupplier<? extends Item> create(DefaultMediaLib lib, String id) {
        if (KodiActionItem.isId(id)) {
            String target = KodiActionItem.getTarget(id);
            if (target == null) return completedNull();
            return completed(new KodiActionItem(new KodiShortcutItem(
                    lib.getKodiFolderForShortcuts(), target), target, actionTitle(target)));
        }
        if (KodiMediaItem.isId(id)) {
            String type = KodiMediaItem.getType(id);
            String category = KodiMediaItem.EPISODE.equals(type) ? TV_SHOWS : MOVIES;
            return completed(new KodiMediaItem(new KodiShortcutItem(
                    lib.getKodiFolderForShortcuts(), category), type,
                    KodiMediaItem.getKodiId(id), "Elemento Kodi"));
        }
        if (KodiShowItem.isId(id)) {
            return completed(new KodiShowItem(new KodiShortcutItem(
                    lib.getKodiFolderForShortcuts(), TV_SHOWS),
                    KodiShowItem.getKodiId(id), "Serie TV"));
        }
        if (getTitleForId(id) == null) return completedNull();
        return completed(new KodiShortcutItem(lib.getKodiFolderForShortcuts(), id));
    }

    public static boolean isKodiShortcutId(String id) {
        return getTitleForId(id) != null;
    }

    public static boolean isKodiPlayableId(String id) {
        return KodiMediaItem.isId(id);
    }

    public static String getWindow(String id) {
        return switch (id) {
            case TV -> "tvchannels";
            case MOVIES, TV_SHOWS, PLAYLISTS -> "videos";
            case FAVORITES -> "favouritesbrowser";
            default -> null;
        };
    }

    public static String getTitle(String id) {
        return getTitleForId(id);
    }

    public static String getPath(String id) {
        return switch (id) {
            case MOVIES -> "videodb://movies/titles/";
            case TV_SHOWS -> "videodb://tvshows/titles/";
            case PLAYLISTS -> "special://videoplaylists/";
            default -> null;
        };
    }

    private static String getTitleForId(String id) {
        return switch (id) {
            case TV -> "TV in diretta Kodi";
            case MOVIES -> "Film Kodi";
            case TV_SHOWS -> "Telefilm Kodi";
            case FAVORITES -> "Preferiti Kodi";
            case PLAYLISTS -> "Playlist Kodi";
            default -> null;
        };
    }

    private static String actionTitle(String id) {
        return switch (id) {
            case TV -> "Apri TV in diretta in Kodi";
            case FAVORITES -> "Apri Preferiti in Kodi";
            case PLAYLISTS -> "Apri Playlist in Kodi";
            default -> "Apri in Kodi";
        };
    }

    @Override
    protected FutureSupplier<List<Item>> listChildren() {
        Promise<List<Item>> result = new Promise<>();
        new Thread(() -> {
            List<Item> items = new ArrayList<>();
            try {
                if (MOVIES.equals(getId())) {
                    KodiJsonRpcClient client = new KodiJsonRpcClient(
                            KodiJsonRpcClient.DEFAULT_HOST, KodiJsonRpcClient.DEFAULT_PORT, null, null);
                    JSONArray movies = client.getMovies(0, 500);
                    if (movies != null) {
                        for (int i = 0; i < movies.length(); i++) {
                            JSONObject movie = movies.optJSONObject(i);
                            if (movie == null) continue;
                            int id = movie.optInt("movieid", -1);
                            if (id < 0) continue;
                            items.add(new KodiMediaItem(this, KodiMediaItem.MOVIE, id,
                                    movie.optString("title", "Film")));
                        }
                    }
                } else if (TV_SHOWS.equals(getId())) {
                    KodiJsonRpcClient client = new KodiJsonRpcClient(
                            KodiJsonRpcClient.DEFAULT_HOST, KodiJsonRpcClient.DEFAULT_PORT, null, null);
                    JSONArray shows = client.getTvShows(0, 500);
                    if (shows != null) {
                        for (int i = 0; i < shows.length(); i++) {
                            JSONObject show = shows.optJSONObject(i);
                            if (show == null) continue;
                            int id = show.optInt("tvshowid", -1);
                            if (id < 0) continue;
                            items.add(new KodiShowItem(this, id,
                                    show.optString("title", "Serie TV")));
                        }
                    }
                } else if (TV.equals(getId()) || FAVORITES.equals(getId())
                        || PLAYLISTS.equals(getId())) {
                    items.add(new KodiActionItem(this, getId(), actionTitle(getId())));
                }
                KodiDiagnostics.info("Voci Kodi caricate per " + getId() + ": " + items.size());
            } catch (Exception e) {
                KodiDiagnostics.error("Caricamento libreria Kodi non riuscito: " + getId(), e);
            }
            result.complete(items);
        }, "Kodi-Library").start();
        return result;
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Libreria Kodi");
    }

    @Override
    public int getIcon() {
        return R.drawable.video;
    }

    @NonNull
    @Override
    public FutureSupplier<Uri> getIconUri() {
        return completedNull();
    }
}
