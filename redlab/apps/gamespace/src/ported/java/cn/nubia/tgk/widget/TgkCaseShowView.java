package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkCaseShowView extends RelativeLayout {
    private static final String TAG = "TgkCaseShowView";
    public static final float TGK_CASE_VIEW_DISABLE_ALPHA = 0.6f;
    public static final float TGK_CASE_VIEW_NORMAL_ALPHA = 0.9f;
    private boolean isSwipe;
    private boolean mIsNormal;
    private View mRootView;
    private LinearLayout mTgkCaseButton;
    private TextView mTgkCaseTitle;
    private View mTgkCaseView;

    public TgkCaseShowView(Context context) {
        this(context, null);
    }

    public TgkCaseShowView(Context context, AttributeSet attributeSet) {
        this(context, null, 0);
    }

    public TgkCaseShowView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.isSwipe = false;
        this.mIsNormal = true;
        initView(context);
    }

    private void initView(Context context) {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setSystemUiVisibility(5638);
        View inflate = ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.tgk_case_show_layout, (ViewGroup) this, true);
        this.mRootView = inflate;
        inflate.setBackgroundResource(R.drawable.tgk_case_show_item_layout_bg);
        this.mRootView.setAlpha(0.9f);
        this.mTgkCaseTitle = (TextView) this.mRootView.findViewById(R.id.tgk_case_title);
        this.mTgkCaseButton = (LinearLayout) this.mRootView.findViewById(R.id.tgk_case_button);
        View findViewById = this.mRootView.findViewById(R.id.tgk_case_view);
        this.mTgkCaseView = findViewById;
        findViewById.setBackgroundResource(R.drawable.tgk_case_show_item_down);
        Log.d(TAG, "in TgkCaseShowView");
    }

    public View getTgkCaseBottonView() {
        return this.mTgkCaseView;
    }

    public void setIsswipe(boolean z) {
        this.isSwipe = z;
    }

    public boolean getIsswipe() {
        return this.isSwipe;
    }

    public boolean getIsNormal() {
        return this.mIsNormal;
    }

    public void setTgkCaseNormal(boolean z) {
        if (z) {
            if (this.isSwipe) {
                this.mTgkCaseView.setBackgroundResource(R.drawable.tgk_case_show_item_up);
            } else {
                this.mTgkCaseView.setBackgroundResource(R.drawable.tgk_case_show_item_down);
            }
            this.mRootView.setBackgroundResource(R.drawable.tgk_case_show_item_layout_bg);
            this.mRootView.setAlpha(0.9f);
            this.mTgkCaseTitle.setTextColor(getResources().getColor(R.color.tgk_text_normal));
        } else {
            if (this.isSwipe) {
                this.mTgkCaseView.setBackgroundResource(R.drawable.tgk_case_show_item_up_disabled);
            } else {
                this.mTgkCaseView.setBackgroundResource(R.drawable.tgk_case_show_item_down_disabled);
            }
            this.mRootView.setBackgroundResource(R.drawable.tgk_case_show_item_layout_bg_disabled);
            this.mRootView.setAlpha(0.6f);
            this.mTgkCaseTitle.setTextColor(getResources().getColor(R.color.tgk_text_disabled));
        }
        this.mIsNormal = z;
    }

    public void setTgkCaseTvTitle(String str) {
        TextView textView = this.mTgkCaseTitle;
        if (textView != null) {
            textView.setText(str);
        }
    }

    public void setTgkCaseStates(boolean z) {
        if (this.mIsNormal) {
            this.isSwipe = z;
            View view = this.mTgkCaseView;
            if (view != null) {
                if (z) {
                    view.setBackgroundResource(R.drawable.tgk_case_show_item_up);
                } else {
                    view.setBackgroundResource(R.drawable.tgk_case_show_item_down);
                }
            }
        }
    }
}
