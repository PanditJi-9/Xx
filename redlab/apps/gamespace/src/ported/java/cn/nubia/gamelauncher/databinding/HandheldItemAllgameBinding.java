package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HandheldItemAllgameBinding implements ViewBinding {
    public final ImageView ivAllgame1;
    public final ImageView ivAllgame2;
    public final ImageView ivAllgame3;
    public final ImageView ivAllgame4;
    private final RelativeLayout rootView;

    private HandheldItemAllgameBinding(RelativeLayout relativeLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, ImageView imageView4) {
        this.rootView = relativeLayout;
        this.ivAllgame1 = imageView;
        this.ivAllgame2 = imageView2;
        this.ivAllgame3 = imageView3;
        this.ivAllgame4 = imageView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static HandheldItemAllgameBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldItemAllgameBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_item_allgame, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldItemAllgameBinding bind(View view) {
        int i = R.id.iv_allgame_1;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_allgame_1);
        if (imageView != null) {
            i = R.id.iv_allgame_2;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_allgame_2);
            if (imageView2 != null) {
                i = R.id.iv_allgame_3;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_allgame_3);
                if (imageView3 != null) {
                    i = R.id.iv_allgame_4;
                    ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_allgame_4);
                    if (imageView4 != null) {
                        return new HandheldItemAllgameBinding((RelativeLayout) view, imageView, imageView2, imageView3, imageView4);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
