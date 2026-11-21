package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class PowerPanelEnableDialogLayoutBinding implements ViewBinding {
    public final Button buttonConfirm;
    public final CheckBox checkboxNotReminding;
    private final LinearLayout rootView;

    private PowerPanelEnableDialogLayoutBinding(LinearLayout linearLayout, Button button, CheckBox checkBox) {
        this.rootView = linearLayout;
        this.buttonConfirm = button;
        this.checkboxNotReminding = checkBox;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static PowerPanelEnableDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PowerPanelEnableDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.power_panel_enable_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PowerPanelEnableDialogLayoutBinding bind(View view) {
        int i = R.id.button_confirm;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.button_confirm);
        if (button != null) {
            i = R.id.checkbox_not_reminding;
            CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.checkbox_not_reminding);
            if (checkBox != null) {
                return new PowerPanelEnableDialogLayoutBinding((LinearLayout) view, button, checkBox);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
