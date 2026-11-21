package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class VirtualSettingsDeleteDialogLayoutBinding implements ViewBinding {
    private final FrameLayout rootView;
    public final TextView virtualHandleDeleteBtn;

    private VirtualSettingsDeleteDialogLayoutBinding(FrameLayout frameLayout, TextView textView) {
        this.rootView = frameLayout;
        this.virtualHandleDeleteBtn = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static VirtualSettingsDeleteDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static VirtualSettingsDeleteDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.virtual_settings_delete_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static VirtualSettingsDeleteDialogLayoutBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.virtual_handle_delete_btn);
        if (textView != null) {
            return new VirtualSettingsDeleteDialogLayoutBinding((FrameLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.virtual_handle_delete_btn)));
    }
}
