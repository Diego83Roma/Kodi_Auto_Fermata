package me.aap.fermata.wiseplay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses Wiseplay W3U JSON catalogues without treating provider pages as streams. */
public final class WiseplayW3uParser {
    private static final int MAX_DEPTH = 24;
    private static final int MAX_ITEMS = 20000;

    private WiseplayW3uParser() {
    }

    public static Playlist parse(String source) throws JSONException {
        if (source == null || source.trim().isEmpty()) {
            throw new JSONException("Lista W3U vuota");
        }
        JSONObject root = new JSONObject(source);
        Counter counter = new Counter();
        List<Item> items = new ArrayList<>();
        append(root.optJSONArray("groups"), true, 0, counter, items);
        append(root.optJSONArray("stations"), false, 0, counter, items);
        return new Playlist(root.optString("name", "Lista Wiseplay"),
                root.optString("author", ""), root.optString("image", ""),
                root.optString("info", ""), items);
    }

    private static void append(JSONArray array, boolean group, int depth,
                               Counter counter, List<Item> target) throws JSONException {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object != null) target.add(parseItem(object, group, depth, counter));
        }
    }

    private static Item parseItem(JSONObject object, boolean group, int depth,
                                  Counter counter) throws JSONException {
        if (depth > MAX_DEPTH) throw new JSONException("Profondità W3U massima superata");
        if (++counter.value > MAX_ITEMS) throw new JSONException("La lista W3U contiene troppi elementi");
        List<Item> children = new ArrayList<>();
        append(object.optJSONArray("groups"), true, depth + 1, counter, children);
        append(object.optJSONArray("stations"), false, depth + 1, counter, children);
        return new Item(object.optString("name", "Senza titolo"),
                object.optString("image", ""), object.optString("url", ""),
                object.optString("subtitle", ""), object.optBoolean("embed", false),
                object.optBoolean("isHost", false), group, children);
    }

    private static final class Counter { int value; }

    public static final class Playlist {
        public final String name, author, image, info;
        public final List<Item> items;
        Playlist(String name, String author, String image, String info, List<Item> items) {
            this.name = name; this.author = author; this.image = image; this.info = info;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }

    public static final class Item {
        public final String name, image, url, subtitle;
        public final boolean embed, host, group;
        public final List<Item> children;
        Item(String name, String image, String url, String subtitle, boolean embed,
             boolean host, boolean group, List<Item> children) {
            this.name = name; this.image = image; this.url = url; this.subtitle = subtitle;
            this.embed = embed; this.host = host; this.group = group;
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }
        public boolean isFolder() { return group || !children.isEmpty(); }
        public boolean hasPlayableTarget() { return !url.isEmpty(); }
    }
}
