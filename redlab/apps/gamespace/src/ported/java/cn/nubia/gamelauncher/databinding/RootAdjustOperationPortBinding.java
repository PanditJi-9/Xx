package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class RootAdjustOperationPortBinding implements ViewBinding {
    public final ViewStub aiAdjust;
    public final LinearLayout gameAdjustAllLayout;
    public final ViewStub gyro;
    private final ScrollView rootView;
    public final ScrollView scrollView;

    private RootAdjustOperationPortBinding(ScrollView scrollView, ViewStub viewStub, LinearLayout linearLayout, ViewStub viewStub2, ScrollView scrollView2) {
        this.rootView = scrollView;
        this.aiAdjust = viewStub;
        this.gameAdjustAllLayout = linearLayout;
        this.gyro = viewStub2;
        this.scrollView = scrollView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ScrollView getRoot() {
        return this.rootView;
    }

    public static RootAdjustOperationPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static RootAdjustOperationPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.root_adjust_operation_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static RootAdjustOperationPortBinding bind(View view) {
        int i = R.id.ai_adjust;
        ViewStub viewStub = (ViewStub) ViewBindings.findChildViewById(view, R.id.ai_adjust);
        if (viewStub != null) {
            i = R.id.game_adjust_all_layout;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.game_adjust_all_layout);
            if (linearLayout != null) {
                i = R.id.gyro;
                ViewStub viewStub2 = (ViewStub) ViewBindings.findChildViewById(view, R.id.gyro);
                if (viewStub2 != null) {
                    ScrollView scrollView = (ScrollView) view;
                    return new RootAdjustOperationPortBinding(scrollView, viewStub, linearLayout, viewStub2, scrollView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
