package org.xbmc.kodi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Java bridge expected by Kodi's JNI layer.
 * The package must match CCompileInfo::GetClass() in the embedded Kodi build.
 */
public final class XBMCBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "KodiAddonEngine";

    native void _onReceive(Intent intent);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            _onReceive(intent);
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "XBMCBroadcastReceiver: native callback is not registered", error);
        }
    }
}
