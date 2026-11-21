package cn.nubia.common.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.util.AttributeSet;
import android.widget.TextView;
import cn.nubia.common.R;

/* loaded from: classes.dex */
public class BatteryView extends TextView {
    private boolean isHostMode;
    private BatteryReceiver mBatteryReceiver;

    public BatteryView(Context context) {
        this(context, null);
    }

    public BatteryView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BatteryView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.isHostMode = false;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.BatteryView, i, 0);
        this.isHostMode = obtainStyledAttributes.getBoolean(R.styleable.BatteryView_isHostMode, false);
        obtainStyledAttributes.recycle();
    }

    @Override // android.widget.TextView
    public void setTextSize(float f) {
        super.setTextSize(f);
    }

    private class BatteryReceiver extends BroadcastReceiver {
        private BatteryReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Trace.beginSection("onReceive");
            int i = (intent.getExtras().getInt("level") * 100) / intent.getExtras().getInt("scale");
            BatteryView.this.setText(i + "%");
            if (!BatteryView.this.isHostMode) {
                BatteryView.this.batteryIconChange(i);
            }
            Trace.endSection();
        }
    }

    public void batteryIconChange(int i) {
        if (80 < i) {
            Drawable drawable = getResources().getDrawable(R.drawable.battery100);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            setCompoundDrawables(drawable, null, null, null);
            return;
        }
        if (60 < i && i <= 80) {
            Drawable drawable2 = getResources().getDrawable(R.drawable.battery80);
            drawable2.setBounds(0, 0, drawable2.getMinimumWidth(), drawable2.getMinimumHeight());
            setCompoundDrawables(drawable2, null, null, null);
            return;
        }
        if (40 < i && i <= 60) {
            Drawable drawable3 = getResources().getDrawable(R.drawable.battery60);
            drawable3.setBounds(0, 0, drawable3.getMinimumWidth(), drawable3.getMinimumHeight());
            setCompoundDrawables(drawable3, null, null, null);
            return;
        }
        if (20 < i && i <= 40) {
            Drawable drawable4 = getResources().getDrawable(R.drawable.battery40);
            drawable4.setBounds(0, 0, drawable4.getMinimumWidth(), drawable4.getMinimumHeight());
            setCompoundDrawables(drawable4, null, null, null);
        } else if (5 < i && i <= 20) {
            Drawable drawable5 = getResources().getDrawable(R.drawable.battery20);
            drawable5.setBounds(0, 0, drawable5.getMinimumWidth(), drawable5.getMinimumHeight());
            setCompoundDrawables(drawable5, null, null, null);
        } else if (i <= 5) {
            Drawable drawable6 = getResources().getDrawable(R.drawable.battery5);
            drawable6.setBounds(0, 0, drawable6.getMinimumWidth(), drawable6.getMinimumHeight());
            setCompoundDrawables(drawable6, null, null, null);
        }
    }

    public void registerObserver() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        this.mBatteryReceiver = new BatteryReceiver();
        if (getContext() != null) {
            getContext().registerReceiver(this.mBatteryReceiver, intentFilter, 2);
        }
    }

    public void unregisterObserver() {
        if (this.mBatteryReceiver == null || getContext() == null) {
            return;
        }
        getContext().unregisterReceiver(this.mBatteryReceiver);
        this.mBatteryReceiver = null;
    }

    @Override // android.widget.TextView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerObserver();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterObserver();
    }
}
