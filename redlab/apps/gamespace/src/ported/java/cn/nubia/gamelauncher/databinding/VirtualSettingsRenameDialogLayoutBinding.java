package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class VirtualSettingsRenameDialogLayoutBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final EditText virtualHandleRename;
    public final TextView virtualHandleRenameCheck;

    private VirtualSettingsRenameDialogLayoutBinding(LinearLayout linearLayout, EditText editText, TextView textView) {
        this.rootView = linearLayout;
        this.virtualHandleRename = editText;
        this.virtualHandleRenameCheck = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static VirtualSettingsRenameDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static VirtualSettingsRenameDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.virtual_settings_rename_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static VirtualSettingsRenameDialogLayoutBinding bind(View view) {
        int i = R.id.virtual_handle_rename;
        EditText editText = (EditText) ViewBindings.findChildViewById(view, R.id.virtual_handle_rename);
        if (editText != null) {
            i = R.id.virtual_handle_rename_check;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.virtual_handle_rename_check);
            if (textView != null) {
                return new VirtualSettingsRenameDialogLayoutBinding((LinearLayout) view, editText, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
