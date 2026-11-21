package dev.redlab.gamespace.commoninterface;

import dev.redlab.gamespace.bean.CheckStateResponse;

/* loaded from: classes.dex */
public interface ICheckStateRequestListener {
    void responseError(String str);

    void responseInfo(CheckStateResponse checkStateResponse);
}
