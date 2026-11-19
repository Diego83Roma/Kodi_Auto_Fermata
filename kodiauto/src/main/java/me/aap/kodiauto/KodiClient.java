package me.aap.kodiauto;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client minimale per il JSON-RPC di Kodi.
 * ATTENZIONE: tutti i metodi sono bloccanti, chiamali fuori dal thread principale.
 */
public class KodiClient {
    private final String base; // es. "http://127.0.0.1:8080"
    private final String authHeader; // null se Kodi non ha utente/password

    public KodiClient(String host, int port, String user, String pass) {
        this.base = "http://" + host + ":" + port;
        if (user != null && !user.isEmpty()) {
            String raw = user + ":" + (pass == null ? "" : pass);
            this.authHeader = "Basic " + Base64.encodeToString(
                    raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        } else {
            this.authHeader = null;
        }
    }

    public static class Movie {
        public int id;
        public String title;
        public int year;
        public String file;      // percorso del file come lo vede Kodi
        public String thumbUrl;  // URL immagine servito da Kodi (puo' essere null)
    }

    public static class Channel {
        public int id;
        public String name;
    }

    /** Chiamata JSON-RPC generica. Lancia eccezione se Kodi risponde con "error". */
    public JSONObject call(String method, JSONObject params) throws Exception {
        JSONObject req = new JSONObject();
        req.put("jsonrpc", "2.0");
        req.put("id", 1);
        req.put("method", method);
        if (params != null) req.put("params", params);

        HttpURLConnection c = (HttpURLConnection) new URL(base + "/jsonrpc").openConnection();
        c.setConnectTimeout(3000);
        c.setReadTimeout(10000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
        try (OutputStream os = c.getOutputStream()) {
            os.write(req.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + " da Kodi (401 = credenziali errate)");

        String body = readAll(c.getInputStream());
        JSONObject resp = new JSONObject(body);
        if (resp.has("error")) throw new Exception("Errore Kodi: " + resp.getJSONObject("error"));
        Object res = resp.get("result");
        // Alcuni metodi rispondono con una stringa ("OK") o un array: li avvolgiamo in {"value": ...}
        return res instanceof JSONObject ? (JSONObject) res : new JSONObject().put("value", res);
    }

    /** Elenco film ordinati per titolo. */
    public List<Movie> getMovies() throws Exception {
        JSONObject p = new JSONObject();
        p.put("properties", new JSONArray().put("title").put("year").put("file").put("thumbnail"));
        p.put("sort", new JSONObject().put("method", "title").put("order", "ascending"));
        JSONObject r = call("VideoLibrary.GetMovies", p);
        List<Movie> out = new ArrayList<>();
        JSONArray arr = r.optJSONArray("movies");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Movie m = new Movie();
            m.id = o.getInt("movieid");
            m.title = o.optString("title");
            m.year = o.optInt("year");
            m.file = o.optString("file");
            String t = o.optString("thumbnail", "");
            m.thumbUrl = t.isEmpty() ? null : base + "/image/" + URLEncoder.encode(t, "UTF-8");
            out.add(m);
        }
        return out;
    }

    /**
     * URL HTTP riproducibile di un file, servito da Kodi.
     * Usa Files.PrepareDownload: funziona anche se il file e' su smb://, nfs:// ecc.
     */
    public String getPlayableUrl(String kodiFilePath) throws Exception {
        JSONObject r = call("Files.PrepareDownload", new JSONObject().put("path", kodiFilePath));
        JSONObject details = r.getJSONObject("details");
        String path = details.getString("path");
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return base + "/" + (path.startsWith("/") ? path.substring(1) : path);
    }

    /** Header da passare al player (ExoPlayer/VLC) per le richieste al server web di Kodi. */
    public Map<String, String> authHeaders() {
        Map<String, String> h = new HashMap<>();
        if (authHeader != null) h.put("Authorization", authHeader);
        return h;
    }

    /** Canali TV di un gruppo. groupId: "alltv" oppure l'ID numerico di PVR.GetChannelGroups. */
    public List<Channel> getChannels(Object groupId) throws Exception {
        JSONObject p = new JSONObject();
        p.put("channelgroupid", groupId); // OBBLIGATORIO: la sua assenza da' -32602
        p.put("properties", new JSONArray().put("thumbnail").put("channeltype").put("hidden"));
        JSONObject r = call("PVR.GetChannels", p);
        List<Channel> out = new ArrayList<>();
        JSONArray arr = r.optJSONArray("channels");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (o.optBoolean("hidden", false)) continue;
            Channel c = new Channel();
            c.id = o.getInt("channelid");
            c.name = o.optString("label");
            out.add(c);
        }
        return out;
    }

    /** Avvia un canale TV direttamente dentro Kodi (video e audio escono da Kodi). */
    public void openChannelInKodi(int channelId) throws Exception {
        call("Player.Open", new JSONObject().put("item", new JSONObject().put("channelid", channelId)));
    }

    /** Voce generica di libreria (serie, episodio, gruppo canali, preferito). */
    public static class Entry {
        public int num;          // tvshowid / episodeid / channelgroupid
        public int order;        // per ordinare gli episodi
        public String title;
        public String subtitle;
        public String file;      // percorso Kodi (episodi e preferiti)
    }

    public List<Entry> getTvShows() throws Exception {
        JSONObject p = new JSONObject();
        p.put("properties", new JSONArray().put("title").put("year"));
        p.put("sort", new JSONObject().put("method", "title").put("order", "ascending"));
        JSONArray arr = call("VideoLibrary.GetTVShows", p).optJSONArray("tvshows");
        List<Entry> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Entry e = new Entry();
            e.num = o.getInt("tvshowid");
            e.title = o.optString("title");
            out.add(e);
        }
        return out;
    }

    public List<Entry> getEpisodes(int tvshowId) throws Exception {
        JSONObject p = new JSONObject();
        p.put("tvshowid", tvshowId);
        p.put("properties", new JSONArray().put("title").put("season").put("episode").put("file"));
        JSONArray arr = call("VideoLibrary.GetEpisodes", p).optJSONArray("episodes");
        List<Entry> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Entry e = new Entry();
            e.num = o.getInt("episodeid");
            int s = o.optInt("season"), ep = o.optInt("episode");
            e.order = s * 10000 + ep;
            e.title = String.format("S%02dE%02d - %s", s, ep, o.optString("title"));
            e.file = o.optString("file");
            out.add(e);
        }
        java.util.Collections.sort(out, (a, b) -> Integer.compare(a.order, b.order));
        return out;
    }

    public List<Entry> getChannelGroups() throws Exception {
        JSONObject p = new JSONObject().put("channeltype", "tv");
        JSONArray arr = call("PVR.GetChannelGroups", p).optJSONArray("channelgroups");
        List<Entry> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Entry e = new Entry();
            e.num = o.getInt("channelgroupid");
            e.title = o.optString("label");
            out.add(e);
        }
        return out;
    }

    /** Preferiti di tipo "media" (gli altri tipi, es. finestre o script, vengono ignorati). */
    public List<Entry> getFavourites() throws Exception {
        JSONObject p = new JSONObject().put("properties", new JSONArray().put("path").put("thumbnail"));
        JSONArray arr = call("Favourites.GetFavourites", p).optJSONArray("favourites");
        List<Entry> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (!"media".equals(o.optString("type"))) continue;
            Entry e = new Entry();
            e.title = o.optString("title");
            e.file = o.optString("path");
            out.add(e);
        }
        return out;
    }

    private int activePlayerId() throws Exception {
        JSONArray a = call("Player.GetActivePlayers", null).getJSONArray("value");
        return a.length() == 0 ? -1 : a.getJSONObject(0).getInt("playerid");
    }

    public void togglePlayPause() throws Exception {
        int id = activePlayerId();
        if (id >= 0) call("Player.PlayPause", new JSONObject().put("playerid", id));
    }

    public void stopPlayer() throws Exception {
        int id = activePlayerId();
        if (id >= 0) call("Player.Stop", new JSONObject().put("playerid", id));
    }

    /** Avvia un file o un preferito dentro Kodi (video e audio escono da Kodi). */
    public void openFileInKodi(String file) throws Exception {
        call("Player.Open", new JSONObject().put("item", new JSONObject().put("file", file)));
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream is = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }
}