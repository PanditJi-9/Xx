package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.ZoomImageView;

/* loaded from: classes.dex */
public final class PopupwindowSwitchModeBinding implements ViewBinding {
    public final ZoomImageView modeAr;
    public final ZoomImageView modeHandheld;
    public final ZoomImageView modeHost;
    public final ZoomImageView modeNormal;
    private final ConstraintLayout rootView;

    private PopupwindowSwitchModeBinding(ConstraintLayout constraintLayout, ZoomImageView zoomImageView, ZoomImageView zoomImageView2, ZoomImageView zoomImageView3, ZoomImageView zoomImageView4) {
        this.rootView = constraintLayout;
        this.modeAr = zoomImageView;
        this.modeHandheld = zoomImageView2;
        this.modeHost = zoomImageView3;
        this.modeNormal = zoomImageView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static PopupwindowSwitchModeBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PopupwindowSwitchModeBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.popupwindow_switch_mode, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PopupwindowSwitchModeBinding bind(View view) {
        int i = R.id.mode_ar;
        ZoomImageView zoomImageView = (ZoomImageView) ViewBindings.findChildViewById(view, R.id.mode_ar);
        if (zoomImageView != null) {
            i = R.id.mode_handheld;
            ZoomImageView zoomImageView2 = (ZoomImageView) ViewBindings.findChildViewById(view, R.id.mode_handheld);
            if (zoomImageView2 != null) {
                i = R.id.mode_host;
                ZoomImageView zoomImageView3 = (ZoomImageView) ViewBindings.findChildViewById(view, R.id.mode_host);
                if (zoomImageView3 != null) {
                    i = R.id.mode_normal;
                    ZoomImageView zoomImageView4 = (ZoomImageView) ViewBindings.findChildViewById(view, R.id.mode_normal);
                    if (zoomImageView4 != null) {
                        return new PopupwindowSwitchModeBinding((ConstraintLayout) view, zoomImageView, zoomImageView2, zoomImageView3, zoomImageView4);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
