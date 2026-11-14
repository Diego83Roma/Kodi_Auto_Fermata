package org.xbmc.kodi;

import android.util.Log;

/** Java bridge registered by Kodi's CJNIXBMCFile. */
public final class XBMCFile {
    private static final String TAG = "Kodi";
    native boolean _open(String path);
    native void _close();
    native byte[] _read();
    native boolean _eof();

    public XBMCFile() {}

    public boolean Open(String path) {
        try { return _open(path); }
        catch (Exception e) { Log.e(TAG, "XBMCFile.Open", e); return false; }
        catch (UnsatisfiedLinkError e) { Log.e(TAG, "XBMCFile.Open native unavailable", e); return false; }
    }
    public void Close() {
        try { _close(); }
        catch (Exception e) { Log.e(TAG, "XBMCFile.Close", e); }
        catch (UnsatisfiedLinkError e) { Log.e(TAG, "XBMCFile.Close native unavailable", e); }
    }
    public byte[] Read() {
        try { byte[] data = _read(); return data == null ? new byte[0] : data; }
        catch (Exception e) { Log.e(TAG, "XBMCFile.Read", e); return new byte[0]; }
        catch (UnsatisfiedLinkError e) { Log.e(TAG, "XBMCFile.Read native unavailable", e); return new byte[0]; }
    }
    public boolean Eof() {
        try { return _eof(); }
        catch (Exception e) { Log.e(TAG, "XBMCFile.Eof", e); return false; }
        catch (UnsatisfiedLinkError e) { Log.e(TAG, "XBMCFile.Eof native unavailable", e); return false; }
    }
}
