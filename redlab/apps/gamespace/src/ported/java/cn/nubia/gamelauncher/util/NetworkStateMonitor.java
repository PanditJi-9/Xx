package dev.redlab.gamespace.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

/* loaded from: classes2.dex */
public class NetworkStateMonitor {
    private static final String TAG = "NetworkMonitor";
    private ConnectivityManager connectivityManager;
    private boolean isNetworkAvailable;
    private OnNetworkStateChangeListener listener;
    private ConnectivityManager.NetworkCallback networkCallback;

    public interface OnNetworkStateChangeListener {
        void onNetworkAvailable(boolean z);
    }

    public NetworkStateMonitor(Context context, final OnNetworkStateChangeListener onNetworkStateChangeListener) {
        this.isNetworkAvailable = false;
        this.connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.listener = onNetworkStateChangeListener;
        this.isNetworkAvailable = isNetworkAvailable(context);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(12);
        builder.addCapability(16);
        this.networkCallback = new ConnectivityManager.NetworkCallback() { // from class: dev.redlab.gamespace.util.NetworkStateMonitor.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(NetworkStateMonitor.TAG, "网络已连接");
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(NetworkStateMonitor.TAG, "网络已断开");
                if (onNetworkStateChangeListener == null || !NetworkStateMonitor.this.isNetworkAvailable) {
                    return;
                }
                NetworkStateMonitor.this.isNetworkAvailable = false;
                onNetworkStateChangeListener.onNetworkAvailable(false);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                if (!networkCapabilities.hasCapability(16) || onNetworkStateChangeListener == null) {
                    return;
                }
                Log.d(NetworkStateMonitor.TAG, "网络已验证可用");
                if (NetworkStateMonitor.this.isNetworkAvailable) {
                    return;
                }
                NetworkStateMonitor.this.isNetworkAvailable = true;
                onNetworkStateChangeListener.onNetworkAvailable(true);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onUnavailable() {
                super.onUnavailable();
                Log.d(NetworkStateMonitor.TAG, "网络不可用");
                if (onNetworkStateChangeListener == null || !NetworkStateMonitor.this.isNetworkAvailable) {
                    return;
                }
                NetworkStateMonitor.this.isNetworkAvailable = false;
                onNetworkStateChangeListener.onNetworkAvailable(false);
            }
        };
        this.connectivityManager.registerNetworkCallback(builder.build(), this.networkCallback);
    }

    public void stopMonitoring() {
        ConnectivityManager connectivityManager;
        ConnectivityManager.NetworkCallback networkCallback = this.networkCallback;
        if (networkCallback == null || (connectivityManager = this.connectivityManager) == null) {
            return;
        }
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager;
        Network activeNetwork;
        NetworkCapabilities networkCapabilities;
        return (context == null || (connectivityManager = (ConnectivityManager) context.getSystemService("connectivity")) == null || (activeNetwork = connectivityManager.getActiveNetwork()) == null || (networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)) == null || !networkCapabilities.hasCapability(12) || !networkCapabilities.hasCapability(16)) ? false : true;
    }
}
