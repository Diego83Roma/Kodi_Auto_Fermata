package org.xbmc.kodi.interfaces;

import android.media.AudioManager.OnAudioFocusChangeListener;

/** Java callback required by Kodi's CJNIXBMCAudioManagerOnAudioFocusChangeListener JNI wrapper. */
public final class XBMCAudioManagerOnAudioFocusChangeListener implements OnAudioFocusChangeListener {
    native void _onAudioFocusChange(int focusChange);

    @Override
    public void onAudioFocusChange(int focusChange) {
        _onAudioFocusChange(focusChange);
    }
}
