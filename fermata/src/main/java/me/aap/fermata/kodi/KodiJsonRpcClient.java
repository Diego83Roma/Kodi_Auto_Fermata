package me.aap.fermata.kodi;

import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal JSON-RPC transport for a Kodi instance running on the same Android device.
 *
 * All methods perform blocking network I/O and MUST be called from a worker thread.
 * The first prototype deliberately supports loopback only; it does not expose Kodi
 * credentials or its web server to the local network.
 */
public final class KodiJsonRpcClient {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 7000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final URL endpoint;
    private final String authorization;
    private final AtomicInteger requestIds = new AtomicInteger();

    public KodiJsonRpcClient(String host, int port, String username, String password)
            throws IOException {
        if (!DEFAULT_HOST.equals(host) && !"localhost".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Kodi backend prototype accepts loopback only");
        }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid Kodi HTTP port");
        endpoint = new URL("http://" + host + ":" + port + "/jsonrpc");

        if (username != null && !username.isEmpty()) {
            String credentials = username + ":" + ((password == null) ? "" : password);
            authorization = "Basic " + Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        } else {
            authorization = null;
        }
    }

    /** Returns the JSON-RPC result object. Invoke from a worker thread, never the UI thread. */
    public JSONObject call(String method, JSONObject params) throws IOException, JSONException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Kodi JSON-RPC must not run on the main thread");
        }

        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", (params == null) ? new JSONObject() : params);
        request.put("id", requestIds.incrementAndGet());

        KodiDiagnostics.info("Richiesta JSON-RPC: " + method);
        long startedAt = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (authorization != null) connection.setRequestProperty("Authorization", authorization);

        try {
            byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int status = connection.getResponseCode();
            InputStream stream = (status >= 200 && status < 300)
                    ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IOException("Kodi HTTP error: " + status);

            byte[] responseBytes;
            try (InputStream in = stream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    if (buffer.size() + read > MAX_RESPONSE_BYTES) {
                        throw new IOException("Kodi response exceeds 2 MB limit");
                    }
                    buffer.write(chunk, 0, read);
                }
                responseBytes = buffer.toByteArray();
            }

            String responseText = new String(responseBytes, StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new IOException("Kodi HTTP error " + status + ": " + responseText);
            }

            JSONObject response = new JSONObject(responseText);
            JSONObject error = response.optJSONObject("error");
            if (error != null) {
                throw new KodiRpcException(error.optInt("code"), error.optString("message"));
            }

            // Kodi methods may return a JSON object OR a scalar status such as "OK".
            // GUI.ActivateWindow returns a scalar on several Kodi versions; treating it
            // as a missing object made a successful navigation look like a failure.
            Object rawResult = response.opt("result");
            if (rawResult == null || rawResult == JSONObject.NULL) {
                throw new IOException("Kodi returned no JSON-RPC result");
            }
            JSONObject result;
            if (rawResult instanceof JSONObject) {
                result = (JSONObject) rawResult;
            } else {
                result = new JSONObject().put("value", rawResult);
            }
            KodiDiagnostics.info("JSON-RPC " + method + " completata (HTTP " + status
                    + ", " + (System.currentTimeMillis() - startedAt) + " ms)");
            return result;
        } catch (IOException | JSONException | RuntimeException e) {
            KodiDiagnostics.error("Errore JSON-RPC " + method, e);
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    public JSONObject getApiVersion() throws IOException, JSONException {
        return call("JSONRPC.Version", null);
    }

    /** Opens a Kodi GUI window, optionally navigating to a Kodi library path. */
    public JSONObject activateWindow(String window, String path)
            throws IOException, JSONException {
        JSONObject params = new JSONObject();
        params.put("window", window);
        if (path != null && !path.isEmpty()) {
            params.put("parameters", new JSONArray().put(path));
        }
        return call("GUI.ActivateWindow", params);
    }

    public JSONArray getMovies(int start, int end) throws IOException, JSONException {
        JSONObject params = new JSONObject();
        params.put("properties", new JSONArray()
                .put("title").put("year").put("genre").put("plot")
                .put("thumbnail").put("file").put("runtime"));
        params.put("limits", new JSONObject().put("start", Math.max(0, start))
                .put("end", Math.max(start, end)));
        return call("VideoLibrary.GetMovies", params).optJSONArray("movies");
    }

    public JSONArray getActivePlayers() throws IOException, JSONException {
        return call("Player.GetActivePlayers", null).optJSONArray("value");
    }

    public JSONObject playPause(int playerId, String play)
            throws IOException, JSONException {
        return call("Player.PlayPause", new JSONObject().put("playerid", playerId)
                .put("play", play));
    }

    public JSONObject stopPlayer(int playerId) throws IOException, JSONException {
        return call("Player.Stop", new JSONObject().put("playerid", playerId));
    }

    public JSONObject openMovie(int movieId) throws IOException, JSONException {
        return call("Player.Open", new JSONObject().put("item",
                new JSONObject().put("movieid", movieId)));
    }

    public JSONObject openEpisode(int episodeId) throws IOException, JSONException {
        return call("Player.Open", new JSONObject().put("item",
                new JSONObject().put("episodeid", episodeId)));
    }

    public JSONArray getTvShows(int start, int end) throws IOException, JSONException {
        JSONObject params = new JSONObject();
        params.put("properties", new JSONArray()
                .put("title").put("year").put("genre").put("plot").put("thumbnail"));
        params.put("limits", new JSONObject().put("start", Math.max(0, start))
                .put("end", Math.max(start, end)));
        return call("VideoLibrary.GetTVShows", params).optJSONArray("tvshows");
    }

    public JSONArray getEpisodes(int tvShowId, int start, int end)
            throws IOException, JSONException {
        JSONObject params = new JSONObject();
        params.put("tvshowid", tvShowId);
        params.put("properties", new JSONArray()
                .put("title").put("season").put("episode").put("plot")
                .put("thumbnail").put("file").put("runtime"));
        params.put("limits", new JSONObject().put("start", Math.max(0, start))
                .put("end", Math.max(start, end)));
        return call("VideoLibrary.GetEpisodes", params).optJSONArray("episodes");
    }

    public JSONArray getFavorites() throws IOException, JSONException {
        return call("Favourites.GetFavourites", new JSONObject()).optJSONArray("favourites");
    }
    public JSONArray getTvChannels() throws IOException, JSONException {
        JSONArray groups=call("PVR.GetChannelGroups",new JSONObject().put("channeltype","tv")).optJSONArray("channelgroups");
        if(groups==null||groups.length()==0)return new JSONArray();
        int id=groups.optJSONObject(0).optInt("channelgroupid",-1); if(id<0)return new JSONArray();
        JSONObject p=new JSONObject().put("channelgroupid",id).put("properties",
                new JSONArray().put("channel").put("channelid").put("channelnumber").put("thumbnail"));
        return call("PVR.GetChannels",p).optJSONArray("channels");
    }
    public JSONArray getVideoPlaylists() throws IOException, JSONException {
        JSONObject p=new JSONObject().put("directory","special://videoplaylists/").put("media","files")
                .put("properties",new JSONArray().put("title").put("file"));
        return call("Files.GetDirectory",p).optJSONArray("files");
    }
    public JSONObject openExternal(JSONObject item) throws IOException, JSONException {
        String kind=item.optString("kind","");
        if("channel".equals(kind)) return call("Player.Open",new JSONObject().put("item",
                new JSONObject().put("channelid",item.getInt("channelid"))));
        if("playlist".equals(kind)) return activateWindow("videos",item.optString("file"));
        if("favorite".equals(kind)) {
            String type=item.optString("type","");
            if("media".equals(type)) return call("Player.Open",new JSONObject().put("item",
                    new JSONObject().put("file",item.getString("path"))));
            if("window".equals(type)) {
                JSONObject p=new JSONObject().put("window",item.getString("window"));
                String parameter=item.optString("windowparameter","");
                if(!parameter.isEmpty())p.put("parameters",new JSONArray().put(parameter));
                return call("GUI.ActivateWindow",p);
            }
            throw new IOException("Tipo preferito Kodi non supportato: "+type);
        }
        throw new IOException("Elemento Kodi non riconosciuto");
    }
    public JSONArray getAddons() throws IOException, JSONException {
        JSONObject params = new JSONObject();
        params.put("properties", new JSONArray().put("name").put("version")
                .put("enabled").put("installed").put("icon"));
        return call("Addons.GetAddons", params).optJSONArray("addons");
    }

    public JSONObject executeAddon(String addonId) throws IOException, JSONException {
        return call("Addons.ExecuteAddon", new JSONObject().put("addonid", addonId));
    }

    /**
     * Kodi may return transport-specific details here; this method intentionally does
     * not assume that the result is already a playable URL for Fermata's media engine.
     */
    public JSONObject prepareDownload(String kodiVfsPath) throws IOException, JSONException {
        return call("Files.PrepareDownload", new JSONObject().put("path", kodiVfsPath));
    }

    public static final class KodiRpcException extends IOException {
        private final int code;

        KodiRpcException(int code, String message) {
            super("Kodi JSON-RPC error " + code + ": " + message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
