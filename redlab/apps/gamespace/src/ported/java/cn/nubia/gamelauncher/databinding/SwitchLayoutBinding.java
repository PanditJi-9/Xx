package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class SwitchLayoutBinding implements ViewBinding {
    public final ImageView autoVrsSwitch;
    public final RelativeLayout autoVrsSwitchLayout;
    private final LinearLayout rootView;

    private SwitchLayoutBinding(LinearLayout linearLayout, ImageView imageView, RelativeLayout relativeLayout) {
        this.rootView = linearLayout;
        this.autoVrsSwitch = imageView;
        this.autoVrsSwitchLayout = relativeLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static SwitchLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SwitchLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.switch_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SwitchLayoutBinding bind(View view) {
        int i = R.id.auto_vrs_switch;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.auto_vrs_switch);
        if (imageView != null) {
            i = R.id.auto_vrs_switch_layout;
            RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.auto_vrs_switch_layout);
            if (relativeLayout != null) {
                return new SwitchLayoutBinding((LinearLayout) view, imageView, relativeLayout);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
