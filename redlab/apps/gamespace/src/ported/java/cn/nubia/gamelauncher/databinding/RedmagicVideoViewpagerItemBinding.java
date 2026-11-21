package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class RedmagicVideoViewpagerItemBinding implements ViewBinding {
    public final ImageView imageCover;
    public final ImageView ivCenterStart;
    private final FrameLayout rootView;

    private RedmagicVideoViewpagerItemBinding(FrameLayout frameLayout, ImageView imageView, ImageView imageView2) {
        this.rootView = frameLayout;
        this.imageCover = imageView;
        this.ivCenterStart = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static RedmagicVideoViewpagerItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static RedmagicVideoViewpagerItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.redmagic_video_viewpager_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static RedmagicVideoViewpagerItemBinding bind(View view) {
        int i = R.id.image_cover;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.image_cover);
        if (imageView != null) {
            i = R.id.iv_center_start;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_center_start);
            if (imageView2 != null) {
                return new RedmagicVideoViewpagerItemBinding((FrameLayout) view, imageView, imageView2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
