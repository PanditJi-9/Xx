package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class GamelauncherCtaPermissionBinding implements ViewBinding {
    public final TextView ctaMessage;
    public final TextView handleCtaTitle;
    private final LinearLayout rootView;

    private GamelauncherCtaPermissionBinding(LinearLayout linearLayout, TextView textView, TextView textView2) {
        this.rootView = linearLayout;
        this.ctaMessage = textView;
        this.handleCtaTitle = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static GamelauncherCtaPermissionBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GamelauncherCtaPermissionBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.gamelauncher_cta_permission, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GamelauncherCtaPermissionBinding bind(View view) {
        int i = R.id.cta_message;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.cta_message);
        if (textView != null) {
            i = R.id.handle_cta_title;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.handle_cta_title);
            if (textView2 != null) {
                return new GamelauncherCtaPermissionBinding((LinearLayout) view, textView, textView2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
