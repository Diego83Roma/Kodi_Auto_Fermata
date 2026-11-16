package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import androidx.annotation.NonNull;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.ItemBase;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;

/** Playable-flag media-browser entries; playback is intercepted and routed to Kodi. */
public final class KodiShortcutItem extends ItemBase {
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

    public static FutureSupplier<KodiShortcutItem> create(DefaultMediaLib lib, String id) {
        KodiFolderItem parent = lib.getKodiFolderForShortcuts();
        if (getTitleForId(id) == null) return completedNull();
        return completed(new KodiShortcutItem(parent, id));
    }

    public static boolean isKodiShortcutId(String id) {
        return getTitleForId(id) != null;
    }

    public static String getWindow(String id) {
        return switch (id) {
            case TV -> "tvchannels";
            case MOVIES, TV_SHOWS, PLAYLISTS -> "videos";
            case FAVORITES -> "favouritesbrowser";
            default -> null;
        };
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
            case TV_SHOWS -> "Serie TV Kodi";
            case FAVORITES -> "Preferiti Kodi";
            case PLAYLISTS -> "Playlist Kodi";
            default -> null;
        };
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Apri in Kodi");
    }

    @Override
    public int getIcon() {
        return R.drawable.video;
    }

    @NonNull
    @Override
    public FutureSupplier<android.net.Uri> getIconUri() {
        return completedNull();
    }
}
