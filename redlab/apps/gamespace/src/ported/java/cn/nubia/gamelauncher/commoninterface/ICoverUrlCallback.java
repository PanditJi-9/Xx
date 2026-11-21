package dev.redlab.gamespace.commoninterface;

import dev.redlab.gamespace.bean.AtmosphereBean;
import java.util.ArrayList;

/* loaded from: classes.dex */
public interface ICoverUrlCallback {
    void responseError(String str);

    void responseInfo(ArrayList<AtmosphereBean> arrayList);
}
