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
public final class RecorderSoundLayoutBinding implements ViewBinding {
    public final ImageView img1;
    public final ImageView img2;
    public final ImageView img3;
    public final ImageView img4;
    private final RelativeLayout rootView;

    private RecorderSoundLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, ImageView imageView4) {
        this.rootView = relativeLayout;
        this.img1 = imageView;
        this.img2 = imageView2;
        this.img3 = imageView3;
        this.img4 = imageView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static RecorderSoundLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static RecorderSoundLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.recorder_sound_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static RecorderSoundLayoutBinding bind(View view) {
        int i = R.id.img_1;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.img_1);
        if (imageView != null) {
            i = R.id.img_2;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.img_2);
            if (imageView2 != null) {
                i = R.id.img_3;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.img_3);
                if (imageView3 != null) {
                    i = R.id.img_4;
                    ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.img_4);
                    if (imageView4 != null) {
                        return new RecorderSoundLayoutBinding((RelativeLayout) view, imageView, imageView2, imageView3, imageView4);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
