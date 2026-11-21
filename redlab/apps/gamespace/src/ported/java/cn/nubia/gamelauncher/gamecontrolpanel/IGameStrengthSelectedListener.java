package dev.redlab.gamespace.gamecontrolpanel;

import dev.redlab.gamespace.gamecontrolpanel.TouchOperationBean;

/* loaded from: classes.dex */
public interface IGameStrengthSelectedListener {
    void onAdjustOperationDataChanged(TouchOperationBean.OperationTypeParams operationTypeParams);

    void onGameStrengthIndicatorSelected(int i);

    void onGameStrengthSelected(int i, int i2, int[] iArr);
}
