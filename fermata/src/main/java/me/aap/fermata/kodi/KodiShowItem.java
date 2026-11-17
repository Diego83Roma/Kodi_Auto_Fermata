package me.aap.fermata.kodi;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;

/** A Kodi TV show whose children are its actual Kodi episodes. */
public final class KodiShowItem extends BrowsableItemBase {
    public static final String PREFIX = "kodi:show:";
    private final int tvShowId;
    private final String title;

    KodiShowItem(KodiShortcutItem parent, int tvShowId, String title) {
        super(PREFIX + tvShowId, parent, null);
        this.tvShowId = tvShowId;
        this.title = (title == null || title.isEmpty()) ? "Serie TV" : title;
    }

    public static boolean isId(String id) {
        return id != null && id.startsWith(PREFIX);
    }

    public static int getKodiId(String id) {
        if (!isId(id)) return -1;
        try {
            return Integer.parseInt(id.substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected FutureSupplier<List<Item>> listChildren() {
        Promise<List<Item>> result = new Promise<>();
        new Thread(() -> {
            List<Item> items = new ArrayList<>();
            try {
                KodiJsonRpcClient client = new KodiJsonRpcClient(
                        KodiJsonRpcClient.DEFAULT_HOST, KodiJsonRpcClient.DEFAULT_PORT, null, null);
                JSONArray episodes = client.getEpisodes(tvShowId, 0, 500);
                if (episodes != null) {
                    for (int i = 0; i < episodes.length(); i++) {
                        JSONObject episode = episodes.optJSONObject(i);
                        if (episode == null) continue;
                        int id = episode.optInt("episodeid", -1);
                        if (id < 0) continue;
                        items.add(new KodiMediaItem(this, KodiMediaItem.EPISODE, id,
                                episode.optString("title", "Episodio")));
                    }
                }
                KodiDiagnostics.info("Episodi Kodi caricati: " + items.size()
                        + " per serie " + tvShowId);
            } catch (Exception e) {
                KodiDiagnostics.error("Caricamento episodi Kodi non riuscito", e);
            }
            result.complete(items);
        }, "Kodi-Episodes").start();
        return result;
    }

    @Override
    protected FutureSupplier<String> buildTitle(int seqNum, BrowsableItemPrefs parentPrefs) {
        return completed(title);
    }

    @Override
    protected FutureSupplier<String> buildSubtitle() {
        return completed("Episodi Kodi");
    }

    @Override
    public int getIcon() {
        return R.drawable.video;
    }

    @NonNull
    @Override
    public FutureSupplier<Uri> getIconUri() {
        return completedNull();
    }
}
