package me.aap.fermata.ui.activity;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KodiAddonEngineService extends Service {
    public static final String EXTRA_ADDON_ID = "addon_id";
    public static final String EXTRA_ADDON_PATH = "addon_path";
    public static final String ACTION_READY = "me.aap.fermata.KODI_ENGINE_READY";
    public static final String ACTION_FAILED = "me.aap.fermata.KODI_ENGINE_FAILED";
    public static final String ACTION_PROGRESS = "me.aap.fermata.KODI_ENGINE_PROGRESS";
    private static final String PREFS = "kodi_engine_state";
    private static final String ASSET_ROOT = "kodi_engine";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        final String addonId = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_ID);
        final String addonPath = intent == null ? null : intent.getStringExtra(EXTRA_ADDON_PATH);
        worker.execute(() -> {
            try {
                progress("Preparazione cartelle private…");
                File root = new File(getFilesDir(), "addons/engine");
                ensureDirectory(root);
                ensureDirectory(new File(root, "addons"));
                ensureDirectory(new File(root, "userdata"));
                ensureDirectory(new File(root, "temp"));
                ensureDirectory(new File(root, "system"));
                ensureDirectory(new File(root, "python"));

                File kodiHome = new File(getFilesDir(), "apk");
                File complete = new File(kodiHome, ".fermata_payload_complete");
                File manifest = new File(kodiHome, "system/addon-manifest.xml");
                if (!complete.isFile() || !manifest.isFile()) {
                    progress("Prima installazione: copia delle risorse Kodi (può richiedere alcuni minuti)…");
                    if (kodiHome.exists()) deleteTree(kodiHome);
                    copyAssetTree(getAssets(), ASSET_ROOT, kodiHome);
                    if (!manifest.isFile()) throw new IOException("Manifest degli add-on Kodi non trovato dopo la copia");
                    try (OutputStream out = new FileOutputStream(complete)) {
                        out.write("Kodi payload staged".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                } else {
                    progress("Risorse Kodi già preparate: salto la copia…");
                }

                progress("Verifica architettura ARM64…");
                if (!android.os.Process.is64Bit()) throw new IOException("Il processo Android non è a 64 bit; Kodi richiede ARM64");
                boolean arm64Supported = false;
                for (String abi : Build.SUPPORTED_ABIS) if ("arm64-v8a".equals(abi)) { arm64Supported = true; break; }
                if (!arm64Supported) throw new IOException("Questo dispositivo non dichiara il supporto ARM64 (arm64-v8a)");

                progress("Verifica libreria Kodi ARM64 e preparazione avvio nativo…");
                // Do not require libkodi.so to exist as an extracted file in nativeLibraryDir.
                // Android may keep JNI libraries inside the APK when extractNativeLibs is false;
                // the platform linker resolves them from the installed package at load time.
                // The authoritative load check is System.loadLibrary("kodi") in org.xbmc.kodi.Main.
                Log.i("KodiAddonEngine", "ARM64 supported; deferring Kodi JNI resolution to " +
                        "org.xbmc.kodi.Main. nativeLibraryDir=" + getApplicationInfo().nativeLibraryDir +
                        ", ABIs=" + java.util.Arrays.toString(Build.SUPPORTED_ABIS));

                if (addonId != null && addonPath != null) {
                    progress("Preparazione add-on " + addonId + "…");
                    String id = safeId(addonId);
                    File source = new File(addonPath);
                    if (!source.isDirectory()) throw new IOException("Cartella dell'add-on non trovata: " + addonPath);
                    File destination = new File(new File(root, "userdata/addons"), id);
                    deleteTree(destination);
                    ensureDirectory(destination);
                    copyDirectory(source, destination);
                    writeAutoexec(new File(root, "userdata/autoexec.py"), id);
                }

                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("payload_staged", true)
                        .putBoolean("native_host_ready", false)
                        .putString("native_core", "libkodi.so caricata dal linker Android")
                        .putString("runtime_root", kodiHome.getAbsolutePath())
                        .putString("last_error", "")
                        .putString("last_stage", "Risorse pronte; avvio dell'attività nativa Kodi…").apply();
                progress("Risorse pronte; apertura schermata Kodi…");
                sendBroadcast(new Intent(ACTION_READY).setPackage(getPackageName())
                        .putExtra(EXTRA_ADDON_ID, addonId == null ? "" : addonId));
            } catch (Throwable e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                Log.e("KodiAddonEngine", "Engine preparation failed", e);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("payload_staged", false).putString("last_error", message)
                        .putString("last_stage", "Errore durante la preparazione").apply();
                sendBroadcast(new Intent(ACTION_FAILED).setPackage(getPackageName()).putExtra("error", message));
            } finally {
                stopSelfResult(startId);
            }
        });
        return START_NOT_STICKY;
    }

    private void progress(String message) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_stage", message).apply();
        Log.i("KodiAddonEngine", message);
        sendBroadcast(new Intent(ACTION_PROGRESS).setPackage(getPackageName()).putExtra("stage", message));
    }
    private String safeId(String id) throws IOException {
        if (id == null || id.isEmpty() || !id.matches("[A-Za-z0-9._-]+") || id.equals(".") || id.equals(".."))
            throw new IOException("ID add-on non valido");
        return id;
    }
    private void ensureDirectory(File d) throws IOException {
        if (!d.isDirectory() && !d.mkdirs()) throw new IOException("Impossibile creare: " + d);
    }
    private void copyAssetTree(AssetManager assets, String path, File dest) throws IOException {
        String[] children = assets.list(path);
        if (children == null || children.length == 0) {
            File parent = dest.getParentFile(); if (parent != null) ensureDirectory(parent);
            try (InputStream in = assets.open(path); OutputStream out = new FileOutputStream(dest)) { copy(in, out); }
            return;
        }
        ensureDirectory(dest);
        for (String child : children) copyAssetTree(assets, path + "/" + child, new File(dest, child));
    }
    private void writeAutoexec(File file, String id) throws IOException {
        ensureDirectory(file.getParentFile());
        String script = "import xbmc\ntry:\n    xbmc.log('Fermata: avvio add-on " + id + "', xbmc.LOGINFO)\n    xbmc.executebuiltin('RunAddon(" + id + ")')\nexcept Exception as exc:\n    xbmc.log('Fermata: errore avvio add-on: %s' % exc, xbmc.LOGERROR)\n";
        try (OutputStream out = new FileOutputStream(file, false)) { out.write(script.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    }
    private void copyDirectory(File source, File dest) throws IOException {
        ensureDirectory(dest);
        File[] children = source.listFiles(); if (children == null) return;
        for (File child : children) {
            File target = new File(dest, child.getName());
            if (child.isDirectory()) copyDirectory(child, target);
            else if (child.isFile()) try (InputStream in = new FileInputStream(child); OutputStream out = new FileOutputStream(target)) { copy(in, out); }
        }
    }
    private void deleteTree(File file) throws IOException {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        if (!file.delete()) throw new IOException("Impossibile rimuovere: " + file);
    }
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536]; int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
    }
    @Override public void onDestroy() { worker.shutdown(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}