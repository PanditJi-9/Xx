package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class SplashScreenBinding implements ViewBinding {
    public final ViewStub guidePanel;
    private final ConstraintLayout rootView;
    public final ViewStub welcomeView;

    private SplashScreenBinding(ConstraintLayout constraintLayout, ViewStub viewStub, ViewStub viewStub2) {
        this.rootView = constraintLayout;
        this.guidePanel = viewStub;
        this.welcomeView = viewStub2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static SplashScreenBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SplashScreenBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.splash_screen, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SplashScreenBinding bind(View view) {
        int i = R.id.guide_panel;
        ViewStub viewStub = (ViewStub) ViewBindings.findChildViewById(view, R.id.guide_panel);
        if (viewStub != null) {
            i = R.id.welcome_view;
            ViewStub viewStub2 = (ViewStub) ViewBindings.findChildViewById(view, R.id.welcome_view);
            if (viewStub2 != null) {
                return new SplashScreenBinding((ConstraintLayout) view, viewStub, viewStub2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
