package cn.nubia.common.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextClock;

/* loaded from: classes.dex */
public class NubiaFontTextClockView extends TextClock {
    public NubiaFontTextClockView(Context context) {
        super(context);
        setNubiaTypeface(context);
    }

    public NubiaFontTextClockView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setNubiaTypeface(context);
    }

    private void setNubiaTypeface(Context context) {
        setTypeface(Typeface.createFromAsset(context.getAssets(), "font/You_She.ttf"));
    }
}
