package cn.nubia.gamecenter.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

/* loaded from: classes.dex */
public class BaseFragment extends Fragment implements FragmentInterface {
    private static final String TAG = "SummaryFragment";
    protected Activity m_activity;
    private String m_tag;
    private View m_view;

    @Override // cn.nubia.gamecenter.settings.FragmentInterface
    public Fragment getFragment() {
        return this;
    }

    @Override // cn.nubia.gamecenter.settings.FragmentInterface
    public void setTestMode() {
    }

    @Override // cn.nubia.gamecenter.settings.FragmentInterface
    public void setInfoTag(String str) {
        this.m_tag = str;
    }

    @Override // cn.nubia.gamecenter.settings.FragmentInterface
    public String getInfoTag() {
        return this.m_tag;
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.m_activity = getActivity();
        return getMainView();
    }

    protected View getMainView() {
        if (this.m_view == null) {
            this.m_view = createMainView();
        }
        return this.m_view;
    }

    protected View createMainView() {
        Activity activity = this.m_activity;
        if (activity == null) {
            return null;
        }
        View inflate = View.inflate(activity, R.layout.gcs_gamecenter_fragment_base, null);
        setDemoText(inflate, "此界面[" + getInfoTag() + "]尚未完成，请相关开发同事处理");
        return inflate;
    }

    private void setDemoText(View view, String str) {
        setText(view, R.id.empty_text, str);
    }

    protected void setText(View view, int i, String str) {
        View findViewById = view.findViewById(i);
        if (findViewById == null || !(findViewById instanceof TextView)) {
            return;
        }
        ((TextView) findViewById).setText(str);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        hideNavigationBar();
    }

    private void hideNavigationBar() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(5894);
    }
}
