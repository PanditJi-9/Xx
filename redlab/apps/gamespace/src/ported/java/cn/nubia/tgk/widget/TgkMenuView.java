package cn.nubia.tgk.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkMenuView extends RelativeLayout {
    private static final String TAG = "TgkMenuView";
    private View mFirstView;
    private OnClickListener mListener;
    private View mSecondView;
    private TextView mTextView;
    private View mView;
    private View.OnClickListener mViewListener;

    public interface OnClickListener {
        void onClick(View view);
    }

    public TgkMenuView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TgkMenuView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mViewListener = new View.OnClickListener() { // from class: cn.nubia.tgk.widget.TgkMenuView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                TgkMenuView.this.mListener.onClick(TgkMenuView.this.mFirstView);
            }
        };
        Log.d(TAG, "in TgkMenuView");
        initView(context, attributeSet);
    }

    private void initView(Context context, AttributeSet attributeSet) {
        View inflate = ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.tgk_menu_view_layout, (ViewGroup) this, true);
        this.mFirstView = inflate;
        this.mSecondView = inflate.findViewById(R.id.tgk_menu);
        this.mTextView = (TextView) this.mFirstView.findViewById(R.id.tgk_menu_title);
        this.mView = this.mFirstView.findViewById(R.id.tgk_icon);
        Log.d(TAG, "in initView");
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.TgkMenuView);
        setFirstBg(obtainStyledAttributes.getDrawable(0));
        setSecondBg(obtainStyledAttributes.getDrawable(1));
        setIcon(obtainStyledAttributes.getDrawable(2));
        obtainStyledAttributes.recycle();
        this.mSecondView.setOnClickListener(this.mViewListener);
    }

    public void setFirstBg(Drawable drawable) {
        this.mFirstView.setBackground(drawable);
    }

    public void setFirstBackgroundResource(int i) {
        this.mFirstView.setBackgroundResource(i);
    }

    public void setSecondBg(Drawable drawable) {
        this.mSecondView.setBackground(drawable);
    }

    public void setSecondBackgroundResource(int i) {
        this.mSecondView.setBackgroundResource(i);
    }

    public void setIcon(Drawable drawable) {
        this.mView.setBackground(drawable);
    }

    public void setTitle(String str) {
        this.mTextView.setText(str);
    }

    public void setMarquee(boolean z) {
        this.mTextView.setSelected(z);
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.mSecondView.setEnabled(z);
        this.mTextView.setEnabled(z);
        this.mView.setEnabled(z);
    }

    public int getClickViewWidth() {
        return this.mSecondView.getWidth();
    }

    public View getClickView() {
        return this.mSecondView;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mListener = onClickListener;
    }
}
