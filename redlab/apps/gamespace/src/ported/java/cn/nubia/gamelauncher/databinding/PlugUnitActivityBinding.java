package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.plug.view.PlugVideoView;

/* loaded from: classes.dex */
public final class PlugUnitActivityBinding implements ViewBinding {
    public final LinearLayout contentContainer;
    public final ConstraintLayout detailContainer;
    public final FrameLayout fragmentContainer;
    public final GridView gridView;
    public final ImageView play;
    public final ImageView plugBackArrow;
    public final ImageView preView;
    private final FrameLayout rootView;
    public final FrameLayout videoContainer;
    public final PlugVideoView videoView;

    private PlugUnitActivityBinding(FrameLayout frameLayout, LinearLayout linearLayout, ConstraintLayout constraintLayout, FrameLayout frameLayout2, GridView gridView, ImageView imageView, ImageView imageView2, ImageView imageView3, FrameLayout frameLayout3, PlugVideoView plugVideoView) {
        this.rootView = frameLayout;
        this.contentContainer = linearLayout;
        this.detailContainer = constraintLayout;
        this.fragmentContainer = frameLayout2;
        this.gridView = gridView;
        this.play = imageView;
        this.plugBackArrow = imageView2;
        this.preView = imageView3;
        this.videoContainer = frameLayout3;
        this.videoView = plugVideoView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static PlugUnitActivityBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PlugUnitActivityBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.plug_unit_activity, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PlugUnitActivityBinding bind(View view) {
        int i = R.id.content_container;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.content_container);
        if (linearLayout != null) {
            i = R.id.detail_container;
            ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.detail_container);
            if (constraintLayout != null) {
                i = R.id.fragment_container;
                FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.fragment_container);
                if (frameLayout != null) {
                    i = R.id.grid_view;
                    GridView gridView = (GridView) ViewBindings.findChildViewById(view, R.id.grid_view);
                    if (gridView != null) {
                        i = R.id.play;
                        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.play);
                        if (imageView != null) {
                            i = R.id.plug_back_arrow;
                            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.plug_back_arrow);
                            if (imageView2 != null) {
                                i = R.id.pre_view;
                                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.pre_view);
                                if (imageView3 != null) {
                                    i = R.id.video_container;
                                    FrameLayout frameLayout2 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.video_container);
                                    if (frameLayout2 != null) {
                                        i = R.id.video_view;
                                        PlugVideoView plugVideoView = (PlugVideoView) ViewBindings.findChildViewById(view, R.id.video_view);
                                        if (plugVideoView != null) {
                                            return new PlugUnitActivityBinding((FrameLayout) view, linearLayout, constraintLayout, frameLayout, gridView, imageView, imageView2, imageView3, frameLayout2, plugVideoView);
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
