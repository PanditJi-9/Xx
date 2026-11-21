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
public final class NubiaTransientNotificationBinding implements ViewBinding {
    public final TextView message;
    private final LinearLayout rootView;

    private NubiaTransientNotificationBinding(LinearLayout linearLayout, TextView textView) {
        this.rootView = linearLayout;
        this.message = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static NubiaTransientNotificationBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static NubiaTransientNotificationBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.nubia_transient_notification, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static NubiaTransientNotificationBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.message);
        if (textView != null) {
            return new NubiaTransientNotificationBinding((LinearLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.message)));
    }
}
