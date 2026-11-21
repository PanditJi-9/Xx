package cn.nubia.plug.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import dev.redlab.gamespace.R;
import cn.nubia.plug.PlugData;

/* loaded from: classes2.dex */
public class BaseFragment extends Fragment {
    protected static final int SINGLE_ADAPTER_DEVICE_NUMBER = 4;
    protected static final int SINGLE_SUPPORT_GAME_NUMBER = 3;
    public static final String TAG = "BaseFragment";
    protected static final int TYPE_ADAPTER_DEVICE = 11;
    protected static final int TYPE_SUPPORT_GAME = 10;
    private static boolean mIsDimensionNarrow;
    protected LinearLayout mAdapterDevicesContainer;
    private TextView mContent;
    protected PlugData mData;
    private LinearLayout mDimensionLayout;
    private LinearLayout mDimensionLine2Layout;
    protected RatingBar[] mDimensionRatingsContainer;
    private int mLayoutRes = -1;
    protected LinearLayout mSupportGameContainer;
    private int mSupportGameItemHeightPx;
    private int mSupportGameItemLeftPx;
    private int mSupportGameTextSizePx;
    protected TextView mSupportGameTitle;
    private TextView mTitle;

    private boolean isVisibility(int i, int i2, int i3, int i4) {
        return (i * i4) + i2 <= i3;
    }

    protected void canAddView() {
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(this.mLayoutRes, viewGroup, false);
        this.mSupportGameTextSizePx = getResources().getDimensionPixelSize(R.dimen.plug_detail_game_text_size);
        this.mSupportGameItemHeightPx = getResources().getDimensionPixelSize(R.dimen.plug_detail_game_rect_height);
        this.mSupportGameItemLeftPx = getResources().getDimensionPixelSize(R.dimen.plug_detail_game_margin_start);
        this.mSupportGameContainer = (LinearLayout) inflate.findViewById(R.id.support_games);
        this.mSupportGameTitle = (TextView) inflate.findViewById(R.id.text_support_game_title);
        this.mAdapterDevicesContainer = (LinearLayout) inflate.findViewById(R.id.adapter_devices);
        this.mDimensionLayout = (LinearLayout) inflate.findViewById(R.id.games_plug_score);
        this.mDimensionLine2Layout = (LinearLayout) inflate.findViewById(R.id.games_plug_score_line2);
        this.mDimensionRatingsContainer = new RatingBar[]{(RatingBar) inflate.findViewById(R.id.plug_rating_strategy), (RatingBar) inflate.findViewById(R.id.plug_rating_operate), (RatingBar) inflate.findViewById(R.id.plug_rating_advanced), (RatingBar) inflate.findViewById(R.id.plug_rating_difficulty)};
        updateDimensionLayout();
        this.mTitle = (TextView) inflate.findViewById(R.id.title);
        this.mContent = (TextView) inflate.findViewById(R.id.content);
        this.mTitle.setText(this.mData.getTitleId());
        this.mContent.setText(this.mData.getContentId());
        canAddView();
        return inflate;
    }

    public void setLayoutRes(int i) {
        this.mLayoutRes = i;
    }

    public void addSupportGames(int i) {
        addView(i, 3, this.mSupportGameContainer, 10);
    }

    public void addAdapterDevices(int i) {
        addView(i, 4, this.mAdapterDevicesContainer, 11);
    }

    private void addView(int i, int i2, ViewGroup viewGroup, int i3) {
        int calculateRow = calculateRow(i, i2);
        Log.d(TAG, "rowT:" + calculateRow);
        int i4 = 0;
        while (i4 < calculateRow) {
            LinearLayout initRowG = initRowG(i4 == 0);
            for (int i5 = 1; i5 <= i2; i5++) {
                addViewToRow(i4, i5, i, i2, initRowG, i3);
            }
            viewGroup.addView(initRowG);
            i4++;
        }
    }

    public void addDimensionRatings() {
        this.mData.getDimensionRatings();
        for (int i = 0; i < 4; i++) {
            this.mDimensionRatingsContainer[i].setRating(this.mData.getDimensionRatings()[i]);
        }
    }

    private LinearLayout initRowG(boolean z) {
        LinearLayout linearLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        if (!z) {
            layoutParams.topMargin = 18;
        }
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(0);
        return linearLayout;
    }

    private void addViewToRow(int i, int i2, int i3, int i4, LinearLayout linearLayout, int i5) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.gravity = 16;
        TextView textView = new TextView(getContext());
        textView.setMinHeight(this.mSupportGameItemHeightPx);
        setAppearance(textView, isVisibility(i, i2, i3, i4), i, i2, i4, i5);
        setMarginStart(i2, layoutParams);
        textView.setLayoutParams(layoutParams);
        linearLayout.addView(textView);
    }

    private void setAppearance(TextView textView, boolean z, int i, int i2, int i3, int i4) {
        textView.setVisibility(z ? 0 : 4);
        textView.setTextColor(-1);
        textView.setBackground(getContext().getDrawable(R.drawable.plug_shape_rectangle));
        if (i4 == 10) {
            textView.setTextSize(this.mSupportGameTextSizePx);
            textView.setAlpha(0.85f);
        } else if (i4 == 11) {
            textView.setTextSize(8.0f);
            textView.setAlpha(0.5f);
        }
        if (z) {
            int i5 = (i2 - 1) + (i * i3);
            if (i4 == 10) {
                textView.setText(this.mData.getSupportGames()[i5]);
            } else if (i4 == 11) {
                textView.setText(this.mData.getAdaptedDevices()[i5]);
            }
        }
        textView.setGravity(17);
    }

    private void setMarginStart(int i, LinearLayout.LayoutParams layoutParams) {
        if (i != 1) {
            layoutParams.setMarginStart(this.mSupportGameItemLeftPx);
        }
    }

    private int calculateRow(int i, int i2) {
        if (i % i2 != 0) {
            return (i / i2) + 1;
        }
        return i / i2;
    }

    public static void setIsDimensionNarrow(boolean z) {
        mIsDimensionNarrow = z;
    }

    private void updateDimensionLayout() {
        if (mIsDimensionNarrow) {
            this.mDimensionLayout.setOrientation(1);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mDimensionLine2Layout.getLayoutParams();
            layoutParams.setMarginStart(0);
            this.mDimensionLine2Layout.setLayoutParams(layoutParams);
        }
    }
}
