package cn.nubia.gamecenter.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;

/* loaded from: classes.dex */
public class NumberTextView extends TextView {
    public static final Property<NumberTextView, Float> CUST_NUMBER = new FloatProperty<NumberTextView>(AnimatorHelper.Item.CUST_MOVE_Y) { // from class: cn.nubia.gamecenter.settings.widget.NumberTextView.1
        @Override // android.util.FloatProperty
        public void setValue(NumberTextView numberTextView, float f) {
            numberTextView.setNumber(f);
        }

        @Override // android.util.Property
        public Float get(NumberTextView numberTextView) {
            return Float.valueOf(numberTextView.getNumber());
        }
    };
    private static final String TAG = "NumberTextView";
    private float m_number;

    public NumberTextView(Context context) {
        this(context, null);
    }

    public NumberTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.m_number = -1.0f;
    }

    public NumberTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.m_number = -1.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNumber(float f) {
        int i = (int) f;
        if (this.m_number == i) {
            return;
        }
        this.m_number = f;
        if (i < 0) {
            setText("");
        } else {
            setText(Integer.toString((int) f));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getNumber() {
        return this.m_number;
    }
}
