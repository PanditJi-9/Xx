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
public final class NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding implements ViewBinding {
    public final TextView nubiaGameStrengthenPerformanceSuperCancel;
    public final TextView nubiaGameStrengthenPerformanceSuperOk;
    public final TextView nubiaGameStrengthenPerformanceSuperTitle;
    private final FrameLayout rootView;

    private NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding(FrameLayout frameLayout, TextView textView, TextView textView2, TextView textView3) {
        this.rootView = frameLayout;
        this.nubiaGameStrengthenPerformanceSuperCancel = textView;
        this.nubiaGameStrengthenPerformanceSuperOk = textView2;
        this.nubiaGameStrengthenPerformanceSuperTitle = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.nubia_game_strengthen_performance_super_confirm_dialog_view, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding bind(View view) {
        int i = R.id.nubia_game_strengthen_performance_super_cancel;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_super_cancel);
        if (textView != null) {
            i = R.id.nubia_game_strengthen_performance_super_ok;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_super_ok);
            if (textView2 != null) {
                i = R.id.nubia_game_strengthen_performance_super_title;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_super_title);
                if (textView3 != null) {
                    return new NubiaGameStrengthenPerformanceSuperConfirmDialogViewBinding((FrameLayout) view, textView, textView2, textView3);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
