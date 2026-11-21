package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class CustomPerfDialogSelectorItemBinding implements ViewBinding {
    private final TextView rootView;

    private CustomPerfDialogSelectorItemBinding(TextView textView) {
        this.rootView = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public TextView getRoot() {
        return this.rootView;
    }

    public static CustomPerfDialogSelectorItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfDialogSelectorItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_dialog_selector_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfDialogSelectorItemBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        return new CustomPerfDialogSelectorItemBinding((TextView) view);
    }
}
