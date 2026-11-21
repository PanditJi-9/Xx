package cn.nubia.volley.toolbox;

import cn.nubia.volley.AuthFailureError;

/* loaded from: classes2.dex */
public interface Authenticator {
    String getAuthToken() throws AuthFailureError;

    void invalidateAuthToken(String str);
}
