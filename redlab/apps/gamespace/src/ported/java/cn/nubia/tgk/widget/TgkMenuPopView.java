package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkMenuPopView extends RelativeLayout {
    private static final String TAG = "TgkMenuPopView";
    private OnClickListener mListener;
    private View mRootView;
    private TextView mTextView;
    private View mView;
    private View.OnClickListener mViewListener;

    public interface OnClickListener {
        void onClick(View view);
    }

    public TgkMenuPopView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TgkMenuPopView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mViewListener = new View.OnClickListener() { // from class: cn.nubia.tgk.widget.TgkMenuPopView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                TgkMenuPopView.this.mListener.onClick(TgkMenuPopView.this.mRootView);
            }
        };
        Log.d(TAG, "in TgkMenuPopView");
        initView(context, attributeSet);
    }

    private void initView(Context context, AttributeSet attributeSet) {
        this.mRootView = ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.tgk_menu_pop_window_layout, (ViewGroup) this, true);
        Log.d(TAG, "in initView");
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mListener = onClickListener;
    }
}
