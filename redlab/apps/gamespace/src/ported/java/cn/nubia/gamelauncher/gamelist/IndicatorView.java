package dev.redlab.gamespace.gamelist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public class IndicatorView extends View {
    private int currentIndicator;
    private Handler handler;
    private int indicatorCount;
    private int indicatorWidth;
    private Bitmap mLightBitmap;
    private Bitmap mNormalBitmap;
    private int marginWidth;

    public IndicatorView(Context context) {
        super(context);
        this.indicatorCount = 0;
        this.currentIndicator = 0;
        this.indicatorWidth = 0;
        this.marginWidth = 0;
        this.handler = new Handler() { // from class: dev.redlab.gamespace.gamelist.IndicatorView.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                if (message.what == 18) {
                    IndicatorView.this.invalidate();
                }
            }
        };
    }

    public IndicatorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.indicatorCount = 0;
        this.currentIndicator = 0;
        this.indicatorWidth = 0;
        this.marginWidth = 0;
        this.handler = new Handler() { // from class: dev.redlab.gamespace.gamelist.IndicatorView.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                if (message.what == 18) {
                    IndicatorView.this.invalidate();
                }
            }
        };
        this.mLightBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.indicator_light);
        Bitmap decodeResource = BitmapFactory.decodeResource(getResources(), R.mipmap.indicator_normal);
        this.mNormalBitmap = decodeResource;
        this.indicatorWidth = decodeResource.getWidth();
        this.marginWidth = (int) getResources().getDimension(R.dimen.indicator_margin_width);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int i = this.indicatorWidth;
        int i2 = this.indicatorCount;
        int i3 = (i * i2) + (this.marginWidth * (i2 - 1));
        if (i2 <= 1) {
            return;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        int i4 = (height - this.indicatorWidth) / 2;
        for (int i5 = 0; i5 < this.indicatorCount; i5++) {
            int i6 = ((width - i3) / 2) + ((this.indicatorWidth + this.marginWidth) * i5);
            if (i5 == this.currentIndicator) {
                canvas.drawBitmap(this.mLightBitmap, i6, i4, paint);
            } else {
                canvas.drawBitmap(this.mNormalBitmap, i6, i4, paint);
            }
        }
    }

    public void setIndicatorCount(int i) {
        this.indicatorCount = i;
    }

    public void setCurrentIndicator(int i) {
        this.currentIndicator = i;
        this.handler.sendEmptyMessage(18);
    }
}
