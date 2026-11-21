package com.zte.toolsdk.entity;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public class SdkInfo {

    @SerializedName("executor")
    private String executor;

    @SerializedName("implement")
    private String implement;

    public String getImplement() {
        return this.implement;
    }

    public String getExecutor() {
        return this.executor;
    }
}
