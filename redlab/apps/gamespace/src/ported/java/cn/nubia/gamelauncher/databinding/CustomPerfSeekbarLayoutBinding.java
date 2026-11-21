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
import dev.redlab.gamespace.gamecontrolpanel.performancetuning.ui.CustomPerfSeekBar;

/* loaded from: classes.dex */
public final class CustomPerfSeekbarLayoutBinding implements ViewBinding {
    public final TextView categoryView;
    public final ImageView resetBt;
    private final ConstraintLayout rootView;
    public final CustomPerfSeekBar seekBar;

    private CustomPerfSeekbarLayoutBinding(ConstraintLayout constraintLayout, TextView textView, ImageView imageView, CustomPerfSeekBar customPerfSeekBar) {
        this.rootView = constraintLayout;
        this.categoryView = textView;
        this.resetBt = imageView;
        this.seekBar = customPerfSeekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfSeekbarLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfSeekbarLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_seekbar_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfSeekbarLayoutBinding bind(View view) {
        int i = R.id.category_view;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.category_view);
        if (textView != null) {
            i = R.id.reset_bt;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.reset_bt);
            if (imageView != null) {
                i = R.id.seek_bar;
                CustomPerfSeekBar customPerfSeekBar = (CustomPerfSeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar);
                if (customPerfSeekBar != null) {
                    return new CustomPerfSeekbarLayoutBinding((ConstraintLayout) view, textView, imageView, customPerfSeekBar);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
