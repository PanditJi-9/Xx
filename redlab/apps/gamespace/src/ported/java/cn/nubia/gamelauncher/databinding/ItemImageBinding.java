package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ItemImageBinding implements ViewBinding {
    public final ImageView imageView;
    private final ImageView rootView;

    private ItemImageBinding(ImageView imageView, ImageView imageView2) {
        this.rootView = imageView;
        this.imageView = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ImageView getRoot() {
        return this.rootView;
    }

    public static ItemImageBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemImageBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.item_image, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ItemImageBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        ImageView imageView = (ImageView) view;
        return new ItemImageBinding(imageView, imageView);
    }
}
