package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class DialogTextExitBinding implements ViewBinding {
    public final TextView exitDesc;
    public final CheckBox isAddShortcut;
    private final RelativeLayout rootView;

    private DialogTextExitBinding(RelativeLayout relativeLayout, TextView textView, CheckBox checkBox) {
        this.rootView = relativeLayout;
        this.exitDesc = textView;
        this.isAddShortcut = checkBox;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static DialogTextExitBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static DialogTextExitBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.dialog_text_exit, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static DialogTextExitBinding bind(View view) {
        int i = R.id.exit_desc;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.exit_desc);
        if (textView != null) {
            i = R.id.is_add_shortcut;
            CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.is_add_shortcut);
            if (checkBox != null) {
                return new DialogTextExitBinding((RelativeLayout) view, textView, checkBox);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
