package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToupingTitleBinding implements ViewBinding {
    public final ImageView leftName;
    private final View rootView;
    public final TextView titlebarText;

    private ToupingTitleBinding(View view, ImageView imageView, TextView textView) {
        this.rootView = view;
        this.leftName = imageView;
        this.titlebarText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static ToupingTitleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingTitleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_title, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingTitleBinding bind(View view) {
        int i = R.id.left_name;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.left_name);
        if (imageView != null) {
            i = R.id.titlebar_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.titlebar_text);
            if (textView != null) {
                return new ToupingTitleBinding(view, imageView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
