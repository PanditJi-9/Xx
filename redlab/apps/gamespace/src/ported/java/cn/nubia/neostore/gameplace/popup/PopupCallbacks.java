package cn.nubia.neostore.gameplace.popup;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/* loaded from: classes2.dex */
public class PopupCallbacks {
    private Function0<Unit> onButtonClick;
    private Function0<Unit> onCloseButtonClick;
    private Function0<Unit> onContentClick;
    private Function0<Unit> onDismiss;
    private Function0<Unit> onTitleClick;

    public PopupCallbacks(Function0<Unit> function0, Function0<Unit> function02, Function0<Unit> function03, Function0<Unit> function04, Function0<Unit> function05) {
        this.onTitleClick = function0;
        this.onContentClick = function02;
        this.onButtonClick = function03;
        this.onDismiss = function04;
        this.onCloseButtonClick = function05;
    }

    public Function0<Unit> getOnTitleClick() {
        return this.onTitleClick;
    }

    public void setOnTitleClick(Function0<Unit> function0) {
        this.onTitleClick = function0;
    }

    public Function0<Unit> getOnContentClick() {
        return this.onContentClick;
    }

    public void setOnContentClick(Function0<Unit> function0) {
        this.onContentClick = function0;
    }

    public Function0<Unit> getOnButtonClick() {
        return this.onButtonClick;
    }

    public void setOnButtonClick(Function0<Unit> function0) {
        this.onButtonClick = function0;
    }

    public Function0<Unit> getOnDismiss() {
        return this.onDismiss;
    }

    public void setOnDismiss(Function0<Unit> function0) {
        this.onDismiss = function0;
    }

    public Function0<Unit> getOnCloseButtonClick() {
        return this.onCloseButtonClick;
    }

    public void setOnCloseButtonClick(Function0<Unit> function0) {
        this.onCloseButtonClick = function0;
    }
}
