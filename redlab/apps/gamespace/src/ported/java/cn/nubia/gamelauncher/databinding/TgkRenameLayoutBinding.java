package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TgkRenameLayoutBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final EditText tgkCaseModifyName;
    public final TextView tgkCaseModifyTitle;
    public final LinearLayout tgkCaseModifyView;
    public final View tgkRenameBottomView;
    public final Button tgkRenameCancel;
    public final Button tgkRenameConfirm;

    private TgkRenameLayoutBinding(LinearLayout linearLayout, EditText editText, TextView textView, LinearLayout linearLayout2, View view, Button button, Button button2) {
        this.rootView = linearLayout;
        this.tgkCaseModifyName = editText;
        this.tgkCaseModifyTitle = textView;
        this.tgkCaseModifyView = linearLayout2;
        this.tgkRenameBottomView = view;
        this.tgkRenameCancel = button;
        this.tgkRenameConfirm = button2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkRenameLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkRenameLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_rename_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkRenameLayoutBinding bind(View view) {
        int i = R.id.tgk_case_modify_name;
        EditText editText = (EditText) ViewBindings.findChildViewById(view, R.id.tgk_case_modify_name);
        if (editText != null) {
            i = R.id.tgk_case_modify_title;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_case_modify_title);
            if (textView != null) {
                LinearLayout linearLayout = (LinearLayout) view;
                i = R.id.tgk_rename_bottom_view;
                View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_rename_bottom_view);
                if (findChildViewById != null) {
                    i = R.id.tgk_rename_cancel;
                    Button button = (Button) ViewBindings.findChildViewById(view, R.id.tgk_rename_cancel);
                    if (button != null) {
                        i = R.id.tgk_rename_confirm;
                        Button button2 = (Button) ViewBindings.findChildViewById(view, R.id.tgk_rename_confirm);
                        if (button2 != null) {
                            return new TgkRenameLayoutBinding(linearLayout, editText, textView, linearLayout, findChildViewById, button, button2);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
