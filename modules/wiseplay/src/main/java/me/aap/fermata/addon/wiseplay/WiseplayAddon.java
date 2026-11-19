package me.aap.fermata.addon.wiseplay;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.MediaLibAddon;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.ui.fragment.ActivityFragment;

@Keep
@SuppressWarnings("unused")
public class WiseplayAddon implements MediaLibAddon {
    @NonNull
    private static final AddonInfo info = FermataAddon.findAddonInfo(WiseplayAddon.class.getName());
    private static WiseplayRootItem root;

    @IdRes
    @Override
    public int getAddonId() {
        return me.aap.fermata.R.id.wiseplay_fragment;
    }

    @NonNull
    @Override
    public AddonInfo getInfo() {
        return info;
    }

    @NonNull
    @Override
    public ActivityFragment createFragment() {
        return new WiseplayFragment();
    }

    @Override
    public boolean isSupportedItem(Item item) {
        return item instanceof WiseplayItem;
    }

    @Override
    public Item getRootItem(DefaultMediaLib lib) {
        if ((root == null) || (root.getLib() != lib)) root = new WiseplayRootItem(lib);
        return root;
    }

    @Nullable
    @Override
    public FutureSupplier<? extends Item> getItem(DefaultMediaLib lib, @Nullable String scheme, String id) {
        return getRootItem(lib).getItem(scheme, id);
    }
}
