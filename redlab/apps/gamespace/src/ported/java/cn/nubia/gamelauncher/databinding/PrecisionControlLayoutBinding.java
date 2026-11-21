package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class PrecisionControlLayoutBinding implements ViewBinding {
    public final ImageView precisionControlCheckbox;
    public final RelativeLayout precisionControlLayout;
    public final TextView precisionControlText;
    private final RelativeLayout rootView;

    private PrecisionControlLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, RelativeLayout relativeLayout2, TextView textView) {
        this.rootView = relativeLayout;
        this.precisionControlCheckbox = imageView;
        this.precisionControlLayout = relativeLayout2;
        this.precisionControlText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static PrecisionControlLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PrecisionControlLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.precision_control_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PrecisionControlLayoutBinding bind(View view) {
        int i = R.id.precision_control_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.precision_control_checkbox);
        if (imageView != null) {
            RelativeLayout relativeLayout = (RelativeLayout) view;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.precision_control_text);
            if (textView != null) {
                return new PrecisionControlLayoutBinding(relativeLayout, imageView, relativeLayout, textView);
            }
            i = R.id.precision_control_text;
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
