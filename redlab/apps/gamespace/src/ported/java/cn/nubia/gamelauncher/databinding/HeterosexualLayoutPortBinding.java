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
public final class HeterosexualLayoutPortBinding implements ViewBinding {
    public final ImageView adrGpuBgH;
    private final LinearLayout rootView;
    public final SeekBar seekBarAdrenoH;

    private HeterosexualLayoutPortBinding(LinearLayout linearLayout, ImageView imageView, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.adrGpuBgH = imageView;
        this.seekBarAdrenoH = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static HeterosexualLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HeterosexualLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.heterosexual_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HeterosexualLayoutPortBinding bind(View view) {
        int i = R.id.adr_gpu_bg_h;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.adr_gpu_bg_h);
        if (imageView != null) {
            i = R.id.seek_bar_adreno_h;
            SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_adreno_h);
            if (seekBar != null) {
                return new HeterosexualLayoutPortBinding((LinearLayout) view, imageView, seekBar);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
