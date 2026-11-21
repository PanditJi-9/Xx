package com.zte.toolsdk.consumer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import com.zte.toolsdk.common.SharedPrefs;
import java.util.Map;

/* loaded from: classes2.dex */
public class SharedPrefsContentProvider extends ContentProvider {
    private static final String TAG = "SharedPrefsContentProvider";
    private SharedPreferences mSharedPreferences;

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        this.mSharedPreferences = context.createDeviceProtectedStorageContext().getSharedPreferences("tools_shared_prefs", 0);
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (this.mSharedPreferences == null) {
            return null;
        }
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{SharedPrefs.KEY_NAME, SharedPrefs.VALUE_NAME});
        if (selection != null) {
            String string = this.mSharedPreferences.getString(selection, null);
            if (string != null) {
                matrixCursor.addRow(new Object[]{selection, string});
            }
        } else {
            for (Map.Entry<String, ?> entry : this.mSharedPreferences.getAll().entrySet()) {
                if (entry.getValue() instanceof String) {
                    matrixCursor.addRow(new Object[]{entry.getKey(), entry.getValue()});
                }
            }
        }
        return matrixCursor;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        if (this.mSharedPreferences == null) {
            return null;
        }
        String asString = values.getAsString(SharedPrefs.KEY_NAME);
        String asString2 = values.getAsString(SharedPrefs.VALUE_NAME);
        if (asString == null) {
            return null;
        }
        this.mSharedPreferences.edit().putString(asString, asString2).apply();
        getContext().getContentResolver().notifyChange(uri, null);
        Log.d(TAG, "Inserted: " + asString + " = " + asString2);
        return Uri.withAppendedPath(uri, asString);
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return "";
    }
}
