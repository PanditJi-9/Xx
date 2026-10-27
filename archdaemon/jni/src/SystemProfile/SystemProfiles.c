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

#include "MaxManager.h"

/**
 * @brief Thread worker function to run GamePreload asynchronously.
 * @param arg Pointer to PreloadArgs structure.
 * @return NULL
 */
void* async_preload_worker(void* arg) {
    PreloadArgs* args = (PreloadArgs*)arg;
    GamePreload(args->package);
    free(args);
    return NULL;
}

/**
 * @brief Applies system tuning parameters specifically for Performance Mode.
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_performance_profile(DaemonContext* ctx) {
    // إعادة تطبيق الوضع نفسه ليست حدثًا. هذا الحرس يمنع `apply_base_profile()`
    // من إعادة ملفّ الأداء كل دورة حين يكون هو اختيار المستخدم الأساس.
    if (ctx->is_initialize_complete && ctx->cur_mode == PERFORMANCE_PROFILE &&
        !ctx->need_profile_checkup)
        return;

    toast("Applying Performance Profile");

    ctx->cur_mode = PERFORMANCE_PROFILE;
    ctx->need_profile_checkup = false;

    // وضع الأداء صار يمكن أن يكون **اختيار المستخدم الأساس** لا وضع لعبة فقط،
    // فيُطبَّق بلا `gamestart`. ولا يُمرَّر NULL إلى `%s`/`strncpy` حينها.
    const char* profile_target = active_app_name ? active_app_name : gamestart;
    if (profile_target != NULL)
        notify("Performance Profile", "Running at %s", false, 0, profile_target);
    else
        notify("Performance Profile", "System is now at Powerful state", false, 0);
    log_zenith(LOG_INFO, "EVENT=PROFILE_APPLY profile=PERFORMANCE target=%s sw=%s",
               profile_target ? profile_target : "system",
               ctx->cur_switch_id[0] ? ctx->cur_switch_id : "unknown");

    if (IS_TRUE(opts.perf_lite_mode)) {
        __system_property_set("persist.sys.maxmanagerconf.litemode", "1");
    } else if (IS_FALSE(opts.perf_lite_mode)) {
        __system_property_set("persist.sys.maxmanagerconf.litemode", "0");
    } else {
        char lite_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.maxmanagerconf.cpulimit", lite_prop);
        __system_property_set("persist.sys.maxmanagerconf.litemode", (strcmp(lite_prop, "1") == 0) ? "1" : "0");
    }

    // DND خاصية لعبة/تطبيق في المقدمة؛ لا تُمسّ حين يكون وضع الأداء أساسًا بلا لعبة.
    if (gamestart != NULL) {
        if (ctx->saved_zen_mode < 0) {
            ctx->saved_zen_mode = current_system_cache.zen_mode;
        }

        if (IS_TRUE(opts.dnd_on_gaming)) {
            if (ctx->saved_zen_mode == 0) {
                systemv("sys.maxmanager-utilityconf enableDND");
            }
            ctx->dnd_enabled = true;
        } else if (!IS_FALSE(opts.dnd_on_gaming)) {
            char dnd_state[PROP_VALUE_MAX] = {0};
            __system_property_get("persist.sys.maxmanagerconf.dnd", dnd_state);
            if (strcmp(dnd_state, "1") == 0) {
                if (ctx->saved_zen_mode == 0) {
                    systemv("sys.maxmanager-utilityconf enableDND");
                }
                ctx->dnd_enabled = true;
            }
        }
    }

    // Governor ownership is established before ANY global profile writes. This
    // is deliberately done for Performance, Balanced and Eco: all three profiles
    // have their own governor paths and any one of them can otherwise overwrite
    // an explicit per-app governor.
    bool per_app_governor_override =
        gamestart != NULL &&
        (!IS_DEFAULT(opts.gpu_profile) || !IS_DEFAULT(opts.thermal_profile) ||
         !IS_DEFAULT(opts.cpu_governor) || !IS_DEFAULT(opts.gpu_governor) ||
         !IS_DEFAULT(opts.gpu_max_freq) || opts.cpu_policy_controls[0] != '\0');
    __system_property_set("sys.maxmanager.perapp.governor_isolation",
                          per_app_governor_override ? "1" : "0");

    // Per-App Settings (Kotlin side) now owns GPU/CPU governor and frequency for any app
    // that has an explicit override configured -- previously run_profiler() below forced
    // max GPU frequency / a "performance" governor on *every* tracked app unconditionally,
    // which silently fought and overrode whatever was chosen per-app (Default, Balanced,
    // Gaming, an explicit governor, or a fixed frequency all looked like "nothing happens"
    // from the per-app screen, because this global forcing ran again on every poll and won).
    // Apps that don't use the newer per-app GPU/CPU controls keep the previous behavior.
    // `opts` تبقى من آخر لعبة بعد خروجها، وتكون فارغة عند الإقلاع؛ فلا تُقرأ
    // كتجاوز صريح. الشرط `gamestart != NULL` يطابق حرس الملكية أعلاه، ويمنع
    // أن يُتخطّى `run_profiler(PERFORMANCE)` في وضع أساس بلا لعبة.
    bool has_per_app_gpu_override =
        gamestart != NULL &&
        (!IS_DEFAULT(opts.gpu_profile) || !IS_DEFAULT(opts.thermal_profile) ||
         !IS_DEFAULT(opts.cpu_governor) ||
         !IS_DEFAULT(opts.gpu_governor) || !IS_DEFAULT(opts.gpu_max_freq) ||
         opts.cpu_policy_controls[0] != '\0');

    if (!has_per_app_gpu_override) {
        EXECUTE("Performance Profile", run_profiler(PERFORMANCE_PROFILE));
    } else {
        log_zenith(LOG_INFO, "EVENT=PROFILE_SKIP_GPU_FORCE reason=per_app_override_active target=%s sw=%s",
                   profile_target ? profile_target : "system",
                   ctx->cur_switch_id[0] ? ctx->cur_switch_id : "unknown");
    }

    bool is_preload_active = false;
    if (!IS_FALSE(opts.game_preload)) {
        char preload_active[PROP_VALUE_MAX] = {0};
        if (__system_property_get("persist.sys.maxmanagerconf.APreload", preload_active) > 0) {
            is_preload_active = (strcmp(preload_active, "1") == 0);
        }
    }

    // التحميل المسبق يحتاج حزمة؛ لا معنى له في وضع أساس بلا لعبة، و`strncpy` من NULL ينهار.
    if (gamestart != NULL && (IS_TRUE(opts.game_preload) || is_preload_active)) {
        notify("MaxManager Preload", "Preloading initiated for: %s", true, 10000, profile_target);

        PreloadArgs* p_args = malloc(sizeof(PreloadArgs));
        if (p_args) {
            strncpy(p_args->package, gamestart, sizeof(p_args->package) - 1);
            p_args->package[sizeof(p_args->package) - 1] = '\0';

            pthread_t preload_thread;
            pthread_attr_t attr;
            pthread_attr_init(&attr);
            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

            if (pthread_create(&preload_thread, &attr, async_preload_worker, p_args) != 0) {
                log_zenith(LOG_ERROR, "EVENT=PRELOAD_SPAWN_FAILED reason=pthread_create_failed target=%s", gamestart);
                free(p_args);
            }
            pthread_attr_destroy(&attr);
        } else {
            log_zenith(LOG_ERROR, "EVENT=PRELOAD_SPAWN_FAILED reason=malloc_failed target=%s", gamestart);
        }
    }
    
    save_daemon_state(ctx);
    
}

/**
 * @brief Restores the HWUI renderer that was live before a per-app session changed it.
 *
 * Three properties of this helper are deliberate:
 *
 * 1. **One copy.** The same block lived twice (Eco and Balanced), byte for byte. A restore
 *    that drifts between the two profiles is a restore that works in one mode only.
 * 2. **The saved pair is cleared even when the live value already matches**, so a later
 *    session starts from a clean slate instead of restoring a value from two games ago.
 * 3. **It is the only place that clears `has_applied_renderer` together with the saved
 *    values**, so "we still own a saved renderer" and the pending-behavior flag cannot
 *    disagree.
 *
 * @param ctx Pointer to DaemonContext.
 * @param reason Short tag for the log line (ECO, BALANCED, per_app_exit, cleanup...).
 */
void restore_saved_renderer(DaemonContext* ctx, const char* reason) {
    if (!ctx || strlen(ctx->saved_renderer) == 0)
        return;

    char current_now[PROP_VALUE_MAX] = {0};
    __system_property_get("debug.hwui.renderer", current_now);

    if (strlen(current_now) == 0)
        strcpy(current_now, "default");

    if (strcmp(current_now, ctx->saved_renderer) != 0) {
        log_zenith(LOG_INFO, "EVENT=RENDERER_RESTORE reason=%s renderer=%s sw=%s",
                   reason ? reason : "unknown", ctx->saved_renderer,
                   ctx->cur_switch_id[0] ? ctx->cur_switch_id : "unknown");
        if (strcmp(ctx->saved_renderer, "default") == 0) {
            systemv("sys.maxmanager-utilityconf setrender default");
            __system_property_set("persist.sys.maxmanagerconf.renderer", "default");
        } else {
            systemv("sys.maxmanager-utilityconf setrender %s", ctx->saved_renderer);
            __system_property_set("persist.sys.maxmanagerconf.renderer", ctx->saved_sys_renderer);
        }
    } else {
        log_zenith(LOG_INFO, "EVENT=RENDERER_RESTORE_SKIP reason=%s renderer=already_live",
                   reason ? reason : "unknown");
    }

    memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    memset(ctx->saved_sys_renderer, 0, sizeof(ctx->saved_sys_renderer));
    ctx->has_applied_renderer = false;
}

/**
 * @brief Asked while a managed game is in front: can the global renderer be restored now?
 *
 * Restoring `debug.hwui.renderer` mid-session is not free. The daemon re-applies the
 * per-app renderer on the next game entry, `apply_smart_renderer()` reports a change, and
 * that triggers `restart_target_app()` -- a force-stop of a game the user is playing. So
 * Eco/Balanced defer the restore until the per-app session ends; the saved value survives
 * in `ctx->saved_renderer` and `restore_saved_renderer()` runs on every exit path.
 */
static bool renderer_restore_deferred(DaemonContext* ctx, const char* profile) {
    if (!ctx || gamestart == NULL || strlen(ctx->saved_renderer) == 0)
        return false;

    log_zenith(LOG_INFO,
               "EVENT=RENDERER_RESTORE_DEFERRED profile=%s target=%s reason=per_app_session_active",
               profile, active_app_name ? active_app_name : gamestart);
    return true;
}

/**
 * @brief Reverts system to Endurance state (Eco Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_eco_profile(DaemonContext* ctx) {

    // Preserve explicit Per-App CPU/GPU governors while this global profile is applied.
    bool per_app_governor_override =
        gamestart != NULL &&
        (!IS_DEFAULT(opts.gpu_profile) || !IS_DEFAULT(opts.thermal_profile) ||
         !IS_DEFAULT(opts.cpu_governor) || !IS_DEFAULT(opts.gpu_governor) ||
         !IS_DEFAULT(opts.gpu_max_freq) || opts.cpu_policy_controls[0] != '\0');
    __system_property_set("sys.maxmanager.perapp.governor_isolation",
                          per_app_governor_override ? "1" : "0");
    if (ctx->cur_mode == ECO_MODE) {
        // الوضع صحيح بالفعل؛ لكن طلب إعادة الفحص لا يبقى معلّقًا إلى الأبد.
        ctx->need_profile_checkup = false;
        return;
    }

    toast("Applying Eco Mode");

    ctx->cur_mode = ECO_MODE;
    ctx->need_profile_checkup = false;

    notify("ECO Mode", "System is now at Endurance state", false, 0);
    log_zenith(LOG_INFO, "EVENT=PROFILE_APPLY profile=ECO sw=%s", ctx->cur_switch_id[0] ? ctx->cur_switch_id : "unknown");

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.maxmanager-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1;

    if (!renderer_restore_deferred(ctx, "ECO"))
        restore_saved_renderer(ctx, "ECO");
    
    EXECUTE("ECO Mode", run_profiler(ECO_MODE));
    
    systemv("rm -rf /data/adb/.config/MaxManager/daemon_state");

}

/**
 * @brief Whether Max AI is enabled (the same property the CLI reads for ai_enabled).
 * @return true when persist.sys.maxmanagerconf.AIenabled is "1".
 */
static bool max_ai_enabled(void) {
    char ai_state[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.maxmanagerconf.AIenabled", ai_state);
    return ai_state[0] == '1';
}

/**
 * @brief Drops the profile card without announcing a profile.
 * Used when the automatic balanced baseline is applied while Max AI is off, so a
 * stale Performance/ECO card does not stay on screen. The app cancels its profile
 * notification on the "clearprofile" extra.
 */
static void clear_profile_notification(void) {
    systemv("su -c \"am broadcast -a nd.max.ACTION_MANAGE "
            "-n nd.max/nd.max.receiver.MaxManagerReceiver --ez clearprofile true "
            ">/dev/null 2>&1\"");
}

/**
 * @brief Reverts system to Optimal state (Balanced Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_balanced_profile(DaemonContext* ctx) {

    // Preserve explicit Per-App CPU/GPU governors while this global profile is applied.
    bool per_app_governor_override =
        gamestart != NULL &&
        (!IS_DEFAULT(opts.gpu_profile) || !IS_DEFAULT(opts.thermal_profile) ||
         !IS_DEFAULT(opts.cpu_governor) || !IS_DEFAULT(opts.gpu_governor) ||
         !IS_DEFAULT(opts.gpu_max_freq) || opts.cpu_policy_controls[0] != '\0');
    __system_property_set("sys.maxmanager.perapp.governor_isolation",
                          per_app_governor_override ? "1" : "0");
    if (ctx->is_initialize_complete && ctx->cur_mode == BALANCED_PROFILE) {
        // الوضع صحيح بالفعل؛ لكن طلب إعادة الفحص لا يبقى معلّقًا إلى الأبد.
        ctx->need_profile_checkup = false;
        return;
    }

    // Balanced may be the user's chosen base or a temporary return. Announce it only
    // while Max AI is on; manual CLI picks announce themselves.
    const bool announce = max_ai_enabled();
    if (announce)
        toast("Applying Balanced Profile");

    ctx->cur_mode = BALANCED_PROFILE;
    ctx->need_profile_checkup = false;

    if (announce)
        notify("Balanced Profile", "System is now at Optimal state", false, 0);
    else
        clear_profile_notification();
    log_zenith(LOG_INFO, "EVENT=PROFILE_APPLY profile=BALANCED sw=%s", ctx->cur_switch_id[0] ? ctx->cur_switch_id : "unknown");

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.maxmanager-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1;

    if (!renderer_restore_deferred(ctx, "BALANCED"))
        restore_saved_renderer(ctx, "BALANCED");

    EXECUTE("Balanced Profile", run_profiler(BALANCED_PROFILE));

    if (!ctx->is_initialize_complete) {
        notify("Daemon Info", "MaxManager is running successfully", false, 60000);
        ctx->is_initialize_complete = true;
    }
    
    systemv("rm -rf /data/adb/.config/MaxManager/daemon_state");
    
}

/**
 * @brief يُطبّق نيّة المستخدم الواحدة (الوضع الأساس) لا وضعًا افتراضيًا من عند الخادم.
 *
 * هذا هو الفرق بين «الخادم يملك ملفًا عامًا» و«الخادم ينفّذ وضع Max AI»: عند
 * غياب لعبة أو بطارية منخفضة يعود الخادم إلى `base_mode` المقروء من
 * `API/max_ai_mode`، لا إلى Balanced. والملفّات الثلاثة نفسها تحتفظ بحرسها،
 * فلا يُعاد تطبيق الوضع نفسه كل دورة.
 */
void apply_base_profile(DaemonContext* ctx) {
    switch (ctx->base_mode) {
        case PERFORMANCE_PROFILE:
            apply_performance_profile(ctx);
            break;
        case ECO_MODE:
            apply_eco_profile(ctx);
            break;
        case BALANCED_PROFILE:
        default:
            apply_balanced_profile(ctx);
            break;
    }
}
