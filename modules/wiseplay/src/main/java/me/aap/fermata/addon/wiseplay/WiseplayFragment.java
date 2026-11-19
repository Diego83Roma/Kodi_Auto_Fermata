package me.aap.fermata.addon.wiseplay;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;

import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib.BrowsableItem;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.service.FermataServiceUiBinder;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.fragment.MediaLibFragment;
import me.aap.utils.ui.fragment.ActivityFragment;

public class WiseplayFragment extends MediaLibFragment {
    @Override
    protected ListAdapter createAdapter(FermataServiceUiBinder binder) {
        return new ListAdapter(getMainActivity(), getRootItem());
    }

    @Override
    public CharSequence getFragmentTitle() {
        return getResources().getString(me.aap.fermata.R.string.addon_name_wiseplay);
    }

    @Override
    public int getFragmentId() {
        return me.aap.fermata.R.id.wiseplay_fragment;
    }

    public WiseplayRootItem getRootItem() {
        return requireNonNull(AddonManager.get().getAddon(WiseplayAddon.class))
                .getRootItem((DefaultMediaLib) getMainActivity().getLib());
    }

    @Override
    protected boolean isSupportedItem(Item item) {
        return item instanceof WiseplayItem;
    }
}
