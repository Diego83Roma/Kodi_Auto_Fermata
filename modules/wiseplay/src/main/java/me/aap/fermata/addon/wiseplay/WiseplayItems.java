package me.aap.fermata.addon.wiseplay;

import java.util.ArrayList;
import java.util.List;

import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.wiseplay.WiseplayW3uParser;
import me.aap.utils.vfs.VirtualResource;

final class WiseplayItems {
    private WiseplayItems() {
    }

    static List<Item> createChildren(MediaLib.BrowsableItem parent,
                                     List<WiseplayW3uParser.Item> sourceItems, String parentPath) {
        List<Item> children = new ArrayList<>(sourceItems.size());
        for (int i = 0; i < sourceItems.size(); i++) {
            WiseplayW3uParser.Item source = sourceItems.get(i);
            String path = parentPath.isEmpty() ? Integer.toString(i) : parentPath + "." + i;
            if (source.isFolder()) {
                children.add(new WiseplayFolderItem(source.name, path, parent, source.children));
            } else if (source.hasPlayableTarget() && !source.embed && !source.host) {
                VirtualResource resource = parent.getLib().getVfsManager().resolve(source.url, null).get(null);
                if (resource != null) {
                    children.add(new WiseplayTrackItem(path, parent, resource, source.name,
                            source.subtitle, source.image));
                }
            }
        }
        return children;
    }
}
