package cn.nubia.gamecenter.settings.recordsdb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import cn.nubia.gamecenter.settings.records.utils.ListUtils;
import java.util.Arrays;
import java.util.HashSet;

/* loaded from: classes.dex */
public class RedMagicTimeProvider extends ContentProvider {
    public static final String AUTHORITY = "cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider";
    public static final String NOTIFY = "notify";
    public static final String TABLE_VIDEO_NAME = "video";
    public static final String VIDEO_APP_NAME = "appName";
    public static final String VIDEO_ISPREVIEW = "isPreview";
    public static final String VIDEO_PACKAGE_NAME = "packageName";
    public static final String VIDEO_PATH = "path";
    private static final HashSet<String> WHITE_APP_SET = new HashSet<>(Arrays.asList("dev.redlab.gamespace", "cn.nubia.gamehighlights"));
    private String[] columnsToFetch = {"path", "isPreview", "packageName", "appName"};
    private volatile Context context;
    private RTimeDataBaseHelper rTimeDataBaseHelper;

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        this.rTimeDataBaseHelper = RTimeDataBaseHelper.getInstance(getContext());
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (!checkPermissions()) {
            return null;
        }
        SQLiteDatabase writableDatabase = this.rTimeDataBaseHelper.getWritableDatabase();
        if (uri.equals(RTimeDataBaseHelper.REDMAGICTIME_QUERY_URI)) {
            return writableDatabase.query(true, "video", strArr, null, null, null, null, str2, null, null);
        }
        return writableDatabase.query("video", strArr, str, strArr2, null, null, null);
    }

    private boolean checkPermissions() {
        this.context = getContext();
        return WHITE_APP_SET.contains(ListUtils.getPackageNameFromUid(this.context, Binder.getCallingUid()));
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase writableDatabase;
        if (!checkPermissions() || (writableDatabase = this.rTimeDataBaseHelper.getWritableDatabase()) == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("path = '");
        sb.append(contentValues.getAsString("path").trim().replace("'", "''")).append("'");
        if (!TextUtils.isEmpty(sb.toString())) {
            Cursor query = writableDatabase.query("video", null, sb.toString(), null, null, null, null);
            if (query == null || !query.moveToFirst()) {
                writableDatabase.insert("video", null, contentValues);
            }
            if (query != null) {
                query.close();
            }
            writableDatabase.close();
            sendNotify(uri);
        }
        return null;
    }

    private void sendNotify(Uri uri) {
        if (uri == null || !"true".equals(uri.getQueryParameter("notify"))) {
            return;
        }
        getContext().getContentResolver().notifyChange(uri, null);
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        SQLiteDatabase writableDatabase;
        if (!checkPermissions() || (writableDatabase = this.rTimeDataBaseHelper.getWritableDatabase()) == null) {
            return 0;
        }
        writableDatabase.delete("video", str, strArr);
        writableDatabase.close();
        sendNotify(uri);
        return 0;
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x0037, code lost:
    
        if (r10 != null) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x004a, code lost:
    
        sendNotify(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x004d, code lost:
    
        return 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0047, code lost:
    
        r10.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0045, code lost:
    
        if (r10 != null) goto L22;
     */
    @Override // android.content.ContentProvider
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public int update(android.net.Uri r12, android.content.ContentValues r13, java.lang.String r14, java.lang.String[] r15) {
        /*
            r11 = this;
            boolean r0 = r11.checkPermissions()
            r1 = 0
            if (r0 != 0) goto L8
            return r1
        L8:
            cn.nubia.gamecenter.settings.recordsdb.RTimeDataBaseHelper r0 = r11.rTimeDataBaseHelper
            android.database.sqlite.SQLiteDatabase r0 = r0.getWritableDatabase()
            r10 = 0
            java.lang.String r3 = "video"
            r2 = 1
            java.lang.String[] r4 = new java.lang.String[r2]     // Catch: java.lang.Throwable -> L3a java.lang.Exception -> L3c
            java.lang.String r2 = "path"
            r4[r1] = r2     // Catch: java.lang.Throwable -> L3a java.lang.Exception -> L3c
            r8 = 0
            r9 = 0
            r7 = 0
            r2 = r0
            r5 = r14
            r6 = r15
            android.database.Cursor r10 = r2.query(r3, r4, r5, r6, r7, r8, r9)     // Catch: java.lang.Throwable -> L3a java.lang.Exception -> L3c
            if (r10 == 0) goto L32
            boolean r2 = r10.moveToFirst()     // Catch: java.lang.Throwable -> L3a java.lang.Exception -> L3c
            if (r2 == 0) goto L32
            java.lang.String r2 = "video"
            r0.update(r2, r13, r14, r15)     // Catch: java.lang.Throwable -> L3a java.lang.Exception -> L3c
        L32:
            if (r0 == 0) goto L37
            r0.close()
        L37:
            if (r10 == 0) goto L4a
            goto L47
        L3a:
            r11 = move-exception
            goto L4e
        L3c:
            r13 = move-exception
            r13.printStackTrace()     // Catch: java.lang.Throwable -> L3a
            if (r0 == 0) goto L45
            r0.close()
        L45:
            if (r10 == 0) goto L4a
        L47:
            r10.close()
        L4a:
            r11.sendNotify(r12)
            return r1
        L4e:
            if (r0 == 0) goto L53
            r0.close()
        L53:
            if (r10 == 0) goto L58
            r10.close()
        L58:
            throw r11
        */
        throw new UnsupportedOperationException("Method not decompiled: cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider.update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[]):int");
    }
}
