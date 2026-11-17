package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import me.aap.fermata.media.lib.PlayableItemBase;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;

/** A real Kodi library entry represented as a selectable media leaf. */
public final class KodiMediaItem extends PlayableItemBase {
    public static final String PREFIX = "kodi:item:";
    public static final String MOVIE = "movie";
    public static final String EPISODE = "episode";

    private final String title;
    private final String type;
    private final int kodiId;

    public KodiMediaItem(KodiShortcutItem parent, String type, int kodiId, String title) {
        // The Kodi player owns the actual resource. Playback is intercepted by media ID
        // and routed to Kodi.Player.Open, so Fermata never opens this null resource.
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
    protected FutureSupplier<String> buildTitle(int seqNum, BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Kodi");
    }

    @NonNull
    @Override
    public FutureSupplier<MediaMetadataCompat> getMediaData() {
        return completed(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, getId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .build());
    }

    @Override
    public boolean isVideo() {
        return true;
    }

    @Override
    public String getOrigId() {
        return getId();
    }

    @NonNull
    @Override
    public FutureSupplier<Uri> getIconUri() {
        return completedNull();
    }
}
