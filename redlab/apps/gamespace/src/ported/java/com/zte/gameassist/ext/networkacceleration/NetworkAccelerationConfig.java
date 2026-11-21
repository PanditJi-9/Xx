package com.zte.gameassist.ext.networkacceleration;

import android.text.TextUtils;

/* loaded from: classes2.dex */
public final class NetworkAccelerationConfig {
    public static final String USER_MANUAL_OFF = "N";
    public static final String USER_MANUAL_ON = "Y";
    public static final String VENDOR_LSACC = "lsacc";
    public static final String VENDOR_SUBAO = "subao";
    public final String mPackageName;
    public boolean mUserManualEnabled = false;
    public String mAcceleratorVendor = VENDOR_LSACC;

    public NetworkAccelerationConfig(String str) {
        this.mPackageName = str;
    }

    public static NetworkAccelerationConfig withDefaults(String str) {
        return new NetworkAccelerationConfig(str);
    }

    public NetworkAccelerationConfig normalizedCopy() {
        NetworkAccelerationConfig networkAccelerationConfig = new NetworkAccelerationConfig(this.mPackageName);
        networkAccelerationConfig.mUserManualEnabled = this.mUserManualEnabled;
        networkAccelerationConfig.mAcceleratorVendor = normalizeVendor(this.mAcceleratorVendor);
        return networkAccelerationConfig;
    }

    public static String normalizeVendor(String str) {
        return (!TextUtils.isEmpty(str) && VENDOR_SUBAO.equals(str.trim().toLowerCase())) ? VENDOR_SUBAO : VENDOR_LSACC;
    }
}
