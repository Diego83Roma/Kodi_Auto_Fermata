package me.aap.fermata.addon.wiseplay;

import static me.aap.utils.async.Completed.completed;

import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import me.aap.fermata.media.lib.PlayableItemBase;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.vfs.VirtualResource;

public class WiseplayTrackItem extends PlayableItemBase implements WiseplayItem {
    public static final String SCHEME = "wplayt";
    private final String name;
    private final String subtitle;
    private final String image;

    WiseplayTrackItem(String id, MediaLib.BrowsableItem parent, VirtualResource resource,
                      String name, String subtitle, String image) {
        super(SCHEME + ":" + id, parent, resource);
        this.name = name;
        this.subtitle = subtitle;
        this.image = image;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isVideo() {
        return true;
    }

    @Override
    public boolean isStream() {
        return true;
    }

    @Override
    public String getOrigId() {
        return getId();
    }

    @NonNull
    @Override
    protected FutureSupplier<MediaMetadataCompat> loadMeta() {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, name);
        if (subtitle != null && !subtitle.isEmpty()) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle);
        }
        if (image != null && !image.isEmpty()) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, image);
        }
        return completed(builder.build());
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, me.aap.fermata.media.pref.BrowsableItemPrefs prefs) {
        return completed(name);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed((subtitle == null) ? "" : subtitle);
    }
}
