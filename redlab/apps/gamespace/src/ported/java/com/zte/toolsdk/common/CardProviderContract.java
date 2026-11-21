package com.zte.toolsdk.common;

import android.net.Uri;

/* loaded from: classes2.dex */
public class CardProviderContract {
    public static final String AUTHORITY_SUFFIX = ".cardprovider";
    public static final String KEY_CALLBACK_MESSENGER = "callback_messenger";
    public static final String KEY_CARD_ID = "card_id";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_RESULT_CODE = "result_code";
    public static final String KEY_RESULT_MSG = "result_msg";
    public static final String METHOD_PUSH_CARD = "push_card";
    public static final String METHOD_QUERY_CARD = "query_card";
    public static final String METHOD_REGISTER_CALLBACK = "register_callback";
    public static final String METHOD_REMOVE_CARD = "remove_card";
    public static final String METHOD_UNREGISTER_CALLBACK = "unregister_callback";
    public static final String METHOD_UPDATE_CARD = "update_card";
    public static final int RESULT_CARD_NOT_FOUND = -2;
    public static final int RESULT_ERROR = -1;
    public static final int RESULT_INVALID_PARAMS = -3;
    public static final int RESULT_SUCCESS = 0;

    public static String getAuthority(String packageName) {
        return packageName + AUTHORITY_SUFFIX;
    }

    public static Uri getProviderUri(String packageName) {
        return Uri.parse("content://" + getAuthority(packageName));
    }

    public static String parsePackageFromAuthority(String authority) {
        if (authority == null || !authority.endsWith(AUTHORITY_SUFFIX)) {
            return null;
        }
        return authority.substring(0, authority.length() - 13);
    }
}
