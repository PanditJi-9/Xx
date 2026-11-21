package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ScoreEnableDialogLayoutBinding implements ViewBinding {
    public final CheckBox checkboxAllGame;
    private final LinearLayout rootView;

    private ScoreEnableDialogLayoutBinding(LinearLayout linearLayout, CheckBox checkBox) {
        this.rootView = linearLayout;
        this.checkboxAllGame = checkBox;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ScoreEnableDialogLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ScoreEnableDialogLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.score_enable_dialog_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ScoreEnableDialogLayoutBinding bind(View view) {
        CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.checkbox_all_game);
        if (checkBox != null) {
            return new ScoreEnableDialogLayoutBinding((LinearLayout) view, checkBox);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.checkbox_all_game)));
    }
}
