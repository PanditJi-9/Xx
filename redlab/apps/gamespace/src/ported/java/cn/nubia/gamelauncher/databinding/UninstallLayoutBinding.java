package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class UninstallLayoutBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final TextView uninstallBtn;
    public final RecyclerView unintsallList;

    private UninstallLayoutBinding(LinearLayout linearLayout, TextView textView, RecyclerView recyclerView) {
        this.rootView = linearLayout;
        this.uninstallBtn = textView;
        this.unintsallList = recyclerView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static UninstallLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static UninstallLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.uninstall_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static UninstallLayoutBinding bind(View view) {
        int i = R.id.uninstall_btn;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.uninstall_btn);
        if (textView != null) {
            i = R.id.unintsall_list;
            RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.unintsall_list);
            if (recyclerView != null) {
                return new UninstallLayoutBinding((LinearLayout) view, textView, recyclerView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
