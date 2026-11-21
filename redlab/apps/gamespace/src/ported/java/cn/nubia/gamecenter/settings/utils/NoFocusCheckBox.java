package cn.nubia.gamecenter.settings.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

/* loaded from: classes.dex */
public class NoFocusCheckBox extends CheckBox {
    @Override // android.widget.CompoundButton, android.widget.Checkable
    public void setChecked(boolean z) {
    }

    public NoFocusCheckBox(Context context) {
        super(context);
    }

    public NoFocusCheckBox(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public NoFocusCheckBox(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public NoFocusCheckBox(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void setCustomChecked(boolean z) {
        super.setChecked(z);
    }
}
