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
public final class InternationalMojiBroadcastLayoutBinding implements ViewBinding {
    public final ImageView background;
    public final RelativeLayout internationalMojiBroadcastLayout;
    public final TextView nameTitle;
    private final RelativeLayout rootView;

    private InternationalMojiBroadcastLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, RelativeLayout relativeLayout2, TextView textView) {
        this.rootView = relativeLayout;
        this.background = imageView;
        this.internationalMojiBroadcastLayout = relativeLayout2;
        this.nameTitle = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static InternationalMojiBroadcastLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static InternationalMojiBroadcastLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.international_moji_broadcast_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static InternationalMojiBroadcastLayoutBinding bind(View view) {
        int i = R.id.background;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.background);
        if (imageView != null) {
            RelativeLayout relativeLayout = (RelativeLayout) view;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.name_title);
            if (textView != null) {
                return new InternationalMojiBroadcastLayoutBinding(relativeLayout, imageView, relativeLayout, textView);
            }
            i = R.id.name_title;
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
