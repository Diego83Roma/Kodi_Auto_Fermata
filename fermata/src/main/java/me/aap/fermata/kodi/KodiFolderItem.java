package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.pref.PreferenceStore;

/** Android Auto folder containing shortcuts to the separate Kodi application. */
public final class KodiFolderItem extends BrowsableItemBase {
    public static final String ID = "KodiFolder";
    private final DefaultMediaLib lib;

    public KodiFolderItem(DefaultMediaLib lib) {
        super(ID, null, null);
        this.lib = lib;
    }

    @NonNull
    @Override
    public DefaultMediaLib getLib() {
        return lib;
    }

    @Nullable
    @Override
    public MediaLib.BrowsableItem getParent() {
        return null;
    }

    @NonNull
    @Override
    public MediaLib.BrowsableItem getRoot() {
        return this;
    }

    @NonNull
    @Override
    public PreferenceStore getParentPreferenceStore() {
        return lib;
    }

    @Override
    protected FutureSupplier<String> buildTitle() {
        return completed("Cartella Kodi");
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Film e telefilm dalla libreria Kodi");
    }

    @Override
    protected FutureSupplier<List<Item>> listChildren() {
        List<Item> items = new ArrayList<>(2);
        items.add(new KodiShortcutItem(this, KodiShortcutItem.MOVIES));
        items.add(new KodiShortcutItem(this, KodiShortcutItem.TV_SHOWS));
        return completed(items);
    }

    @Override
    public int getIcon() {
        return R.drawable.add_folder;
    }

    @NonNull
    @Override
    public FutureSupplier<android.net.Uri> getIconUri() {
        return completedNull();
    }
}
