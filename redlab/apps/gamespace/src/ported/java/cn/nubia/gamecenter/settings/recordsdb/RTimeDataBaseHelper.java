package cn.nubia.gamecenter.settings.recordsdb;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/* loaded from: classes.dex */
public class RTimeDataBaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "redmagictime.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "video";
    public static final String VIDEO_APP_NAME = "appName";
    public static final String VIDEO_ISPREVIEW = "isPreview";
    public static final String VIDEO_PACKAGE_NAME = "packageName";
    public static final String VIDEO_PATH = "path";
    private String sql;
    public static Uri REDMAGICTIME_NOTIFY_URI = Uri.parse("content://cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider/video?notify=true");
    public static Uri REDMAGICTIME_NOT_NOTIFY_URI = Uri.parse("content://cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider/video?notify=false");
    public static Uri REDMAGICTIME_QUERY_URI = Uri.parse("content://cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider/video?path");
    private static RTimeDataBaseHelper mInstance = null;

    public RTimeDataBaseHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i) {
        super(context, str, cursorFactory, i);
        this.sql = "CREATE TABLE video(_id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT NOT NULL, isPreview INTEGER, packageName TEXT, appName TEXT)";
    }

    public RTimeDataBaseHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 2);
        this.sql = "CREATE TABLE video(_id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT NOT NULL, isPreview INTEGER, packageName TEXT, appName TEXT)";
    }

    public static synchronized RTimeDataBaseHelper getInstance(Context context) {
        RTimeDataBaseHelper rTimeDataBaseHelper;
        synchronized (RTimeDataBaseHelper.class) {
            if (mInstance == null) {
                mInstance = new RTimeDataBaseHelper(context);
            }
            rTimeDataBaseHelper = mInstance;
        }
        return rTimeDataBaseHelper;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(this.sql);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        while (i < i2) {
            doDbUpgrade(sQLiteDatabase, i);
            i++;
        }
    }

    private void doDbUpgrade(SQLiteDatabase sQLiteDatabase, int i) {
        Log.d("db", "doDbUpgrade() oldVersion : " + i);
        if (i != 1) {
            return;
        }
        addColumn(sQLiteDatabase, "packageName", "text", false, null);
        addColumn(sQLiteDatabase, "appName", "text", false, null);
    }

    private boolean addColumn(SQLiteDatabase sQLiteDatabase, String str, String str2, boolean z, String str3) {
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL("ALTER TABLE video ADD COLUMN " + str + " " + str2 + (z ? " NOT NULL DEFAULT " + str3 : "") + ";");
            sQLiteDatabase.setTransactionSuccessful();
            sQLiteDatabase.endTransaction();
            return true;
        } catch (SQLException unused) {
            sQLiteDatabase.endTransaction();
            return false;
        } catch (Throwable th) {
            sQLiteDatabase.endTransaction();
            throw th;
        }
    }
}
