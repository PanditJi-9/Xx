package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class WelcomeBinding implements ViewBinding {
    public final Guideline guidelineH;
    private final ConstraintLayout rootView;
    public final Button welcomeAccept;
    public final Button welcomeCancel;
    public final TextView welcomeContent;
    public final TextView welcomeLink;
    public final TextView welcomeTitle;

    private WelcomeBinding(ConstraintLayout constraintLayout, Guideline guideline, Button button, Button button2, TextView textView, TextView textView2, TextView textView3) {
        this.rootView = constraintLayout;
        this.guidelineH = guideline;
        this.welcomeAccept = button;
        this.welcomeCancel = button2;
        this.welcomeContent = textView;
        this.welcomeLink = textView2;
        this.welcomeTitle = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static WelcomeBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static WelcomeBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.welcome, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static WelcomeBinding bind(View view) {
        int i = R.id.guideline_h;
        Guideline guideline = (Guideline) ViewBindings.findChildViewById(view, R.id.guideline_h);
        if (guideline != null) {
            i = R.id.welcome_accept;
            Button button = (Button) ViewBindings.findChildViewById(view, R.id.welcome_accept);
            if (button != null) {
                i = R.id.welcome_cancel;
                Button button2 = (Button) ViewBindings.findChildViewById(view, R.id.welcome_cancel);
                if (button2 != null) {
                    i = R.id.welcome_content;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.welcome_content);
                    if (textView != null) {
                        i = R.id.welcome_link;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.welcome_link);
                        if (textView2 != null) {
                            i = R.id.welcome_title;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.welcome_title);
                            if (textView3 != null) {
                                return new WelcomeBinding((ConstraintLayout) view, guideline, button, button2, textView, textView2, textView3);
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
