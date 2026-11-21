package dev.redlab.gamespace.gamecontrolpanel;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.util.LogUtil;

/* loaded from: classes.dex */
public class TgkCaseRenameDialog extends Dialog {
    private static final String DB_GUIDE = "settings_gcs_game_guide";
    private static final String TAG = "TgkCaseRenameDialog";
    View.OnClickListener listener;
    private String mCaseName;
    private Context mContext;
    private OnDismissListener mDismissListener;
    private EditText mTgkCaseNameEditor;

    public interface OnDismissListener {
        int onDismiss(String str);
    }

    public TgkCaseRenameDialog(Context context, String str) {
        super(context);
        this.listener = new View.OnClickListener() { // from class: dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.1
            /* JADX WARN: Removed duplicated region for block: B:10:? A[RETURN, SYNTHETIC] */
            /* JADX WARN: Removed duplicated region for block: B:7:0x003e  */
            @Override // android.view.View.OnClickListener
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void onClick(android.view.View r3) {
                /*
                    r2 = this;
                    int r3 = r3.getId()
                    r0 = 2131363447(0x7f0a0677, float:1.8346703E38)
                    java.lang.String r1 = "ResourceSettings"
                    if (r0 != r3) goto L31
                    java.lang.String r3 = "confirm"
                    dev.redlab.gamespace.util.LogUtil.i(r1, r3)
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog r3 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.this
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog$OnDismissListener r3 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.access$000(r3)
                    if (r3 == 0) goto L3b
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog r3 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.this
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog$OnDismissListener r3 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.access$000(r3)
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog r0 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.this
                    android.widget.EditText r0 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.access$100(r0)
                    android.text.Editable r0 = r0.getText()
                    java.lang.String r0 = r0.toString()
                    int r3 = r3.onDismiss(r0)
                    goto L3c
                L31:
                    r0 = 2131363446(0x7f0a0676, float:1.8346701E38)
                    if (r0 != r3) goto L3b
                    java.lang.String r3 = "cancel"
                    dev.redlab.gamespace.util.LogUtil.i(r1, r3)
                L3b:
                    r3 = 0
                L3c:
                    if (r3 != 0) goto L43
                    dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog r2 = dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.this
                    r2.dismiss()
                L43:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: dev.redlab.gamespace.gamecontrolpanel.TgkCaseRenameDialog.AnonymousClass1.onClick(android.view.View):void");
            }
        };
        this.mContext = context;
        this.mCaseName = str;
        initView();
    }

    private void initView() {
        LogUtil.d(TAG, "in initView");
        setContentView(R.layout.control_panel_tgk_rename_layout);
        setAttributes();
        this.mTgkCaseNameEditor = (EditText) findViewById(R.id.tgk_case_modify_name);
        TextView textView = (TextView) findViewById(R.id.tgk_rename_confirm);
        TextView textView2 = (TextView) findViewById(R.id.tgk_rename_cancel);
        this.mTgkCaseNameEditor.setText(this.mCaseName);
        this.mTgkCaseNameEditor.selectAll();
        textView.setOnClickListener(this.listener);
        textView2.setOnClickListener(this.listener);
    }

    private void setAttributes() {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = 1080;
        attributes.height = 648;
        attributes.type = 2038;
        window.setAttributes(attributes);
        window.setBackgroundDrawable(new ColorDrawable(0));
    }

    @Override // android.app.Dialog
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.mDismissListener = onDismissListener;
    }
}
