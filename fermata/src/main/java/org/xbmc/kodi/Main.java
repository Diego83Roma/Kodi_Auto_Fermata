package org.xbmc.kodi;

import android.app.NativeActivity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
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
 private String previousExitDetail = "";

 private void diag(String stage, Throwable error) {
  String detail = "";
  if (error == null) detail = previousExitDetail;
  if (error != null) {
   try {
    java.io.StringWriter sw = new java.io.StringWriter();
    error.printStackTrace(new java.io.PrintWriter(sw));
    detail = sw.toString();
   } catch (Throwable ignored) { detail = error.getClass().getName() + ": " + error.getMessage(); }
  }
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

 private void collectPreviousExitInfo() {
  if (Build.VERSION.SDK_INT < 30) return;
  try {
   ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
   if (am == null) return;
   java.util.List<ApplicationExitInfo> exits = am.getHistoricalProcessExitReasons(getPackageName(), 0, 5);
   if (exits == null || exits.isEmpty()) return;
   ApplicationExitInfo info = exits.get(0);
   StringBuilder report = new StringBuilder();
   report.append("REPORT ANDROID ULTIMA TERMINAZIONE (non è un'eccezione Java)")
    .append("\\nProcesso: ").append(info.getProcessName())
    .append("\\nMotivo Android (codice): ").append(info.getReason())
    .append("\\nDescrizione: ").append(info.getDescription())
    .append("\\nStatus: ").append(info.getStatus())
    .append("\\nTimestamp: ").append(info.getTimestamp())
    .append("\\nImportanza processo: ").append(info.getImportance());
   try (java.io.InputStream trace = info.getTraceInputStream()) {
    if (trace != null) {
     byte[] buffer = new byte[24576];
     int count = trace.read(buffer);
     if (count > 0) report.append("\\nTraccia diagnostica Android:\\n")
      .append(new String(buffer, 0, count, java.nio.charset.StandardCharsets.UTF_8));
    }
   } catch (Throwable traceError) {
    report.append("\\nTraccia non disponibile: ").append(traceError.getClass().getSimpleName());
   }
   previousExitDetail = report.toString();
  } catch (Throwable t) {
   previousExitDetail = "Lettura report terminazione Android non riuscita: " + t;
  }
 }

 @Override public void onCreate(Bundle state) {
  collectPreviousExitInfo();
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
   diag("03 Libreria Kodi verificata: caricamento affidato a NativeActivity Android", null);
  } catch (Throwable e) {
   diag("ERRORE 03 caricamento libreria JNI Kodi", e);
   throw new RuntimeException("Caricamento della libreria Kodi non riuscito", e);
  }
  try {
   diag("04 NativeActivity.super.onCreate: avvio caricamento nativo standard Android (lib_name=kodi)", null);
   super.onCreate(state);
   diag("05 NativeActivity.super.onCreate: completato; libreria e ANativeActivity avviate", null);
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
   diag("ERRORE 04-07 caricamento NativeActivity o inizializzazione UI", e);
   throw new RuntimeException("Inizializzazione Activity/UI Kodi non riuscita", e);
  }
 }

 private String lifecycleState(String callback) {
  return callback + " | isFinishing=" + isFinishing()
   + " | isChangingConfigurations=" + isChangingConfigurations()
   + " | isTaskRoot=" + isTaskRoot()
   + " | taskId=" + getTaskId()
   + " | intentFlags=0x" + Integer.toHexString(getIntent() != null ? getIntent().getFlags() : 0)
   + " | processId=" + android.os.Process.myPid();
 }

 @Override protected void onPause() {
  diag(lifecycleState("LIFECYCLE onPause: prima di super"), null);
  super.onPause();
  diag(lifecycleState("LIFECYCLE onPause: dopo super"), null);
 }

 @Override protected void onStop() {
  diag(lifecycleState("LIFECYCLE onStop: prima di super"), null);
  super.onStop();
  diag(lifecycleState("LIFECYCLE onStop: dopo super"), null);
 }

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
  diag("08A onStart: ingresso prima di NativeActivity.super.onStart", null);
  super.onStart();
  diag("08B onStart: NativeActivity.super.onStart completato", null);
  Choreographer.getInstance().removeFrameCallback(this);
  Choreographer.getInstance().postFrameCallback(this);
 }

 @Override public void doFrame(long time) {
  if (!shuttingDown) {
   Choreographer.getInstance().postFrameCallback(this);
   if (firstFrame) {
    firstFrame = false;
    diag("09A doFrame: ingresso prima della prima chiamata JNI _doFrame", null);
   }
   try {
    _doFrame(time);
    if (firstFrame == false && !getSharedPreferences(DIAG_PREFS, MODE_PRIVATE).getBoolean("frame_ok", false)) {
     getSharedPreferences(DIAG_PREFS, MODE_PRIVATE).edit().putBoolean("frame_ok", true).commit();
     diag("09B JNI _doFrame: prima chiamata completata", null);
    }
   } catch (Throwable e) {
    diag("ERRORE 09 JNI _doFrame", e);
    throw e;
   }
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
  diag("10A onResume: ingresso prima di NativeActivity.super.onResume", null);
  super.onResume();
  diag("10B onResume: NativeActivity.super.onResume completato", null);
  applyImmersive();
  diag("10C onResume: UI immersiva applicata", null);
 }

 @Override protected void onDestroy() {
  diag(lifecycleState("LIFECYCLE onDestroy: ingresso prima di super"), null);
  shuttingDown = true;
  Choreographer.getInstance().removeFrameCallback(this);
  if (inputListener != null) {
   InputManager manager = (InputManager) getSystemService(INPUT_SERVICE);
   manager.unregisterInputDeviceListener(inputListener);
  }
  if (settingsObserver != null) getContentResolver().unregisterContentObserver(settingsObserver);
  if (MainActivity == this) MainActivity = null;
  super.onDestroy();
  diag(lifecycleState("LIFECYCLE onDestroy: super completato"), null);
 }
}
