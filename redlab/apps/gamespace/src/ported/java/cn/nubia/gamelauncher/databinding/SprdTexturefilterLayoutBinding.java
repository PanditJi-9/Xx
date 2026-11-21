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
public final class SprdTexturefilterLayoutBinding implements ViewBinding {
    public final ImageView adrGpuBgT;
    private final LinearLayout rootView;
    public final SeekBar seekBarAdrenoT;

    private SprdTexturefilterLayoutBinding(LinearLayout linearLayout, ImageView imageView, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.adrGpuBgT = imageView;
        this.seekBarAdrenoT = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static SprdTexturefilterLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SprdTexturefilterLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.sprd_texturefilter_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SprdTexturefilterLayoutBinding bind(View view) {
        int i = R.id.adr_gpu_bg_t;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.adr_gpu_bg_t);
        if (imageView != null) {
            i = R.id.seek_bar_adreno_t;
            SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_adreno_t);
            if (seekBar != null) {
                return new SprdTexturefilterLayoutBinding((LinearLayout) view, imageView, seekBar);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
