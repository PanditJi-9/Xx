package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaSuperResolutionSettingsLayoutBinding implements ViewBinding {
    public final ConstraintLayout bottomLayout;
    public final TextView cancelView;
    public final ImageView closeView;
    public final TextView instructionTitleView;
    public final TextView instructionView;
    public final TextView positiveView;
    public final RecyclerView recyclerView;
    private final ConstraintLayout rootView;
    public final ConstraintLayout titleLayout;

    private NubiaSuperResolutionSettingsLayoutBinding(ConstraintLayout constraintLayout, ConstraintLayout constraintLayout2, TextView textView, ImageView imageView, TextView textView2, TextView textView3, TextView textView4, RecyclerView recyclerView, ConstraintLayout constraintLayout3) {
        this.rootView = constraintLayout;
        this.bottomLayout = constraintLayout2;
        this.cancelView = textView;
        this.closeView = imageView;
        this.instructionTitleView = textView2;
        this.instructionView = textView3;
        this.positiveView = textView4;
        this.recyclerView = recyclerView;
        this.titleLayout = constraintLayout3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static NubiaSuperResolutionSettingsLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static NubiaSuperResolutionSettingsLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.nubia_super_resolution_settings_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static NubiaSuperResolutionSettingsLayoutBinding bind(View view) {
        int i = R.id.bottom_layout;
        ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.bottom_layout);
        if (constraintLayout != null) {
            i = R.id.cancel_view;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.cancel_view);
            if (textView != null) {
                i = R.id.close_View;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.close_View);
                if (imageView != null) {
                    i = R.id.instruction_title_view;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.instruction_title_view);
                    if (textView2 != null) {
                        i = R.id.instruction_view;
                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.instruction_view);
                        if (textView3 != null) {
                            i = R.id.positive_view;
                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.positive_view);
                            if (textView4 != null) {
                                i = R.id.recycler_view;
                                RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.recycler_view);
                                if (recyclerView != null) {
                                    i = R.id.title_layout;
                                    ConstraintLayout constraintLayout2 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.title_layout);
                                    if (constraintLayout2 != null) {
                                        return new NubiaSuperResolutionSettingsLayoutBinding((ConstraintLayout) view, constraintLayout, textView, imageView, textView2, textView3, textView4, recyclerView, constraintLayout2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
