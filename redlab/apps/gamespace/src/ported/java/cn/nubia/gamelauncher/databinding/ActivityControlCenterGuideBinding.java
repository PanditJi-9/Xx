package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ActivityControlCenterGuideBinding implements ViewBinding {
    public final ConstraintLayout controlCenterGuideFragment;
    private final ConstraintLayout rootView;

    private ActivityControlCenterGuideBinding(ConstraintLayout constraintLayout, ConstraintLayout constraintLayout2) {
        this.rootView = constraintLayout;
        this.controlCenterGuideFragment = constraintLayout2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static ActivityControlCenterGuideBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityControlCenterGuideBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.activity_control_center_guide, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ActivityControlCenterGuideBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        ConstraintLayout constraintLayout = (ConstraintLayout) view;
        return new ActivityControlCenterGuideBinding(constraintLayout, constraintLayout);
    }
}
