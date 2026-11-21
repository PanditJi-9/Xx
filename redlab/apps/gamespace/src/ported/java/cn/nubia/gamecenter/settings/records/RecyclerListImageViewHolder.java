package cn.nubia.gamecenter.settings.records;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import cn.nubia.gamecenter.settings.R;

/* compiled from: ImagelistAdapter.java */
/* loaded from: classes.dex */
class RecyclerListImageViewHolder extends RecyclerView.ViewHolder {
    ImageView mImageView;
    FrameLayout mItemLayout;

    public RecyclerListImageViewHolder(View view) {
        super(view);
        this.mItemLayout = (FrameLayout) view.findViewById(R.id.gcs_game_image_record_image_layout);
        this.mImageView = (ImageView) view.findViewById(R.id.gcs_game_image_record_image);
    }
}
