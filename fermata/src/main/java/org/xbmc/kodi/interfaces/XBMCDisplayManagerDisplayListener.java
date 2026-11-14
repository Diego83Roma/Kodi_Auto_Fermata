package org.xbmc.kodi.interfaces;

import android.hardware.display.DisplayManager;

/** Java callback required by Kodi's CJNIXBMCDisplayManagerDisplayListener JNI wrapper. */
public final class XBMCDisplayManagerDisplayListener implements DisplayManager.DisplayListener {
    public XBMCDisplayManagerDisplayListener() {
        // Public no-argument constructor required by Kodi JNI.
    }

    native void _onDisplayAdded(int displayId);
    native void _onDisplayChanged(int displayId);
    native void _onDisplayRemoved(int displayId);

    @Override
    public void onDisplayAdded(int displayId) {
        _onDisplayAdded(displayId);
    }

    @Override
    public void onDisplayChanged(int displayId) {
        _onDisplayChanged(displayId);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        _onDisplayRemoved(displayId);
    }
}
