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
public final class YAxisSensitiveLayoutPortBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final SeekBar yAxisSeekBar;
    public final ImageView ySensitiveDown;
    public final TextView ySensitiveLevel;
    public final ImageView ySensitiveUp;

    private YAxisSensitiveLayoutPortBinding(LinearLayout linearLayout, SeekBar seekBar, ImageView imageView, TextView textView, ImageView imageView2) {
        this.rootView = linearLayout;
        this.yAxisSeekBar = seekBar;
        this.ySensitiveDown = imageView;
        this.ySensitiveLevel = textView;
        this.ySensitiveUp = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static YAxisSensitiveLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static YAxisSensitiveLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.y_axis_sensitive_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static YAxisSensitiveLayoutPortBinding bind(View view) {
        int i = R.id.y_axis_seek_bar;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.y_axis_seek_bar);
        if (seekBar != null) {
            i = R.id.y_sensitive_down;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.y_sensitive_down);
            if (imageView != null) {
                i = R.id.y_sensitive_level;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.y_sensitive_level);
                if (textView != null) {
                    i = R.id.y_sensitive_up;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.y_sensitive_up);
                    if (imageView2 != null) {
                        return new YAxisSensitiveLayoutPortBinding((LinearLayout) view, seekBar, imageView, textView, imageView2);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
