package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkTopVisualEffectView extends LinearLayout {
    private static final String TAG = "TgkTopVisualEffectView";
    private Context mContext;
    private ToggleButton mTgBtn;

    public TgkTopVisualEffectView(Context context) {
        this(context, null);
        this.mContext = context;
    }

    public TgkTopVisualEffectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        initView();
    }

    public void initView() {
        LayoutInflater.from(this.mContext).inflate(R.layout.tgk_top_visual_effect_layout, this);
        this.mTgBtn = (ToggleButton) findViewById(R.id.tgk_top_visual_effect_btn);
    }

    public void setClickListener(View.OnClickListener onClickListener) {
        this.mTgBtn.setOnClickListener(onClickListener);
    }

    public void setChecked(boolean z) {
        this.mTgBtn.setChecked(z);
    }
}
