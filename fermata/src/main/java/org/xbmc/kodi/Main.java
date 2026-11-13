package org.xbmc.kodi;

import android.app.NativeActivity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.hardware.input.InputManager;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

/** Kodi JNI host with persistent checkpoints for startup crash diagnosis. */
public final class Main extends NativeActivity implements Choreographer.FrameCallback {
 public static Main MainActivity;
 public XBMCMainView mMainView;
 private View decor;
 private boolean shuttingDown;
 private boolean firstFrame = true;
 private Handler handler;
 private XBMCSettingsContentObserver settingsObserver;
 private XBMCInputDeviceListener inputListener;
 private static final String DIAG_PREFS = "kodi_engine_state";

 private void diag(String stage, Throwable error) {
  String detail = error == null ? "" : error.getClass().getName() + ": " + error.getMessage();
  try {
   getSharedPreferences(DIAG_PREFS, MODE_PRIVATE).edit()
    .putString("last_stage", stage).putString("last_error", detail)
    .putLong("last_stage_time", System.currentTimeMillis()).commit();
  } catch (Throwable ignored) { }
  if (error == null) android.util.Log.i("KodiAddonEngine", "CHECKPOINT: " + stage);
  else android.util.Log.e("KodiAddonEngine", "CHECKPOINT: " + stage, error);
 }


 private void recordNativeLibraryDiagnostics() {
  String apkPath = getApplicationInfo().sourceDir;
  String nativeDir = getApplicationInfo().nativeLibraryDir;
  StringBuilder files = new StringBuilder();
  try {
   java.io.File dir = new java.io.File(nativeDir);
   java.io.File[] entries = dir.listFiles();
   if (entries != null) for (java.io.File f : entries) {
    if (files.length() > 0) files.append(", ");
    files.append(f.getName()).append("(").append(f.length()).append(")");
   }
   if (files.length() == 0) files.append("(vuota o non accessibile)");
  } catch (Throwable t) { files.append("lettura fallita: ").append(t); }
  boolean inApk = false;
  long libSize = -1L;
  String zipError = "";
  try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(apkPath)) {
   java.util.zip.ZipEntry entry = zip.getEntry("lib/arm64-v8a/libkodi.so");
   if (entry != null) { inApk = true; libSize = entry.getSize(); }
  } catch (Throwable t) { zipError = t.getClass().getName() + ": " + t.getMessage(); }
  try {
   getSharedPreferences(DIAG_PREFS, MODE_PRIVATE).edit()
    .putString("native_apk_path", apkPath)
    .putString("native_dir_path", nativeDir)
    .putString("native_dir_files", files.toString())
    .putBoolean("native_lib_in_apk", inApk)
    .putLong("native_lib_apk_size", libSize)
    .putString("native_zip_error", zipError)
    .putString("native_abi", android.os.Build.SUPPORTED_ABIS[0])
    .commit();
  } catch (Throwable ignored) { }
  android.util.Log.i("KodiAddonEngine", "Native diagnostics: apk=" + apkPath
   + ", nativeDir=" + nativeDir + ", libkodi in APK=" + inApk
   + ", size=" + libSize + ", nativeDir files=" + files + ", zipError=" + zipError);
 }

 private native void _onNewIntent(Intent intent);
 private native void _onActivityResult(int requestCode, int resultCode, Intent resultData);
 private native void _doFrame(long frameTimeNanos);
 private native void _onVisibleBehindCanceled();
 private native void _callNative(long funcAddr, long variantAddr);

 public Main() { super(); MainActivity = this; }

 @Override public void onCreate(Bundle state) {
  diag("01 Main.onCreate: ingresso", null);
  java.io.File home = new java.io.File(getFilesDir(), "apk");
  java.io.File userdata = new java.io.File(getFilesDir(), "addons/engine/userdata");
  java.io.File temp = new java.io.File(getCacheDir(), "kodi");
  home.mkdirs(); userdata.mkdirs(); temp.mkdirs();
  System.setProperty("xbmc.home", home.getAbsolutePath());
  System.setProperty("xbmc.data", userdata.getAbsolutePath());
  System.setProperty("xbmc.temp", temp.getAbsolutePath());
  System.setProperty("xbmc.proploaded", "yes");
  diag("02 Percorsi Kodi impostati", null);
  try {
   recordNativeLibraryDiagnostics();
   diag("03 System.loadLibrary(kodi): avvio", null);
   System.loadLibrary("kodi");
   diag("04 Libreria JNI Kodi caricata", null);
  } catch (Throwable e) {
   diag("ERRORE 03 caricamento libreria JNI Kodi", e);
   throw new RuntimeException("Caricamento della libreria Kodi non riuscito", e);
  }
  try {
   diag("05 NativeActivity.super.onCreate: avvio", null);
   super.onCreate(state);
   diag("06 NativeActivity.super.onCreate: completato", null);
   getWindow().takeSurface(null);
   decor = getWindow().getDecorView();
   decor.setBackground(null);
   FrameLayout root = new FrameLayout(this);
   mMainView = new XBMCMainView(this);
   root.addView(mMainView, new FrameLayout.LayoutParams(-1, -1));
   setContentView(root);
   setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
   handler = new Handler(getMainLooper());
   settingsObserver = new XBMCSettingsContentObserver(getApplicationContext(), handler);
   getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsObserver);
   inputListener = new XBMCInputDeviceListener();
   InputManager manager = (InputManager) getSystemService(INPUT_SERVICE);
   manager.registerInputDeviceListener(inputListener, handler);
   applyImmersive();
   diag("07 Main.onCreate completato", null);
  } catch (Throwable e) {
   diag("ERRORE 05-07 inizializzazione Activity/UI", e);
   throw new RuntimeException("Inizializzazione Activity/UI Kodi non riuscita", e);
  }
 }

 @Override protected void onPause() { diag("onPause", null); super.onPause(); }
 @Override protected void onStop() { diag("onStop", null); super.onStop(); }

 private void applyImmersive() {
  if (Build.VERSION.SDK_INT >= 30) {
   getWindow().setDecorFitsSystemWindows(false);
   WindowInsetsController c = getWindow().getInsetsController();
   if (c != null) {
    c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
    c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
   }
  } else if (decor != null) {
   decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }
 }

 public Rect getDisplayRect() {
  Rect r = new Rect();
  if (decor != null) { r.right = decor.getRootView().getWidth(); r.bottom = decor.getRootView().getHeight(); }
  return r;
 }

 private void runNativeOnUiThread(long funcAddr, long variantAddr) {
  runOnUiThread(() -> {
   diag("JNI _callNative: avvio", null);
   _callNative(funcAddr, variantAddr);
  });
 }

 @Override public void onStart() {
  super.onStart();
  diag("08 onStart: avvio ciclo grafico", null);
  Choreographer.getInstance().removeFrameCallback(this);
  Choreographer.getInstance().postFrameCallback(this);
 }

 @Override public void doFrame(long time) {
  if (!shuttingDown) {
   Choreographer.getInstance().postFrameCallback(this);
   if (firstFrame) {
    firstFrame = false;
    diag("09 JNI _doFrame: prima chiamata in avvio", null);
   }
   _doFrame(time);
  }
 }

 @Override protected void onNewIntent(Intent intent) {
  super.onNewIntent(intent);
  setIntent(intent);
  diag("JNI _onNewIntent: avvio", null);
  _onNewIntent(intent);
 }

 @Override protected void onActivityResult(int request, int result, Intent data) {
  super.onActivityResult(request, result, data);
  diag("JNI _onActivityResult: avvio", null);
  _onActivityResult(request, result, data);
 }

 @Override public void onVisibleBehindCanceled() {
  diag("JNI _onVisibleBehindCanceled: avvio", null);
  _onVisibleBehindCanceled();
  super.onVisibleBehindCanceled();
 }

 @Override protected void onResume() {
  super.onResume();
  applyImmersive();
  diag("10 onResume: Kodi in primo piano", null);
 }

 @Override protected void onDestroy() {
  diag("onDestroy: schermata Kodi distrutta", null);
  shuttingDown = true;
  Choreographer.getInstance().removeFrameCallback(this);
  if (inputListener != null) {
   InputManager manager = (InputManager) getSystemService(INPUT_SERVICE);
   manager.unregisterInputDeviceListener(inputListener);
  }
  if (settingsObserver != null) getContentResolver().unregisterContentObserver(settingsObserver);
  if (MainActivity == this) MainActivity = null;
  super.onDestroy();
 }
}
