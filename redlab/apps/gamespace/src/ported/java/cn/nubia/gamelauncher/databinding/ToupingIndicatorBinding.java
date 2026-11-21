package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToupingIndicatorBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final ImageView toupingIndicator1;
    public final ImageView toupingIndicator2;

    private ToupingIndicatorBinding(LinearLayout linearLayout, ImageView imageView, ImageView imageView2) {
        this.rootView = linearLayout;
        this.toupingIndicator1 = imageView;
        this.toupingIndicator2 = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ToupingIndicatorBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingIndicatorBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_indicator, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingIndicatorBinding bind(View view) {
        int i = R.id.touping_indicator_1;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.touping_indicator_1);
        if (imageView != null) {
            i = R.id.touping_indicator_2;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.touping_indicator_2);
            if (imageView2 != null) {
                return new ToupingIndicatorBinding((LinearLayout) view, imageView, imageView2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
