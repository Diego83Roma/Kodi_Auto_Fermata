package me.aap.fermata.ui.activity;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Isolated Kodi payload staging host.
 *
 * The ARM64 native libraries are installed by Android in nativeLibraryDir.
 * Kodi's packaged assets are copied into this process-private data tree.
 * NativeActivity/JNI and the Python xbmc.* bridge are deliberately not
 * started until their Android host integration is present.
 */
public final class KodiAddonEngineService extends Service {
    public static final String EXTRA_ADDON_ID = "addon_id";
    public static final String EXTRA_ADDON_PATH = "addon_path";
    public static final String ACTION_READY = "me.aap.fermata.KODI_ENGINE_READY";
    public static final String ACTION_FAILED = "me.aap.fermata.KODI_ENGINE_FAILED";
    private static final String PREFS = "kodi_engine_state";
    private static final String ASSET_ROOT = "kodi_engine";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String addonId = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_ID);
        final String addonPath = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_PATH);
        worker.execute(() -> {
            try {
                File root = new File(getFilesDir(), "addons/engine");
                ensureDirectory(root);
                ensureDirectory(new File(root, "addons"));
                ensureDirectory(new File(root, "userdata"));
                ensureDirectory(new File(root, "temp"));
                ensureDirectory(new File(root, "system"));
                ensureDirectory(new File(root, "python"));

                File kodiHome = new File(getFilesDir(), "apk");
                copyAssetTree(getAssets(), ASSET_ROOT, kodiHome);
                File nativeCore = new File(getApplicationInfo().nativeLibraryDir, "libkodi.so");
                if (!nativeCore.isFile() || nativeCore.length() < 1024 * 1024) {
                    throw new IOException("Libreria nativa Kodi ARM64 non installata");
                }
                File manifest = new File(kodiHome, "system/addon-manifest.xml");
                if (!manifest.isFile()) {
                    throw new IOException("Manifest degli add-on Kodi non trovato");
                }

                if (addonId != null && addonPath != null) {
                    File source = new File(addonPath);
                    File destination = new File(new File(kodiHome, "addons"), safeId(addonId));
                    if (source.isDirectory()) copyDirectory(source, destination);
                }

                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("payload_staged", true)
                        .putBoolean("native_host_ready", false)
                        .putString("native_core", nativeCore.getAbsolutePath())
                        .putString("runtime_root", kodiHome.getAbsolutePath())
                        .putString("last_error", "")
                        .apply();
                sendBroadcast(new Intent(ACTION_READY).setPackage(getPackageName())
                        .putExtra(EXTRA_ADDON_ID, addonId == null ? "" : addonId));
            } catch (Exception e) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("payload_staged", false)
                        .putString("last_error", e.getMessage() == null ? e.toString() : e.getMessage())
                        .apply();
                sendBroadcast(new Intent(ACTION_FAILED).setPackage(getPackageName())
                        .putExtra("error", e.getMessage() == null ? e.toString() : e.getMessage()));
            } finally {
                stopSelfResult(startId);
            }
        });
        return START_NOT_STICKY;
    }

    private String safeId(String id) throws IOException {
        if (id.isEmpty() || id.contains("/") || id.contains("\\") || id.equals(".") || id.equals(".."))
            throw new IOException("ID add-on non valido");
        return id;
    }

    private void ensureDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IOException("Impossibile creare: " + directory);
    }

    private void copyAssetTree(AssetManager assets, String assetPath, File destination) throws IOException {
        String[] children = assets.list(assetPath);
        if (children == null || children.length == 0) {
            File parent = destination.getParentFile();
            if (parent != null) ensureDirectory(parent);
            try (InputStream in = assets.open(assetPath); OutputStream out = new FileOutputStream(destination)) {
                copy(in, out);
            }
            return;
        }
        ensureDirectory(destination);
        for (String child : children) {
            copyAssetTree(assets, assetPath + "/" + child, new File(destination, child));
        }
    }

    private void copyDirectory(File source, File destination) throws IOException {
        ensureDirectory(destination);
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            File target = new File(destination, child.getName());
            if (child.isDirectory()) copyDirectory(child, target);
            else if (child.isFile()) {
                try (InputStream in = new FileInputStream(child); OutputStream out = new FileOutputStream(target)) {
                    copy(in, out);
                }
            }
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[32768];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
    }

    @Override
    public void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
