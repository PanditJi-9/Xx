package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ControlPanelTgkCaseListItemLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final ImageButton tgkCaseDeleteCtlPanel;
    public final ImageButton tgkCaseRenameCtlPanel;
    public final ImageButton tgkCaseShareCtlPanel;
    public final TextView tgkCaseShowname;
    public final ImageButton tgkCaseTypeIcon;

    private ControlPanelTgkCaseListItemLayoutBinding(RelativeLayout relativeLayout, ImageButton imageButton, ImageButton imageButton2, ImageButton imageButton3, TextView textView, ImageButton imageButton4) {
        this.rootView = relativeLayout;
        this.tgkCaseDeleteCtlPanel = imageButton;
        this.tgkCaseRenameCtlPanel = imageButton2;
        this.tgkCaseShareCtlPanel = imageButton3;
        this.tgkCaseShowname = textView;
        this.tgkCaseTypeIcon = imageButton4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static ControlPanelTgkCaseListItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ControlPanelTgkCaseListItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.control_panel_tgk_case_list_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ControlPanelTgkCaseListItemLayoutBinding bind(View view) {
        int i = R.id.tgk_case_delete_ctl_panel;
        ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_delete_ctl_panel);
        if (imageButton != null) {
            i = R.id.tgk_case_rename_ctl_panel;
            ImageButton imageButton2 = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_rename_ctl_panel);
            if (imageButton2 != null) {
                i = R.id.tgk_case_share_ctl_panel;
                ImageButton imageButton3 = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_share_ctl_panel);
                if (imageButton3 != null) {
                    i = R.id.tgk_case_showname;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_case_showname);
                    if (textView != null) {
                        i = R.id.tgk_case_type_icon;
                        ImageButton imageButton4 = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_type_icon);
                        if (imageButton4 != null) {
                            return new ControlPanelTgkCaseListItemLayoutBinding((RelativeLayout) view, imageButton, imageButton2, imageButton3, textView, imageButton4);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
