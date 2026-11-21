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
public final class UninstallTitlebarLayoutBinding implements ViewBinding {
    public final TextView allSelect;
    public final TextView cancel;
    public final TextView middleName;
    private final RelativeLayout rootView;

    private UninstallTitlebarLayoutBinding(RelativeLayout relativeLayout, TextView textView, TextView textView2, TextView textView3) {
        this.rootView = relativeLayout;
        this.allSelect = textView;
        this.cancel = textView2;
        this.middleName = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static UninstallTitlebarLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static UninstallTitlebarLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.uninstall_titlebar_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static UninstallTitlebarLayoutBinding bind(View view) {
        int i = R.id.all_select;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.all_select);
        if (textView != null) {
            i = R.id.cancel;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.cancel);
            if (textView2 != null) {
                i = R.id.middle_name;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.middle_name);
                if (textView3 != null) {
                    return new UninstallTitlebarLayoutBinding((RelativeLayout) view, textView, textView2, textView3);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
