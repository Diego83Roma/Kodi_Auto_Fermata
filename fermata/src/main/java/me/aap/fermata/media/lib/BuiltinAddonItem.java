package me.aap.fermata.media.lib;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import android.net.Uri;

import androidx.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import me.aap.fermata.R;
import me.aap.utils.async.FutureSupplier;

/** Android Auto shortcuts to the built-in browser and YouTube screens. */
public final class BuiltinAddonItem extends ExtRoot {
    public static final String YOUTUBE = "youtube";
    public static final String BROWSER = "browser";

    private final String title;
    private final int icon;

    public BuiltinAddonItem(DefaultMediaLib lib, String id) {
        super(id, lib);
        if (YOUTUBE.equals(id)) {
            title = "YouTube";
            icon = R.drawable.youtube;
        } else if (BROWSER.equals(id)) {
            title = "Browser";
            icon = R.drawable.web;
        } else {
            throw new IllegalArgumentException("Unsupported built-in addon: " + id);
        }
    }

    /** Android Auto should execute these shortcuts rather than browse an empty folder. */
    @Override
    public FutureSupplier<MediaItem> asMediaItem() {
        return getMediaDescription().map(md -> new MediaItem(md, MediaItem.FLAG_PLAYABLE));
    }

    @Override
    protected FutureSupplier<String> buildTitle() {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("");
    }

    @Override
    public int getIcon() {
        return icon;
    }

    @NonNull
    @Override
    public FutureSupplier<Uri> getIconUri() {
        return completedNull();
    }
}
