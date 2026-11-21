package dev.redlab.gamespace;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog;

/**
 * First on-device proof that an original, sealed UI inflates and renders inside RedLab.
 *
 * <p>It hosts {@code TgkCaseRenameDialog} unchanged. That dialog sets window type 2038
 * (TYPE_APPLICATION_OVERLAY) for its original floating-window use; a normal activity cannot show
 * that type without the overlay permission, so this host switches the window to TYPE_APPLICATION
 * just before showing it. Nothing else about the original dialog or its layout is altered.
 */
public class ProofActivity extends Activity {
    private TgkCaseRenameDialog dialog;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        dialog = new TgkCaseRenameDialog(this, "RedLab");
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
    }
}
