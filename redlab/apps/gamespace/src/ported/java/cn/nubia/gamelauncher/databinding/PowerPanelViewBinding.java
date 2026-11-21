package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.gamepanel.PowerPanelView;

/* loaded from: classes.dex */
public final class PowerPanelViewBinding implements ViewBinding {
    public final TextView cpsPercentage;
    public final LinearLayout llCps;
    public final LinearLayout llData;
    public final LinearLayout llMpm;
    public final RelativeLayout llPowerPanel;
    public final TextView morePointsView;
    public final TextView onePointView;
    public final PowerPanelView powerPanelView;
    private final RelativeLayout rootView;

    private PowerPanelViewBinding(RelativeLayout relativeLayout, TextView textView, LinearLayout linearLayout, LinearLayout linearLayout2, LinearLayout linearLayout3, RelativeLayout relativeLayout2, TextView textView2, TextView textView3, PowerPanelView powerPanelView) {
        this.rootView = relativeLayout;
        this.cpsPercentage = textView;
        this.llCps = linearLayout;
        this.llData = linearLayout2;
        this.llMpm = linearLayout3;
        this.llPowerPanel = relativeLayout2;
        this.morePointsView = textView2;
        this.onePointView = textView3;
        this.powerPanelView = powerPanelView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static PowerPanelViewBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PowerPanelViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.power_panel_view, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PowerPanelViewBinding bind(View view) {
        int i = R.id.cps_percentage;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.cps_percentage);
        if (textView != null) {
            i = R.id.ll_cps;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.ll_cps);
            if (linearLayout != null) {
                i = R.id.ll_data;
                LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.ll_data);
                if (linearLayout2 != null) {
                    i = R.id.ll_mpm;
                    LinearLayout linearLayout3 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.ll_mpm);
                    if (linearLayout3 != null) {
                        RelativeLayout relativeLayout = (RelativeLayout) view;
                        i = R.id.more_points_view;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.more_points_view);
                        if (textView2 != null) {
                            i = R.id.one_point_view;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.one_point_view);
                            if (textView3 != null) {
                                i = R.id.power_panel_view;
                                PowerPanelView powerPanelView = (PowerPanelView) ViewBindings.findChildViewById(view, R.id.power_panel_view);
                                if (powerPanelView != null) {
                                    return new PowerPanelViewBinding(relativeLayout, textView, linearLayout, linearLayout2, linearLayout3, relativeLayout, textView2, textView3, powerPanelView);
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
