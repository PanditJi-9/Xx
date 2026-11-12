/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.settings

import android.content.Context
import nd.max.island.core.IslandSettings
import nd.max.core.persist.PersistStore
import nd.max.ui.util.Dex2oatUtil
import nd.max.ui.util.RebootManager

/**
 * «إعادة كل شيء إلى الوضع الافتراضي» — **اختيارات التطبيق**، لا بيانات المستخدم ولا خصائص النظام.
 *
 * النطاق مقصود ومكتوب: اختيارات الواجهة ([SettingsPreference]) · تطبيق التعديلات بعد الإقلاع
 * وإقراره الخطير ([PersistStore]) · شكل الجُزُر ([IslandSettings.resetAll]) · اختيارات
 * dex2oat والإقلاع ([Dex2oatUtil] · [RebootManager]). وما كان ملكًا لنظام التشغيل (خصائص
 * `persist.sys.*`) أو لملفّات المستخدم لا يُمسّ: تصفيره من هنا كان سيمحو إعدادًا لا نملكه.
 */
fun resetAppToDefaults(context: Context) {
    SettingsPreference.getInstance(context).resetToDefaults()
    PersistStore.setEnabled(context, false)
    PersistStore.setDangerousAutoEnabled(context, false)
    IslandSettings.resetAll()
    Dex2oatUtil.resetAll()
    RebootManager.resetAll()
}
