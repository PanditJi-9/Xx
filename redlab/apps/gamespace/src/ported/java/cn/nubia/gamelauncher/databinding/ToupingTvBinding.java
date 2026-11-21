package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToupingTvBinding implements ViewBinding {
    public final LinearLayout mirrorTVBtn;
    private final LinearLayout rootView;
    public final TextView title;
    public final TextView toupingTvSummary;

    private ToupingTvBinding(LinearLayout linearLayout, LinearLayout linearLayout2, TextView textView, TextView textView2) {
        this.rootView = linearLayout;
        this.mirrorTVBtn = linearLayout2;
        this.title = textView;
        this.toupingTvSummary = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ToupingTvBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingTvBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_tv, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingTvBinding bind(View view) {
        int i = R.id.mirrorTVBtn;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.mirrorTVBtn);
        if (linearLayout != null) {
            i = R.id.title;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.title);
            if (textView != null) {
                i = R.id.touping_tv_summary;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.touping_tv_summary);
                if (textView2 != null) {
                    return new ToupingTvBinding((LinearLayout) view, linearLayout, textView, textView2);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
