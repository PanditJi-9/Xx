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
public final class ToupingComputerBinding implements ViewBinding {
    public final LinearLayout mirrorPCBtn;
    private final LinearLayout rootView;
    public final TextView title;
    public final TextView toupingPcSummary;

    private ToupingComputerBinding(LinearLayout linearLayout, LinearLayout linearLayout2, TextView textView, TextView textView2) {
        this.rootView = linearLayout;
        this.mirrorPCBtn = linearLayout2;
        this.title = textView;
        this.toupingPcSummary = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ToupingComputerBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingComputerBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_computer, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingComputerBinding bind(View view) {
        int i = R.id.mirrorPCBtn;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.mirrorPCBtn);
        if (linearLayout != null) {
            i = R.id.title;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.title);
            if (textView != null) {
                i = R.id.touping_pc_summary;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.touping_pc_summary);
                if (textView2 != null) {
                    return new ToupingComputerBinding((LinearLayout) view, linearLayout, textView, textView2);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
