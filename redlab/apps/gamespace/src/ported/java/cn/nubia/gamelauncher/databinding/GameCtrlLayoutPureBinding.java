package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class GameCtrlLayoutPureBinding implements ViewBinding {
    private final ConstraintLayout rootView;

    private GameCtrlLayoutPureBinding(ConstraintLayout constraintLayout) {
        this.rootView = constraintLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GameCtrlLayoutPureBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameCtrlLayoutPureBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_ctrl_layout_pure, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameCtrlLayoutPureBinding bind(View view) {
        if (view == null) {
            throw new NullPointerException("rootView");
        }
        return new GameCtrlLayoutPureBinding((ConstraintLayout) view);
    }
}
