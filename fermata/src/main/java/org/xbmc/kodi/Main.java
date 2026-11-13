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
/** Kodi upstream JNI host: package, class and native signatures match xbmc Android. */
public final class Main extends NativeActivity implements Choreographer.FrameCallback {
 public static Main MainActivity; public XBMCMainView mMainView; private View decor; private boolean shuttingDown; private Handler handler; private XBMCSettingsContentObserver settingsObserver; private XBMCInputDeviceListener inputListener;
 private static final String DIAG_PREFS = "kodi_engine_state";
 private void diag(String stage, Throwable error){ try { getSharedPreferences(DIAG_PREFS, MODE_PRIVATE).edit().putString("last_stage", stage).putString("last_error", error == null ? "" : (error.getClass().getName()+": "+error.getMessage())).putLong("last_stage_time", System.currentTimeMillis()).apply(); } catch(Throwable ignored){} android.util.Log.i("KodiAddonEngine", "ACTIVITY: "+stage, error); }
 private native void _onNewIntent(Intent intent); private native void _onActivityResult(int requestCode,int resultCode,Intent resultData);
 private native void _doFrame(long frameTimeNanos); private native void _onVisibleBehindCanceled(); private native void _callNative(long funcAddr,long variantAddr);
 public Main(){super();MainActivity=this;}
 @Override public void onCreate(Bundle state){
  diag("Main.onCreate: iniziato", null);
  java.io.File home=new java.io.File(getFilesDir(),"apk");
  java.io.File userdata=new java.io.File(getFilesDir(),"addons/engine/userdata");
  java.io.File temp=new java.io.File(getCacheDir(),"kodi");
  home.mkdirs();userdata.mkdirs();temp.mkdirs();
  System.setProperty("xbmc.home",home.getAbsolutePath());
  System.setProperty("xbmc.data",userdata.getAbsolutePath());
  System.setProperty("xbmc.temp",temp.getAbsolutePath());
  System.setProperty("xbmc.proploaded","yes");
  try { diag("Caricamento JNI Kodi in corso", null); System.loadLibrary("kodi"); diag("Libreria JNI Kodi caricata", null); } catch(Throwable e) { diag("ERRORE caricamento JNI Kodi", e); throw e; }
  super.onCreate(state); diag("NativeActivity super.onCreate completato", null); getWindow().takeSurface(null);decor=getWindow().getDecorView();decor.setBackground(null);FrameLayout root=new FrameLayout(this);mMainView=new XBMCMainView(this);root.addView(mMainView,new FrameLayout.LayoutParams(-1,-1));setContentView(root);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  handler=new Handler(getMainLooper());
  settingsObserver=new XBMCSettingsContentObserver(getApplicationContext(),handler);
  getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI,true,settingsObserver);
  inputListener=new XBMCInputDeviceListener();
  InputManager manager=(InputManager)getSystemService(INPUT_SERVICE);
  manager.registerInputDeviceListener(inputListener,handler);
  applyImmersive(); diag("Main.onCreate completato", null);}
 @Override protected void onStart(){super.onStart();diag("Main.onStart", null);}
 @Override protected void onPause(){diag("Main.onPause", null);super.onPause();}
 @Override protected void onStop(){diag("Main.onStop", null);super.onStop();}
 private void applyImmersive(){if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else{decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}}
 public Rect getDisplayRect(){Rect r=new Rect();if(decor!=null){r.right=decor.getRootView().getWidth();r.bottom=decor.getRootView().getHeight();}return r;}
 private void runNativeOnUiThread(long funcAddr,long variantAddr){runOnUiThread(()->_callNative(funcAddr,variantAddr));}
 @Override public void onStart(){super.onStart();Choreographer.getInstance().removeFrameCallback(this);Choreographer.getInstance().postFrameCallback(this);}
 @Override public void doFrame(long time){if(!shuttingDown){Choreographer.getInstance().postFrameCallback(this);_doFrame(time);}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);_onNewIntent(intent);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);_onActivityResult(request,result,data);}
 @Override public void onVisibleBehindCanceled(){_onVisibleBehindCanceled();super.onVisibleBehindCanceled();}
 @Override protected void onResume(){super.onResume();applyImmersive();diag("Main.onResume: Kodi in primo piano", null);}
 @Override protected void onDestroy(){diag("Main.onDestroy: schermata Kodi distrutta", null);shuttingDown=true;Choreographer.getInstance().removeFrameCallback(this);if(inputListener!=null){InputManager manager=(InputManager)getSystemService(INPUT_SERVICE);manager.unregisterInputDeviceListener(inputListener);}
  if(settingsObserver!=null)getContentResolver().unregisterContentObserver(settingsObserver);
  if(MainActivity==this)MainActivity=null;super.onDestroy();}
}