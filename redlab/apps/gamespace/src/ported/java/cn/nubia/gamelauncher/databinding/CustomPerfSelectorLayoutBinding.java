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
public final class CustomPerfSelectorLayoutBinding implements ViewBinding {
    public final ConstraintLayout btnItemSelector;
    public final TextView categoryView;
    public final ImageView ivItemSelector;
    public final ImageView resetBt;
    private final ConstraintLayout rootView;
    public final TextView tvItemSelector;

    private CustomPerfSelectorLayoutBinding(ConstraintLayout constraintLayout, ConstraintLayout constraintLayout2, TextView textView, ImageView imageView, ImageView imageView2, TextView textView2) {
        this.rootView = constraintLayout;
        this.btnItemSelector = constraintLayout2;
        this.categoryView = textView;
        this.ivItemSelector = imageView;
        this.resetBt = imageView2;
        this.tvItemSelector = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfSelectorLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfSelectorLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_selector_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfSelectorLayoutBinding bind(View view) {
        int i = R.id.btn_item_selector;
        ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.btn_item_selector);
        if (constraintLayout != null) {
            i = R.id.category_view;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.category_view);
            if (textView != null) {
                i = R.id.iv_item_selector;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_item_selector);
                if (imageView != null) {
                    i = R.id.reset_bt;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.reset_bt);
                    if (imageView2 != null) {
                        i = R.id.tv_item_selector;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_item_selector);
                        if (textView2 != null) {
                            return new CustomPerfSelectorLayoutBinding((ConstraintLayout) view, constraintLayout, textView, imageView, imageView2, textView2);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
