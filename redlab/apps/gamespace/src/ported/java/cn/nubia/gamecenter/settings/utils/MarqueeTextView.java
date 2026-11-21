package cn.nubia.gamecenter.settings.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/* loaded from: classes.dex */
public class MarqueeTextView extends TextView {
    private static final int MARQUEE_REPEAT_COUNT = 2;
    private H mH;
    private boolean marquee;

    @Override // android.view.View
    public boolean isFocused() {
        return true;
    }

    public MarqueeTextView(Context context) {
        super(context);
        this.mH = new H();
        setMarqueeAttr();
    }

    public MarqueeTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mH = new H();
        setMarqueeAttr();
    }

    public MarqueeTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mH = new H();
        setMarqueeAttr();
    }

    public void setMarquee(boolean z) {
        this.marquee = z;
    }

    public void setMarqueeAttr() {
        H h = this.mH;
        if (h != null) {
            h.postDelayed(new Runnable() { // from class: cn.nubia.gamecenter.settings.utils.MarqueeTextView.1
                @Override // java.lang.Runnable
                public void run() {
                    MarqueeTextView.this.onWindowFocusChanged(true);
                }
            }, 500L);
        }
        if (!this.marquee) {
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
            setMarqueeRepeatLimit(2);
            setSingleLine(true);
            return;
        }
        setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }

    private class H extends Handler {
        private static final int STATE_CHANGED = 1;

        @Override // android.os.Handler
        public void handleMessage(Message message) {
        }

        public H() {
            super(Looper.getMainLooper());
        }
    }
}
