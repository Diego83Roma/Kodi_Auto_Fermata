package org.xbmc.kodi;
import android.hardware.input.InputManager.InputDeviceListener;
/** Kodi JNI input-device callbacks, matching upstream class and native names. */
public final class XBMCInputDeviceListener implements InputDeviceListener {
 native void _onInputDeviceAdded(int deviceId);
 native void _onInputDeviceChanged(int deviceId);
 native void _onInputDeviceRemoved(int deviceId);
 @Override public void onInputDeviceAdded(int id){_onInputDeviceAdded(id);}
 @Override public void onInputDeviceChanged(int id){_onInputDeviceChanged(id);}
 @Override public void onInputDeviceRemoved(int id){_onInputDeviceRemoved(id);}
}