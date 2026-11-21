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
public final class MicroSensitivityLayoutPortBinding implements ViewBinding {
    public final LinearLayout microSensitivityLayout;
    private final LinearLayout rootView;
    public final SeekBar seekBarMicroKeen;

    private MicroSensitivityLayoutPortBinding(LinearLayout linearLayout, LinearLayout linearLayout2, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.microSensitivityLayout = linearLayout2;
        this.seekBarMicroKeen = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static MicroSensitivityLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static MicroSensitivityLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.micro_sensitivity_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static MicroSensitivityLayoutPortBinding bind(View view) {
        LinearLayout linearLayout = (LinearLayout) view;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_micro_keen);
        if (seekBar != null) {
            return new MicroSensitivityLayoutPortBinding(linearLayout, linearLayout, seekBar);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.seek_bar_micro_keen)));
    }
}
