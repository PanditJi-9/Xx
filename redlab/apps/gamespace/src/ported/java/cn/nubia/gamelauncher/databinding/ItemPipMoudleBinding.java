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
public final class ItemPipMoudleBinding implements ViewBinding {
    public final ImageButton pipModule;
    private final LinearLayout rootView;

    private ItemPipMoudleBinding(LinearLayout linearLayout, ImageButton imageButton) {
        this.rootView = linearLayout;
        this.pipModule = imageButton;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ItemPipMoudleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemPipMoudleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.item_pip_moudle, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ItemPipMoudleBinding bind(View view) {
        ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.pip_module);
        if (imageButton != null) {
            return new ItemPipMoudleBinding((LinearLayout) view, imageButton);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.pip_module)));
    }
}
