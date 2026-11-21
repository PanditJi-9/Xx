package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToastLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final TextView toastText;

    private ToastLayoutBinding(RelativeLayout relativeLayout, TextView textView) {
        this.rootView = relativeLayout;
        this.toastText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static ToastLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToastLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.toast_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToastLayoutBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.toast_text);
        if (textView != null) {
            return new ToastLayoutBinding((RelativeLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.toast_text)));
    }
}
