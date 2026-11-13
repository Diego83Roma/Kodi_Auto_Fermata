package org.xbmc.kodi;
import android.content.Context; import android.graphics.PixelFormat; import android.view.SurfaceHolder; import android.view.SurfaceView;
/** Surface callbacks registered by Kodi's native Android platform layer. */
public final class XBMCMainView extends SurfaceView implements SurfaceHolder.Callback {
 public boolean mIsCreated;
 private native void _attach(); private native void _surfaceChanged(SurfaceHolder holder,int format,int width,int height);
 private native void _surfaceCreated(SurfaceHolder holder); private native void _surfaceDestroyed(SurfaceHolder holder);
 public XBMCMainView(Context context){super(context);setZOrderOnTop(true);getHolder().addCallback(this);getHolder().setFormat(PixelFormat.TRANSPARENT);}
 @Override public void surfaceCreated(SurfaceHolder holder){mIsCreated=true;_attach();_surfaceCreated(holder);}
 @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){if(holder==getHolder())_surfaceChanged(holder,format,width,height);}
 @Override public void surfaceDestroyed(SurfaceHolder holder){mIsCreated=false;_surfaceDestroyed(holder);}
}