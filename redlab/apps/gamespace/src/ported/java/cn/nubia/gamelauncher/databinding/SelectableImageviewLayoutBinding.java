package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class SelectableImageviewLayoutBinding implements ViewBinding {
    public final ImageView ivBg;
    public final ImageView ivSelect;
    private final FrameLayout rootView;
    public final TextView tvText;

    private SelectableImageviewLayoutBinding(FrameLayout frameLayout, ImageView imageView, ImageView imageView2, TextView textView) {
        this.rootView = frameLayout;
        this.ivBg = imageView;
        this.ivSelect = imageView2;
        this.tvText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static SelectableImageviewLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SelectableImageviewLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.selectable_imageview_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SelectableImageviewLayoutBinding bind(View view) {
        int i = R.id.iv_bg;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_bg);
        if (imageView != null) {
            i = R.id.iv_select;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_select);
            if (imageView2 != null) {
                i = R.id.tv_text;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_text);
                if (textView != null) {
                    return new SelectableImageviewLayoutBinding((FrameLayout) view, imageView, imageView2, textView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
