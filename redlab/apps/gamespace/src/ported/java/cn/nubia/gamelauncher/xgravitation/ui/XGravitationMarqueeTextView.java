package dev.redlab.gamespace.xgravitation.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/* loaded from: classes2.dex */
public class XGravitationMarqueeTextView extends TextView {
    public XGravitationMarqueeTextView(Context context) {
        super(context);
        setMarquee();
    }

    public XGravitationMarqueeTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setMarquee();
    }

    public XGravitationMarqueeTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setMarquee();
    }

    public XGravitationMarqueeTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setMarquee();
    }

    private void setMarquee() {
        setSelected(true);
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
    }
}
