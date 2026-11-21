package cn.nubia.plug.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import dev.redlab.gamespace.R;
import cn.nubia.plug.PlugData;

/* loaded from: classes2.dex */
public class PlugFragment extends BaseFragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static PlugFragment newInstance(PlugData plugData) {
        PlugFragment plugFragment = new PlugFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_PARAM1, plugData);
        plugFragment.setArguments(bundle);
        return plugFragment;
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getArguments() != null) {
            this.mData = (PlugData) getArguments().getSerializable(ARG_PARAM1);
        }
    }

    @Override // cn.nubia.plug.fragment.BaseFragment, androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        setLayoutRes(R.layout.plug_fragment);
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @Override // cn.nubia.plug.fragment.BaseFragment
    protected void canAddView() {
        addSupportGames(this.mData.getSupportGames().length);
        addDimensionRatings();
    }

    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override // androidx.fragment.app.Fragment
    public void onDetach() {
        super.onDetach();
    }
}
