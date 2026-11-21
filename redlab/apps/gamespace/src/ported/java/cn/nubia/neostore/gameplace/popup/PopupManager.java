package cn.nubia.neostore.gameplace.popup;

import android.content.Context;
import android.view.View;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/* loaded from: classes2.dex */
public class PopupManager {
    private Context context;
    private boolean isShowing = false;

    public PopupManager(Context context) {
        this.context = context;
    }

    public void requestPopup(Function1<Boolean, Unit> function1) {
        if (function1 != null) {
            function1.invoke(false);
        }
    }

    public boolean isPopupShowing() {
        return this.isShowing;
    }

    public void showPopup(View view, PopupConfig popupConfig, PopupCallbacks popupCallbacks) {
        if (view == null || this.isShowing) {
            return;
        }
        this.isShowing = true;
    }

    public void dismissPopup() {
        if (this.isShowing) {
            this.isShowing = false;
        }
    }
}
