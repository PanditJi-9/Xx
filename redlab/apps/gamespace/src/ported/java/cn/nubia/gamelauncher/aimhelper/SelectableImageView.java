package dev.redlab.gamespace.aimhelper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public class SelectableImageView extends FrameLayout {
    private Drawable bg;
    private boolean isSelected;
    private ImageView mIvBg;
    private ImageView mIvSrc;
    private TextView mTv;
    private String text;

    public SelectableImageView(Context context) {
        this(context, null, 0);
    }

    public SelectableImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SelectableImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SelectableImageView);
        this.bg = obtainStyledAttributes.getDrawable(0);
        this.isSelected = obtainStyledAttributes.getBoolean(1, false);
        this.text = obtainStyledAttributes.getString(2);
        obtainStyledAttributes.recycle();
        init(context);
    }

    private void init(Context context) {
        View inflate = inflate(context, R.layout.selectable_imageview_layout, this);
        this.mIvBg = (ImageView) inflate.findViewById(R.id.iv_bg);
        this.mIvSrc = (ImageView) inflate.findViewById(R.id.iv_select);
        this.mTv = (TextView) inflate.findViewById(R.id.tv_text);
        Drawable drawable = this.bg;
        if (drawable != null) {
            this.mIvBg.setImageDrawable(drawable);
        }
        this.mIvSrc.setVisibility(this.isSelected ? 0 : 8);
        if (!TextUtils.isEmpty(this.text)) {
            this.mTv.setText(this.text);
            this.mTv.setVisibility(0);
        } else {
            this.mTv.setVisibility(8);
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setSelect(boolean z) {
        this.isSelected = z;
        this.mIvSrc.setVisibility(z ? 0 : 4);
        postInvalidate();
    }
}
