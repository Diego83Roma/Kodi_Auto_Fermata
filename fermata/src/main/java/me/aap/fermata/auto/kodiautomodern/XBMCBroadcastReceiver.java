package me.aap.fermata.auto.kodiautomodern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Kodi's native Android layer resolves this receiver from the runtime
 * application package name, not from the org.xbmc.kodi Activity package.
 */
public final class XBMCBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "KodiAddonEngine";

    private native void _onReceive(Intent intent);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            _onReceive(intent);
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "Kodi native broadcast callback is not registered yet", error);
        }
    }
}
