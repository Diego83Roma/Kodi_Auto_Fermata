package me.aap.fermata.addon.wiseplay;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.log.Log;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.vfs.VirtualResource;
import me.aap.fermata.wiseplay.WiseplayW3uParser;

public class WiseplayRootItem extends BrowsableItemBase implements WiseplayItem {
    public static final String ID = "WISEPLAY";
    public static final String DEFAULT_SOURCE = "https://bit.ly/wise_mandra";
    private static final int MAX_BYTES = 15 * 1024 * 1024;
    private static final Pref<IntSupplier> SOURCE_VERSION = Pref.i("SOURCE_VERSION", 0).withInheritance(false);
    private final DefaultMediaLib lib;

    WiseplayRootItem(DefaultMediaLib lib) {
        super(ID, null, null);
        this.lib = lib;
    }

    @Nullable
    public FutureSupplier<? extends Item> getItem(@Nullable String scheme, String id) {
        if (scheme == null) return ID.equals(id) ? completed(this) : null;
        if (!WiseplayFolderItem.SCHEME.equals(scheme) && !WiseplayTrackItem.SCHEME.equals(scheme)) {
            return null;
        }
        String prefix = scheme + ":";
        if (!id.startsWith(prefix)) return completedNull();
        String[] parts = id.substring(prefix.length()).split("\\.");
        int[] indexes = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) indexes[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException ex) {
            return completedNull();
        }
        return find(this, indexes, 0).map(item -> {
            if (WiseplayFolderItem.SCHEME.equals(scheme)) {
                return (item instanceof WiseplayFolderItem) ? item : null;
            }
            return (item instanceof WiseplayTrackItem) ? item : null;
        });
    }

    private FutureSupplier<Item> find(BrowsableItemBase parent, int[] indexes, int pos) {
        return parent.getUnsortedChildren().then(children -> {
            int index = indexes[pos];
            if ((index < 0) || (index >= children.size())) return completedNull();
            Item item = children.get(index);
            if (pos == indexes.length - 1) return completed(item);
            if (!(item instanceof BrowsableItemBase)) return completedNull();
            return find((BrowsableItemBase) item, indexes, pos + 1);
        });
    }

    @NonNull
    @Override
    public DefaultMediaLib getLib() {
        return lib;
    }

    @Override
    public MediaLib.BrowsableItem getParent() {
        return null;
    }

    @NonNull
    @Override
    public PreferenceStore getParentPreferenceStore() {
        return getLib();
    }

    @NonNull
    @Override
    public MediaLib.BrowsableItem getRoot() {
        return this;
    }

    @Override
    protected FutureSupplier<List<Item>> listChildren() {
        return App.get().execute(this::loadPlaylist).map(this::createItems)
                .ifFail(err -> {
                    Log.e(err, "Unable to load Wiseplay W3U source");
                    return new ArrayList<>();
                });
    }

    private WiseplayW3uParser.Playlist loadPlaylist() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(DEFAULT_SOURCE).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "FermataAuto-Wiseplay/1.0");
            int status = connection.getResponseCode();
            if ((status < 200) || (status >= 300)) {
                throw new IllegalStateException("Risposta HTTP " + status);
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16384];
                int total = 0, read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_BYTES) throw new IllegalStateException("Lista W3U troppo grande");
                    out.write(buffer, 0, read);
                }
                return WiseplayW3uParser.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private List<Item> createItems(WiseplayW3uParser.Playlist playlist) {
        return WiseplayItems.createChildren(this, playlist.items, "");
    }

    @Override
    protected FutureSupplier<String> buildTitle() {
        return completed(getLib().getContext().getString(me.aap.fermata.R.string.addon_name_wiseplay));
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed(DEFAULT_SOURCE);
    }

    @Override
    public boolean sortChildrenEnabled() {
        return false;
    }
}
