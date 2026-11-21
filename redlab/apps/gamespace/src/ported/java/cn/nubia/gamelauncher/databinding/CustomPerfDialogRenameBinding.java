package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class CustomPerfDialogRenameBinding implements ViewBinding {
    public final EditText edtName;
    private final LinearLayout rootView;

    private CustomPerfDialogRenameBinding(LinearLayout linearLayout, EditText editText) {
        this.rootView = linearLayout;
        this.edtName = editText;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static CustomPerfDialogRenameBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static CustomPerfDialogRenameBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.custom_perf_dialog_rename, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static CustomPerfDialogRenameBinding bind(View view) {
        EditText editText = (EditText) ViewBindings.findChildViewById(view, R.id.edt_name);
        if (editText != null) {
            return new CustomPerfDialogRenameBinding((LinearLayout) view, editText);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.edt_name)));
    }
}
