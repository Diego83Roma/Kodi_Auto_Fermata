package me.aap.fermata.auto.kodiautomodern;

import android.hardware.input.InputManager.InputDeviceListener;

/** Kodi JNI input-device listener: the native class loader resolves classes under the installed application ID. */
public final class XBMCInputDeviceListener implements InputDeviceListener {
    native void _onInputDeviceAdded(int deviceId);
    native void _onInputDeviceChanged(int deviceId);
    native void _onInputDeviceRemoved(int deviceId);

    @Override public void onInputDeviceAdded(int deviceId) {
        _onInputDeviceAdded(deviceId);
    }

    @Override public void onInputDeviceChanged(int deviceId) {
        _onInputDeviceChanged(deviceId);
    }

    @Override public void onInputDeviceRemoved(int deviceId) {
        _onInputDeviceRemoved(deviceId);
    }
}
