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
public final class RequestPermissionCustomDialogBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final TextView title;

    private RequestPermissionCustomDialogBinding(LinearLayout linearLayout, TextView textView) {
        this.rootView = linearLayout;
        this.title = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static RequestPermissionCustomDialogBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static RequestPermissionCustomDialogBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.request_permission_custom_dialog, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static RequestPermissionCustomDialogBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.title);
        if (textView != null) {
            return new RequestPermissionCustomDialogBinding((LinearLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.title)));
    }
}
