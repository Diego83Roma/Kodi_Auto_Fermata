package me.aap.fermata.addon;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads Kodi add-on archives and validates their addon.xml manifest.
 * Packages remain private app data; this class does not execute add-on code.
 */
public final class KodiAddonPackageManager {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final long MAX_PACKAGE_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 4000;

    private KodiAddonPackageManager() { }

    public static final class Dependency {
        public final String id;
        public final String version;
        public final boolean optional;

        Dependency(String id, String version, boolean optional) {
            this.id = id;
            this.version = version;
            this.optional = optional;
        }

        @Override public String toString() {
            return id + (version.isEmpty() ? "" : "  (min. " + version + ")")
                    + (optional ? "  · opzionale" : "");
        }
    }

    public static final class Result {
        public final File archive;
        public final String id;
        public final String version;
        public final List<Dependency> dependencies;

        Result(File archive, String id, String version, List<Dependency> dependencies) {
            this.archive = archive;
            this.id = id;
            this.version = version;
            this.dependencies = dependencies;
        }
    }

    public static Result downloadAndValidate(Context context, KodiRepositoryClient.Addon addon)
            throws Exception {
        if (addon == null || addon.id == null
                || !addon.id.matches("[A-Za-z0-9._-]{2,100}")) {
            throw new IllegalArgumentException("Identificativo add-on non valido");
        }
        URL source = new URL(addon.packageUrl);
        if (!"https".equalsIgnoreCase(source.getProtocol())) {
            throw new IllegalArgumentException("Il pacchetto deve essere scaricato tramite HTTPS");
        }

        File folder = new File(context.getFilesDir(), "kodi-packages/" + addon.id);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new java.io.IOException("Cartella pacchetti non disponibile");
        }
        String safeVersion = addon.version == null ? "unknown" : addon.version.replaceAll("[^A-Za-z0-9._-]", "_");
        File target = new File(folder, safeVersion + ".zip");
        File temporary = new File(folder, safeVersion + ".zip.part");
        if (temporary.exists()) temporary.delete();

        HttpURLConnection connection = (HttpURLConnection) source.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/zip,application/octet-stream,*/*");
        connection.setInstanceFollowRedirects(true);
        try {
            int status = connection.getResponseCode();
            if (!"https".equalsIgnoreCase(connection.getURL().getProtocol())) {
                throw new java.io.IOException("Reindirizzamento non HTTPS rifiutato");
            }
            if (status < 200 || status >= 300) {
                throw new java.io.IOException("Download pacchetto: HTTP " + status);
            }
            long length = connection.getContentLengthLong();
            if (length > MAX_PACKAGE_BYTES) {
                throw new java.io.IOException("Pacchetto oltre il limite di 100 MB");
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[16384];
                long total = 0;
                int count;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_PACKAGE_BYTES) {
                        throw new java.io.IOException("Pacchetto oltre il limite di 100 MB");
                    }
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
            }
        } catch (Exception e) {
            temporary.delete();
            throw e;
        } finally {
            connection.disconnect();
        }

        try {
            Manifest manifest = inspect(temporary, addon.id);
            if (!manifest.id.equals(addon.id)) {
                throw new java.io.IOException("L'ID nel pacchetto non corrisponde al catalogo");
            }
            if (target.exists() && !target.delete()) {
                throw new java.io.IOException("Impossibile sostituire il pacchetto precedente");
            }
            if (!temporary.renameTo(target)) {
                throw new java.io.IOException("Impossibile salvare il pacchetto verificato");
            }
            return new Result(target, manifest.id, manifest.version, manifest.dependencies);
        } catch (Exception e) {
            temporary.delete();
            throw e;
        }
    }

    private static final class Manifest {
        String id;
        String version;
        List<Dependency> dependencies = new ArrayList<>();
    }

    private static Manifest inspect(File archive, String expectedId) throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            ZipEntry manifestEntry = null;
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > MAX_ZIP_ENTRIES) {
                    throw new java.io.IOException("Archivio con troppi file");
                }
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\\\', '/');
                if (name.startsWith("/") || name.contains("../") || name.equals("..")
                        || name.contains("/..") || name.matches("^[A-Za-z]:.*")) {
                    throw new java.io.IOException("Percorso non sicuro nell'archivio ZIP");
                }
                if (name.equals("addon.xml") || name.equals(expectedId + "/addon.xml")) {
                    manifestEntry = entry;
                }
            }
            if (manifestEntry == null) {
                throw new java.io.IOException("Nel pacchetto manca addon.xml: non è un add-on Kodi valido");
            }
            if (manifestEntry.getSize() > MAX_MANIFEST_BYTES) {
                throw new java.io.IOException("Manifest addon.xml troppo grande");
            }
            byte[] xml;
            try (InputStream in = zip.getInputStream(manifestEntry);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int total = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > MAX_MANIFEST_BYTES) {
                        throw new java.io.IOException("Manifest addon.xml troppo grande");
                    }
                    out.write(buffer, 0, n);
                }
                xml = out.toByteArray();
            }
            return parseManifest(xml);
        }
    }

    private static Manifest parseManifest(byte[] xml) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new java.io.ByteArrayInputStream(xml), "UTF-8");
        Manifest manifest = new Manifest();
        boolean inRequires = false;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("addon".equals(tag) && manifest.id == null) {
                    manifest.id = parser.getAttributeValue(null, "id");
                    manifest.version = parser.getAttributeValue(null, "version");
                } else if ("requires".equals(tag)) {
                    inRequires = true;
                } else if (inRequires && "import".equals(tag)) {
                    String id = parser.getAttributeValue(null, "addon");
                    if (id != null && !id.trim().isEmpty()) {
                        String version = parser.getAttributeValue(null, "version");
                        String optional = parser.getAttributeValue(null, "optional");
                        manifest.dependencies.add(new Dependency(id.trim(),
                                version == null ? "" : version.trim(),
                                "true".equalsIgnoreCase(optional)));
                    }
                }
            } else if (event == XmlPullParser.END_TAG && "requires".equals(parser.getName())) {
                inRequires = false;
            }
            event = parser.next();
        }
        if (manifest.id == null || !manifest.id.matches("[A-Za-z0-9._-]{2,100}")) {
            throw new java.io.IOException("Manifest Kodi privo di ID valido");
        }
        if (manifest.version == null || manifest.version.trim().isEmpty()) {
            throw new java.io.IOException("Manifest Kodi privo di versione");
        }
        return manifest;
    }
}
