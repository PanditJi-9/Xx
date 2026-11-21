package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HandheldStreamImageBinding implements ViewBinding {
    private final ImageView rootView;
    public final ImageView streamBanner;

    private HandheldStreamImageBinding(ImageView imageView, ImageView imageView2) {
        this.rootView = imageView;
        this.streamBanner = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ImageView getRoot() {
        return this.rootView;
    }

    public static HandheldStreamImageBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldStreamImageBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_stream_image, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldStreamImageBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        ImageView imageView = (ImageView) view;
        return new HandheldStreamImageBinding(imageView, imageView);
    }
}
