package me.aap.fermata.wiseplay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reader for Wiseplay W3U JSON playlists.
 *
 * This class only parses catalogue structure. It does not resolve web pages,
 * execute embedded players, or claim that a station URL is a direct media URL.
 */
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
        String name = root.optString("name", "Lista Wiseplay");
        String author = root.optString("author", "");
        String image = root.optString("image", "");
        String info = root.optString("info", "");

        Counter counter = new Counter();
        List<Item> items = new ArrayList<>();

        JSONArray groups = root.optJSONArray("groups");
        if (groups != null) {
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.optJSONObject(i);
                if (group != null) {
                    items.add(parseItem(group, true, 0, counter));
                }
            }
        }

        JSONArray stations = root.optJSONArray("stations");
        if (stations != null) {
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.optJSONObject(i);
                if (station != null) {
                    items.add(parseItem(station, false, 0, counter));
                }
            }
        }

        return new Playlist(name, author, image, info, items);
    }

    private static Item parseItem(JSONObject json, boolean group, int depth, Counter counter)
            throws JSONException {
        if (depth > MAX_DEPTH) {
            throw new JSONException("La struttura W3U supera la profondità massima");
        }
        if (++counter.value > MAX_ITEMS) {
            throw new JSONException("La lista W3U contiene troppi elementi");
        }

        String name = json.optString("name", "Senza titolo");
        String image = json.optString("image", "");
        String url = json.optString("url", "");
        String subtitle = json.optString("subtitle", "");
        boolean embed = json.optBoolean("embed", false);
        boolean host = json.optBoolean("isHost", false);

        List<Item> children = new ArrayList<>();
        // Wiseplay lists can nest both category groups and stations. Keep the
        // original order within each array and preserve the distinction so the
        // browser can render nested categories separately from playable entries.
        JSONArray nestedGroups = json.optJSONArray("groups");
        if (nestedGroups != null) {
            for (int i = 0; i < nestedGroups.length(); i++) {
                JSONObject child = nestedGroups.optJSONObject(i);
                if (child != null) {
                    children.add(parseItem(child, true, depth + 1, counter));
                }
            }
        }

        JSONArray nestedStations = json.optJSONArray("stations");
        if (nestedStations != null) {
            for (int i = 0; i < nestedStations.length(); i++) {
                JSONObject child = nestedStations.optJSONObject(i);
                if (child != null) {
                    children.add(parseItem(child, false, depth + 1, counter));
                }
            }
        }

        return new Item(name, image, url, subtitle, embed, host, group, children);
    }

    private static final class Counter {
        int value;
    }

    public static final class Playlist {
        public final String name;
        public final String author;
        public final String image;
        public final String info;
        public final List<Item> items;

        Playlist(String name, String author, String image, String info, List<Item> items) {
            this.name = name;
            this.author = author;
            this.image = image;
            this.info = info;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }

    public static final class Item {
        public final String name;
        public final String image;
        public final String url;
        public final String subtitle;
        public final boolean embed;
        public final boolean host;
        public final boolean group;
        public final List<Item> children;

        Item(String name, String image, String url, String subtitle, boolean embed,
             boolean host, boolean group, List<Item> children) {
            this.name = name;
            this.image = image;
            this.url = url;
            this.subtitle = subtitle;
            this.embed = embed;
            this.host = host;
            this.group = group;
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }

        public boolean isFolder() {
            return group || !children.isEmpty();
        }

        public boolean hasPlayableTarget() {
            return !url.isEmpty();
        }
    }
}
