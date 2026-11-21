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
public final class ShowContentLayoutBinding implements ViewBinding {
    public final TextView contentView;
    public final TextView forwardButton;
    private final ConstraintLayout rootView;

    private ShowContentLayoutBinding(ConstraintLayout constraintLayout, TextView textView, TextView textView2) {
        this.rootView = constraintLayout;
        this.contentView = textView;
        this.forwardButton = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static ShowContentLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ShowContentLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.show_content_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ShowContentLayoutBinding bind(View view) {
        int i = R.id.content_view;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.content_view);
        if (textView != null) {
            i = R.id.forward_button;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.forward_button);
            if (textView2 != null) {
                return new ShowContentLayoutBinding((ConstraintLayout) view, textView, textView2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
