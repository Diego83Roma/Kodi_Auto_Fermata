package org.xbmc.kodi.interfaces;

import android.media.AudioManager.OnAudioFocusChangeListener;

/** Java callback required by Kodi's CJNIXBMCAudioManagerOnAudioFocusChangeListener JNI wrapper. */
public final class XBMCAudioManagerOnAudioFocusChangeListener implements OnAudioFocusChangeListener {
    public XBMCAudioManagerOnAudioFocusChangeListener() {
        // Explicit public no-argument constructor required by JNI NewObject("<init>", "()V").
    }

    native void _onAudioFocusChange(int focusChange);

    @Override
    public void onAudioFocusChange(int focusChange) {
        _onAudioFocusChange(focusChange);
    }
}
