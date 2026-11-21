package dev.redlab.gamespace.helper;

import dev.redlab.gamespace.util.LogUtil;

/* loaded from: classes.dex */
public class FlavorsHelper {

    private static class FlavorsHelperHolder {
        public static final FlavorsHelper INSTANCE = new FlavorsHelper();

        private FlavorsHelperHolder() {
        }
    }

    public static FlavorsHelper getInstance() {
        return FlavorsHelperHolder.INSTANCE;
    }

    public void showFlavorLog() {
        LogUtil.d("flavor", "showFlavorLog()nubiaInter");
    }
}
