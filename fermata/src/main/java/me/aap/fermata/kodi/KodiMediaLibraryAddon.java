package me.aap.fermata.kodi;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.aap.fermata.R;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.MediaLibAddon;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.ExtBrowsable;
import me.aap.fermata.media.lib.ExtRoot;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.BrowsableItem;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.pref.PreferenceStore;

import static me.aap.utils.async.Completed.completed;

/**
 * Kodi catalogue source for Fermata's existing media browser.
 *
 * This first catalogue step exposes Movies, TV Shows and their episodes.
 * Playback URLs are deliberately not fabricated: Kodi VFS resolution and
 * Fermata-player handoff are a separate, subsequent implementation step.
 */
@Keep
@SuppressWarnings({"unused", "deprecation"})
public final class KodiMediaLibraryAddon implements MediaLibAddon {
    private static final AddonInfo INFO =
            FermataAddon.findAddonInfo(KodiMediaLibraryAddon.class.getName());
    private KodiRootItem root;

    @IdRes
    @Override
    public int getAddonId() {
        // This catalogue contributes a media-library root, not a standalone fragment.
        return 0;
    }

    @NonNull
    @Override
    public AddonInfo getInfo() {
        return INFO;
    }

    @NonNull
    @Override
    public ActivityFragment createFragment() {
        return null;
    }

    @Override
    public boolean isSupportedItem(Item item) {
        return item instanceof KodiRootItem || item instanceof KodiCategoryItem
                || item instanceof KodiCatalogItem;
    }

    @NonNull
    @Override
    public synchronized KodiRootItem getRootItem(DefaultMediaLib lib) {
        if (root == null || root.getLib() != lib) root = new KodiRootItem(lib);
        return root;
    }

    @Nullable
    @Override
    public FutureSupplier<? extends Item> getItem(DefaultMediaLib lib,
                                                   @Nullable String scheme, String id) {
        KodiRootItem r = getRootItem(lib);
        if (scheme == null) return KodiRootItem.ID.equals(id) ? completed(r) : null;
        if (!KodiRootItem.SCHEME.equals(scheme)) return null;

        if (KodiCategoryItem.MOVIES_ID.equals(id)) return completed(r.movies());
        if (KodiCategoryItem.TV_ID.equals(id)) return completed(r.tvShows());

        String[] parts = id.split(":");
        if (parts.length == 3 && "movie".equals(parts[1])) {
            return loadMovie(r, parts[2]);
        }
        if (parts.length == 3 && "show".equals(parts[1])) {
            return loadShow(r, parts[2]);
        }
        if (parts.length == 4 && "episode".equals(parts[1])) {
            return loadEpisode(r, parts[2], parts[3]);
        }
        return null;
    }

    private FutureSupplier<? extends Item> loadMovie(KodiRootItem root, String movieId) {
        return App.get().execute(() -> {
            JSONArray movies = client().getMovies(0, 500);
            JSONObject movie = find(movies, "movieid", movieId);
            return movie == null ? null : KodiCatalogItem.fromMovie(root.movies(), movie);
        });
    }

    private FutureSupplier<? extends Item> loadShow(KodiRootItem root, String showId) {
        return App.get().execute(() -> {
            JSONArray shows = client().getTvShows(0, 500);
            JSONObject show = find(shows, "tvshowid", showId);
            return show == null ? null : KodiCatalogItem.fromShow(root.tvShows(), show);
        });
    }

    private FutureSupplier<? extends Item> loadEpisode(KodiRootItem root, String showId,
                                                         String episodeId) {
        return App.get().execute(() -> {
            KodiJsonRpcClient client = client();
            JSONArray shows = client.getTvShows(0, 500);
            JSONObject show = find(shows, "tvshowid", showId);
            if (show == null) return null;
            KodiCatalogItem parent = KodiCatalogItem.fromShow(root.tvShows(), show);
            JSONArray episodes = client.getEpisodes(Integer.parseInt(showId), 0, 1000);
            JSONObject episode = find(episodes, "episodeid", episodeId);
            return episode == null ? null : KodiCatalogItem.fromEpisode(parent, episode);
        });
    }

    private static JSONObject find(JSONArray values, String key, String wanted)
            throws JSONException {
        if (values == null) return null;
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.optJSONObject(i);
            if (value != null && wanted.equals(String.valueOf(value.optInt(key, -1)))) {
                return value;
            }
        }
        return null;
    }

    private static KodiJsonRpcClient client() throws Exception {
        return new KodiJsonRpcClient(KodiJsonRpcClient.DEFAULT_HOST,
                KodiJsonRpcClient.DEFAULT_PORT, null, null);
    }

    static final class KodiRootItem extends ExtRoot {
        static final String ID = "Kodi";
        static final String SCHEME = "kodi";

        KodiRootItem(DefaultMediaLib lib) {
            super(ID, lib);
        }

        KodiCategoryItem movies() {
            return new KodiCategoryItem(this, true);
        }

        KodiCategoryItem tvShows() {
            return new KodiCategoryItem(this, false);
        }

        @Override
        protected FutureSupplier<String> buildTitle() {
            return completed("Kodi");
        }

        @Override
        protected FutureSupplier<String> buildSubtitle() {
            return completed("Libreria locale Kodi");
        }

        @Override
        protected FutureSupplier<List<Item>> listChildren() {
            List<Item> children = new ArrayList<>(2);
            children.add(movies());
            children.add(tvShows());
            return completed(children);
        }
    }

    static final class KodiCategoryItem extends ExtBrowsable {
        static final String MOVIES_ID = "kodi:movies";
        static final String TV_ID = "kodi:tvshows";
        private final KodiRootItem root;
        private final boolean movies;

        KodiCategoryItem(KodiRootItem root, boolean movies) {
            super(movies ? MOVIES_ID : TV_ID, root, null);
            this.root = root;
            this.movies = movies;
        }

        @Override
        public String getName() {
            return movies ? "Film" : "Serie TV";
        }

        @Override
        protected FutureSupplier<String> buildTitle(int seqNum,
                                                     me.aap.fermata.media.pref.BrowsableItemPrefs prefs) {
            return completed(getName());
        }

        @Override
        protected FutureSupplier<String> buildSubtitle() {
            return completed("Catalogo Kodi");
        }

        @Override
        protected FutureSupplier<List<Item>> listChildren() {
            return App.get().execute(() -> {
                KodiJsonRpcClient client = client();
                JSONArray values = movies ? client.getMovies(0, 500)
                        : client.getTvShows(0, 500);
                if (values == null) return Collections.emptyList();
                List<Item> items = new ArrayList<>(values.length());
                for (int i = 0; i < values.length(); i++) {
                    JSONObject value = values.optJSONObject(i);
                    if (value == null) continue;
                    items.add(movies ? KodiCatalogItem.fromMovie(this, value)
                            : KodiCatalogItem.fromShow(this, value));
                }
                KodiDiagnostics.info("Catalogo Kodi caricato: " + getName()
                        + " (" + items.size() + " elementi)");
                return items;
            });
        }
    }

    static final class KodiCatalogItem extends ExtBrowsable {
        private final String title;
        private final String subtitle;
        private final String kind;
        private final int showId;

        private KodiCatalogItem(String id, BrowsableItem parent, String title,
                                String subtitle, String kind, int showId) {
            super(id, parent, null);
            this.title = title;
            this.subtitle = subtitle;
            this.kind = kind;
            this.showId = showId;
        }

        static KodiCatalogItem fromMovie(BrowsableItem parent, JSONObject movie) {
            int id = movie.optInt("movieid", -1);
            return new KodiCatalogItem("kodi:movie:" + id, parent,
                    movie.optString("title", "Film senza titolo"),
                    yearAndPlot(movie), "movie", -1);
        }

        static KodiCatalogItem fromShow(BrowsableItem parent, JSONObject show) {
            int id = show.optInt("tvshowid", -1);
            return new KodiCatalogItem("kodi:show:" + id, parent,
                    show.optString("title", "Serie senza titolo"),
                    yearAndPlot(show), "show", id);
        }

        static KodiCatalogItem fromEpisode(BrowsableItem parent, JSONObject episode) {
            int id = episode.optInt("episodeid", -1);
            int season = episode.optInt("season", 0);
            int number = episode.optInt("episode", 0);
            String title = episode.optString("title", "Episodio");
            String label = String.format(java.util.Locale.ITALY, "S%02d · E%02d · %s",
                    season, number, title);
            return new KodiCatalogItem("kodi:episode:" + ((KodiCatalogItem) parent).showId
                    + ":" + id, parent, label, episode.optString("plot", ""),
                    "episode", ((KodiCatalogItem) parent).showId);
        }

        private static String yearAndPlot(JSONObject value) {
            String year = value.has("year") && value.optInt("year") > 0
                    ? String.valueOf(value.optInt("year")) : "";
            String plot = value.optString("plot", "");
            if (plot.length() > 120) plot = plot.substring(0, 117) + "...";
            if (year.isEmpty()) return plot;
            return plot.isEmpty() ? year : year + " · " + plot;
        }

        @Override
        public String getName() {
            return title;
        }

        @Override
        protected FutureSupplier<String> buildTitle(int seqNum,
                                                     me.aap.fermata.media.pref.BrowsableItemPrefs prefs) {
            return completed(title);
        }

        @Override
        protected FutureSupplier<String> buildSubtitle() {
            return completed(subtitle);
        }

        @Override
        protected FutureSupplier<List<Item>> listChildren() {
            if (!"show".equals(kind)) return completed(Collections.emptyList());
            return App.get().execute(() -> {
                JSONArray episodes = client().getEpisodes(showId, 0, 1000);
                if (episodes == null) return Collections.emptyList();
                List<Item> items = new ArrayList<>(episodes.length());
                for (int i = 0; i < episodes.length(); i++) {
                    JSONObject episode = episodes.optJSONObject(i);
                    if (episode != null) items.add(fromEpisode(this, episode));
                }
                KodiDiagnostics.info("Episodi Kodi caricati: " + items.size());
                return items;
            });
        }
    }
}
