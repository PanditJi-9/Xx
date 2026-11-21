package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenPerformanceRectangleViewBinding implements ViewBinding {
    public final TextView nubiaGameStrengthenPerformanceMode;
    public final View nubiaGameStrengthenPerformanceRectangle1;
    public final View nubiaGameStrengthenPerformanceRectangle2;
    public final View nubiaGameStrengthenPerformanceRectangle3;
    public final View nubiaGameStrengthenPerformanceRectangle4;
    private final View rootView;

    private NubiaGameStrengthenPerformanceRectangleViewBinding(View view, TextView textView, View view2, View view3, View view4, View view5) {
        this.rootView = view;
        this.nubiaGameStrengthenPerformanceMode = textView;
        this.nubiaGameStrengthenPerformanceRectangle1 = view2;
        this.nubiaGameStrengthenPerformanceRectangle2 = view3;
        this.nubiaGameStrengthenPerformanceRectangle3 = view4;
        this.nubiaGameStrengthenPerformanceRectangle4 = view5;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenPerformanceRectangleViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_performance_rectangle_view, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenPerformanceRectangleViewBinding bind(View view) {
        int i = R.id.nubia_game_strengthen_performance_mode;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_mode);
        if (textView != null) {
            i = R.id.nubia_game_strengthen_performance_rectangle1;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_rectangle1);
            if (findChildViewById != null) {
                i = R.id.nubia_game_strengthen_performance_rectangle2;
                View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_rectangle2);
                if (findChildViewById2 != null) {
                    i = R.id.nubia_game_strengthen_performance_rectangle3;
                    View findChildViewById3 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_rectangle3);
                    if (findChildViewById3 != null) {
                        i = R.id.nubia_game_strengthen_performance_rectangle4;
                        View findChildViewById4 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_rectangle4);
                        if (findChildViewById4 != null) {
                            return new NubiaGameStrengthenPerformanceRectangleViewBinding(view, textView, findChildViewById, findChildViewById2, findChildViewById3, findChildViewById4);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
