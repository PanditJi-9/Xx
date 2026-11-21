package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class LargeTabLayoutBinding implements ViewBinding {
    public final TextView epic;
    private final ConstraintLayout rootView;
    public final TextView station;
    public final TextView steam;
    public final TextView xbox;

    private LargeTabLayoutBinding(ConstraintLayout constraintLayout, TextView textView, TextView textView2, TextView textView3, TextView textView4) {
        this.rootView = constraintLayout;
        this.epic = textView;
        this.station = textView2;
        this.steam = textView3;
        this.xbox = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static LargeTabLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static LargeTabLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.large_tab_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static LargeTabLayoutBinding bind(View view) {
        int i = R.id.epic;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.epic);
        if (textView != null) {
            i = R.id.station;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.station);
            if (textView2 != null) {
                i = R.id.steam;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.steam);
                if (textView3 != null) {
                    i = R.id.xbox;
                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.xbox);
                    if (textView4 != null) {
                        return new LargeTabLayoutBinding((ConstraintLayout) view, textView, textView2, textView3, textView4);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
