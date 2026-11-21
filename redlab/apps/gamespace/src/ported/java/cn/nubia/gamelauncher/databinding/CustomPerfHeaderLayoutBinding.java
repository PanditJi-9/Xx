package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import com.zte.mifavor.widget.TextViewZTE;

/* loaded from: classes.dex */
public final class CustomPerfHeaderLayoutBinding implements ViewBinding {
    public final ConstraintLayout btnPlanSelector;
    public final ImageView deleteCustomParameterBt;
    public final ImageView detailHelpBt;
    public final View driver;
    public final ImageView editCustomParameterBt;
    public final TextViewZTE instructionTitleView;
    public final ImageView ivPlanSelector;
    public final ImageView resetCustomParameterBt;
    private final ConstraintLayout rootView;
    public final ImageView saveAsCustomParameterBt;
    public final TextView tvPlanSelector;

    private CustomPerfHeaderLayoutBinding(ConstraintLayout constraintLayout, ConstraintLayout constraintLayout2, ImageView imageView, ImageView imageView2, View view, ImageView imageView3, TextViewZTE textViewZTE, ImageView imageView4, ImageView imageView5, ImageView imageView6, TextView textView) {
        this.rootView = constraintLayout;
        this.btnPlanSelector = constraintLayout2;
        this.deleteCustomParameterBt = imageView;
        this.detailHelpBt = imageView2;
        this.driver = view;
        this.editCustomParameterBt = imageView3;
        this.instructionTitleView = textViewZTE;
        this.ivPlanSelector = imageView4;
        this.resetCustomParameterBt = imageView5;
        this.saveAsCustomParameterBt = imageView6;
        this.tvPlanSelector = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfHeaderLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfHeaderLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_header_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfHeaderLayoutBinding bind(View view) {
        int i = R.id.btn_plan_selector;
        ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.btn_plan_selector);
        if (constraintLayout != null) {
            i = R.id.delete_custom_parameter_bt;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.delete_custom_parameter_bt);
            if (imageView != null) {
                i = R.id.detail_help_bt;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.detail_help_bt);
                if (imageView2 != null) {
                    i = R.id.driver;
                    View findChildViewById = ViewBindings.findChildViewById(view, R.id.driver);
                    if (findChildViewById != null) {
                        i = R.id.edit_custom_parameter_bt;
                        ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.edit_custom_parameter_bt);
                        if (imageView3 != null) {
                            i = R.id.instruction_title_view;
                            TextViewZTE textViewZTE = (TextViewZTE) ViewBindings.findChildViewById(view, R.id.instruction_title_view);
                            if (textViewZTE != null) {
                                i = R.id.iv_plan_selector;
                                ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_plan_selector);
                                if (imageView4 != null) {
                                    i = R.id.reset_custom_parameter_bt;
                                    ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.reset_custom_parameter_bt);
                                    if (imageView5 != null) {
                                        i = R.id.save_as_custom_parameter_bt;
                                        ImageView imageView6 = (ImageView) ViewBindings.findChildViewById(view, R.id.save_as_custom_parameter_bt);
                                        if (imageView6 != null) {
                                            i = R.id.tv_plan_selector;
                                            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_plan_selector);
                                            if (textView != null) {
                                                return new CustomPerfHeaderLayoutBinding((ConstraintLayout) view, constraintLayout, imageView, imageView2, findChildViewById, imageView3, textViewZTE, imageView4, imageView5, imageView6, textView);
                                            }
                                        }
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
