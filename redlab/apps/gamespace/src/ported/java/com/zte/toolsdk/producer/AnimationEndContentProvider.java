package com.zte.toolsdk.producer;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.zte.toolsdk.common.CardProviderContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes2.dex */
public class AnimationEndContentProvider extends ContentProvider {
    public static final String KEY_ANIMATION_ENDED = "animation_ended";
    public static final String METHOD_NOTIFY_ANIMATION_END = "notify_animation_end";
    public static final int RESULT_ERROR = -1;
    public static final int RESULT_INVALID_PARAMS = -3;
    public static final int RESULT_SUCCESS = 0;
    private static final String TAG = "AnimationEndProvider";
    private static volatile AnimationEndContentProvider sInstance;
    private final List<OnAnimationEndListener> mAnimationEndListeners = new ArrayList();
    private Handler mMainHandler;

    public interface OnAnimationEndListener {
        void onAnimationEnd(boolean animationEnded);
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public static AnimationEndContentProvider getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        return ensureCreated(context);
    }

    public static boolean isCreated() {
        return sInstance != null;
    }

    private static AnimationEndContentProvider ensureCreated(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        if (context == null) {
            Log.e(TAG, "ensureCreated: context is null");
            return null;
        }
        try {
            ContentProviderClient acquireContentProviderClient = context.getContentResolver().acquireContentProviderClient(Uri.parse("content://" + (context.getPackageName() + ".animationendprovider")));
            if (acquireContentProviderClient != null) {
                acquireContentProviderClient.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "ensureCreated error: " + e.getMessage());
        }
        return sInstance;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        Log.d(TAG, "AnimationEndContentProvider onCreate");
        sInstance = this;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        return true;
    }

    @Override // android.content.ContentProvider
    public Bundle call(String method, String arg, Bundle extras) {
        Log.d(TAG, "call method: " + method + ", arg: " + arg);
        Bundle bundle = new Bundle();
        try {
        } catch (Exception e) {
            Log.e(TAG, "call error: " + e.getMessage(), e);
            bundle.putInt("result_code", -1);
            bundle.putString(CardProviderContract.KEY_RESULT_MSG, "Exception: " + e.getMessage());
        }
        if (method.hashCode() == 2137581354 && method.equals(METHOD_NOTIFY_ANIMATION_END)) {
            handleAnimationEnd(extras, bundle);
            return bundle;
        }
        bundle.putInt("result_code", -1);
        bundle.putString(CardProviderContract.KEY_RESULT_MSG, "Unknown method: " + method);
        return bundle;
    }

    private void handleAnimationEnd(Bundle extras, Bundle result) {
        boolean z = extras != null ? extras.getBoolean(KEY_ANIMATION_ENDED, true) : true;
        Log.d(TAG, "Animation end received: ended=" + z);
        notifyAnimationEndListeners(z);
        result.putInt("result_code", 0);
        result.putString(CardProviderContract.KEY_RESULT_MSG, "Animation end notification received");
    }

    private void notifyAnimationEndListeners(final boolean animationEnded) {
        Handler handler = this.mMainHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.zte.toolsdk.producer.AnimationEndContentProvider.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (AnimationEndContentProvider.this.mAnimationEndListeners) {
                        Iterator it = AnimationEndContentProvider.this.mAnimationEndListeners.iterator();
                        while (it.hasNext()) {
                            try {
                                ((OnAnimationEndListener) it.next()).onAnimationEnd(animationEnded);
                            } catch (Exception e) {
                                Log.e(AnimationEndContentProvider.TAG, "Error notifying listener: " + e.getMessage());
                            }
                        }
                    }
                }
            });
        }
    }

    public void addAnimationEndListener(OnAnimationEndListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mAnimationEndListeners) {
            if (!this.mAnimationEndListeners.contains(listener)) {
                this.mAnimationEndListeners.add(listener);
            }
        }
    }

    public void removeAnimationEndListener(OnAnimationEndListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mAnimationEndListeners) {
            this.mAnimationEndListeners.remove(listener);
        }
    }

    public void clearAnimationEndListeners() {
        synchronized (this.mAnimationEndListeners) {
            this.mAnimationEndListeners.clear();
        }
    }
}
