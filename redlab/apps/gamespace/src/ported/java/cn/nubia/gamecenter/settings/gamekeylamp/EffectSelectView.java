package cn.nubia.gamecenter.settings.gamekeylamp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import cn.nubia.gamecenter.settings.R;

/* loaded from: classes.dex */
public class EffectSelectView extends FrameLayout {
    private Context mContext;
    private View mSelectedBgView;

    public EffectSelectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.gcs_colorfullight_effect_select_view, (ViewGroup) this, true);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSelectedBgView = findViewById(R.id.effect_selected_bg);
    }

    @Override // android.view.View
    public void setSelected(boolean z) {
        super.setSelected(z);
        this.mSelectedBgView.setVisibility(z ? 0 : 8);
    }
}
