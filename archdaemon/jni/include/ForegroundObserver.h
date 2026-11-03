/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#ifndef MAXMANAGER_FOREGROUND_OBSERVER_H
#define MAXMANAGER_FOREGROUND_OBSERVER_H

#include <stdbool.h>
#include <stddef.h>

/*
 * مراقب المقدّمة الأصليّ — **للوضع المستقلّ فقط** (`ro.maxmanager.sepolicy=1`، بلا جذر في التطبيق).
 *
 * الرفيق Java (`AppMonitor`) يحتاج جذرًا لأنه يدخل `ActivityTaskManager` من `app_process`، فلا وجود له بلا جذر.
 * وهذا المراقب يؤدّي منه **ما يستهلكه الخادم فعلًا** من `app_status` (التطبيق المُدار في المقدّمة، الشاشة، البطارية)
 * من مصادر النواة التي يقرؤها الخادم بنطاقه: مجموعة `top-app` في cgroup وعمليّاتها في `/proc`. ويكتب
 * الملفّ نفسه بالصيغة نفسها، فلا يتغيّر في الخادم شيء مما بعده.
 *
 * **حدّه المعلن:** يعرف «هل تطبيق مُدار (من قائمة الألعاب) في المقدّمة؟» ولا يعرف اسم/اسم عرض كل تطبيق
 * آخر؛ ولا يكتب `battery_saver` و`zen_mode` (0). وما يفعله الرفيق وراء ذلك (ضبط Settings لكل تطبيق، معدّل التحديث،
 * اللوحة الجانبيّة) يبقى للرفيق ويحتاج جذرًا. وإن أمسك الرفيق قفله (جذر متاح) سكت المراقب فلا يتنازعان الملفّ.
 */

bool fg_package_from_cmdline(const char* cmdline, char* out, size_t out_size);
bool fg_gamelist_contains(const char* json, const char* package);
int fg_format_status(char* out, size_t size, const char* package, int pid, int uid, int screen_awake,
                     int battery_level, int is_charging, const char* switch_id);
void start_foreground_observer(const char* java_lock_path);

#endif /* MAXMANAGER_FOREGROUND_OBSERVER_H */
