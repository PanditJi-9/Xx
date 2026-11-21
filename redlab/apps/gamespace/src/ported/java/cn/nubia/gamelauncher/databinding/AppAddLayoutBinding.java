package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class AppAddLayoutBinding implements ViewBinding {
    public final AppAddTitlebarLayoutBinding addviewActionbar;
    public final RecyclerView appAddList;
    public final HostModeLowViewBinding bottomView;
    public final TextView hostActionbar;
    private final ConstraintLayout rootView;

    private AppAddLayoutBinding(ConstraintLayout constraintLayout, AppAddTitlebarLayoutBinding appAddTitlebarLayoutBinding, RecyclerView recyclerView, HostModeLowViewBinding hostModeLowViewBinding, TextView textView) {
        this.rootView = constraintLayout;
        this.addviewActionbar = appAddTitlebarLayoutBinding;
        this.appAddList = recyclerView;
        this.bottomView = hostModeLowViewBinding;
        this.hostActionbar = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static AppAddLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AppAddLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.app_add_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AppAddLayoutBinding bind(View view) {
        int i = R.id.addview_actionbar;
        View findChildViewById = ViewBindings.findChildViewById(view, R.id.addview_actionbar);
        if (findChildViewById != null) {
            AppAddTitlebarLayoutBinding bind = AppAddTitlebarLayoutBinding.bind(findChildViewById);
            i = R.id.app_add_list;
            RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.app_add_list);
            if (recyclerView != null) {
                i = R.id.bottom_view;
                View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.bottom_view);
                if (findChildViewById2 != null) {
                    HostModeLowViewBinding bind2 = HostModeLowViewBinding.bind(findChildViewById2);
                    i = R.id.host_actionbar;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.host_actionbar);
                    if (textView != null) {
                        return new AppAddLayoutBinding((ConstraintLayout) view, bind, recyclerView, bind2, textView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
