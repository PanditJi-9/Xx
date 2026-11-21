package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class WallpaperListLayoutBinding implements ViewBinding {
    public final ImageView actionbarBg;
    public final ImageView backIcon;
    private final ConstraintLayout rootView;
    public final TextView title;
    public final RecyclerView wallpaperList;

    private WallpaperListLayoutBinding(ConstraintLayout constraintLayout, ImageView imageView, ImageView imageView2, TextView textView, RecyclerView recyclerView) {
        this.rootView = constraintLayout;
        this.actionbarBg = imageView;
        this.backIcon = imageView2;
        this.title = textView;
        this.wallpaperList = recyclerView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static WallpaperListLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static WallpaperListLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.wallpaper_list_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static WallpaperListLayoutBinding bind(View view) {
        int i = R.id.actionbar_bg;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.actionbar_bg);
        if (imageView != null) {
            i = R.id.back_icon;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.back_icon);
            if (imageView2 != null) {
                i = R.id.title;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.title);
                if (textView != null) {
                    i = R.id.wallpaper_list;
                    RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.wallpaper_list);
                    if (recyclerView != null) {
                        return new WallpaperListLayoutBinding((ConstraintLayout) view, imageView, imageView2, textView, recyclerView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
