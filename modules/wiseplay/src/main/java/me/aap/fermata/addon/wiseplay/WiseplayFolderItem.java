package me.aap.fermata.addon.wiseplay;

import static me.aap.utils.async.Completed.completed;

import java.util.List;

import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.utils.async.FutureSupplier;
import me.aap.fermata.wiseplay.WiseplayW3uParser;

public class WiseplayFolderItem extends BrowsableItemBase implements WiseplayItem {
    public static final String SCHEME = "wplayf";
    private final String name;
    private final String path;
    private final List<WiseplayW3uParser.Item> sourceItems;

    WiseplayFolderItem(String name, String path, MediaLib.BrowsableItem parent,
                       List<WiseplayW3uParser.Item> sourceItems) {
        super(SCHEME + ":" + path, parent, null);
        this.name = name;
        this.path = path;
        this.sourceItems = sourceItems;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIcon() {
        return me.aap.fermata.R.drawable.folder;
    }

    @Override
    protected FutureSupplier<List<Item>> listChildren() {
        return completed(WiseplayItems.createChildren(this, sourceItems, path));
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, me.aap.fermata.media.pref.BrowsableItemPrefs prefs) {
        return completed(name);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("");
    }

    @Override
    public boolean sortChildrenEnabled() {
        return false;
    }
}
