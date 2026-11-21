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
public final class XGravitationEntryLayoutBinding implements ViewBinding {
    public final ImageView background;
    public final TextView nameTitle;
    private final RelativeLayout rootView;
    public final RelativeLayout xGravitationLayout;

    private XGravitationEntryLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, TextView textView, RelativeLayout relativeLayout2) {
        this.rootView = relativeLayout;
        this.background = imageView;
        this.nameTitle = textView;
        this.xGravitationLayout = relativeLayout2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static XGravitationEntryLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static XGravitationEntryLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.x_gravitation_entry_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static XGravitationEntryLayoutBinding bind(View view) {
        int i = R.id.background;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.background);
        if (imageView != null) {
            i = R.id.name_title;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.name_title);
            if (textView != null) {
                RelativeLayout relativeLayout = (RelativeLayout) view;
                return new XGravitationEntryLayoutBinding(relativeLayout, imageView, textView, relativeLayout);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
