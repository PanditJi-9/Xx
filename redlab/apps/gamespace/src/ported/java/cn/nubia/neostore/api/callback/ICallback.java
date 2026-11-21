package cn.nubia.neostore.api.callback;

import cn.nubia.neostore.api.model.ErrorMsg;

/* loaded from: classes2.dex */
public interface ICallback<T> {
    void onError(ErrorMsg errorMsg);

    void onSuccess(T t);
}
