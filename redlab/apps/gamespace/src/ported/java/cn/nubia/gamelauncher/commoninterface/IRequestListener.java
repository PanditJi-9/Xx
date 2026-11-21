package dev.redlab.gamespace.commoninterface;

import dev.redlab.gamespace.bean.ResponseBean;

/* loaded from: classes.dex */
public interface IRequestListener {
    void responseError(String str);

    void responseInfo(ResponseBean responseBean);
}
