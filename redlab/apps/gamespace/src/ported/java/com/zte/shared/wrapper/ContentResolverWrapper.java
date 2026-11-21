package com.zte.shared.wrapper;

import android.content.ContentResolver;
import android.os.Bundle;

/* loaded from: classes2.dex */
public class ContentResolverWrapper {
    public static Bundle call(ContentResolver contentResolver, String str, String str2, String str3, Bundle bundle) {
        return contentResolver.call(str, str2, str3, bundle);
    }
}
