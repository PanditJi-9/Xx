package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToupingGravitationTitleBinding implements ViewBinding {
    public final ImageView backIcon;
    public final TextView projectHelp;
    private final RelativeLayout rootView;
    public final TextView titlebarText;
    public final LinearLayout toupingHelp;

    private ToupingGravitationTitleBinding(RelativeLayout relativeLayout, ImageView imageView, TextView textView, TextView textView2, LinearLayout linearLayout) {
        this.rootView = relativeLayout;
        this.backIcon = imageView;
        this.projectHelp = textView;
        this.titlebarText = textView2;
        this.toupingHelp = linearLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static ToupingGravitationTitleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingGravitationTitleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_gravitation_title, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingGravitationTitleBinding bind(View view) {
        int i = R.id.back_icon;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.back_icon);
        if (imageView != null) {
            i = R.id.project_help;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.project_help);
            if (textView != null) {
                i = R.id.titlebar_text;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.titlebar_text);
                if (textView2 != null) {
                    i = R.id.touping_help;
                    LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.touping_help);
                    if (linearLayout != null) {
                        return new ToupingGravitationTitleBinding((RelativeLayout) view, imageView, textView, textView2, linearLayout);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
