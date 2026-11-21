package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import cn.nubia.common.view.ProgressView;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HandheldItemImageBinding implements ViewBinding {
    public final ImageView cardMask;
    public final CardView cardView;
    public final ProgressView iconProgress;
    public final ImageView ivGameCover;
    public final ImageView ivGameIcon;
    public final ImageView ivGameLogo;
    private final CardView rootView;

    private HandheldItemImageBinding(CardView cardView, ImageView imageView, CardView cardView2, ProgressView progressView, ImageView imageView2, ImageView imageView3, ImageView imageView4) {
        this.rootView = cardView;
        this.cardMask = imageView;
        this.cardView = cardView2;
        this.iconProgress = progressView;
        this.ivGameCover = imageView2;
        this.ivGameIcon = imageView3;
        this.ivGameLogo = imageView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public CardView getRoot() {
        return this.rootView;
    }

    public static HandheldItemImageBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldItemImageBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_item_image, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldItemImageBinding bind(View view) {
        int i = R.id.card_mask;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.card_mask);
        if (imageView != null) {
            CardView cardView = (CardView) view;
            i = R.id.icon_progress;
            ProgressView progressView = (ProgressView) ViewBindings.findChildViewById(view, R.id.icon_progress);
            if (progressView != null) {
                i = R.id.iv_game_cover;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_game_cover);
                if (imageView2 != null) {
                    i = R.id.iv_game_icon;
                    ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_game_icon);
                    if (imageView3 != null) {
                        i = R.id.iv_game_logo;
                        ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_game_logo);
                        if (imageView4 != null) {
                            return new HandheldItemImageBinding(cardView, imageView, cardView, progressView, imageView2, imageView3, imageView4);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
