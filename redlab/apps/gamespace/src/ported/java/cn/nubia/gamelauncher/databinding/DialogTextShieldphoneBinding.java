package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class DialogTextShieldphoneBinding implements ViewBinding {
    private final RelativeLayout rootView;

    private DialogTextShieldphoneBinding(RelativeLayout relativeLayout) {
        this.rootView = relativeLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static DialogTextShieldphoneBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static DialogTextShieldphoneBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.dialog_text_shieldphone, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static DialogTextShieldphoneBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        return new DialogTextShieldphoneBinding((RelativeLayout) view);
    }
}
