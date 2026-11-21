package cn.nubia.tgk.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkCustomRadioButton extends LinearLayout {
    private boolean mChecked;
    private Context mContext;
    private boolean mEnabled;
    private OnClickListener mListener;
    private LinearLayout mRightImageView;
    private View mRootView;
    private View mSelectBtn;
    private OnClickSettingListener mSettingListener;
    private TextView mTextView;

    public interface OnClickListener {
        void onClick(View view);

        void onDisableClick(View view);
    }

    public interface OnClickSettingListener {
        void onClick(View view);
    }

    public TgkCustomRadioButton(Context context) {
        this(context, null);
    }

    public TgkCustomRadioButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TgkCustomRadioButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mEnabled = true;
        this.mChecked = false;
        this.mListener = null;
        this.mSettingListener = null;
        initView(context, attributeSet);
    }

    public void initView(Context context, AttributeSet attributeSet) {
        this.mContext = context;
        View inflate = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.tgk_radiobutton_layout, (ViewGroup) this, true);
        this.mRootView = inflate;
        this.mRightImageView = (LinearLayout) inflate.findViewById(R.id.nubia_select_btn);
        this.mTextView = (TextView) this.mRootView.findViewById(R.id.nubia_rb_text);
        this.mSelectBtn = this.mRootView.findViewById(R.id.nubia_select_btn);
        this.mRootView.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.tgk.widget.TgkCustomRadioButton.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (!TgkCustomRadioButton.this.mEnabled || TgkCustomRadioButton.this.mListener == null) {
                    if (TgkCustomRadioButton.this.mEnabled) {
                        return;
                    }
                    TgkCustomRadioButton.this.mListener.onDisableClick(TgkCustomRadioButton.this.mRootView);
                    return;
                }
                TgkCustomRadioButton.this.mListener.onClick(TgkCustomRadioButton.this.mRootView);
            }
        });
        this.mRightImageView.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.tgk.widget.TgkCustomRadioButton.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (!TgkCustomRadioButton.this.mEnabled || TgkCustomRadioButton.this.mSettingListener == null) {
                    return;
                }
                TgkCustomRadioButton.this.mSettingListener.onClick(TgkCustomRadioButton.this.mRootView);
            }
        });
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R.styleable.TgkCustomRadioButton);
        setTextView(obtainStyledAttributes.getString(0));
        obtainStyledAttributes.recycle();
        this.mTextView.setSelected(true);
    }

    public void setTextView(String str) {
        this.mTextView.setText(str);
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        updateView();
    }

    private void updateView() {
        if (this.mChecked) {
            this.mTextView.setTextColor(getResources().getColor(R.color.tgk_text_red));
        } else {
            this.mTextView.setTextColor(getResources().getColor(R.color.tgk_text_press));
        }
    }

    public void setShowSettinView(boolean z) {
        if (z) {
            this.mRightImageView.setVisibility(0);
        } else {
            this.mRightImageView.setVisibility(8);
        }
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mListener = onClickListener;
    }

    public void setOnClickSettingListener(OnClickSettingListener onClickSettingListener) {
        this.mSettingListener = onClickSettingListener;
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        this.mEnabled = z;
        if (z) {
            this.mRightImageView.setAlpha(1.0f);
            this.mTextView.setAlpha(1.0f);
        } else {
            this.mRightImageView.setAlpha(0.5f);
            this.mTextView.setAlpha(0.5f);
        }
    }

    public String getTitle() {
        return this.mTextView.getText().toString();
    }
}
