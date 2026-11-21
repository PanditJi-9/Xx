package cn.nubia.plug.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/* loaded from: classes2.dex */
public class PlugMarqueeTextView extends TextView {
    private boolean mIsInitTextPadding;

    @Override // android.view.View
    public boolean isFocused() {
        return true;
    }

    public PlugMarqueeTextView(Context context) {
        this(context, null);
    }

    public PlugMarqueeTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsInitTextPadding = false;
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
    }

    @Override // android.widget.TextView, android.view.View
    protected void onDraw(Canvas canvas) {
        if (!this.mIsInitTextPadding) {
            int baseline = getBaseline();
            int textSize = (int) getPaint().getTextSize();
            if (baseline > textSize) {
                setPadding(0, 0, 0, baseline - textSize);
            }
            this.mIsInitTextPadding = true;
        }
        super.onDraw(canvas);
    }

    @Override // android.widget.TextView, android.view.View
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (z) {
            super.onFocusChanged(z, i, rect);
        }
    }

    @Override // android.widget.TextView, android.view.View
    public void onWindowFocusChanged(boolean z) {
        if (z) {
            super.onWindowFocusChanged(z);
        }
    }
}
