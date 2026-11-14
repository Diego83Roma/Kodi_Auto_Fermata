package org.xbmc.kodi.interfaces;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
public final class XBMCNsdManagerDiscoveryListener implements NsdManager.DiscoveryListener {
    native void _onDiscoveryStarted(String serviceType);
    native void _onDiscoveryStopped(String serviceType);
    native void _onServiceFound(NsdServiceInfo serviceInfo);
    native void _onServiceLost(NsdServiceInfo serviceInfo);
    native void _onStartDiscoveryFailed(String serviceType, int errorCode);
    native void _onStopDiscoveryFailed(String serviceType, int errorCode);
    public XBMCNsdManagerDiscoveryListener() {}
    @Override public void onDiscoveryStarted(String s) { _onDiscoveryStarted(s); }
    @Override public void onDiscoveryStopped(String s) { _onDiscoveryStopped(s); }
    @Override public void onServiceFound(NsdServiceInfo i) { _onServiceFound(i); }
    @Override public void onServiceLost(NsdServiceInfo i) { _onServiceLost(i); }
    @Override public void onStartDiscoveryFailed(String s, int e) { _onStartDiscoveryFailed(s,e); }
    @Override public void onStopDiscoveryFailed(String s, int e) { _onStopDiscoveryFailed(s,e); }
}
