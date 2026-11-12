package me.aap.fermata.addon;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Lightweight Kodi repository catalogue reader.
 *
 * This class reads Kodi repository metadata only. It does not execute Kodi
 * Python add-ons or emulate the xbmc Python API.
 */
public final class KodiRepositoryClient {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 25000;
    private static final int MAX_CATALOG_BYTES = 32 * 1024 * 1024;
    private static final int MAX_ADDONS = 30000;

    private KodiRepositoryClient() {
    }

    public static final class Addon {
        public final String id;
        public final String name;
        public final String version;
        public final String provider;
        public final String summary;
        public final String description;
        public final String type;
        public final String iconUrl;
        public final String packageUrl;

        Addon(String id, String name, String version, String provider,
              String summary, String description, String type,
              String iconUrl, String packageUrl) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.provider = provider;
            this.summary = summary;
            this.description = description;
            this.type = type;
            this.iconUrl = iconUrl;
            this.packageUrl = packageUrl;
        }

        @Override
        public String toString() {
            return name + "  ·  " + version;
        }
    }

    public static List<Addon> load(String repositoryAddress) throws Exception {
        String address = repositoryAddress == null ? "" : repositoryAddress.trim();
        if (!address.startsWith("https://")) {
            throw new IllegalArgumentException("Il repository deve usare HTTPS");
        }

        String infoUrl = address;
        if (!infoUrl.toLowerCase(Locale.ROOT).endsWith("addons.xml")
                && !infoUrl.toLowerCase(Locale.ROOT).endsWith("addons.xml.gz")) {
            if (!infoUrl.endsWith("/")) infoUrl += "/";
            infoUrl += "addons.xml.gz";
        }

        byte[] xml;
        try {
            xml = download(infoUrl);
        } catch (Exception first) {
            if (!infoUrl.toLowerCase(Locale.ROOT).endsWith("addons.xml.gz")) throw first;
            String plainUrl = infoUrl.substring(0, infoUrl.length() - 3);
            xml = download(plainUrl);
        }

        boolean gzip = xml.length > 2 && (xml[0] & 0xff) == 0x1f && (xml[1] & 0xff) == 0x8b;
        if (gzip) {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(xml))) {
                xml = readLimited(in);
            }
        }
        return parse(xml, infoUrl);
    }

    private static byte[] download(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/xml,text/xml,*/*");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setInstanceFollowRedirects(true);
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new java.io.IOException("HTTP " + status + " durante il download del catalogo");
            }
            try (InputStream raw = new BufferedInputStream(connection.getInputStream())) {
                byte[] bytes = readLimited(raw);
                String encoding = connection.getContentEncoding();
                if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                        return readLimited(gzip);
                    }
                }
                return bytes;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readLimited(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        int total = 0;
        int count;
        while ((count = in.read(buffer)) != -1) {
            total += count;
            if (total > MAX_CATALOG_BYTES) {
                throw new java.io.IOException("Catalogo troppo grande (limite 32 MB)");
            }
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    private static List<Addon> parse(byte[] xml, String infoUrl) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(xml), "UTF-8");
        List<Addon> result = new ArrayList<>();
        String baseUrl = infoUrl;
        int slash = baseUrl.lastIndexOf('/');
        if (slash >= 0) baseUrl = baseUrl.substring(0, slash + 1);
        int event = parser.getEventType();
        String id = null, name = null, version = null, provider = null;
        String summary = "", description = "", type = "Altro", icon = "";
        String currentTag = null;
        int depth = 0;
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                depth++;
                if ("addon".equals(tag) && depth == 2) {
                    id = parser.getAttributeValue(null, "id");
                    name = parser.getAttributeValue(null, "name");
                    version = parser.getAttributeValue(null, "version");
                    provider = parser.getAttributeValue(null, "provider-name");
                    summary = "";
                    description = "";
                    type = "Altro";
                    icon = "";
                } else if (id != null && "extension".equals(tag)) {
                    String point = parser.getAttributeValue(null, "point");
                    if (point != null) {
                        if (point.contains("pluginsource")) type = "Plugin";
                        else if (point.contains("script")) type = "Script";
                        else if (point.contains("repository")) type = "Repository";
                        else if (point.contains("xbmc.service")) type = "Servizio";
                        else if (point.contains("xbmc.python")) type = "Python";
                        else if (point.contains("xbmc.addon")) type = "Componente";
                    }
                } else if (id != null && "icon".equals(tag)) {
                    currentTag = "icon";
                } else if (id != null && "summary".equals(tag)) {
                    currentTag = "summary";
                } else if (id != null && "description".equals(tag)) {
                    currentTag = "description";
                }
            } else if (event == XmlPullParser.TEXT && id != null && currentTag != null) {
                String value = parser.getText() == null ? "" : parser.getText().trim();
                if ("icon".equals(currentTag) && !value.isEmpty()) icon = value;
                else if ("summary".equals(currentTag) && !value.isEmpty()) summary = value;
                else if ("description".equals(currentTag) && !value.isEmpty()) description = value;
            } else if (event == XmlPullParser.END_TAG) {
                String tag = parser.getName();
                if ("icon".equals(tag) || "summary".equals(tag) || "description".equals(tag)) {
                    currentTag = null;
                }
                if ("addon".equals(tag) && id != null) {
                    String packageUrl = baseUrl + id + "/" + id + "-" + version + ".zip";
                    result.add(new Addon(id, safe(name, id), safe(version, ""),
                            safe(provider, ""), summary, description, type,
                            resolve(baseUrl, icon), packageUrl));
                    if (result.size() >= MAX_ADDONS) break;
                    id = null;
                }
                depth--;
            }
            event = parser.next();
        }
        Collections.sort(result, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String resolve(String base, String path) {
        if (path == null || path.isEmpty()) return "";
        if (path.startsWith("https://")) return path;
        if (path.startsWith("/")) {
            try {
                URL u = new URL(base);
                return u.getProtocol() + "://" + u.getAuthority() + path;
            } catch (Exception ignored) {
                return "";
            }
        }
        return base + path;
    }
}
