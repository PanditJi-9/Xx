package cn.nubia.gamecenter.settings.basic;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import cn.nubia.gamecenter.settings.R;

/* loaded from: classes.dex */
public class GameRefreshRateActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.gcs_activity_game_refresh_rate);
        initActionBar();
    }

    private void initActionBar() {
        findViewById(R.id.game_refresh_rate_action_bar).setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.basic.GameRefreshRateActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                GameRefreshRateActivity.this.onBackPressed();
            }
        });
    }
}
