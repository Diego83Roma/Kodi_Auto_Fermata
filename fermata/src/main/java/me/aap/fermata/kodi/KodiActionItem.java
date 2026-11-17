package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import me.aap.fermata.media.lib.PlayableItemBase;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;

/** Selectable action that opens a Kodi section not yet mirrored as a Fermata list. */
public final class KodiActionItem extends PlayableItemBase {
    private static final String PREFIX = "kodi:action:";
    private final String target;
    private final String title;

    KodiActionItem(KodiShortcutItem parent, String target, String title) {
        super(PREFIX + target.substring("kodi:".length()), parent, null);
        this.target = target;
        this.title = title;
    }

    public static boolean isId(String id) {
        return id != null && id.startsWith(PREFIX);
    }

    public static String getTarget(String id) {
        if (!isId(id)) return null;
        String suffix = id.substring(PREFIX.length());
        return switch (suffix) {
            case "tv" -> KodiShortcutItem.TV;
            case "favorites" -> KodiShortcutItem.FAVORITES;
            case "playlists" -> KodiShortcutItem.PLAYLISTS;
            default -> null;
        };
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Apri la sezione nell'app Kodi");
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
