package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenViewResourceBinding implements ViewBinding {
    public final ListView controlPanelTgkCaseListView;
    private final View rootView;
    public final ImageButton tgkCaseImportBtn;
    public final ImageButton tgkCaseImportHelperBtn;
    public final TextView tgkCaseLibTitle;

    private NubiaGameStrengthenViewResourceBinding(View view, ListView listView, ImageButton imageButton, ImageButton imageButton2, TextView textView) {
        this.rootView = view;
        this.controlPanelTgkCaseListView = listView;
        this.tgkCaseImportBtn = imageButton;
        this.tgkCaseImportHelperBtn = imageButton2;
        this.tgkCaseLibTitle = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenViewResourceBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_view_resource, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenViewResourceBinding bind(View view) {
        int i = R.id.control_panel_tgk_case_list_view;
        ListView listView = (ListView) ViewBindings.findChildViewById(view, R.id.control_panel_tgk_case_list_view);
        if (listView != null) {
            i = R.id.tgk_case_import_btn;
            ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_import_btn);
            if (imageButton != null) {
                i = R.id.tgk_case_import_helper_btn;
                ImageButton imageButton2 = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_case_import_helper_btn);
                if (imageButton2 != null) {
                    i = R.id.tgk_case_lib_title;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_case_lib_title);
                    if (textView != null) {
                        return new NubiaGameStrengthenViewResourceBinding(view, listView, imageButton, imageButton2, textView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
