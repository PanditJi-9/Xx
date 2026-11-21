package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ChargeSeparationCusttomDialogLayoutBinding implements ViewBinding {
    public final CheckBox chargeSeperationCheckBox;
    public final TextView checkBoxNote;
    public final TextView dialogTitle;
    private final LinearLayout rootView;

    private ChargeSeparationCusttomDialogLayoutBinding(LinearLayout linearLayout, CheckBox checkBox, TextView textView, TextView textView2) {
        this.rootView = linearLayout;
        this.chargeSeperationCheckBox = checkBox;
        this.checkBoxNote = textView;
        this.dialogTitle = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ChargeSeparationCusttomDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ChargeSeparationCusttomDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.charge_separation_custtom_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ChargeSeparationCusttomDialogLayoutBinding bind(View view) {
        int i = R.id.charge_seperation_check_box;
        CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.charge_seperation_check_box);
        if (checkBox != null) {
            i = R.id.check_box_note;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.check_box_note);
            if (textView != null) {
                i = R.id.dialog_title;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.dialog_title);
                if (textView2 != null) {
                    return new ChargeSeparationCusttomDialogLayoutBinding((LinearLayout) view, checkBox, textView, textView2);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
