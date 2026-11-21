package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class CustomPerfDialogPlanSelectorItemBinding implements ViewBinding {
    private final TextView rootView;

    private CustomPerfDialogPlanSelectorItemBinding(TextView textView) {
        this.rootView = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public TextView getRoot() {
        return this.rootView;
    }

    public static CustomPerfDialogPlanSelectorItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfDialogPlanSelectorItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_dialog_plan_selector_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfDialogPlanSelectorItemBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        return new CustomPerfDialogPlanSelectorItemBinding((TextView) view);
    }
}
