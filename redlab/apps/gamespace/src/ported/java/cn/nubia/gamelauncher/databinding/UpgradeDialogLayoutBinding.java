package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class UpgradeDialogLayoutBinding implements ViewBinding {
    public final TextView checkBoxNote;
    public final Button dialogButtonCancel;
    public final Button dialogButtonCancelOnly;
    public final Button dialogButtonOk;
    public final TextView dialogContent;
    public final ProgressBar dialogProgress;
    public final TextView dialogTitle;
    public final View placeHolder;
    private final ConstraintLayout rootView;
    public final CheckBox upgradeCheckBox;

    private UpgradeDialogLayoutBinding(ConstraintLayout constraintLayout, TextView textView, Button button, Button button2, Button button3, TextView textView2, ProgressBar progressBar, TextView textView3, View view, CheckBox checkBox) {
        this.rootView = constraintLayout;
        this.checkBoxNote = textView;
        this.dialogButtonCancel = button;
        this.dialogButtonCancelOnly = button2;
        this.dialogButtonOk = button3;
        this.dialogContent = textView2;
        this.dialogProgress = progressBar;
        this.dialogTitle = textView3;
        this.placeHolder = view;
        this.upgradeCheckBox = checkBox;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static UpgradeDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static UpgradeDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.upgrade_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static UpgradeDialogLayoutBinding bind(View view) {
        int i = R.id.check_box_note;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.check_box_note);
        if (textView != null) {
            i = R.id.dialog_button_cancel;
            Button button = (Button) ViewBindings.findChildViewById(view, R.id.dialog_button_cancel);
            if (button != null) {
                i = R.id.dialog_button_cancel_only;
                Button button2 = (Button) ViewBindings.findChildViewById(view, R.id.dialog_button_cancel_only);
                if (button2 != null) {
                    i = R.id.dialog_button_ok;
                    Button button3 = (Button) ViewBindings.findChildViewById(view, R.id.dialog_button_ok);
                    if (button3 != null) {
                        i = R.id.dialog_content;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.dialog_content);
                        if (textView2 != null) {
                            i = R.id.dialog_progress;
                            ProgressBar progressBar = (ProgressBar) ViewBindings.findChildViewById(view, R.id.dialog_progress);
                            if (progressBar != null) {
                                i = R.id.dialog_title;
                                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.dialog_title);
                                if (textView3 != null) {
                                    i = R.id.place_holder;
                                    View findChildViewById = ViewBindings.findChildViewById(view, R.id.place_holder);
                                    if (findChildViewById != null) {
                                        i = R.id.upgrade_check_box;
                                        CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.upgrade_check_box);
                                        if (checkBox != null) {
                                            return new UpgradeDialogLayoutBinding((ConstraintLayout) view, textView, button, button2, button3, textView2, progressBar, textView3, findChildViewById, checkBox);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
