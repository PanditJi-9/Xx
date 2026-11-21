package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class XAxisSensitiveLayoutPortBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final SeekBar xAxisSeekBar;
    public final ImageView xSensitiveDown;
    public final TextView xSensitiveLevel;
    public final ImageView xSensitiveUp;

    private XAxisSensitiveLayoutPortBinding(LinearLayout linearLayout, SeekBar seekBar, ImageView imageView, TextView textView, ImageView imageView2) {
        this.rootView = linearLayout;
        this.xAxisSeekBar = seekBar;
        this.xSensitiveDown = imageView;
        this.xSensitiveLevel = textView;
        this.xSensitiveUp = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static XAxisSensitiveLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static XAxisSensitiveLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.x_axis_sensitive_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static XAxisSensitiveLayoutPortBinding bind(View view) {
        int i = R.id.x_axis_seek_bar;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.x_axis_seek_bar);
        if (seekBar != null) {
            i = R.id.x_sensitive_down;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.x_sensitive_down);
            if (imageView != null) {
                i = R.id.x_sensitive_level;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.x_sensitive_level);
                if (textView != null) {
                    i = R.id.x_sensitive_up;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_sensitive_up);
                    if (imageView2 != null) {
                        return new XAxisSensitiveLayoutPortBinding((LinearLayout) view, seekBar, imageView, textView, imageView2);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
