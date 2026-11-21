package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class KeenLayoutPortBinding implements ViewBinding {
    public final LinearLayout keenLayout;
    private final LinearLayout rootView;
    public final SeekBar seekBarKeen;

    private KeenLayoutPortBinding(LinearLayout linearLayout, LinearLayout linearLayout2, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.keenLayout = linearLayout2;
        this.seekBarKeen = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static KeenLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static KeenLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.keen_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static KeenLayoutPortBinding bind(View view) {
        LinearLayout linearLayout = (LinearLayout) view;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_keen);
        if (seekBar != null) {
            return new KeenLayoutPortBinding(linearLayout, linearLayout, seekBar);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.seek_bar_keen)));
    }
}
