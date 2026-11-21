package cn.nubia.gamecenter.settings.records.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/* loaded from: classes.dex */
public class DateUtils {
    public static String format(long j, boolean z) {
        if (!z) {
            j *= 1000;
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Long.valueOf(j));
    }

    public static String getFormatDuration(int i) {
        return new SimpleDateFormat("mm:ss", Locale.getDefault()).format(Integer.valueOf(i));
    }
}
