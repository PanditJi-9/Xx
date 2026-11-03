/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
 * Host parity suite for `sys.maxmanager-service`.
 *
 * It links the real translation units (PropValidator, DaemonUtility,
 * ProfileUtility, PerAppKernel, ChargingNodes, ResolutionChanger, RenderingHandler,
 * StatusMonitor, CLIUtility) and asserts their observable decisions. Every
 * assertion names a behaviour that would regress silently on a device — the
 * whitelist that was wiping user settings, the shell-escaping that guards
 * command injection, the profile gate, the bypass node registry, the
 * renderer/resolution branches, the per-app governor guard that rejected the
 * kernel's own governors, the app_status wire format the companion
 * writes (fixtures/contracts/), and the --profile/--from-ai decision table.
 */
#include <ctype.h>
#include <fcntl.h>
#include <stdlib.h>
#include <errno.h>
#include <dirent.h>
#include <stddef.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <unistd.h>

#include <ForegroundObserver.h>
#include <SepolicyGen.h>
#include <IpcServer.h>
#include <MaxManager.h>

#include "host/host.h"

/* ────────────────────────────────────────────────────────────────────────────
 * ١) PropValidator — the whitelist that used to delete live user settings.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_prop_validator(void) {
    printf("\n\033[1m[prop_validator] property whitelist / stale deletion\033[0m\n");

    host_reset_capture();
    host_prop_reset();

    /* Must survive: exact legacy name, conf namespace, custom_ namespace, debug namespace. */
    host_prop_set("persist.sys.maxmanager.state", "running");
    host_prop_set("persist.sys.maxmanagerconf.detailedlog", "1");
    host_prop_set("persist.sys.maxmanagerconf.AIenabled", "true");
    host_prop_set("persist.sys.maxmanager.custom_zram_swappiness", "60");
    host_prop_set("persist.sys.maxmanagerdebug.trace", "1");
    /* Must be removed: under our namespace but owned by nobody we know. */
    host_prop_set("persist.sys.maxmanager.bogus_key", "stale");
    host_prop_set("persist.sys.maxmanager.orphan_two", "stale");
    /* Must be ignored entirely: not under our namespace. */
    host_prop_set("ro.build.version.sdk", "34");

    validateprop();

    CHECK(!host_saw_systemv("detailedlog"), "conf namespace key is never deleted (the regression the comment names)");
    CHECK(!host_saw_systemv("custom_zram_swappiness"), "custom_ namespace key is never deleted");
    CHECK(!host_saw_systemv("maxmanagerdebug.trace"), "debug namespace key is never deleted");
    CHECK(!host_saw_systemv("maxmanager.state"), "exact whitelisted key is never deleted");
    CHECK(!host_saw_systemv("ro.build.version.sdk"), "foreign namespace is not our business");
    CHECK(host_saw_systemv("resetprop -p --delete persist.sys.maxmanager.bogus_key"),
          "unknown key under our namespace is deleted by name");
    CHECK(host_saw_systemv("resetprop -p --delete persist.sys.maxmanager.orphan_two"),
          "second unknown key is deleted too");
    CHECK(host_systemv_count() == 2, "exactly the two unknown keys are deleted");

    /* Boundary: the naive prefix check treats `<ns>X` as ours, so it must be flagged
     * (documented behaviour, not a silent pass). */
    host_reset_capture();
    host_prop_reset();
    host_prop_set("persist.sys.maxmanagerX", "edge");
    validateprop();
    CHECK(host_saw_systemv("persist.sys.maxmanagerX"),
          "prefix without separator is still caught (documented naive-prefix boundary)");

    /* Boundary: the pending list is capped at MAX_PENDING_DELETE; overflow is skipped,
     * never silently truncated into a delete. */
    host_reset_capture();
    host_prop_reset();
    char name[64];
    for (int i = 0; i < MAX_PENDING_DELETE + 5; i++) {
        snprintf(name, sizeof(name), "persist.sys.maxmanager.unknown_%03d", i);
        host_prop_set(name, "x");
    }
    validateprop();
    CHECK(host_systemv_count() == MAX_PENDING_DELETE, "deletions are capped at MAX_PENDING_DELETE");
    CHECK(host_log_contains("STALE_PROP_SKIPPED"), "overflow is logged as SKIPPED, not silently dropped");

    /*
     * ── سمة السطح المُعلَن: لا خاصية مُعلَنة تُحذف في كل إقلاع ────────────────────────────
     *
     * هذه الدعوى وُجدت بقياس: `fixtures/contracts/system_properties.tsv` كشف أن مفاتيح
     * `persist.sys.maxmanager.gpu_studio.*` الأربعة تقع تحت نطاق الفحص (`persist.sys.maxmanager`)
     * بلا غطاء ⇒ فكان الخادم يوسمها `STALE_PROP` **ويحذفها عند كل تشغيل**، أي أن «حفظ GPU Studio»
     * في التطبيق يُمحى بلا سطر عطل ظاهر. والعلاج بادئة رابعة في `VALID_PROP_PREFIXES` (وقياسها
     * مكتوب هناك بجانبها)، وهذه الدعوى تُسقط عودة العطب.
     *
     * والمفاتيح المفحوصة ليست كل السطح (ذلك يقيسه `SystemPropertiesContractTest` على الجدول كاملًا)
     * بل **واحد عن كل صنف غطاء**: بادئة جديدة، وبادئة قديمة، والقائمة الدقيقة، ونطاق التصحيح.
     */
    static const char* declared_keys[] = {
        "persist.sys.maxmanager.gpu_studio.min_freq",
        "persist.sys.maxmanager.gpu_studio.max_freq",
        "persist.sys.maxmanager.gpu_studio.governor",
        "persist.sys.maxmanager.gpu_studio.mode",
        "persist.sys.maxmanagerconf.detailedlog",
        "persist.sys.maxmanagerconf.logmaxkb",
        "persist.sys.maxmanager.custom_doze_gms_mode",
        "persist.sys.maxmanagerdebug.soctype",
        "persist.sys.maxmanagerconf.AIenabled",
    };

    host_reset_capture();
    host_prop_reset();
    for (size_t i = 0; i < sizeof(declared_keys) / sizeof(declared_keys[0]); i++)
        host_prop_set(declared_keys[i], "1");

    validateprop();

    for (size_t i = 0; i < sizeof(declared_keys) / sizeof(declared_keys[0]); i++) {
        char label[192];
        snprintf(label, sizeof(label), "declared property is never deleted: %s", declared_keys[i]);
        CHECK(!host_saw_systemv(declared_keys[i]), label);
    }
    CHECK(host_systemv_count() == 0, "a declared property surface produces no resetprop delete at all");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٢) String utilities — trimming, shell escaping, value extraction.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_string_utils(void) {
    printf("\n\033[1m[string_utils] trim / escape / extract\033[0m\n");

    char a[] = "line\n";
    CHECK_STR_EQ(trim_newline(a), "line", "trailing newline is trimmed");
    char b[] = "no-newline";
    CHECK_STR_EQ(trim_newline(b), "no-newline", "string without newline is unchanged");
    char c[] = "a\nb\n";
    CHECK_STR_EQ(trim_newline(c), "a", "only the first newline terminates");
    CHECK(trim_newline(NULL) == NULL, "NULL is handled, not dereferenced");

    char esc[256];
    escape_shell_string(esc, "it's", sizeof(esc));
    CHECK_STR_EQ(esc, "it'\\''s", "single quote is escaped for the shell");
    escape_shell_string(esc, "plain", sizeof(esc));
    CHECK_STR_EQ(esc, "plain", "safe string passes through unchanged");
    escape_shell_string(esc, "", sizeof(esc));
    CHECK_STR_EQ(esc, "", "empty string yields empty string");

    char dst[64];
    extract_string_value(dst, "key: \"value\"", sizeof(dst));
    CHECK_STR_EQ(dst, "value", "quoted value after colon is extracted");
    extract_string_value(dst, "key:\t\"spaced\"", sizeof(dst));
    CHECK_STR_EQ(dst, "spaced", "leading tab/space is skipped");
    extract_string_value(dst, "key: unquoted", sizeof(dst));
    CHECK_STR_EQ(dst, "default", "unquoted value falls back to default");
    extract_string_value(dst, "no-colon-here", sizeof(dst));
    CHECK_STR_EQ(dst, "default", "missing colon falls back to default");
    extract_string_value(dst, NULL, sizeof(dst));
    CHECK_STR_EQ(dst, "default", "NULL input falls back to default");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٣) ProfileUtility — screen/low-power gates and per-app option resolution.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_profile_logic(void) {
    printf("\n\033[1m[profile_logic] screen state / low power / gamestart\033[0m\n");

    SystemStateCache cache;
    memset(&cache, 0, sizeof(cache));

    CHECK(get_screenstate_normal(NULL) == true, "missing cache defaults to screen-awake");
    cache.screen_awake = 0;
    CHECK(get_screenstate_normal(&cache) == false, "screen_awake=0 reads as off");
    cache.screen_awake = 1;
    CHECK(get_screenstate_normal(&cache) == true, "screen_awake=1 reads as on");

    CHECK(get_low_power_state_normal(NULL) == false, "missing cache defaults to no low-power");
    cache.battery_saver = 1;
    CHECK(get_low_power_state_normal(&cache) == true, "battery_saver=1 reads as on");
    cache.battery_saver = 0;
    CHECK(get_low_power_state_normal(&cache) == false, "battery_saver=0 reads as off");

    /* get_gamestart: NULL visible package ⇒ NULL; matching cache ⇒ options copied. */
    host_set_visible_package(NULL);
    CHECK(get_gamestart(NULL, &cache) == NULL, "no visible package yields no gamestart");

    host_set_visible_package("com.example.game");
    g_game_cache = NULL;
    g_game_cache_count = 0;
    CHECK(get_gamestart(NULL, &cache) == NULL, "empty cache yields no gamestart");

    static GameConfig one;
    memset(&one, 0, sizeof(one));
    strcpy(one.package, "com.example.game");
    strcpy(one.refresh_rate, "120");
    strcpy(one.renderer, "vulkan");
    strcpy(one.gpu_profile, "performance");
    g_game_cache = &one;
    g_game_cache_count = 1;

    GameConfig out;
    memset(&out, 0, sizeof(out));
    char* pkg = get_gamestart(&out, &cache);
    CHECK(pkg != NULL && strcmp(pkg, "com.example.game") == 0, "matching package is returned");
    CHECK_STR_EQ(out.refresh_rate, "120", "refresh_rate is copied from the cache");
    CHECK_STR_EQ(out.renderer, "vulkan", "renderer is copied from the cache");
    CHECK_STR_EQ(out.gpu_profile, "performance", "gpu_profile is copied from the cache");
    free(pkg);

    /* Unmatched visible package ⇒ NULL, and no partial copy is promised. */
    host_set_visible_package("com.example.other");
    CHECK(get_gamestart(&out, &cache) == NULL, "unmatched package yields no gamestart");

    host_set_visible_package(NULL);
    g_game_cache = NULL;
    g_game_cache_count = 0;
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٢أ) الوضع الواحد — نيّة المستخدم مقابل الوضع الفاعل.
 *
 * العطب الذي يحرسه: كان الإقلاع يفرض Balanced و`InotifyWatcher` يفرضه عند تشغيل
 * AI، بينما ملفّ `current_profile` قد يكون وضع لعبة مؤقّتًا. فتُقاس هنا ثلاث
 * دعاوى: الملفّ الصالح يفوز، والهدف القديم يُرحَّل عند غيابه، وAI المطفأ لا
 * يُطبّق شيئًا عند الإقلاع.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_max_ai_mode(void) {
    printf("\n\033[1m[max_ai_mode] saved intent / legacy objective / boot gate\033[0m\n");

    CHECK(max_ai_mode_from_saved(1) == PERFORMANCE_PROFILE, "saved 1 = performance");
    CHECK(max_ai_mode_from_saved(2) == BALANCED_PROFILE, "saved 2 = balanced");
    CHECK(max_ai_mode_from_saved(3) == ECO_MODE, "saved 3 = battery");
    CHECK(max_ai_mode_from_saved(0) == BALANCED_PROFILE, "saved 0 (initialize) is not a mode");
    CHECK(max_ai_mode_from_saved(9) == BALANCED_PROFILE, "out-of-range saved mode falls back to balanced");

    CHECK(max_ai_mode_from_objective("performance") == PERFORMANCE_PROFILE, "legacy performance objective");
    CHECK(max_ai_mode_from_objective("battery") == ECO_MODE, "legacy battery objective");
    CHECK(max_ai_mode_from_objective("balanced") == BALANCED_PROFILE, "legacy balanced objective");
    CHECK(max_ai_mode_from_objective(NULL) == BALANCED_PROFILE, "missing legacy objective = balanced");

    CHECK(max_ai_mode_resolve(true, 3, "performance") == ECO_MODE,
          "valid saved mode wins over the legacy objective");
    CHECK(max_ai_mode_resolve(true, -1, "battery") == ECO_MODE,
          "missing saved mode migrates from the legacy objective");
    CHECK(max_ai_mode_resolve(false, 3, "battery") == PERFCOMMON,
          "AI off applies no boot profile");
    CHECK(strcmp(max_ai_objective_for_mode(PERFORMANCE_PROFILE), "performance") == 0,
          "performance mode mirrors the legacy objective");
    CHECK(strcmp(max_ai_objective_for_mode(ECO_MODE), "battery") == 0,
          "battery mode mirrors the legacy objective");
    CHECK(strcmp(max_ai_objective_for_mode(BALANCED_PROFILE), "balanced") == 0,
          "balanced mode mirrors the legacy objective");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٢ب) freq-balance gate — «بالحدث لا بالإيقاع» (جولة الحرارة واللاغ).
 *
 * العطب الذي يحرسه هذا الجدول: كان الخادم ينادي `applyfreqbalance` في **كل** دورة ما دامت
 * الشاشة مضاءة والوضع متوازنًا/موفّرًا — وكل دورة = ولادة عملية + كتابة عقد التردد لكل نواة،
 * والقيم لم تتغيّر. فهذه الدعاوى تُثبّت **متى** يُنادى ومتى لا يُنادى، ولو عاد أحدهم إلى
 * «الإيقاع» لسقط الجدول هنا قبل الجهاز.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_freq_balance_gate(void) {
    printf("\n\033[1m[freq_balance_gate] event-triggered re-application\033[0m\n");

    /* لا يُطبَّق بلا شاشة، ولا في وضع أداء (الميزان يعنى المتوازن/الموفّر). */
    CHECK(!should_apply_freq_balance(false, true, true, true),
          "screen off => no call, even when the screen-on edge is claimed");
    CHECK(!should_apply_freq_balance(true, false, true, true),
          "performance mode => no call (the balance targets balanced/eco only)");

    /* الحالتان اللتان تعنيان «تغيّر حقيقي»: إضاءة الشاشة، أو تغيّر الوضع/الطلب. */
    CHECK(should_apply_freq_balance(true, true, true, false),
          "screen just turned on => apply once");
    CHECK(should_apply_freq_balance(true, true, false, true),
          "mode or request changed => apply once");

    /* والمهم: دورة لا حدث فيها ⇒ **لا نداء** (وهو العطب نفسه: كل دورة كانت تُنادي). */
    CHECK(!should_apply_freq_balance(true, true, false, false),
          "an idle loop iteration makes no call — this is the regression guard");

    /* وحالة الإقلاع (`prev_screen_state = -1` حتى تُقرأ الشاشة) ليست «إضاءة»: التحوّل يُعلن
     * بالصفر صراحةً، فلا يُخترع حدث من حالة غير مقروءة. */
    CHECK(!should_apply_freq_balance(true, true, (-1 == 0), false),
          "an unknown previous screen state is not an on-edge");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٣.١) app_status → profile checkup: "event" لا "كل كتابة".
 *
 * **العطب الذي تحرسه هذه الدعاوى (جولة الحرارة واللاغ):** كل `IN_MODIFY` على `app_status`
 * كان يضبط `need_profile_checkup` بلا مقارنة. والملفّ يُكتب عند كل تغيّر (نسبة البطارية،
 * معدّل الشاشة، حالة الصمت…) — أي دورة فحص كاملة لعشرات المرات في الساعة بلا سبب.
 * ولا تُفقد حالة: البطارية والوضع يُقرآن من الكاش في كل دورة، والمقيم هو ما يحدّد الفحص.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_app_status_gate(void) {
    printf("\n\033[1m[app_status_gate] profile checkup only on a real foreground change\033[0m\n");

    /* تغيّر الحزمة المقيمة: الفحص مطلوب — وإلا بقيت اللعبة السابقة هي الهدف. */
    CHECK(app_status_needs_profile_checkup("com.miui.home", "sw-1", "com.game", "sw-2"),
          "a new foreground package asks for a checkup");

    /* نفس الحزمة، وكتابة جديدة بلا تغيّر: **لا فحص** — وهذا هو العطب نفسه. */
    CHECK(!app_status_needs_profile_checkup("com.game", "sw-2", "com.game", "sw-2"),
          "same package + same session (battery/refresh write) asks for nothing");

    /* نفس الحزمة بجلسة جديدة: جلسة أُعيد فتحها (عودة تطبيق بعد موت عمليته) ⇒ فحص. */
    CHECK(app_status_needs_profile_checkup("com.game", "sw-2", "com.game", "sw-9"),
          "a reopened session on the same package still asks for a checkup");

    /* أول قراءة (الكاش صفر): "" ليست حزمة، فيُطلب فحص. */
    CHECK(app_status_needs_profile_checkup("", "", "com.game", "sw-1"),
          "the first status read is always a change");

    /* وحزمة فارغة (لا مقدّمة) لا تطلب فحصًا بنفسها: الإطفاء يمرّ بمسار آخر. */
    CHECK(!app_status_needs_profile_checkup("", "", "", "sw-1"),
          "an empty foreground is not a change worth a checkup");
    CHECK(!app_status_needs_profile_checkup(NULL, NULL, NULL, NULL),
          "NULL inputs are tolerated, not dereferenced");
    CHECK(app_status_needs_profile_checkup(NULL, "sw-1", "com.game", "sw-1"),
          "an unknown previous package is a change");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٤) ChargingNodes — the bypass registry's structural invariants.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_bypass_registry(void) {
    printf("\n\033[1m[bypass_registry] node table invariants\033[0m\n");

    /* The header exposes `bypass_list` as an incomplete extern array, so the count is
     * read from `bypass_list_size` and pinned here: a silent add/remove of a node is a
     * parity change and must be re-measured, not slipped through. (Measured 2026-09-26:
     * this pin first read 78 from a miscount; the suite failed and corrected it to 77 —
     * which is exactly the point of pinning.) */
    CHECK(bypass_list_size == 77, "registry size is pinned at 77 (re-measure on intentional change)");

    bool names_unique = true, paths_unique = true, paths_absolute = true;
    bool values_present = true, values_differ = true;
    for (int i = 0; i < bypass_list_size; i++) {
        if (!bypass_list[i].name || !bypass_list[i].name[0])
            values_present = false;
        if (!bypass_list[i].path || bypass_list[i].path[0] != '/')
            paths_absolute = false;
        if (!bypass_list[i].on_val || !bypass_list[i].off_val ||
            bypass_list[i].on_val[0] == '\0' || bypass_list[i].off_val[0] == '\0')
            values_present = false;
        else if (strcmp(bypass_list[i].on_val, bypass_list[i].off_val) == 0)
            values_differ = false;
        for (int j = i + 1; j < bypass_list_size; j++) {
            if (strcmp(bypass_list[i].name, bypass_list[j].name) == 0)
                names_unique = false;
            if (strcmp(bypass_list[i].path, bypass_list[j].path) == 0)
                paths_unique = false;
        }
    }
    CHECK(values_present, "every node has a name and both on/off values");
    CHECK(names_unique, "node names are unique");
    CHECK(paths_unique, "node paths are unique");
    CHECK(paths_absolute, "every node path is absolute");
    CHECK(values_differ, "on and off values differ for every node");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٥) ResolutionChanger — SDK branch and command construction.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_resolution(void) {
    printf("\n\033[1m[resolution] game-mode API vs legacy device_config\033[0m\n");

    DaemonContext ctx;
    memset(&ctx, 0, sizeof(ctx));
    strcpy(ctx.cur_switch_id, "sw-test");

    host_reset_capture();
    CHECK(apply_resolution_target(&ctx, "com.example.game", "default") == false,
          "'default' downscale is a no-op");
    CHECK(host_systemv_count() == 0, "no-op emits no shell command");

    ctx.resolution_applied = true;
    CHECK(apply_resolution_target(&ctx, "com.example.game", "0.75") == false,
          "already-applied context short-circuits");
    ctx.resolution_applied = false;

    setenv("HOST_FAKE_SDK", "14", 1);
    host_reset_capture();
    CHECK(apply_resolution_target(&ctx, "com.example.game", "0.75") == true,
          "modern SDK applies the downscale");
    CHECK(ctx.resolution_applied == true, "context records the intervention");
    CHECK(ctx.used_legacy_fallback == false, "modern SDK is not the legacy path");
    CHECK(host_saw_systemv("cmd game set --mode 2 --downscale 0.75 com.example.game"),
          "modern SDK emits the game-mode API command");

    memset(&ctx, 0, sizeof(ctx));
    setenv("HOST_FAKE_SDK", "12", 1);
    host_reset_capture();
    CHECK(apply_resolution_target(&ctx, "com.example.game", "0.75") == true,
          "legacy SDK still applies");
    CHECK(ctx.used_legacy_fallback == true, "legacy SDK is marked as fallback");
    CHECK(host_saw_systemv("cmd device_config put game_overlay com.example.game mode=2,downscaleFactor=0.75"),
          "legacy path writes the device_config overlay");
    CHECK(host_saw_systemv("cmd game mode 2 com.example.game"),
          "legacy path also sets the game mode");

    host_reset_capture();
    restore_resolution_target(&ctx, "com.example.game");
    CHECK(host_saw_systemv("cmd device_config delete game_overlay com.example.game"),
          "legacy restore deletes the overlay");
    CHECK(host_saw_systemv("cmd game reset com.example.game"),
          "legacy restore resets the game mode");
    CHECK(ctx.resolution_applied == false, "restore clears the applied flag");

    memset(&ctx, 0, sizeof(ctx));
    host_reset_capture();
    restore_resolution_target(&ctx, "com.example.game");
    CHECK(host_systemv_count() == 0, "restore on a clean context emits nothing");

    /* Modern-path restore uses the API reset form. */
    ctx.resolution_applied = true;
    ctx.used_legacy_fallback = false;
    host_reset_capture();
    restore_resolution_target(&ctx, "com.example.game");
    CHECK(host_saw_systemv("cmd game reset --mode 2 com.example.game"),
          "modern restore resets via the game-mode API");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٦) RenderingHandler — renderer switch decision.
 * ──────────────────────────────────────────────────────────────────────────── */
void suite_renderer(void) {
    printf("\n\033[1m[renderer] smart renderer switching\033[0m\n");

    char saved_ref[PROP_VALUE_MAX] = {0};
    char saved_sys[PROP_VALUE_MAX] = {0};

    host_reset_capture();
    CHECK(apply_smart_renderer(NULL, saved_ref, saved_sys) == false, "NULL target is a no-op");
    CHECK(apply_smart_renderer("default", saved_ref, saved_sys) == false, "'default' target is a no-op");
    CHECK(apply_smart_renderer("", saved_ref, saved_sys) == false, "empty target is a no-op");

    host_prop_reset();
    host_prop_set("debug.hwui.renderer", "skiagl");
    host_prop_set("persist.sys.maxmanagerconf.renderer", "skiagl");
    saved_ref[0] = '\0';
    saved_sys[0] = '\0';
    host_reset_capture();
    CHECK(apply_smart_renderer("vulkan", saved_ref, saved_sys) == true,
          "a real difference switches the renderer");
    CHECK_STR_EQ(saved_ref, "skiagl", "previous HWUI renderer is saved for restore");
    CHECK_STR_EQ(saved_sys, "skiagl", "previous sys renderer is saved for restore");
    CHECK(host_saw_systemv("sys.maxmanager-utilityconf setrender vulkan"),
          "the utility binary is asked to switch");
    char now[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.maxmanagerconf.renderer", now);
    CHECK_STR_EQ(now, "vulkan", "the sys property reflects the new renderer");

    /* Division of labour: the daemon asks the utility binary to switch and updates only
     * the sys property — it never writes debug.hwui.renderer itself. So idempotency
     * depends on the utility's side effect; model it, don't assume it. */
    char hwui[PROP_VALUE_MAX] = {0};
    __system_property_get("debug.hwui.renderer", hwui);
    CHECK_STR_EQ(hwui, "skiagl", "daemon leaves the HWUI renderer for the utility to change");
    host_prop_set("debug.hwui.renderer", "vulkan"); /* what `setrender vulkan` does on device */

    host_reset_capture();
    CHECK(apply_smart_renderer("vulkan", saved_ref, saved_sys) == false,
          "no switch when already on the target");
    CHECK(!host_saw_systemv("setrender"), "an idempotent call does not re-emit the command");
}

/* ────────────────────────────────────────────────────────────────────────────
 * ٧) PerAppKernel — the per-app governor that nobody was writing.
 * ────────────────────────────────────────────────────────────────────────────
 *
 * The defect this pins was measured on a `TECNO POVA 5 Pro` (MT6833GP, Aetherium kernel): the
 * kernel advertises `[sugov_ext, reflex, conservative, powersave, performance, schedhorizon,
 * schedutil]`, the app offers the user exactly what the kernel advertises, and the daemon's
 * hardcoded `is_safe_cpu_governor()` list of five names rejected `reflex` and `schedhorizon` with
 * `reason=unsafe_value` — so the node was never written. The profiles binary never allowlists: it
 * reads the device. So the two suites below assert that the guard no longer *is* an allowlist, and
 * that a malformed token still cannot reach a kernel node.
 *
 * What is NOT measured here, and must not be claimed: that a write lands on a real node (there is
 * no device sysfs on the host), and that System.c actually reaches this function (that is a
 * code-reading claim about the caller, not a runtime one). Both stay device claims.
 */

/*
 * وحدات تخطّاها **المضيف** لا نحن: تُعدّ وتُعلَن في الملخّص، فلا يُقرأ «261/261» على مضيف
 * أسقط 27 دعوى كأنّه مرّ عليها. والفرق **مقيس في CI نفسه**: التشغيل `36501257551` أعطى
 * `261/261` على runner بلا `policyN`، والتشغيل `36504291660` أعطى `234/234` على runner
 * يُعلنها — والفرق **27** دعوى في مجموعة الحاكم وحدها.
 */
static int g_suites_skipped = 0;

/*
 * Refuses to run the write-path assertions on a machine that exposes real CPU policies: the
 * daemon writes `/sys/devices/system/cpu/cpufreq/policyN/scaling_governor`, and a test host that
 * has them would be writing to the CPU the suite is running on. On a normal container the
 * directory exists but holds no `policyN`, so `collect_cpu_policies()` returns 0 and the branch
 * ends at `reason=no_cpu_policies` without touching anything.
 */
static bool host_exposes_real_cpu_policies(void) {
    DIR* dir = opendir("/sys/devices/system/cpu/cpufreq");
    if (!dir)
        return false;

    bool found = false;
    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strncmp(entry->d_name, "policy", 6) == 0 && isdigit((unsigned char)entry->d_name[6])) {
            found = true;
            break;
        }
    }
    closedir(dir);
    return found;
}

void suite_per_app_governor(void) {
    printf("\n\033[1m[per_app_governor] guard by shape, kernel as authority\033[0m\n");

    GameConfig cfg;

    /* Nothing configured ⇒ not even a log line: the daemon must not touch a governor it was
     * not asked about. */
    memset(&cfg, 0, sizeof(cfg));
    strcpy(cfg.cpu_governor, "default");
    strcpy(cfg.gpu_governor, "default");
    host_reset_capture();
    enforce_per_app_governors(&cfg, "com.example.game");
    CHECK(host_log_count() == 0, "both fields 'default' produce no governor attempt at all");

    /* The empty-field trap: IS_DEFAULT("") is false (the macro tests NULL and the literal
     * "default" only), so an unset field must be excluded explicitly or a zeroed GameConfig
     * reads as "an explicit governor was configured". */
    cfg.cpu_governor[0] = '\0';
    cfg.gpu_governor[0] = '\0';
    host_reset_capture();
    enforce_per_app_governors(&cfg, "com.example.game");
    CHECK(host_log_count() == 0, "an empty field is not read as configured (IS_DEFAULT ignores emptiness)");

    memset(&cfg, 0, sizeof(cfg));
    strcpy(cfg.cpu_governor, "performance");
    strcpy(cfg.gpu_governor, "performance");
    host_reset_capture();
    enforce_per_app_governors(NULL, "com.example.game");
    enforce_per_app_governors(&cfg, NULL);
    enforce_per_app_governors(&cfg, "");
    CHECK(host_log_count() == 0, "NULL options, NULL package and empty package are all no-ops");

    if (host_exposes_real_cpu_policies()) {
        g_suites_skipped++;
        printf("  \033[33mSKIP\033[0m this host exposes real policyN nodes — refusing to run a "
               "write-path assertion against a live CPU; the rest of this suite is "
               "\033[1mNOT VERIFIED here\033[0m\n");
        return;
    }

    /* ── the measured regression: every governor this device's kernel advertises ── */
    static const char* const device_advertised[] = {
        "sugov_ext", "reflex", "conservative", "powersave", "performance",
        "schedhorizon", "schedutil",
    };
    for (size_t i = 0; i < sizeof(device_advertised) / sizeof(device_advertised[0]); i++) {
        memset(&cfg, 0, sizeof(cfg));
        strcpy(cfg.cpu_governor, device_advertised[i]);
        strcpy(cfg.gpu_governor, "default");
        host_reset_capture();
        enforce_per_app_governors(&cfg, "com.example.game");
        char label[192];
        snprintf(label, sizeof(label),
                 "kernel-advertised CPU governor is not rejected as unsafe: %s",
                 device_advertised[i]);
        CHECK(!host_log_contains("PERAPP_GOVERNOR_REJECTED"), label);
    }

    /* ── the native guard is no longer narrower than its sibling in binprofiles ── */
    static const char* const profiles_known[] = {
        "scx", "walt", "sched_pixel", "uag", "schedplus", "energy_step",
        "ondemand", "interactive", "userspace",
    };
    for (size_t i = 0; i < sizeof(profiles_known) / sizeof(profiles_known[0]); i++) {
        memset(&cfg, 0, sizeof(cfg));
        strcpy(cfg.cpu_governor, profiles_known[i]);
        strcpy(cfg.gpu_governor, "default");
        host_reset_capture();
        enforce_per_app_governors(&cfg, "com.example.game");
        char label[192];
        snprintf(label, sizeof(label),
                 "a governor the profiles binary knows is not rejected by the daemon: %s",
                 profiles_known[i]);
        CHECK(!host_log_contains("PERAPP_GOVERNOR_REJECTED"), label);
    }

    /* ── the GPU side: the driver's published list is the authority, not a hardcoded nine ── */
    static const char* const gpu_tokens[] = {
        "performance", "apupassive-pe", "simple_ondemand", "msm-adreno-tz", "mali_ondemand",
    };
    for (size_t i = 0; i < sizeof(gpu_tokens) / sizeof(gpu_tokens[0]); i++) {
        memset(&cfg, 0, sizeof(cfg));
        strcpy(cfg.cpu_governor, "default");
        strcpy(cfg.gpu_governor, gpu_tokens[i]);
        host_reset_capture();
        enforce_per_app_governors(&cfg, "com.example.game");
        char label[192];
        snprintf(label, sizeof(label),
                 "GPU governor token passes the shape guard instead of being rejected: %s",
                 gpu_tokens[i]);
        CHECK(!host_log_contains("PERAPP_GOVERNOR_REJECTED"), label);
    }

    /* ── and the guard still bites: shape, not membership ── */
    static const char* const malformed[] = {
        "../../etc/passwd",          /* a path, not a governor */
        "performance; rm -rf /",     /* command-shaped */
        "perf ormance",              /* whitespace */
        "performance\n",             /* newline smuggled from a settings file */
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", /* 36 chars: cannot fit cpu_governor[32] */
    };
    for (size_t i = 0; i < sizeof(malformed) / sizeof(malformed[0]); i++) {
        memset(&cfg, 0, sizeof(cfg));
        strcpy(cfg.cpu_governor, malformed[i]);
        strcpy(cfg.gpu_governor, "default");
        host_reset_capture();
        enforce_per_app_governors(&cfg, "com.example.game");
        char label[192];
        snprintf(label, sizeof(label),
                 "malformed governor is rejected before any write: value #%zu", i);
        CHECK(host_log_contains("reason=unsafe_value"), label);
    }

    /* The rejection line is the user's own evidence channel (tools/log_gate.py reads it), so its
     * wire format is pinned, not just its presence. */
    memset(&cfg, 0, sizeof(cfg));
    strcpy(cfg.cpu_governor, "../../etc/passwd");
    strcpy(cfg.gpu_governor, "default");
    host_reset_capture();
    enforce_per_app_governors(&cfg, "com.example.game");
    CHECK(host_log_contains("EVENT=PERAPP_GOVERNOR_REJECTED knob=cpu_governor pkg=com.example.game "
                            "requested=../../etc/passwd reason=unsafe_value"),
          "the rejection names the knob, the package and the offending value");
}

/* ────────────────────────────────────────────────────────────────────────────
 *  Bootstrap: a fake `getprop` on PATH so the popen-based reader is real.
 * ──────────────────────────────────────────────────────────────────────────── */
static char g_path_dir[64];

static void install_fake_getprop(void) {
    char tmpl[] = "/tmp/mm-parity-XXXXXX";
    char* dir = mkdtemp(tmpl);
    if (!dir) {
        fprintf(stderr, "harness: could not create temp dir\n");
        return;
    }
    strncpy(g_path_dir, dir, sizeof(g_path_dir) - 1);

    char script[128];
    snprintf(script, sizeof(script), "%s/getprop", g_path_dir);
    FILE* fp = fopen(script, "w");
    if (!fp)
        return;
    fprintf(fp, "#!/bin/sh\necho \"${HOST_FAKE_SDK:-0}\"\n");
    fclose(fp);
    chmod(script, 0755);

    const char* old = getenv("PATH");
    char newpath[2048];
    snprintf(newpath, sizeof(newpath), "%s:%s", g_path_dir, old ? old : "");
    setenv("PATH", newpath, 1);
}

/*
 * ── module integrity: the guard that must not fire on the module's own prop ──
 *
 * العطب الذي أُخرج الخادم عند **كل إقلاع نظيف**: الحارس كان ينفّذ
 * `grep -q '^name=Max Manager$'`، و`mainfiles/module.prop` يقول `name=MaxManager`
 * (تسمية مقصودة لاحقة). فالحارس أخرج الوحدة لأنها هي هي — وسجل الجهاز يقولها:
 *
 *   F MaxManager: EVENT=MODULE_INTEGRITY_FAILED reason=modified_by_third_party
 *
 * ثم `exit`، فسكت كل ما يملكه الخادم: الملف العام، والحاكم لكل تطبيق، و`--checkbypasschg`
 * الذي تقرأ نتيجته شاشة تجاوز الشحن — وهي رسالة المستخدم نفسها.
 *
 * وهذه المجموعة تقيس **الملفّ المشحون نفسه**: التوجيه إلى `mainfiles/module.prop`
 * الحقيقي، لا إلى نصّ مكتوب في الاختبار. فلو غيّر أحد الاسم في `module.prop` ولم يُتبعه في
 * `MODULE_IDENTITY_NAME`، سقطت الدعوى الأولى هنا وسقطت بوابة `module_identity` في
 * `tools/bundle_contract.py` — أي أنّ العطب يُمسك قبل أن يُثبَّت على جهاز.
 */
void suite_module_integrity(void) {
    printf("\n\033[1m[module_integrity] the daemon's own module.prop must pass its own guard\033[0m\n");

    /* ── ١) الدالّة النقيّة على مدخلات مكتوبة: أين تنحرف الحدود؟ ── */
    CHECK(!module_identity_ok(NULL), "NULL text is not an identity");
    CHECK(module_identity_ok("id=MaxManager\nname=MaxManager\nversion=v1.0\nversionCode=1\nauthor=MaxManager Project\n"),
          "the shipped key order and layout are accepted");
    CHECK(module_identity_ok("name=MaxManager\r\nauthor=MaxManager Project\r\n"),
          "CRLF (a prop rewritten by another tool) is still the same identity");
    CHECK(!module_identity_ok("name=Max Manager\nauthor=MaxManager Project\n"),
          "the old spaced name is refused — it is the name this repo no longer ships");
    CHECK(!module_identity_ok("name=MaxManagerX\nauthor=MaxManager Project\n"),
          "a longer name is not a prefix match");
    CHECK(!module_identity_ok("name =MaxManager\nauthor=MaxManager Project\n"),
          "a space before '=' is not the same key");
    CHECK(!module_identity_ok("name=MaxManager\n"), "the author line is required too, not just the name");
    CHECK(!module_identity_ok("author=MaxManager Project\n"), "the name line is required too, not just the author");
    CHECK(!module_identity_ok("# name=MaxManager\nauthor=MaxManager Project\n"),
          "a commented-out line is not a property (the match is the whole line)");

    /* ── ٢) البوّابة الحقيقية: `fopen` موجَّه إلى الملفّ المشحون نفسه ── */
    host_clear_routes();
    host_route_open(MODULE_PROP, REPO_DIR "/mainfiles/module.prop");
    host_reset_capture();
    CHECK(!module_identity_violated(),
          "the module's own shipped module.prop passes the gate (the daemon survives boot)");
    CHECK(host_log_count() + host_notify_count() == 0,
          "a clean identity logs nothing and notifies no one");

    /* ── ٣) والحارس ليس أعمى: تغيير الاسم من طرف ثالث ما زال يُمسَك ── */
    host_clear_routes();
    const char* renamed = BUILD_DIR "/module_prop_renamed.prop";
    FILE* fixture = fopen(renamed, "w");
    CHECK(fixture != NULL, "the host can write the renamed-prop fixture");
    if (fixture) {
        fputs("id=MaxManager\nname=Max Manager\nversion=v1.0\nversionCode=1\n"
              "author=MaxManager Project\nupdateJson=\n",
              fixture);
        fclose(fixture);
    }
    host_route_open(MODULE_PROP, renamed);
    CHECK(module_identity_violated(),
          "a third party renaming the module is still caught — the guard did not become a no-op");

    /* ── ٤) ملفّ غير مقروء = «مُساء إليه» لا «سليم» (نفس سلوك ما قبل الإصلاح) ── */
    host_clear_routes();
    host_route_open(MODULE_PROP, BUILD_DIR "/no_such_module_prop_here.prop");
    CHECK(module_identity_violated(), "an unreadable module.prop is refused, never assumed clean");

    /* ── ٥) وضع الروم: القياس مسار الثنائيّ نفسه — وثنائيّ الاختبار ليس تحت `/system/bin` ──
     * فيبقى الحارس يعمل هنا بلا تخفيف (وإلا سقط ٣ و٤ أعلاه). والفرع الموجب (ثنائيّ تحت
     * `/system/bin` يعبر الحارس دون `module.prop`) **يحتاج جهازًا** — `readlink(/proc/self/exe)` لا يُوجَّه. */
    CHECK(!is_rom_install(), "a host test binary is not a ROM-installed daemon, so the guard stays armed");

    host_clear_routes();
}

/* ── قناة التطبيق ↔ الخادم (المرحلة الأولى) ─────────────────────────────────────────────── */
static int g_ipc_calls;
static char g_ipc_profile;
static bool g_ipc_ai;
static int g_ipc_rc;

static int fake_profile_runner(char profile, bool from_ai) {
    g_ipc_calls++;
    g_ipc_profile = profile;
    g_ipc_ai = from_ai;
    return g_ipc_rc;
}

static int g_ipc_setprops;
static char g_ipc_prop_key[IPC_KEY_MAX];
static char g_ipc_prop_value[IPC_VALUE_MAX];

static int fake_property_setter(const char* key, const char* value) {
    g_ipc_setprops++;
    snprintf(g_ipc_prop_key, sizeof(g_ipc_prop_key), "%s", key);
    snprintf(g_ipc_prop_value, sizeof(g_ipc_prop_value), "%s", value);
    return 0;
}

static const IpcBackend FAKE_BACKEND = {.profile = fake_profile_runner, .setprop = fake_property_setter};

static void ipc_roundtrip(uid_t peer, const char* request, char* reply, size_t size) {
    int pair[2];
    reply[0] = '\0';
    if (socketpair(AF_UNIX, SOCK_STREAM, 0, pair) != 0) {
        return;
    }
    if (write(pair[1], request, strlen(request)) < 0) {
        close(pair[0]);
        close(pair[1]);
        return;
    }
    ipc_handle_connection(pair[0], peer, &FAKE_BACKEND);
    const ssize_t got = read(pair[1], reply, size - 1);
    reply[got > 0 ? got : 0] = '\0';
    close(pair[0]);
    close(pair[1]);
}

/* LockFile.c ليست في وحدات الاختبار؛ المراقب لا يستدعيها إلا من خيطه الحيّ، فيكفي مرساة للرابط. */
bool is_java_lock_held(const char* lock_path) {
    (void)lock_path;
    return false;
}

static void suite_sepolicy_gen(void) {
    printf("\n\033[1m[sepolicy] the on-phone generator (--gen-sepolicy)\033[0m\n");
    SepolRules set;
    CHECK(sepol_init(&set), "the rule set allocates");

    CHECK(sepol_parse_avc_line("[  1.2] audit: type=1400 avc:  denied  { read open } for comm=\"x\" scontext=u:r:maxmanager:s0 tcontext=u:object_r:sysfs_batteryinfo:s0 tclass=file permissive=1", "maxmanager", &set),
          "a dmesg-shaped denial is accepted");
    CHECK(sepol_parse_avc_line("avc:  denied  { write read } for scontext=u:r:maxmanager:s0 tcontext=u:object_r:sysfs_batteryinfo:s0 tclass=file permissive=0", "maxmanager", &set),
          "a logcat-shaped denial is accepted and merged");
    CHECK(sepol_parse_avc_line("avc:  denied  { connectto } for scontext=u:r:priv_app:s0:c512,c768 tcontext=u:r:maxmanager:s0 tclass=unix_stream_socket permissive=0", "maxmanager", &set),
          "the app's denied connect (daemon as TARGET) is accepted");
    CHECK(!sepol_parse_avc_line("avc:  denied  { read } for scontext=u:r:priv_app:s0 tcontext=u:object_r:sysfs:s0 tclass=file permissive=0", "maxmanager", &set),
          "a denial unrelated to the daemon is ignored");
    CHECK(!sepol_parse_avc_line("ordinary log line scontext=u:r:maxmanager:s0", "maxmanager", &set), "a non-avc line is ignored");
    CHECK(!sepol_parse_avc_line("avc:  denied  { read } for scontext=u:r:maxmanager:s0 tcontext=u:object_r:x:s0", "maxmanager", &set),
          "a denial without a class is ignored");
    CHECK(set.count == 2 && set.denials_seen == 3 && set.enforcing_seen == 2, "two rules, three denials, two enforcing");
    CHECK(!sepol_add(&set, "maxmanager", "bad name;rm", "file", "read"), "a malformed name never reaches the policy file");
    CHECK(!sepol_add(&set, "maxmanager", "x", "file", "re ad"), "a malformed permission never reaches the policy file");

    char* text = NULL;
    size_t text_size = 0;
    FILE* out = open_memstream(&text, &text_size);
    sepol_render(&set, out);
    fclose(out);
    CHECK(strstr(text, "allow maxmanager sysfs_batteryinfo:file { open read write };") != NULL, "permissions are merged and sorted");
    CHECK(strstr(text, "allow priv_app maxmanager:unix_stream_socket { connectto };") != NULL, "the app rule is rendered");

    SepolRules again;
    CHECK(sepol_init(&again), "a second set allocates");
    FILE* in = fmemopen(text, strlen(text), "r");
    CHECK(sepol_load_existing(in, &again) == 4 && again.count == 2, "the rendered file loads back for the next round (iteration)");
    fclose(in);
    sepol_add(&again, "maxmanager", "ptrace_target", "process", "ptrace");
    char* second = NULL;
    size_t second_size = 0;
    FILE* out2 = open_memstream(&second, &second_size);
    sepol_render(&again, out2);
    fclose(out2);
    CHECK(strstr(second, "REVIEW") != NULL, "a dangerous permission is flagged for review");
    free(second);
    free(text);
    sepol_free(&again);
    sepol_free(&set);

    /* الأمر كاملًا على ملفّات حقيقيّة: جولتان تتراكمان في الملفّ نفسه */
    {
        char avc_path[] = "/tmp/sepol_avc_XXXXXX";
        char out_path[] = "/tmp/sepol_out_XXXXXX";
        const int avc_fd = mkstemp(avc_path);
        const int out_fd = mkstemp(out_path);
        CHECK(avc_fd >= 0 && out_fd >= 0, "scratch files are created");
        if (avc_fd >= 0 && out_fd >= 0) {
            close(out_fd);
            unlink(out_path); /* الجولة الأولى تبدأ بلا ملفّ ناتج */
            const char* round1 = "avc:  denied  { read } for scontext=u:r:maxmanager:s0 tcontext=u:object_r:sysfs_a:s0 tclass=file permissive=1\n";
            CHECK(write(avc_fd, round1, strlen(round1)) == (ssize_t)strlen(round1), "round 1 log is written");
            close(avc_fd);
            char* args1[] = {"x", "--gen-sepolicy", avc_path, "-o", out_path};
            CHECK(handle_gen_sepolicy(5, args1) == 0, "round 1 succeeds");
            FILE* check = fopen(out_path, "r");
            char body[2048] = {0};
            if (check) {
                (void)fread(body, 1, sizeof(body) - 1, check);
                fclose(check);
            }
            CHECK(strstr(body, "allow maxmanager sysfs_a:file { read };") != NULL, "round 1 wrote the rule");

            FILE* append = fopen(avc_path, "w");
            if (append) {
                fputs("avc:  denied  { write } for scontext=u:r:maxmanager:s0 tcontext=u:object_r:sysfs_a:s0 tclass=file permissive=1\n", append);
                fclose(append);
            }
            char* args2[] = {"x", "--gen-sepolicy", avc_path, "-o", out_path};
            CHECK(handle_gen_sepolicy(5, args2) == 0, "round 2 succeeds");
            check = fopen(out_path, "r");
            memset(body, 0, sizeof(body));
            if (check) {
                (void)fread(body, 1, sizeof(body) - 1, check);
                fclose(check);
            }
            CHECK(strstr(body, "allow maxmanager sysfs_a:file { read write };") != NULL, "round 2 merged with round 1 instead of replacing it");
            unlink(avc_path);
            unlink(out_path);
        }
        char* none[] = {"x", "--gen-sepolicy", "/tmp/definitely_absent_avc_log", "-o", "-"};
        CHECK(handle_gen_sepolicy(5, none) == 1, "no denials found is reported as a failure, not an empty success");
    }
}

static int fake_con(const char* path, char* type, size_t size) {
    const char* value = "sysfs";
    if (strcmp(path, "/proc/meminfo") == 0) {
        value = "proc_meminfo";
    } else if (strncmp(path, "/proc", 5) == 0) {
        value = "proc";
    } else if (strncmp(path, "/dev/cpuset", 11) == 0) {
        value = "cgroup";
    } else if (strcmp(path, "/data/system/packages.list") == 0) {
        value = "packages_list_file";
    } else if (strncmp(path, "/data/adb", 9) == 0) {
        value = "adb_data_file";
    } else if (strncmp(path, "/data", 5) == 0) {
        value = "system_data_root_file";
    } else if (strncmp(path, "/dev", 4) == 0) {
        value = "device";
    } else if (strncmp(path, "/sys/kernel/debug", 17) == 0) {
        value = "debugfs";
    } else if (strncmp(path, "/sys/class/devfreq/x.mali/max_freq", 34) == 0) {
        value = "sysfs_devfreq";
    } else if (strstr(path, "/power_supply/battery/") != NULL) {
        value = "sysfs_batteryinfo";
    }
    snprintf(type, size, "%s", value);
    return 0;
}

static void make_dirs(const char* path) {
    char copy[256];
    snprintf(copy, sizeof(copy), "%s", path);
    for (char* slash = strchr(copy + 1, '/'); slash; slash = strchr(slash + 1, '/')) {
        *slash = '\0';
        (void)mkdir(copy, 0755);
        *slash = '/';
    }
    (void)mkdir(copy, 0755);
}

static void make_file(const char* root, const char* logical) {
    char full[300];
    snprintf(full, sizeof(full), "%s%s", root, logical);
    char dir[300];
    snprintf(dir, sizeof(dir), "%s", full);
    *strrchr(dir, '/') = '\0';
    make_dirs(dir);
    const int fd = open(full, O_WRONLY | O_CREAT, 0644);
    if (fd >= 0) {
        close(fd);
    }
}

static void suite_sepolicy_scan(void) {
    printf("\n\033[1m[sepolicy-scan] types read from the device, no logs and no rebuild\033[0m\n");
    char root[] = "/tmp/sepol_fake_XXXXXX";
    CHECK(mkdtemp(root) != NULL, "a fake device root is created");
    make_file(root, "/sys/class/power_supply/battery/capacity");
    make_file(root, "/sys/class/power_supply/battery/constant_charge_current_max");
    make_file(root, "/sys/class/devfreq/x.mali/max_freq");
    make_file(root, "/sys/kernel/debug/secret");
    make_file(root, "/proc/meminfo");
    make_file(root, "/dev/cpuset/top-app/cgroup.procs");
    make_file(root, "/data/system/packages.list");
    char config[300];
    snprintf(config, sizeof(config), "%s/data/adb/.config/MaxManager/x", root);
    make_dirs(config);

    sepol_fs_root = root;
    sepol_con_hook = fake_con;
    SepolRules set;
    CHECK(sepol_init(&set), "the scan rule set allocates");
    const int scanned = sepol_scan_device(&set, "maxmanager");
    sepol_con_hook = NULL;
    sepol_fs_root = "";
    CHECK(scanned > 5, "the scan checked the nodes it found");

    char* text = NULL;
    size_t text_size = 0;
    FILE* out = open_memstream(&text, &text_size);
    sepol_render(&set, out);
    fclose(out);
    CHECK(strstr(text, "allow maxmanager sysfs_batteryinfo:file { getattr open read setattr write };") != NULL,
          "a read node and a write node of one type merge into one rule");
    CHECK(strstr(text, "allow maxmanager sysfs_devfreq:file { getattr open read setattr write };") != NULL,
          "a variable-middle write node (devfreq max_freq) gets write permission");
    CHECK(strstr(text, "allow maxmanager proc_meminfo:file { getattr open read };") != NULL, "a read-only exact file gets read permission only");
    CHECK(strstr(text, "allow maxmanager cgroup:file { getattr open read };") != NULL, "the top-app cgroup file is covered");
    CHECK(strstr(text, "allow maxmanager packages_list_file:file { getattr open read };") != NULL, "packages.list is covered");
    CHECK(strstr(text, "allow maxmanager adb_data_file:dir {") != NULL && strstr(text, "add_name") != NULL,
          "the daemon's own config directory gets directory permissions");
    CHECK(strstr(text, "allow maxmanager adb_data_file:file {") != NULL && strstr(text, "create") != NULL,
          "files created inside the config directory are covered");
    {
        const char* dir_rule = strstr(text, "allow maxmanager sysfs:dir {");
        const char* dir_end = dir_rule ? strchr(dir_rule, ';') : NULL;
        char dir_text[160] = {0};
        if (dir_rule && dir_end && (size_t)(dir_end - dir_rule) < sizeof(dir_text)) {
            memcpy(dir_text, dir_rule, (size_t)(dir_end - dir_rule));
        }
        CHECK(strstr(dir_text, "search") != NULL && strstr(dir_text, "getattr") != NULL,
              "ancestor and walked directories get search and getattr (merged into one dir rule)");
    }
    CHECK(strstr(text, "debugfs") == NULL, "a node outside the allowlists never gets a rule");
    free(text);
    sepol_free(&set);
    char cleanup[320];
    snprintf(cleanup, sizeof(cleanup), "rm -rf %s", root);
    CHECK(system(cleanup) == 0, "the fake root is removed");
}

static void suite_foreground_observer(void) {
    printf("\n\033[1m[foreground] the native observer that replaces the root companion\033[0m\n");

    char pkg[128];
    CHECK(fg_package_from_cmdline("com.miHoYo.GenshinImpact", pkg, sizeof(pkg)) && strcmp(pkg, "com.miHoYo.GenshinImpact") == 0,
          "a plain package name is accepted");
    CHECK(fg_package_from_cmdline("com.tencent.ig:remote", pkg, sizeof(pkg)) && strcmp(pkg, "com.tencent.ig") == 0,
          "a :subprocess suffix is stripped back to the package");
    CHECK(!fg_package_from_cmdline("zygote64", pkg, sizeof(pkg)), "a system process without a dot is not an app");
    CHECK(!fg_package_from_cmdline("/system/bin/surfaceflinger", pkg, sizeof(pkg)), "a path is not a package");
    CHECK(!fg_package_from_cmdline("0.85", pkg, sizeof(pkg)), "a number is not a package (the documented 0.85 failure)");
    CHECK(!fg_package_from_cmdline(".hidden.app", pkg, sizeof(pkg)), "a leading dot is not a package");
    CHECK(!fg_package_from_cmdline("com.x.", pkg, sizeof(pkg)), "a trailing dot is not a package");
    CHECK(!fg_package_from_cmdline("com.a b.c", pkg, sizeof(pkg)), "a space is not a package character");
    CHECK(!fg_package_from_cmdline("", pkg, sizeof(pkg)), "an empty command line is not a package");
    CHECK(!fg_package_from_cmdline(NULL, pkg, sizeof(pkg)), "NULL is not a package");

    const char* list = "{\"com.tencent.ig\": {\"mode\": 1}, \"com.mobile.legends\" :{\"mode\": 2},\n \"note\": \"com.fake.value\"}";
    CHECK(fg_gamelist_contains(list, "com.tencent.ig"), "a managed package is found as a key");
    CHECK(fg_gamelist_contains(list, "com.mobile.legends"), "whitespace before the colon is tolerated");
    CHECK(!fg_gamelist_contains(list, "com.fake.value"), "a string value is not a key");
    CHECK(!fg_gamelist_contains(list, "com.tencent"), "a package prefix is not a match");
    CHECK(!fg_gamelist_contains("{}", "com.tencent.ig"), "an empty list manages nothing");
    CHECK(!fg_gamelist_contains(NULL, "com.tencent.ig"), "no list manages nothing");

    char text[512];
    fg_format_status(text, sizeof(text), "com.tencent.ig", 4242, 10234, 1, 87, 0, "sw-1");
    CHECK_STR_EQ(text,
                 "focused_app com.tencent.ig 4242 10234\nscreen_awake 1\nbattery_saver 0\nzen_mode 0\n"
                 "app_name com.tencent.ig\nbattery_level 87\nis_charging 0\nswitch_id sw-1\n",
                 "the status text matches the format the daemon parses");
}

void suite_ipc(void) {
    suite_foreground_observer();
    suite_sepolicy_scan();
    suite_sepolicy_gen();
    printf("\n\033[1m[ipc] the app-to-daemon channel: closed whitelist, identity first\033[0m\n");

    IpcCommand cmd;
    CHECK(ipc_parse_command("PING\n", &cmd) && cmd.kind == IPC_CMD_PING, "PING parses");
    CHECK(ipc_parse_command("STATUS", &cmd) && cmd.kind == IPC_CMD_STATUS, "STATUS parses without a newline");
    CHECK(ipc_parse_command("PROFILE 2\n", &cmd) && cmd.kind == IPC_CMD_PROFILE && cmd.profile == '2' && !cmd.from_ai,
          "PROFILE 2 is a manual selection");
    CHECK(ipc_parse_command("PROFILE 1 AI\r\n", &cmd) && cmd.profile == '1' && cmd.from_ai,
          "PROFILE 1 AI carries the from-ai flag (CRLF tolerated)");

    CHECK(!ipc_parse_command("PROFILE 4\n", &cmd), "profile 4 does not exist");
    CHECK(!ipc_parse_command("PROFILE 0\n", &cmd), "profile 0 is initialize-only and not reachable");
    CHECK(!ipc_parse_command("PROFILE 12\n", &cmd), "two digits are not a profile");
    CHECK(!ipc_parse_command("PROFILE 1; reboot\n", &cmd), "a shell separator is refused before any parsing");
    CHECK(!ipc_parse_command("PROFILE $(id)\n", &cmd), "command substitution is refused");
    CHECK(!ipc_parse_command("PROFILE 1 AI extra\n", &cmd), "trailing words are refused");
    CHECK(!ipc_parse_command("PROFILE 1 ROOT\n", &cmd), "only the AI flag exists");
    CHECK(!ipc_parse_command("PING now\n", &cmd), "PING takes no argument");
    CHECK(!ipc_parse_command("RUN id\n", &cmd), "there is no generic run verb");
    CHECK(!ipc_parse_command("\n", &cmd), "an empty line is not a command");
    CHECK(!ipc_parse_command(NULL, &cmd), "NULL is not a command");

    uid_t uid = 0;
    const char* list = "com.android.shell 2000 0 /data/user/0/com.android.shell default:targetSdk=36\n"
                       "nd.max 10234 0 /data/user/0/nd.max default:targetSdk=36 1078,3002\n"
                       "nd.max.other 10999 0 /data/user/0/nd.max.other default:targetSdk=36\n";
    CHECK(ipc_app_uid_from_text(list, "nd.max", &uid) && uid == 10234, "the app uid is read from packages.list");
    CHECK(!ipc_app_uid_from_text("nd.max.other 10999 0 /x\n", "nd.max", &uid), "a longer package name is not a prefix match");
    CHECK(!ipc_app_uid_from_text("", "nd.max", &uid), "an empty list has no app");

    CHECK(ipc_uid_allowed(10234, true, 10234), "the installed app uid is allowed");
    CHECK(ipc_uid_allowed(0, false, 0), "root is allowed for local tools");
    CHECK(!ipc_uid_allowed(10235, true, 10234), "another app is refused");
    CHECK(!ipc_uid_allowed(2000, true, 10234), "the adb shell is refused in production");
    CHECK(!ipc_uid_allowed(10234, false, 0), "unknown app uid means refuse, never allow");

    char reply[IPC_REPLY_MAX];
    g_ipc_calls = 0;
    g_ipc_rc = 0;
    ipc_roundtrip(0, "PING\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK 1\n", "PING round-trips");
    ipc_roundtrip(0, "STATUS\n", reply, sizeof(reply));
    CHECK(strncmp(reply, "OK state=", 9) == 0, "STATUS reports daemon state");
    ipc_roundtrip(0, "PROFILE 3 AI\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK\n", "an allowed profile request succeeds");
    CHECK(g_ipc_calls == 1 && g_ipc_profile == '3' && g_ipc_ai, "the runner received exactly profile 3 with the AI flag");

    g_ipc_rc = 1;
    ipc_roundtrip(0, "PROFILE 2\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR exit=1\n", "a runner failure (e.g. manual blocked by AI) is reported, not hidden");

    const int calls_before = g_ipc_calls;
    ipc_roundtrip(12345, "PROFILE 1\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR denied\n", "an unknown peer is denied");
    CHECK(g_ipc_calls == calls_before, "a denied peer never reaches the runner");
    ipc_roundtrip(0, "PROFILE 9\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR invalid\n", "an invalid command is refused");
    CHECK(g_ipc_calls == calls_before, "an invalid command never reaches the runner");


    /* ── المرحلة الثانية: قراءة العقد، فحص الوجود، كتابة خصائص الإعداد ── */
    CHECK(ipc_parse_command("READ /sys/class/power_supply/battery/capacity\n", &cmd) && cmd.kind == IPC_CMD_READ &&
              strcmp(cmd.path, "/sys/class/power_supply/battery/capacity") == 0,
          "READ of a battery node parses");
    CHECK(ipc_parse_command("EXISTS /sys/class/kgsl/kgsl-3d0/gpubusy", &cmd) && cmd.kind == IPC_CMD_EXISTS,
          "EXISTS of a GPU node parses");
    CHECK(ipc_parse_command("READ /proc/meminfo", &cmd), "an allowed /proc file parses");

    CHECK(!ipc_parse_command("READ /etc/passwd", &cmd), "a path outside the list is refused");
    CHECK(!ipc_parse_command("READ /data/adb/modules/x", &cmd), "/data is never readable through the channel");
    CHECK(!ipc_parse_command("READ /proc/self/environ", &cmd), "/proc/self is not on the list");
    CHECK(!ipc_parse_command("READ /sys/kernel/debug/tracing/trace", &cmd), "debugfs is not on the list");
    CHECK(!ipc_parse_command("READ /sys/class/power_supply/../../../etc/passwd", &cmd), "dot-dot traversal is refused");
    CHECK(!ipc_parse_command("READ /sys/class/thermal//thermal_zone0/temp", &cmd), "a double slash is refused");
    CHECK(!ipc_parse_command("READ /sys/class/thermal/", &cmd), "a bare directory prefix is not a node");
    CHECK(!ipc_parse_command("READ /sys/class/thermal/a b", &cmd), "a space splits tokens, so extra words are refused");
    CHECK(!ipc_parse_command("READ /sys/class/thermal/$(id)", &cmd), "shell metacharacters are refused");
    CHECK(!ipc_parse_command("READ", &cmd), "READ needs a path");

    CHECK(ipc_parse_command("SETPROP persist.sys.maxmanagerconf.fastchg_ma 3000", &cmd) && cmd.kind == IPC_CMD_SETPROP &&
              strcmp(cmd.key, "persist.sys.maxmanagerconf.fastchg_ma") == 0 && strcmp(cmd.value, "3000") == 0,
          "SETPROP of a config key parses");
    CHECK(!ipc_parse_command("SETPROP persist.sys.maxmanager.state 1", &cmd), "the daemon's own state keys are not writable");
    CHECK(!ipc_parse_command("SETPROP ro.build.fingerprint x", &cmd), "system keys are not writable");
    CHECK(!ipc_parse_command("SETPROP persist.sys.maxmanagerconf. 1", &cmd), "an empty key suffix is refused");
    CHECK(!ipc_parse_command("SETPROP persist.sys.maxmanagerconf.a one two", &cmd), "extra tokens are refused");
    CHECK(!ipc_parse_command("SETPROP persist.sys.maxmanagerconf.a $(id)", &cmd), "a value with shell syntax is refused");
    CHECK(!ipc_parse_command("SETPROP persist.sys.maxmanagerconf.a", &cmd), "SETPROP needs a value");

    CHECK(ipc_resolved_allowed("/sys/devices/platform/charger/power_supply/battery/capacity"), "a real sysfs target is fine");
    CHECK(!ipc_resolved_allowed("/etc/passwd"), "a link that resolves outside the kernel filesystems is refused");
    CHECK(!ipc_resolved_allowed("/sys/kernel/debug/x"), "a link into debugfs is refused");
    CHECK(!ipc_resolved_allowed("/proc/self/environ"), "a link into /proc/self is refused");

    ipc_roundtrip(0, "EXISTS /proc/meminfo\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK 1\n", "EXISTS finds a real node");
    ipc_roundtrip(0, "EXISTS /sys/class/power_supply/definitely_absent_node\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK 0\n", "EXISTS reports an allowed-but-absent node as 0");
    ipc_roundtrip(0, "READ /proc/meminfo\n", reply, sizeof(reply));
    CHECK(strncmp(reply, "OK ", 3) == 0 && strstr(reply, "\nMemTotal") != NULL, "READ returns a length header then the node body");
    ipc_roundtrip(0, "READ /etc/passwd\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR invalid\n", "an unlisted path never reaches open()");

    g_ipc_setprops = 0;
    ipc_roundtrip(0, "SETPROP persist.sys.maxmanagerconf.sic_boost 1\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK\n", "an allowed SETPROP succeeds");
    CHECK(g_ipc_setprops == 1 && strcmp(g_ipc_prop_key, "persist.sys.maxmanagerconf.sic_boost") == 0 &&
              strcmp(g_ipc_prop_value, "1") == 0,
          "the setter received exactly the requested pair");
    ipc_roundtrip(0, "SETPROP ro.debuggable 1\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR invalid\n", "a forbidden SETPROP is refused");
    CHECK(g_ipc_setprops == 1, "a forbidden SETPROP never reaches the setter");
    ipc_roundtrip(12345, "SETPROP persist.sys.maxmanagerconf.sic_boost 0\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR denied\n", "a denied peer cannot set properties");
    CHECK(g_ipc_setprops == 1, "a denied peer never reaches the setter");


    /* ── المرحلة الثالثة: الكتابة (WRITE) — أضيق قائمة، ورقصة chmod، وقيم ضيّقة ── */
    CHECK(ipc_parse_command("WRITE /sys/devices/system/cpu/cpu6/cpufreq/scaling_max_freq 2500000\n", &cmd) &&
              cmd.kind == IPC_CMD_WRITE && strcmp(cmd.value, "2500000") == 0,
          "WRITE of a CPU ceiling parses");
    CHECK(ipc_parse_command("WRITE /sys/devices/system/cpu/cpufreq/policy4/scaling_governor schedutil", &cmd), "a cpufreq policy governor parses");
    CHECK(ipc_parse_command("WRITE /sys/class/devfreq/13000000.mali/max_freq 900000000", &cmd), "a devfreq ceiling parses");
    CHECK(ipc_parse_command("WRITE /sys/module/ged/parameters/gpu_cust_upbound_freq 1000000", &cmd), "a GED parameter parses");
    CHECK(ipc_parse_command("WRITE /proc/sys/vm/swappiness 60", &cmd), "swappiness parses");
    CHECK(ipc_parse_command("WRITE /sys/block/zram0/disksize 4294967296", &cmd), "zram disksize parses");
    CHECK(ipc_parse_command("WRITE /sys/block/sda/queue/scheduler mq-deadline", &cmd), "an IO scheduler parses");
    CHECK(ipc_parse_command("WRITE /sys/class/power_supply/battery/constant_charge_current_max 3000000", &cmd), "a charge-current node parses");
    CHECK(ipc_parse_command("WRITE /sys/devices/virtual/thermal/thermal_message/cpu_limits 480000 1400000", &cmd) &&
              strcmp(cmd.value, "480000 1400000") == 0,
          "a value with an inner space keeps the rest of the line");
    CHECK(ipc_parse_command("READ /proc/sys/net/ipv4/tcp_congestion_control", &cmd), "a writable node is also readable, for read-back verification");

    CHECK(!ipc_parse_command("WRITE /proc/sys/kernel/core_pattern x", &cmd), "core_pattern (code execution as root) is not writable");
    CHECK(!ipc_parse_command("WRITE /proc/sys/kernel/modprobe x", &cmd), "modprobe is not writable");
    CHECK(!ipc_parse_command("WRITE /sys/devices/system/cpu/online 0", &cmd), "an empty middle segment is not a CPU node");
    CHECK(!ipc_parse_command("WRITE /proc/1234/dolby_vision/enable 1", &cmd), "a fixed node cannot be reached through a variable middle");
    CHECK(!ipc_parse_command("WRITE /sys/class/power_supply/battery/capacity 1", &cmd), "a read-only attribute is not writable");
    CHECK(!ipc_parse_command("WRITE /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq/../x 1", &cmd), "dot-dot is refused for writes too");
    CHECK(!ipc_parse_command("WRITE /etc/passwd root", &cmd), "arbitrary files are not writable");
    CHECK(!ipc_parse_command("WRITE /data/adb/x 1", &cmd), "/data is not writable");
    CHECK(!ipc_parse_command("WRITE /proc/sys/vm/swappiness", &cmd), "WRITE needs a value");
    CHECK(!ipc_parse_command("WRITE /proc/sys/vm/swappiness 1;reboot", &cmd), "shell syntax in a value is refused");
    CHECK(!ipc_parse_command("WRITE /proc/sys/vm/swappiness $(id)", &cmd), "command substitution in a value is refused");

    CHECK(ipc_write_value_allowed("1") && ipc_write_value_allowed("schedutil") && ipc_write_value_allowed("480000 1400000"), "ordinary values are accepted");
    CHECK(!ipc_write_value_allowed("") && !ipc_write_value_allowed(" 1") && !ipc_write_value_allowed("1 "), "empty or padded values are refused");
    CHECK(!ipc_write_value_allowed("0123456789012345678901234567890123456789012345678901234567890123456789"), "an over-long value is refused");

    {
        char tmp_template[] = "/tmp/ipc_write_node_XXXXXX";
        const int tmp_fd = mkstemp(tmp_template);
        CHECK(tmp_fd >= 0, "a scratch node can be created");
        if (tmp_fd >= 0) {
            close(tmp_fd);
            chmod(tmp_template, 0444); /* مثل عقدة HyperOS 3: لا كتابة */
            CHECK(ipc_write_node(tmp_template, "7") == 0, "a 0444 node is written through the chmod dance");
            struct stat info;
            CHECK(stat(tmp_template, &info) == 0 && (info.st_mode & 07777) == 0444, "the original mode is restored after the write");
            char back[16] = {0};
            FILE* check = fopen(tmp_template, "r");
            if (check) {
                (void)fgets(back, sizeof(back), check);
                fclose(check);
            }
            CHECK_STR_EQ(back, "7\n", "the node holds the written value plus a newline");
            unlink(tmp_template);
        }
        CHECK(ipc_write_node("/tmp", "1") == EINVAL, "a directory is not a node");
        CHECK(ipc_write_node("/tmp/definitely_absent_node_xyz", "1") == ENOENT, "an absent node reports ENOENT");
    }

    ipc_roundtrip(0, "WRITE /sys/class/devfreq/zzz.absent/governor performance\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR notfound\n", "a write to an absent node is reported, not invented");
    CHECK(ipc_parse_command("CANWRITE /proc/sys/vm/swappiness", &cmd) && cmd.kind == IPC_CMD_CANWRITE, "CANWRITE of a listed node parses");
    CHECK(!ipc_parse_command("CANWRITE /proc/meminfo", &cmd), "a read-only node is not a CANWRITE candidate");
    CHECK(!ipc_parse_command("CANWRITE /proc/sys/vm/swappiness 1", &cmd), "CANWRITE takes no value");
    ipc_roundtrip(0, "CANWRITE /sys/class/devfreq/zzz.absent/governor\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "OK 0\n", "CANWRITE on an absent node is 0");
    ipc_roundtrip(0, "WRITE /proc/sys/kernel/core_pattern x\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR invalid\n", "an unlisted write never reaches open()");
    ipc_roundtrip(12345, "WRITE /proc/sys/vm/swappiness 60\n", reply, sizeof(reply));
    CHECK_STR_EQ(reply, "ERR denied\n", "a denied peer cannot write");

    /* السوكيت الحقيقيّ في الفضاء المجرّد + SO_PEERCRED: الطرف هنا هو هذه العملية نفسها (uid الاختبار)،
     * وهو ليس تطبيقًا مسجَّلًا في packages.list — فالجواب الصحيح **رفض** (والجذر يُسمح له). */
    start_ipc_server();
    const int client = socket(AF_UNIX, SOCK_STREAM, 0);
    struct sockaddr_un address;
    memset(&address, 0, sizeof(address));
    address.sun_family = AF_UNIX;
    memcpy(address.sun_path + 1, IPC_SOCKET_NAME, strlen(IPC_SOCKET_NAME));
    const socklen_t address_length = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + strlen(IPC_SOCKET_NAME));
    CHECK(client >= 0 && connect(client, (struct sockaddr*)&address, address_length) == 0,
          "the abstract @maxmanager.sock listener accepts a connection");
    if (client >= 0) {
        char live[64] = {0};
        (void)send(client, "PING\n", 5, 0);
        const ssize_t got = recv(client, live, sizeof(live) - 1, 0);
        live[got > 0 ? got : 0] = '\0';
        CHECK_STR_EQ(live, getuid() == 0 ? "OK 1\n" : "ERR denied\n",
                     "the live socket applies the peer-uid check (root allowed, an unregistered uid denied)");
        close(client);
    }

    /* وضع الخادم المستقلّ: يتبع علم الروم، ويغيب بغيره فيبقى سلوك الموديول كما كان. */
    CHECK(!is_standalone_mode(), "without ro.maxmanager.sepolicy the daemon is not standalone");
}

int main(void) {
    printf("\033[1mMaxManager — archdaemon host parity suite\033[0m\n");
    printf("Real daemon units compiled for the host with an Android-property shim.\n");
    printf("This proves LOGIC parity only; device/sysfs behaviour is out of scope here.\n");

    install_fake_getprop();

    suite_prop_validator();
    suite_string_utils();
    suite_profile_logic();
    suite_max_ai_mode();
    suite_freq_balance_gate();
    suite_app_status_gate();
    suite_bypass_registry();
    suite_resolution();
    suite_renderer();
    suite_per_app_governor();
    suite_module_integrity();
    suite_ipc();
    suite_file_protocols();
    suite_app_loader();
    suite_cli_profile();
    suite_broadcast();

    printf("\n\033[1mSummary: %d/%d checks passed\033[0m\n",
           g_checks_run - g_checks_failed, g_checks_run);
    if (g_suites_skipped) {
        /* لا يُطوى التخطّي في رقم النجاح: «234/234» على مضيف يُعلن `policyN` ليست تغطيةً كاملة. */
        printf("\033[33m%d suite(s) SKIPPED by this host — their assertions are NOT VERIFIED "
               "here, and no claim is made about them\033[0m\n", g_suites_skipped);
    }
    if (g_checks_failed) {
        printf("\033[31m%d check(s) FAILED\033[0m\n", g_checks_failed);
        return 1;
    }
    printf("\033[32mall checks passed\033[0m\n");
    return 0;
}
