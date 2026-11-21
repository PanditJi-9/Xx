package cn.nubia.neostore.api;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import cn.nubia.neostore.api.callback.ICallback;
import cn.nubia.neostore.api.model.CallbackInfo;
import cn.nubia.neostore.api.model.NubiaGameCardInfo;
import cn.nubia.neostore.api.model.NubiaGameNotice;
import java.util.List;

/* loaded from: classes2.dex */
public class NubiaNgcApi {
    public static void checkUnionNewData(Context context, ICallback<Boolean> iCallback) {
    }

    public static void getCardList(Context context, ICallback<List<NubiaGameCardInfo>> iCallback) {
    }

    public static void getDialogNotice(Context context, ICallback<NubiaGameNotice> iCallback) {
    }

    public static void getGiftListForPackage(String str, String str2, int i, ICallback<CallbackInfo> iCallback) {
    }

    public static void getRevelantAppByPackageName(String str, int i, ICallback<CallbackInfo> iCallback) {
    }

    public static void init(Application application) {
    }

    public static void isVipGame(String str, ICallback<CallbackInfo> iCallback) {
    }

    public static void startDialogNotice(Activity activity) {
    }

    public static void startGameCardSofts(Activity activity, int i, String str) {
    }

    public static void startRedMagicUnionActivity(Activity activity, int i) {
    }
}
