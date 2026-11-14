package org.xbmc.kodi.interfaces;
import android.graphics.SurfaceTexture;
public final class XBMCSurfaceTextureOnFrameAvailableListener implements SurfaceTexture.OnFrameAvailableListener {
    native void _onFrameAvailable(SurfaceTexture surface);
    public XBMCSurfaceTextureOnFrameAvailableListener() {}
    @Override public void onFrameAvailable(SurfaceTexture surface){_onFrameAvailable(surface);}
}
