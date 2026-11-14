package me.aap.fermata.auto.kodiautomodern;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/** Kodi JNI volume observer in the package expected by Kodi's native class loader. */
public final class XBMCSettingsContentObserver extends ContentObserver {
    native void _onVolumeChanged(int newVolume);
    private int previousVolume;
    private final Context context;

    public XBMCSettingsContentObserver(Context context, Handler handler) {
        super(handler);
        this.context = context;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange);
        if (uri == null) return;
        if (uri.compareTo(Uri.parse("content://settings/system/volume_music_speaker")) == 0 ||
            uri.compareTo(Uri.parse("content://settings/system/volume_music_hdmi")) == 0) {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (current != previousVolume) {
                try {
                    _onVolumeChanged(current);
                    previousVolume = current;
                } catch (UnsatisfiedLinkError error) {
                    Log.e("KodiAutoFermata", "Native volume callback not registered", error);
                }
            }
        }
    }
}
