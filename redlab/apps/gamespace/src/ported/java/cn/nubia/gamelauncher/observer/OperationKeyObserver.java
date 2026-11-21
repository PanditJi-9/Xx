package dev.redlab.gamespace.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public class OperationKeyObserver extends ContentObserver {
    public static final String OPERATION_KEY = "db_recommended_content_game";
    private static Context mContext;
    CopyOnWriteArrayList<Callback> mCallbacks;

    public interface Callback {
        void onOperationKeyChanged(boolean z);
    }

    public OperationKeyObserver(Handler handler) {
        super(handler);
        this.mCallbacks = new CopyOnWriteArrayList<>();
    }

    private static class OperationKeyObserverHolder {
        public static final OperationKeyObserver INSTANCE = new OperationKeyObserver(new Handler());

        private OperationKeyObserverHolder() {
        }
    }

    public static OperationKeyObserver getInstance(Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
        }
        return OperationKeyObserverHolder.INSTANCE;
    }

    public void register() {
        Log.d("Full", "register()");
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(OPERATION_KEY), false, this);
    }

    public void unregister() {
        Log.d("Full", "unregister()");
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        boolean isOperationKeyClose = isOperationKeyClose();
        Log.d("Full", "onChange() selfChange = " + z + ", isOperationKeyClose = " + isOperationKeyClose);
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onOperationKeyChanged(isOperationKeyClose);
        }
    }

    public boolean isOperationKeyClose() {
        boolean z = (Settings.Global.getInt(mContext.getContentResolver(), OPERATION_KEY, 1) & 1) != 1;
        Log.d("Full", "isOperationKeyClose() isClose = " + z);
        return z;
    }

    public void addCallback(Callback callback) {
        if (!this.mCallbacks.contains(callback)) {
            this.mCallbacks.add(callback);
        }
        if (this.mCallbacks.size() == 1) {
            register();
        }
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
        if (this.mCallbacks.isEmpty()) {
            unregister();
        }
    }
}
