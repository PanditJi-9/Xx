package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import cn.nubia.common.view.BatteryView;
import cn.nubia.common.view.NubiaFontTextClockView;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HostModeLowViewBinding implements ViewBinding {
    public final BatteryView levelRemain;
    private final ConstraintLayout rootView;
    public final NubiaFontTextClockView timeDisplay;

    private HostModeLowViewBinding(ConstraintLayout constraintLayout, BatteryView batteryView, NubiaFontTextClockView nubiaFontTextClockView) {
        this.rootView = constraintLayout;
        this.levelRemain = batteryView;
        this.timeDisplay = nubiaFontTextClockView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static HostModeLowViewBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HostModeLowViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.host_mode_low_view, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HostModeLowViewBinding bind(View view) {
        int i = R.id.level_remain;
        BatteryView batteryView = (BatteryView) ViewBindings.findChildViewById(view, R.id.level_remain);
        if (batteryView != null) {
            i = R.id.time_display;
            NubiaFontTextClockView nubiaFontTextClockView = (NubiaFontTextClockView) ViewBindings.findChildViewById(view, R.id.time_display);
            if (nubiaFontTextClockView != null) {
                return new HostModeLowViewBinding((ConstraintLayout) view, batteryView, nubiaFontTextClockView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
