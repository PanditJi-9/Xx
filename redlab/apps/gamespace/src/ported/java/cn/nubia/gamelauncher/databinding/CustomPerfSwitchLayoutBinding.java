package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class CustomPerfSwitchLayoutBinding implements ViewBinding {
    public final TextView categoryView;
    public final ImageView resetBt;
    private final ConstraintLayout rootView;
    public final View segmentLine;
    public final ImageView switchBt;

    private CustomPerfSwitchLayoutBinding(ConstraintLayout constraintLayout, TextView textView, ImageView imageView, View view, ImageView imageView2) {
        this.rootView = constraintLayout;
        this.categoryView = textView;
        this.resetBt = imageView;
        this.segmentLine = view;
        this.switchBt = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfSwitchLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfSwitchLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_switch_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfSwitchLayoutBinding bind(View view) {
        int i = R.id.category_view;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.category_view);
        if (textView != null) {
            i = R.id.reset_bt;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.reset_bt);
            if (imageView != null) {
                i = R.id.segment_line;
                View findChildViewById = ViewBindings.findChildViewById(view, R.id.segment_line);
                if (findChildViewById != null) {
                    i = R.id.switch_bt;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_bt);
                    if (imageView2 != null) {
                        return new CustomPerfSwitchLayoutBinding((ConstraintLayout) view, textView, imageView, findChildViewById, imageView2);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
