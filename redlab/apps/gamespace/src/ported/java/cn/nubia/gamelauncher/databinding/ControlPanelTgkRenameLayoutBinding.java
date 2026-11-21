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
public final class ControlPanelTgkRenameLayoutBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final EditText tgkCaseModifyName;
    public final TextView tgkRenameCancel;
    public final TextView tgkRenameConfirm;

    private ControlPanelTgkRenameLayoutBinding(LinearLayout linearLayout, EditText editText, TextView textView, TextView textView2) {
        this.rootView = linearLayout;
        this.tgkCaseModifyName = editText;
        this.tgkRenameCancel = textView;
        this.tgkRenameConfirm = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ControlPanelTgkRenameLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ControlPanelTgkRenameLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.control_panel_tgk_rename_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ControlPanelTgkRenameLayoutBinding bind(View view) {
        int i = R.id.tgk_case_modify_name;
        EditText editText = (EditText) ViewBindings.findChildViewById(view, R.id.tgk_case_modify_name);
        if (editText != null) {
            i = R.id.tgk_rename_cancel;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_rename_cancel);
            if (textView != null) {
                i = R.id.tgk_rename_confirm;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_rename_confirm);
                if (textView2 != null) {
                    return new ControlPanelTgkRenameLayoutBinding((LinearLayout) view, editText, textView, textView2);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
