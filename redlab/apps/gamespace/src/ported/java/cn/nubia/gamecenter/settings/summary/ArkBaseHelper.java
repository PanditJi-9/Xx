package cn.nubia.gamecenter.settings.summary;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import cn.nubia.gamecenter.settings.summary.entities.AccountLabel;
import cn.nubia.gamecenter.settings.summary.entities.GameRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public final class ArkBaseHelper {
    private static Map<String, String> mAccountInfo = new HashMap();
    private static List<GameRecord> mRecords = new ArrayList();
    private static List<AccountLabel> mAccountLabels = new ArrayList();
    private static String mOpenId = "";

    public static void cacheAccountInfo(Map<String, String> map) {
        mAccountInfo = map;
        if (map != null) {
            mOpenId = map.get("open_id");
        }
    }

    public static void cacheAccountLabels(List<AccountLabel> list) {
        mAccountLabels = list;
    }

    public static void cacheRecords(List<GameRecord> list) {
        mRecords = list;
    }

    public static String getCacheOpenId() {
        return mOpenId;
    }

    public static Map<String, String> getCacheAccountInfo() {
        return mAccountInfo;
    }

    public static List<GameRecord> getCacheRecords() {
        return mRecords;
    }

    public static List<AccountLabel> getCacheAccountLabels() {
        return mAccountLabels;
    }

    public static int millisToHour(long j) {
        int i = (int) (j / 60000);
        if (i < 60) {
            return 0;
        }
        return i / 60;
    }

    public static int millisToHourRoundedUp(long j) {
        int i = (int) (j / 60000);
        int i2 = i < 60 ? 0 : i / 60;
        return j % 60000 > 0 ? i2 + 1 : i2;
    }

    public static RoundedBitmapDrawable toRoundDrawable(Bitmap bitmap, Context context) {
        RoundedBitmapDrawable create = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
        create.setAntiAlias(true);
        create.setCornerRadius(bitmap.getWidth() / 2.0f);
        return create;
    }
}
