package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class MipmapLodLayoutBinding implements ViewBinding {
    public final ImageView mipMapLod;
    public final LinearLayout mipmapLodLayout;
    public final SeekBar mipmapLodSeekBar;
    private final LinearLayout rootView;

    private MipmapLodLayoutBinding(LinearLayout linearLayout, ImageView imageView, LinearLayout linearLayout2, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.mipMapLod = imageView;
        this.mipmapLodLayout = linearLayout2;
        this.mipmapLodSeekBar = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static MipmapLodLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static MipmapLodLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.mipmap_lod_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static MipmapLodLayoutBinding bind(View view) {
        int i = R.id.mipMap_lod;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.mipMap_lod);
        if (imageView != null) {
            LinearLayout linearLayout = (LinearLayout) view;
            SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.mipmap_lod_seekBar);
            if (seekBar != null) {
                return new MipmapLodLayoutBinding(linearLayout, imageView, linearLayout, seekBar);
            }
            i = R.id.mipmap_lod_seekBar;
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
