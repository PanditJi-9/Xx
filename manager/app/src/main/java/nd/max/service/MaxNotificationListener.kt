/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import nd.max.ui.util.IslandLive
import nd.max.ui.util.IslandModel
import nd.max.ui.util.IslandNotice

/**
 * مستمع الإشعارات — **مصدر الجزيرة الديناميكية العابر** (`DYNAMIC-ISLAND-01`).
 *
 * ### وما يُمرَّر وما يُسكَت عنه
 *
 * الكبسولة تعرض **الحدث**: إشعارًا وقع الآن. فما ليس حدثًا — أو ما هو حدث في مكان آخر —
 * يُسكَت عنه عند المصدر لا في الرسم:
 *
 * | يُسكَت عنه | لماذا |
 * | --- | --- |
 * | إشعارات التطبيق نفسه | `MaxManager` يعرض كبسولته بنفسه، وعرضها لنفسها حلقة تُقرأ عطبًا |
 * | `isOngoing` | حالة دائمة لا حدث: التحميل الجاري والشحن والإشعارات الأمامية |
 * | ملخّص المجموعة (`FLAG_GROUP_SUMMARY`) | عنوان فارغ معناه «هناك أكثر» — يُقرأ كإشعار مجهول |
 * | إشعارات الوسائط (`EXTRA_MEDIA_SESSION`) | لها مصدرها الأوّل في الكبسولة (جلسة الوسائط)، وتكرارها نسخة ثانية |
 * | بلا عنوان | لا شيء يُعرض: كبسولة بلا نصّ أسوأ من لا كبسولة |
 *
 * **والأهمية تُقرأ من ترتيب النظام** (`Ranking.importance`) لا من حقل مهجور، وهي التي تحدّد
 * مدّة العرض في `IslandModel`. وإشعار لا يجد له ترتيبًا يُعامَل كعادي — لا يُسقط.
 *
 * **والحدّ المعلن:** اسم التطبيق يُقرأ من `PackageManager`، وقد يمنعه حجب رؤية الحزم (`<queries>`)
 * فيعود `null` — والكبسولة تعرض العنوان وحده. وهذا هو السلوك الصحيح: ما لا يُقرأ لا يُخترع.
 */
class MaxNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        // **أول شرط: هل للجزيرة مالك؟** المستمع يعمل بصلاحيته ما دامت ممنوحة، ولو كانت الجزيرة
        // متوقّفة لكان كل إشعار يُكلّف قراءة اسم حزمة من `PackageManager` وترتيبًا من النظام ثم
        // يضيع في `recompute()` بلا مستهلك (تكملة `PERF-AUDIT-01`). و`IslandLive.start()` يصفّر
        // المصادر عند الإقلاع، فالإشعار الذي يصل قبل الملك لا يُفقد من عرضٍ قائم أصلًا.
        if (!IslandLive.isLive) return
        if (posted.packageName == packageName) return
        if (posted.isOngoing) return
        val notification = posted.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras ?: return
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        if (title.isNullOrEmpty()) return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        IslandLive.pushNotice(
            IslandNotice(
                appLabel = appLabelOf(posted.packageName),
                title = title,
                text = text?.takeIf { it.isNotEmpty() && it != title },
                // زمن نشر النظام لا زمن الوصول: إشعار نُشر قبل لحظة يبقى له من عمره ما بقي.
                postedAtMs = posted.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
                importance = importanceOf(posted.key),
                // الحزمة تُمرَّر ليُقرأ شعار التطبيق لحظة الرسم — وهي نصّ لا صورة، فالنموذج يبقى نقيًّا.
                packageName = posted.packageName
            )
        )
    }

    private fun appLabelOf(pkg: String): String? = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun importanceOf(key: String): Int = runCatching {
        // `RankingMap` لا يملك إلا النسخة بمعلَّمَي الإخراج (مقيس في `android.jar`):
        // `getRanking(String, Ranking)` — تُملأ النسخة وتُعاد `true` عند النجاح.
        val ranking = NotificationListenerService.Ranking()
        if (currentRanking?.getRanking(key, ranking) == true) ranking.importance else null
    }.getOrNull() ?: IslandModel.IMPORTANCE_DEFAULT

    companion object {

        /** هل مُنح التطبيق صلاحية الوصول إلى الإشعارات؟ تُقرأ ولا تُفترض. */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** وجهة المنح — تُفتح من الشاشة عند الحاجة، ولا يُفترض المنح. */
        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
