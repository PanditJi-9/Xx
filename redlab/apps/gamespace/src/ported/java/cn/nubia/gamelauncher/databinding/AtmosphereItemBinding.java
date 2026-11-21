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
public final class AtmosphereItemBinding implements ViewBinding {
    public final TextView atmosphereLabel;
    public final View bottomPlace;
    public final ImageView itemDivider;
    private final ConstraintLayout rootView;
    public final ImageView thumbnailIcon;
    public final View topPlace;
    public final ImageView typeFlag;

    private AtmosphereItemBinding(ConstraintLayout constraintLayout, TextView textView, View view, ImageView imageView, ImageView imageView2, View view2, ImageView imageView3) {
        this.rootView = constraintLayout;
        this.atmosphereLabel = textView;
        this.bottomPlace = view;
        this.itemDivider = imageView;
        this.thumbnailIcon = imageView2;
        this.topPlace = view2;
        this.typeFlag = imageView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static AtmosphereItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AtmosphereItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.atmosphere_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AtmosphereItemBinding bind(View view) {
        int i = R.id.atmosphere_label;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.atmosphere_label);
        if (textView != null) {
            i = R.id.bottom_place;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.bottom_place);
            if (findChildViewById != null) {
                i = R.id.item_divider;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.item_divider);
                if (imageView != null) {
                    i = R.id.thumbnail_icon;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.thumbnail_icon);
                    if (imageView2 != null) {
                        i = R.id.top_place;
                        View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.top_place);
                        if (findChildViewById2 != null) {
                            i = R.id.type_flag;
                            ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.type_flag);
                            if (imageView3 != null) {
                                return new AtmosphereItemBinding((ConstraintLayout) view, textView, findChildViewById, imageView, imageView2, findChildViewById2, imageView3);
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
