package org.xbmc.kodi.interfaces;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
public final class XBMCNsdManagerResolveListener implements NsdManager.ResolveListener {
    native void _onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);
    native void _onServiceResolved(NsdServiceInfo serviceInfo);
    public XBMCNsdManagerResolveListener() {}
    @Override public void onResolveFailed(NsdServiceInfo i,int e){_onResolveFailed(i,e);}
    @Override public void onServiceResolved(NsdServiceInfo i){_onServiceResolved(i);}
}
