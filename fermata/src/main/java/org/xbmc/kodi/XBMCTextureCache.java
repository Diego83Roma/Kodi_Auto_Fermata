package org.xbmc.kodi;
public final class XBMCTextureCache {
    native String _unwrapImageURL(String image);
    public XBMCTextureCache() {}
    public String unwrapImageURL(String image) {
        try { return _unwrapImageURL(image); }
        catch (RuntimeException e) { android.util.Log.e("Kodi","unwrapImageURL failed",e); return null; }
    }
}
