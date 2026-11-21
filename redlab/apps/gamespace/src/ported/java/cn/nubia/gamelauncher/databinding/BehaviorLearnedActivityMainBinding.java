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

/* loaded from: classes.dex */
public final class BehaviorLearnedActivityMainBinding implements ViewBinding {
    public final ImageView back;
    public final ImageView ivBehaviorBg;
    private final ConstraintLayout rootView;
    public final ImageView titleBackground;
    public final TextView tvBehaviorOne;
    public final TextView tvBehaviorThree;
    public final TextView tvBehaviorTwo;
    public final TextView xGravitationTitle;

    private BehaviorLearnedActivityMainBinding(ConstraintLayout constraintLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, TextView textView, TextView textView2, TextView textView3, TextView textView4) {
        this.rootView = constraintLayout;
        this.back = imageView;
        this.ivBehaviorBg = imageView2;
        this.titleBackground = imageView3;
        this.tvBehaviorOne = textView;
        this.tvBehaviorThree = textView2;
        this.tvBehaviorTwo = textView3;
        this.xGravitationTitle = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static BehaviorLearnedActivityMainBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static BehaviorLearnedActivityMainBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.behavior_learned_activity_main, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static BehaviorLearnedActivityMainBinding bind(View view) {
        int i = R.id.back;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.back);
        if (imageView != null) {
            i = R.id.iv_behavior_bg;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_behavior_bg);
            if (imageView2 != null) {
                i = R.id.title_background;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.title_background);
                if (imageView3 != null) {
                    i = R.id.tv_behavior_one;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_behavior_one);
                    if (textView != null) {
                        i = R.id.tv_behavior_three;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_behavior_three);
                        if (textView2 != null) {
                            i = R.id.tv_behavior_two;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_behavior_two);
                            if (textView3 != null) {
                                i = R.id.x_gravitation_title;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_title);
                                if (textView4 != null) {
                                    return new BehaviorLearnedActivityMainBinding((ConstraintLayout) view, imageView, imageView2, imageView3, textView, textView2, textView3, textView4);
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
