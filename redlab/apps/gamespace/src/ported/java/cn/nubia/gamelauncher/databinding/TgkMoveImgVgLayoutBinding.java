package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MovableImageViewGroup;

/* loaded from: classes.dex */
public final class TgkMoveImgVgLayoutBinding implements ViewBinding {
    private final MovableImageViewGroup rootView;
    public final Button tgkPreviewCloseBtn;
    public final ImageView tgkPreviewImg;

    private TgkMoveImgVgLayoutBinding(MovableImageViewGroup movableImageViewGroup, Button button, ImageView imageView) {
        this.rootView = movableImageViewGroup;
        this.tgkPreviewCloseBtn = button;
        this.tgkPreviewImg = imageView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MovableImageViewGroup getRoot() {
        return this.rootView;
    }

    public static TgkMoveImgVgLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMoveImgVgLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_move_img_vg_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMoveImgVgLayoutBinding bind(View view) {
        int i = R.id.tgk_preview_close_btn;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.tgk_preview_close_btn);
        if (button != null) {
            i = R.id.tgk_preview_img;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.tgk_preview_img);
            if (imageView != null) {
                return new TgkMoveImgVgLayoutBinding((MovableImageViewGroup) view, button, imageView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
