package dev.redlab.gamespace.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class SelectedButton extends TextView {
    Drawable mCheckedBg;
    boolean mIsChecked;
    Drawable mUnCheckedBg;

    public SelectedButton(Context context) {
        this(context, null);
    }

    public SelectedButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsChecked = false;
        init();
    }

    private void init() {
        this.mCheckedBg = getResources().getDrawable(R.drawable.large_btn_checked, null);
        this.mUnCheckedBg = getResources().getDrawable(R.drawable.large_btn_uncheck, null);
    }

    public void updateState() {
        setBackground(this.mIsChecked ? this.mCheckedBg : this.mUnCheckedBg);
    }

    public void setChecked(boolean z) {
        this.mIsChecked = z;
        updateState();
    }

    @Override // android.view.View
    public void setAlpha(float f) {
        super.setAlpha(f);
        if (0.0f == f) {
            setClickable(false);
        } else {
            setClickable(true);
        }
    }
}
