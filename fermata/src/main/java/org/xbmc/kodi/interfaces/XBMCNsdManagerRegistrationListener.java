package org.xbmc.kodi.interfaces;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
public final class XBMCNsdManagerRegistrationListener implements NsdManager.RegistrationListener {
    native void _onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode);
    native void _onServiceRegistered(NsdServiceInfo serviceInfo);
    native void _onServiceUnregistered(NsdServiceInfo serviceInfo);
    native void _onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode);
    public XBMCNsdManagerRegistrationListener() {}
    @Override public void onRegistrationFailed(NsdServiceInfo i,int e){_onRegistrationFailed(i,e);}
    @Override public void onServiceRegistered(NsdServiceInfo i){_onServiceRegistered(i);}
    @Override public void onServiceUnregistered(NsdServiceInfo i){_onServiceUnregistered(i);}
    @Override public void onUnregistrationFailed(NsdServiceInfo i,int e){_onUnregistrationFailed(i,e);}
}
