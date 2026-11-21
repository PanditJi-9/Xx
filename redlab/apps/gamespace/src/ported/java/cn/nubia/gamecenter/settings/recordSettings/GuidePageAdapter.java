package cn.nubia.gamecenter.settings.recordSettings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import cn.nubia.gamecenter.settings.R;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class GuidePageAdapter extends RecyclerView.Adapter {
    private ArrayList<Integer> mDataHint;
    private ArrayList<Integer> mDatas;
    private Callback m_callback;

    public interface Callback {
        void onConfirm();
    }

    public GuidePageAdapter(Callback callback, ArrayList<Integer> arrayList, ArrayList<Integer> arrayList2) {
        this.m_callback = callback;
        this.mDatas = arrayList;
        this.mDataHint = arrayList2;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new GuidePageHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.gcs_guide_page_item_layout, viewGroup, false), this.m_callback);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        int size = this.mDatas.size();
        if (i < 0 || i >= size || size != this.mDataHint.size() || !(viewHolder instanceof GuidePageHolder)) {
            return;
        }
        ((GuidePageHolder) viewHolder).apply(i, size, this.mDatas.get(i).intValue(), this.mDataHint.get(i).intValue());
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mDatas.size();
    }

    class GuidePageHolder extends RecyclerView.ViewHolder {
        private final int GRAY;
        private final int LIGHT;
        private Callback m_callback;
        private Button m_confirmButton;
        private ImageView m_image;
        private ImageView m_indicator_1;
        private ImageView m_indicator_2;
        private TextView m_text;

        public GuidePageHolder(View view, Callback callback) {
            super(view);
            this.LIGHT = R.mipmap.gcs_guide_page_indicator_light;
            this.GRAY = R.mipmap.gcs_guide_page_indicator_gray;
            this.m_callback = callback;
            this.m_image = (ImageView) view.findViewById(R.id.image);
            this.m_text = (TextView) view.findViewById(R.id.text);
            this.m_indicator_1 = (ImageView) view.findViewById(R.id.indicator_1);
            this.m_indicator_2 = (ImageView) view.findViewById(R.id.indicator_2);
            this.m_confirmButton = (Button) view.findViewById(R.id.confirm_button);
        }

        public void apply(int i, int i2, int i3, int i4) {
            this.m_image.setBackgroundResource(i3);
            this.m_text.setText(i4);
            setIndicatorBg(this.m_indicator_1, 0, i);
            setIndicatorBg(this.m_indicator_2, 1, i);
            setConfirm(i == GuidePageAdapter.this.mDatas.size() - 1);
            setImageTopParams(i);
        }

        private void setIndicatorBg(ImageView imageView, int i, int i2) {
            imageView.setBackgroundResource(i == i2 ? this.LIGHT : this.GRAY);
        }

        private void setConfirm(boolean z) {
            if (!z) {
                this.m_confirmButton.setVisibility(8);
            } else {
                this.m_confirmButton.setVisibility(0);
                this.m_confirmButton.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.recordSettings.GuidePageAdapter.GuidePageHolder.1
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        GuidePageHolder.this.m_callback.onConfirm();
                    }
                });
            }
        }

        private void setImageTopParams(int i) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.m_image.getLayoutParams();
            layoutParams.topMargin = GuidePageAdapter.this.dp2px(this.m_image.getContext(), i == 0 ? 0.0f : 25.0f);
            this.m_image.setLayoutParams(layoutParams);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }
}
