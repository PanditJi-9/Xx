package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ItemArrowBinding implements ViewBinding {
    public final ImageButton arrow;
    private final LinearLayout rootView;

    private ItemArrowBinding(LinearLayout linearLayout, ImageButton imageButton) {
        this.rootView = linearLayout;
        this.arrow = imageButton;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ItemArrowBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemArrowBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.item_arrow, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ItemArrowBinding bind(View view) {
        ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.arrow);
        if (imageButton != null) {
            return new ItemArrowBinding((LinearLayout) view, imageButton);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.arrow)));
    }
}
