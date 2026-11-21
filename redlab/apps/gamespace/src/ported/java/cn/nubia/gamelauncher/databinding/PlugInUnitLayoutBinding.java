package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class PlugInUnitLayoutBinding implements ViewBinding {
    public final ImageView plugUnitBackground;
    public final RelativeLayout plugUnitLayout;
    public final TextView plugUnitNameTitle;
    public final FrameLayout plugUnitParentLayout;
    public final ImageView resourcePoolHasUpdateWaring;
    private final FrameLayout rootView;
    public final ImageView sourcePoolBackground;
    public final RelativeLayout sourcePoolLayout;
    public final TextView sourcePoolNameTitle;

    private PlugInUnitLayoutBinding(FrameLayout frameLayout, ImageView imageView, RelativeLayout relativeLayout, TextView textView, FrameLayout frameLayout2, ImageView imageView2, ImageView imageView3, RelativeLayout relativeLayout2, TextView textView2) {
        this.rootView = frameLayout;
        this.plugUnitBackground = imageView;
        this.plugUnitLayout = relativeLayout;
        this.plugUnitNameTitle = textView;
        this.plugUnitParentLayout = frameLayout2;
        this.resourcePoolHasUpdateWaring = imageView2;
        this.sourcePoolBackground = imageView3;
        this.sourcePoolLayout = relativeLayout2;
        this.sourcePoolNameTitle = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static PlugInUnitLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PlugInUnitLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.plug_in_unit_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PlugInUnitLayoutBinding bind(View view) {
        int i = R.id.plug_unit_background;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.plug_unit_background);
        if (imageView != null) {
            i = R.id.plug_unit_layout;
            RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.plug_unit_layout);
            if (relativeLayout != null) {
                i = R.id.plug_unit_name_title;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.plug_unit_name_title);
                if (textView != null) {
                    FrameLayout frameLayout = (FrameLayout) view;
                    i = R.id.resource_pool_has_update_waring;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.resource_pool_has_update_waring);
                    if (imageView2 != null) {
                        i = R.id.source_pool_background;
                        ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.source_pool_background);
                        if (imageView3 != null) {
                            i = R.id.source_pool_layout;
                            RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.source_pool_layout);
                            if (relativeLayout2 != null) {
                                i = R.id.source_pool_name_title;
                                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.source_pool_name_title);
                                if (textView2 != null) {
                                    return new PlugInUnitLayoutBinding(frameLayout, imageView, relativeLayout, textView, frameLayout, imageView2, imageView3, relativeLayout2, textView2);
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
