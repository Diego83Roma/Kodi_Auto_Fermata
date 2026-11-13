package me.aap.fermata.ui.activity;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;

/**
 * Isolated process and private directory bootstrap for the embedded add-on runtime.
 *
 * This is deliberately only the host boundary. Kodi's native core and Python API
 * must be integrated before third-party Kodi add-ons can be executed.
 */
public final class KodiAddonEngineService extends Service {
    public static final String EXTRA_ADDON_ID = "addon_id";
    public static final String EXTRA_ADDON_PATH = "addon_path";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        File root = new File(getFilesDir(), "addons/engine");
        ensureDirectory(root);
        ensureDirectory(new File(root, "addons"));
        ensureDirectory(new File(root, "userdata"));
        ensureDirectory(new File(root, "temp"));
        ensureDirectory(new File(root, "system"));
        ensureDirectory(new File(root, "python"));

        // Keep add-on payloads in the app-private engine tree, separate from
        // Fermata's own media/player data. Execution is intentionally gated
        // until the real Kodi native runtime and Python bindings are packaged.
        String addonId = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_ID);
        String addonPath = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_PATH);
        if (addonId != null && addonPath != null) {
            File source = new File(addonPath);
            File destination = new File(new File(root, "addons"), addonId);
            if (source.isDirectory() && !destination.exists()) {
                copyDirectory(source, destination);
            }
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void ensureDirectory(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create engine directory: " + directory);
        }
    }

    private void copyDirectory(File source, File destination) {
        ensureDirectory(destination);
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            File target = new File(destination, child.getName());
            if (child.isDirectory()) copyDirectory(child, target);
            else if (child.isFile()) {
                try (java.io.InputStream in = new java.io.FileInputStream(child);
                     java.io.OutputStream out = new java.io.FileOutputStream(target)) {
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("Cannot stage add-on file: " + child.getName(), e);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
