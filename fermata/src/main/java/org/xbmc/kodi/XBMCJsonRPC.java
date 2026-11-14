package org.xbmc.kodi;

/**
 * Minimal Java endpoint for Kodi's native JSON-RPC bridge.
 * The full Kodi app adds search-provider UI around this native method; the
 * embedded engine only needs the exact class and JNI signature.
 */
public final class XBMCJsonRPC {
    native String _requestJSON(String request);
    public XBMCJsonRPC() {}
    public String requestJSON(String request) {
        return _requestJSON(request);
    }
}
