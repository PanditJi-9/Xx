package cn.nubia.common;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public class GameKeyObserver extends ContentObserver {
    public static final String GAME_KEY_STATE = "gcs_need_kill_game_launcher";
    private static final String TAG = "GameKeyObserver";
    private static Context mContext;
    CopyOnWriteArrayList<Callback> mCallbacks;

    public interface Callback {
        void onGameKeyChanged(boolean z);
    }

    private GameKeyObserver(Handler handler) {
        super(handler);
        this.mCallbacks = new CopyOnWriteArrayList<>();
        register();
    }

    private static class GameKeyObserverHolder {
        public static final GameKeyObserver INSTANCE = new GameKeyObserver(new Handler());

        private GameKeyObserverHolder() {
        }
    }

    public static GameKeyObserver getInstance(Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
        }
        return GameKeyObserverHolder.INSTANCE;
    }

    public void register() {
        Log.d(TAG, " register ");
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(GAME_KEY_STATE), false, this);
    }

    public void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        Log.d(TAG, "onChange selfChange: " + z);
        int i = Settings.Global.getInt(mContext.getContentResolver(), GAME_KEY_STATE, -1);
        boolean z2 = (i & 1) == 1;
        Log.i(TAG, "isNeedExit() exit = " + z2 + ", value = " + i);
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onGameKeyChanged(z2);
        }
    }

    public boolean isNeedExit() {
        int i = Settings.Global.getInt(mContext.getContentResolver(), GAME_KEY_STATE, -1);
        boolean z = (i & 1) == 1;
        Log.d(TAG, "isNeedExit() exit = " + z + ", value = " + i);
        return z;
    }

    public void addCallback(Callback callback) {
        if (this.mCallbacks.contains(callback)) {
            return;
        }
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }
}
