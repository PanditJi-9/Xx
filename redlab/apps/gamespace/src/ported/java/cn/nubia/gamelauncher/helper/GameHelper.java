package dev.redlab.gamespace.helper;

import android.util.Log;

/* loaded from: classes.dex */
public class GameHelper {

    private static class GameHelperHolder {
        public static final GameHelper INSTANCE = new GameHelper();

        private GameHelperHolder() {
        }
    }

    public static GameHelper getInstance() {
        return GameHelperHolder.INSTANCE;
    }

    public void test() {
        Log.d("Test", "test(nubia)");
    }
}
