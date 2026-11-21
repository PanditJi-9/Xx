package dev.redlab.gamespace.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

/* loaded from: classes2.dex */
public class ZoomButtonX extends ZoomButton {
    @Override // android.view.View
    public boolean isFocused() {
        return true;
    }

    public ZoomButtonX(Context context) {
        this(context, null);
    }

    public ZoomButtonX(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setFocusable(true);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
    }
}
