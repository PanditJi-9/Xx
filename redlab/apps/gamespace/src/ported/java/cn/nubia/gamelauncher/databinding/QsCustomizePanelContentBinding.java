package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class QsCustomizePanelContentBinding implements ViewBinding {
    public final Button cancelButton;
    public final Button confirmButton;
    public final RecyclerView customList;
    private final LinearLayout rootView;

    private QsCustomizePanelContentBinding(LinearLayout linearLayout, Button button, Button button2, RecyclerView recyclerView) {
        this.rootView = linearLayout;
        this.cancelButton = button;
        this.confirmButton = button2;
        this.customList = recyclerView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static QsCustomizePanelContentBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static QsCustomizePanelContentBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.qs_customize_panel_content, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static QsCustomizePanelContentBinding bind(View view) {
        int i = R.id.cancel_button;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.cancel_button);
        if (button != null) {
            i = R.id.confirm_button;
            Button button2 = (Button) ViewBindings.findChildViewById(view, R.id.confirm_button);
            if (button2 != null) {
                i = R.id.custom_list;
                RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.custom_list);
                if (recyclerView != null) {
                    return new QsCustomizePanelContentBinding((LinearLayout) view, button, button2, recyclerView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
