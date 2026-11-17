package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import androidx.annotation.NonNull;

import android.net.Uri;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.ItemBase;
import me.aap.utils.async.FutureSupplier;

/** A Kodi library entry exposed as a playable MediaBrowser leaf. */
public final class KodiMediaItem extends ItemBase {
    public static final String PREFIX = "kodi:item:";
    public static final String MOVIE = "movie";
    public static final String EPISODE = "episode";

    private final String title;
    private final String type;
    private final int kodiId;

    public KodiMediaItem(KodiShortcutItem parent, String type, int kodiId, String title) {
        super(PREFIX + type + ":" + kodiId, parent, null);
        this.type = type;
        this.kodiId = kodiId;
        this.title = (title == null || title.isEmpty()) ? "Elemento Kodi" : title;
    }

    public static boolean isId(String id) {
        return id != null && id.startsWith(PREFIX);
    }

    public static String getType(String id) {
        if (!isId(id)) return null;
        String[] parts = id.split(":");
        return (parts.length == 4) ? parts[2] : null;
    }

    public static int getKodiId(String id) {
        if (!isId(id)) return -1;
        try {
            return Integer.parseInt(id.substring(id.lastIndexOf(':') + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum,
            me.aap.fermata.media.pref.BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Kodi");
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
