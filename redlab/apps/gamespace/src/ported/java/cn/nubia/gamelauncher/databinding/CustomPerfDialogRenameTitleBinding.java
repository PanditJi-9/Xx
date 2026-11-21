package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class CustomPerfDialogRenameTitleBinding implements ViewBinding {
    private final FrameLayout rootView;
    public final TextView tvTitle;

    private CustomPerfDialogRenameTitleBinding(FrameLayout frameLayout, TextView textView) {
        this.rootView = frameLayout;
        this.tvTitle = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfDialogRenameTitleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfDialogRenameTitleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_dialog_rename_title, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfDialogRenameTitleBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_title);
        if (textView != null) {
            return new CustomPerfDialogRenameTitleBinding((FrameLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.tv_title)));
    }
}
