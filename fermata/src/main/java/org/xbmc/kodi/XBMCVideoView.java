package org.xbmc.kodi;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

/** Video surface bridge with the method contract expected by Kodi JNI. */
public final class XBMCVideoView extends SurfaceView implements SurfaceHolder.Callback {
    native void _surfaceChanged(SurfaceHolder holder, int format, int width, int height);
    native void _surfaceCreated(SurfaceHolder holder);
    native void _surfaceDestroyed(SurfaceHolder holder);
    public boolean mIsCreated = false;

    public static XBMCVideoView createVideoView() {
        return new XBMCVideoView(Main.MainActivity);
    }

    public XBMCVideoView(Context context) {
        super(context);
        setZOrderMediaOverlay(true);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    public void add() {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() {
                android.view.View content = Main.MainActivity.findViewById(android.R.id.content);
                if (content instanceof FrameLayout && getParent() == null)
                    ((FrameLayout) content).addView(XBMCVideoView.this,
                        new FrameLayout.LayoutParams(-1, -1));
            }
        });
    }

    public void release() {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() {
                if (getParent() instanceof FrameLayout)
                    ((FrameLayout) getParent()).removeView(XBMCVideoView.this);
            }
        });
    }

    public Surface getSurface() {
        return mIsCreated ? getHolder().getSurface() : null;
    }

    public void setSurfaceRect(final int left, final int top, final int right, final int bottom) {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    Math.max(1, right-left), Math.max(1, bottom-top));
                lp.leftMargin = left;
                lp.topMargin = top;
                setLayoutParams(lp);
                requestLayout();
            }
        });
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        mIsCreated = true;
        _surfaceCreated(holder);
    }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) {
        if (holder == getHolder()) _surfaceChanged(holder,format,width,height);
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        mIsCreated = false;
        _surfaceDestroyed(holder);
    }
}
