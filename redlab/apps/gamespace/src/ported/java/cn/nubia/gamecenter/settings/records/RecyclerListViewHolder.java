package cn.nubia.gamecenter.settings.records;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import cn.nubia.gamecenter.settings.R;

/* compiled from: VideolistAdapter.java */
/* loaded from: classes.dex */
class RecyclerListViewHolder extends RecyclerView.ViewHolder {
    ImageView mImageView;
    FrameLayout mItemLayout;
    ImageView mNewIcon;
    TextView mVideoLabel;
    TextView mVideoLengthTime;

    public RecyclerListViewHolder(View view) {
        super(view);
        this.mItemLayout = (FrameLayout) view.findViewById(R.id.gcs_game_video_record_image_layout);
        this.mImageView = (ImageView) view.findViewById(R.id.gcs_game_video_record_image);
        this.mVideoLengthTime = (TextView) view.findViewById(R.id.gcs_game_video_record_duration);
        this.mVideoLabel = (TextView) view.findViewById(R.id.gcs_game_video_record_label);
        this.mNewIcon = (ImageView) view.findViewById(R.id.gcs_game_video_record_new);
    }
}
