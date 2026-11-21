package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.plug.view.PlugMarqueeTextView;

/* loaded from: classes.dex */
public final class PlugGridviewItemBinding implements ViewBinding {
    public final ImageView head;
    public final LinearLayout root;
    private final LinearLayout rootView;
    public final PlugMarqueeTextView title;

    private PlugGridviewItemBinding(LinearLayout linearLayout, ImageView imageView, LinearLayout linearLayout2, PlugMarqueeTextView plugMarqueeTextView) {
        this.rootView = linearLayout;
        this.head = imageView;
        this.root = linearLayout2;
        this.title = plugMarqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static PlugGridviewItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PlugGridviewItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.plug_gridview_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PlugGridviewItemBinding bind(View view) {
        int i = R.id.head;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.head);
        if (imageView != null) {
            LinearLayout linearLayout = (LinearLayout) view;
            PlugMarqueeTextView plugMarqueeTextView = (PlugMarqueeTextView) ViewBindings.findChildViewById(view, R.id.title);
            if (plugMarqueeTextView != null) {
                return new PlugGridviewItemBinding(linearLayout, imageView, linearLayout, plugMarqueeTextView);
            }
            i = R.id.title;
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
