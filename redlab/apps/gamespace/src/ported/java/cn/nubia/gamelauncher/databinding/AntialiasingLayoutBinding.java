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
public final class AntialiasingLayoutBinding implements ViewBinding {
    public final ImageView adrGpuBgA;
    private final LinearLayout rootView;
    public final SeekBar seekBarAdrenoA;

    private AntialiasingLayoutBinding(LinearLayout linearLayout, ImageView imageView, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.adrGpuBgA = imageView;
        this.seekBarAdrenoA = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static AntialiasingLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AntialiasingLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.antialiasing_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AntialiasingLayoutBinding bind(View view) {
        int i = R.id.adr_gpu_bg_a;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.adr_gpu_bg_a);
        if (imageView != null) {
            i = R.id.seek_bar_adreno_a;
            SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_adreno_a);
            if (seekBar != null) {
                return new AntialiasingLayoutBinding((LinearLayout) view, imageView, seekBar);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
