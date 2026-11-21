package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class QsCustomizeTileFrameBinding implements ViewBinding {
    public final ImageView icon;
    public final TextView label;
    private final FrameLayout rootView;

    private QsCustomizeTileFrameBinding(FrameLayout frameLayout, ImageView imageView, TextView textView) {
        this.rootView = frameLayout;
        this.icon = imageView;
        this.label = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static QsCustomizeTileFrameBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static QsCustomizeTileFrameBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.qs_customize_tile_frame, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static QsCustomizeTileFrameBinding bind(View view) {
        int i = R.id.icon;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.icon);
        if (imageView != null) {
            i = R.id.label;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.label);
            if (textView != null) {
                return new QsCustomizeTileFrameBinding((FrameLayout) view, imageView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
