package org.xbmc.kodi;
public final class XBMCURIUtils {
    native String _substitutePath(String path);
    public XBMCURIUtils() {}
    public String substitutePath(String path) {
        try { return _substitutePath(path); }
        catch (RuntimeException e) { android.util.Log.e("Kodi","substitutePath failed",e); return null; }
    }
}
