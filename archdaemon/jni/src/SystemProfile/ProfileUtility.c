/*
 * Copyright (C) 2024-2025 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <MaxManager.h>
#include <time.h>

// Function pointers initialized to default handlers
bool (*get_screenstate)(SystemStateCache*) = get_screenstate_normal;
bool (*get_low_power_state)(SystemStateCache*) = get_low_power_state_normal;

/**
 * @brief Retrieves the current screen wakefulness state from cache.
 * @param cache Pointer to the system state cache.
 * @return true if screen was awake, false otherwise.
 */
bool get_screenstate_normal(SystemStateCache* cache) {
    if (!cache)
        return true; // Default to awake if cache is missing
    return cache->screen_awake != 0;
}

/**
 * @brief Checks if the device's Battery Saver mode is enabled from cache.
 * @param cache Pointer to the system state cache.
 * @return true if Battery Saver is enabled, false otherwise.
 */
bool get_low_power_state_normal(SystemStateCache* cache) {
    if (!cache)
        return false;
    return cache->battery_saver != 0;
}

/**
 * @brief Switch to specified performance profile.
 * @param profile 0 for perfcommon, 1 for performance, 2 for balanced, 3 for powersave.
 */
static void set_per_app_governor_isolation(bool enabled) {
    __system_property_set("sys.maxmanager.perapp.governor_isolation", enabled ? "1" : "0");
}

/**
 * @brief هل يُعاد تطبيق ميزان الترددات (freqoffset) في هذه الدورة؟
 *
 * **العطب المقيس الذي وُلد منه هذا القرار (جولة الحرارة واللاغ):** كان `System.c` ينادي
 * `sys.maxmanager-profilesettings applyfreqbalance` في **كل** دورة من دورات الخادم ما دامت
 * الشاشة مضاءة والوضع متوازنًا/موفّرًا والميزان مُفعَّلًا — والدورات تُطلقها أحداث inotify على
 * مجلّد الإعداد، والرفيق Java يكتب فيه `app_status`/`background_apps` باستمرار. فكل حدث ملف
 * ⇒ ولادة عملية كاملة لثنائية Rust ⇒ وهي تكتب `scaling_max_freq` و`scaling_min_freq` لكل
 * نواة (‏١٦ كتابة و‏٤٨ `chmod` في الأسوأ) — **والقيم لا تتغيّر**. وهذا بعينه ما يُقاس على
 * الجهاز حرارةً ولاغًا: كل كتابة تُشغّل سياسة التردد في النواة من جديد.
 *
 * **فصار «بالحدث لا بالإيقاع»:** يُعاد التطبيق عند حدث يغيّر الحالة فعلًا — إضاءة الشاشة،
 * أو تغيّر الوضع، أو تغيّر طلب الميزان نفسه — لا عند كل دورة.
 *
 * **وحدوده المُعلنة:** هذا حرس على **النداء** لا على القيمة (والقيمة يحرسها
 * `value_already_applied` في `binprofiles/src/utils/mod.rs`)، فهو لا يمنع كتابة لازمة،
 * ولا يُسقط حالة: كل ما كان يُطبَّق عند تغيّر حقيقي يبقى يُطبَّق.
 *
 * @param screen_on           الشاشة مضاءة الآن (القراءة الفعليّة لا حالة السماح).
 * @param balanced_or_eco     الوضع الحالي متوازن أو موفّر (الوضعان اللذان يعنيهما الميزان).
 * @param screen_turned_on    الشاشة **أُضيئت في هذه الدورة** (انتقال 0 ⇒ 1 مقيس).
 * @param request_changed     طلب الميزان تغيّر منذ آخر تطبيق (أو لم يُطبَّق بعد).
 */
bool should_apply_freq_balance(bool screen_on, bool balanced_or_eco, bool screen_turned_on,
                               bool request_changed) {
    if (!screen_on || !balanced_or_eco)
        return false;
    return screen_turned_on || request_changed;
}

/**
 * @brief هل تستحقّ كتابةٌ جديدة لـ`app_status` إعادةَ فحصٍ للملفّ الشخصيّ؟
 *
 * **العطب المقيس الذي وُلد منه هذا القرار (جولة الحرارة واللاغ):** كان كل حدث `IN_MODIFY`
 * على `app_status` يضبط `need_profile_checkup = true` — بلا مقارنة. والرفيق Java يكتب الملفّ
 * عند **كل تغيّر في محتواه**: نسبة البطارية (‏١٪)، ومعدّل الشاشة، واسم التطبيق، وحالة الصمت،
 * و`perapp_active`. فكل واحدة منها كانت تُعيد دورة فحص كاملة: قراءة `app_status`، و`get_gamestart`
 * (‏`get_visible_package` + قفل + مسح قائمة الإعدادات)، ثم العودة إلى `apply_base_profile`.
 * وما لا يتغيّر فيها شيئًا يخصّ الملفّ هو **التطبيق المقيم** — وهو وحده ما يحدّد `gamestart`.
 *
 * **والقاعدة:** إعادةُ الفحص تُطلب عند **تغيّر الحزمة المقيمة** أو **معرّف الجلسة**، لا عند
 * تغيّر أيّ حقل آخر. ولا يُسقط ذلك حالة: البطارية والوضع يُقرآن من الكاش في كل دورة على أيّ
 * حال، و`perapp_active` يُقرأ عند بناء القرار نفسه.
 *
 * **وحدّه المُعلن:** المقارنة على حقلين لا على الملفّ كلّه؛ فمن يغيّر إعدادًا يخصّ `gamestart`
 * دون تغيير الحزمة أو الجلسة لا يُطلب له فحص (ولا يوجد اليوم حقل كهذا في العقد — يُقاس في
 * `fixtures/contracts/`).
 *
 * @param previous_focused الحزمة المقيمة قبل القراءة (‏`""` عند أول قراءة).
 * @param previous_switch  معرّف الجلسة قبل القراءة.
 * @param focused          الحزمة المقيمة بعد القراءة.
 * @param switch_id        معرّف الجلسة بعد القراءة.
 * @return true حين يجب أن يُعاد الفحص.
 */
bool app_status_needs_profile_checkup(const char* previous_focused, const char* previous_switch,
                                      const char* focused, const char* switch_id) {
    if (!focused || !focused[0])
        return false;
    if (!previous_focused || strcmp(previous_focused, focused) != 0)
        return true;
    if (previous_switch && switch_id && strcmp(previous_switch, switch_id) != 0)
        return true;
    return false;
}

void run_profiler(const int profile) {
    is_kanged();

    time_t t = time(NULL);
    struct tm tm = *localtime(&t);
    char time_str[64];

    snprintf(time_str, sizeof(time_str), "%02d:%02d:%02d", tm.tm_hour, tm.tm_min, tm.tm_sec);

    if (profile == 1) {
        // Assuming game_pids and gamestart are still managed globally or passed correctly
        pid_t main_pid = (game_pid_count > 0) ? game_pids[0] : 0;
        // وضع الأداء صار يُطبَّق بلا لعبة أيضًا (اختيار أساس/إقلاع)، و`gamestart`
        // يكون NULL حينها؛ لا يُمرَّر NULL إلى `%s` بل يُكتب نفس شكل فرع «بلا هدف».
        write2file(GAME_INFO, false, false, "%s %d %d\nTime: %s\n",
                   gamestart ? gamestart : "NULL", main_pid, uidof(main_pid), time_str);
    } else {
        write2file(GAME_INFO, false, false, "NULL 0 0\nTime: %s\n", time_str);
    }

    write2file(PROFILE_MODE, false, false, "%d\n", profile);
    write2file(PROFILE_MODE_APP, false, false, "%d\n", profile);

    // Explicit per-app CPU/GPU governors must remain authoritative regardless of
    // whether the global mode is Performance, Balanced, or Eco. The profile binary
    // consumes this transient property and skips its own governor write when set.
    bool per_app_governor = gamestart != NULL &&
                            (!IS_DEFAULT(opts.gpu_profile) || !IS_DEFAULT(opts.thermal_profile) ||
                             !IS_DEFAULT(opts.cpu_governor) || !IS_DEFAULT(opts.gpu_governor) ||
                             !IS_DEFAULT(opts.gpu_max_freq) || opts.cpu_policy_controls[0] != '\0');

    /*
     * The one-shot CLI (`sys.maxmanager-service -p/-profile`) is a **separate process**:
     * `gamestart` and `opts` are its own empty globals even while the daemon is running a
     * Per-App session. Recomputing the flag from them published 0 and let `binprofiles`
     * overwrite the live per-app governor and frequency ceiling -- the reported "profile
     * jumps back after a tap in the game panel" (the daemon's 10 s drift repair then made
     * it jump again). With no local target this process cannot know better than the owner,
     * so an already-published 1 is preserved; the daemon is the only writer of 1 and clears
     * it on every session end and at its own startup.
     */
    if (!per_app_governor && gamestart == NULL) {
        char isolated[PROP_VALUE_MAX] = {0};
        if (__system_property_get("sys.maxmanager.perapp.governor_isolation", isolated) > 0 &&
            isolated[0] == '1') {
            per_app_governor = true;
            log_zenith(LOG_INFO, "EVENT=PERAPP_ISOLATION_PRESERVED reason=no_local_target profile=%d",
                       profile);
        }
    }

    set_per_app_governor_isolation(per_app_governor);

    // Suggestion for future: Replace systemv with native property setting for performance
    (void)systemv("sys.maxmanager-profilesettings %d", profile);
}

/**
 * @brief Searches for the currently visible app that has a Per-App configuration entry.
 * @param options Pointer to extract the configured Per-App options.
 * @param cache Pointer to system state cache to determine visibility.
 * @return Malloc-ed string containing package name, or NULL. Caller must free.
 */
char* get_gamestart(GameConfig* options, SystemStateCache* cache) {
    char* pkg = get_visible_package(cache);
    if (!pkg)
        return NULL;

    pthread_mutex_lock(&cache_mutex);

    if (g_game_cache == NULL || g_game_cache_count == 0) {
        pthread_mutex_unlock(&cache_mutex);
        free(pkg);
        return NULL;
    }

    bool match_found = false;

    for (int i = 0; i < g_game_cache_count; i++) {
        if (strcmp(g_game_cache[i].package, pkg) == 0) {
            match_found = true;

            if (options) {
                strcpy(options->perf_lite_mode, g_game_cache[i].perf_lite_mode);
                strcpy(options->dnd_on_gaming, g_game_cache[i].dnd_on_gaming);
                strcpy(options->app_priority, g_game_cache[i].app_priority);
                strcpy(options->game_preload, g_game_cache[i].game_preload);
                strcpy(options->refresh_rate, g_game_cache[i].refresh_rate);
                strcpy(options->renderer, g_game_cache[i].renderer);
                strcpy(options->resolution_downscale, g_game_cache[i].resolution_downscale);
                strcpy(options->bypass_charging, g_game_cache[i].bypass_charging);
                strcpy(options->gpu_profile, g_game_cache[i].gpu_profile);
                strcpy(options->thermal_profile, g_game_cache[i].thermal_profile);
                strcpy(options->cpu_governor, g_game_cache[i].cpu_governor);
                strcpy(options->gpu_governor, g_game_cache[i].gpu_governor);
                strcpy(options->gpu_max_freq, g_game_cache[i].gpu_max_freq);
                strcpy(options->cpu_policy_controls, g_game_cache[i].cpu_policy_controls);
            }
            break;
        }
    }

    pthread_mutex_unlock(&cache_mutex);

    if (!match_found) {
        free(pkg);
        return NULL;
    }

    return pkg;
}
