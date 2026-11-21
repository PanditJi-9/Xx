package cn.nubia.common.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/* loaded from: classes.dex */
public class NubiaFontTextView extends TextView {
    public NubiaFontTextView(Context context) {
        super(context);
        setNubiaTypeface(context);
    }

    public NubiaFontTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setNubiaTypeface(context);
    }

    private void setNubiaTypeface(Context context) {
        setTypeface(Typeface.createFromAsset(context.getAssets(), "font/You_She.ttf"));
    }
}
