/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
 * gamelist contract suite — the C half of `fixtures/contracts/gamelist.compact.json`.
 *
 * ما يقيسه هذا الملف، ولماذا وُجد
 * ------------------------------
 * `AppLoader/AppLoader.c::reload_gamelist_cache()` هو **القارئ الوحيد** لملفّ
 * `gamelist/maxmanagerApplist.json`، وهو مدخل كل ميزة لكل تطبيق في الخادم: بروفايل الأداء،
 * وDND، والتردد، والعرض، والدقّة، والحاكم — كلها تبدأ من `get_gamestart()`, وهو لا يُعيد
 * شيئًا إن كانت هذه الذاكرة فارغة. وكان الشكل **ضمنيًّا**: التطبيق يكتبه بـKotlin، والخادم
 * يقرؤه بـC، ولا شيء يقيس التهجئة بينهما — فصار الاثنان يتكلّمان شكلين مختلفين:
 *
 *   يكتب التطبيق (`JsonObject.toString()`, بلا فراغ):  {"com.x":{"perf_lite_mode":"lite"}}
 *   ويبحث الخادم عن (`strstr`, بفراغ واحد):            `": {"`
 *
 * ```
 * (جهاز المستخدم · POCO X6 Pro · 2026-10-15 · ١٢ تطبيقًا في ملفّه)
 * EVENT=GAMELIST_LOADED count=0
 * API/gameinfo  NULL 0 0
 * ```
 *
 * فلم يُطبَّق شيء: «يبقى على المتوازن ولو فتحت اللعبة». وهذه المجموعة تُثبّت أن القارئ يقرأ
 * **الشكل الذي يكتبه التطبيق فعلًا**، والشكل المتفارج، واسم المفتاح القديم في الملفّ المزروع —
 * ويُقاس الملفّ نفسه من الطرفين: `GamelistContractTest` (Kotlin) يُنتجه من مسار الكتابة
 * الحقيقيّ **بالحرف**، وهذا الملفّ يُمرّره إلى القارئ الحقيقيّ عبر `--wrap=fopen`.
 *
 * والحدّ: هذا يقيس **القراءة**، لا مواصلة المسار بعده. أن `System.c` ينادي `get_gamestart()`
 * في المسار الصحيح يبقى ادّعاء قراءةِ كود؛ وأن الجهاز يكتب الملفّ ويُشعل `inotify` **يحتاج جهازًا**.
 */
#include <MaxManager.h>

#include "host/host.h"

#ifndef FIXTURE_DIR
#error "FIXTURE_DIR must be defined by the harness Makefile (the path to fixtures/contracts/)"
#endif
#ifndef REPO_DIR
#error "REPO_DIR must be defined by the harness Makefile (the repository root)"
#endif

/* يوجّه `fopen(GAMELIST)` — أي المسار المدفون في المصدر — إلى ملفّ من العُقَد. */
static void route_gamelist_to(const char* fixture_name) {
    static char path[512];
    snprintf(path, sizeof(path), "%s/%s", FIXTURE_DIR, fixture_name);
    host_route_open(GAMELIST, path);
}

/*
 * `is_initialize_complete = false` مقصود: هو الفرع الذي يكتب `EVENT=GAMELIST_LOADED count=N` —
 * السطر الذي كان يقول `count=0` على جهاز المستخدم ولا أحد يقرؤه. فالدعوى تقيس السطر نفسه.
 */
static void load_gamelist(void) {
    DaemonContext ctx;
    memset(&ctx, 0, sizeof(ctx));
    reload_gamelist_cache(&ctx);
}

void suite_app_loader(void) {
    printf("\n\033[1m[app_loader] gamelist contract (Kotlin writes / C reads)\033[0m\n");

    /*
     * ── ١) الشكل الذي يكتبه التطبيق: مضغوط بلا فراغ بعد النقطتين ──────────────────────────
     *
     * القيم هنا **هي** القيم المُعلَنة في `GamelistContractTest.kt` من الطرف الآخر: ملفّ واحد
     * يُقاس من اللغتين. وكل حقل بقيمة مختلفة عن غيره وعن الافتراضي، فانزياح حقل إلى حقل
     * ظاهر في السطر لا مُخمَّن.
     */
    route_gamelist_to("gamelist.compact.json");
    host_reset_capture();
    load_gamelist();

    CHECK(g_game_cache_count == 1, "the compact (no-space) document yields one app entry");
    CHECK(host_log_contains("EVENT=GAMELIST_LOADED count=1"),
          "the field-evidence line reports the real count, not 0");

    CHECK_STR_EQ(g_game_cache[0].package, "com.example.game", "package name parses from the compact key");
    CHECK_STR_EQ(g_game_cache[0].perf_lite_mode, "lite", "perf_lite_mode parses");
    CHECK_STR_EQ(g_game_cache[0].app_priority, "high", "app_priority parses");
    CHECK_STR_EQ(g_game_cache[0].game_preload, "true", "game_preload parses");
    CHECK_STR_EQ(g_game_cache[0].gpu_profile, "performance", "gpu_profile parses");
    CHECK_STR_EQ(g_game_cache[0].cpu_governor, "performance", "cpu_governor parses");
    CHECK_STR_EQ(g_game_cache[0].gpu_governor, "msm-adreno-tz", "gpu_governor parses");
    CHECK_STR_EQ(g_game_cache[0].gpu_max_freq, "650000000", "gpu_max_freq parses");
    CHECK_STR_EQ(g_game_cache[0].thermal_profile, "power", "thermal_profile parses");
    CHECK_STR_EQ(g_game_cache[0].refresh_rate, "120", "refresh_rate parses");
    CHECK_STR_EQ(g_game_cache[0].renderer, "vulkan", "renderer parses");
    CHECK_STR_EQ(g_game_cache[0].resolution_downscale, "0.75", "resolution_downscale parses");
    CHECK_STR_EQ(g_game_cache[0].dnd_on_gaming, "true", "dnd_on_gaming parses");
    CHECK_STR_EQ(g_game_cache[0].bypass_charging, "true", "bypass_charging parses");
    CHECK_STR_EQ(g_game_cache[0].cpu_policy_controls, "policy0|lock|300000|1800000",
                 "cpu_policy_controls parses; a value with `|` and `;`-free payload stays intact");

    /*
     * ── والنتيجة التي يراها المستخدم: المدخل الفعليّ لا الذاكرة ────────────────────────────
     *
     * `get_gamestart()` هو ما يناديه `System.c` ليُطبّق كل مقبض، والقيمة التي يعيدها هي
     * "هل فيه شيء يُنفَّذ لهذا التطبيق". فدعوى على الحقول وحدها لا تكفي: تُقاس هنا الدالّة
     * التي كان الوضع في الميدان يقول عنها «لا شيء».
     *
     * والتطبيق المرئيّ يأتي من `host_set_visible_package` — وهو نفس ما تفعله المجموعة السابقة:
     * قراءة التطبيق المرئيّ من النظام ليست من وحدات هذا الـharness (تُقرأ في الجهاز من
     * `/proc`)، فالبديل المُعلَن هو تمريره صريحًا.
     */
    host_set_visible_package("com.example.game");

    GameConfig out;
    memset(&out, 0, sizeof(out));
    char* matched = get_gamestart(&out, NULL);
    CHECK(matched != NULL, "the foregrounded app is found — this is what stayed NULL on the device");
    if (matched) {
        CHECK_STR_EQ(matched, "com.example.game", "get_gamestart returns the foregrounded package");
        CHECK_STR_EQ(out.gpu_profile, "performance", "the profile path receives the per-app gpu_profile");
        CHECK_STR_EQ(out.dnd_on_gaming, "true", "the DND path receives the per-app setting");
        CHECK_STR_EQ(out.resolution_downscale, "0.75", "the resolution path receives the per-app value");
        free(matched);
    }

    /* وتطبيق غير موجود يبقى غير موجود: القارئ لا «يجد» أي شيء لمجرد أن الملفّ مقروء. */
    host_set_visible_package("com.absent.app");
    CHECK(get_gamestart(&out, NULL) == NULL, "an app with no entry is still not a match");
    host_set_visible_package(NULL);

    /*
     * ── ٢) الشكل المتفارج واسم المفتاح القديم ─────────────────────────────────────────────
     *
     * ثلاثة صفوف في `gamelist.legacy.json` تقيس ثلاث قواعد دفعة واحدة:
     *   (أ) `"com.legacy.seed" : {` — فراغ **قبل** النقطتين (شكل يدويّ مشروع في JSON).
     *   (ب) مفتاح الملفّ المزروع `resolution_target` يُقرأ حين يغيب الاسم القانونيّ.
     *   (ج) البحث **لا يتجاوز كتلة التطبيق**: صفّ بلا مفتاح تصغير يقرأ `default` ولو كان
     *       المفتاح موجودًا في صفّ لاحق — وهو ما كان `strstr` بلا حدّ يخالفـه.
     */
    route_gamelist_to("gamelist.legacy.json");
    host_reset_capture();
    load_gamelist();

    CHECK(g_game_cache_count == 3, "the three legacy/spaced entries all parse");

    CHECK_STR_EQ(g_game_cache[0].package, "com.legacy.seed", "a space before the colon is still a member");
    CHECK_STR_EQ(g_game_cache[0].resolution_downscale, "0.5",
                 "the shipped seed's `resolution_target` spelling is read, not dropped to default");
    CHECK_STR_EQ(g_game_cache[0].dnd_on_gaming, "true", "a spaced value still parses");
    CHECK_STR_EQ(g_game_cache[0].renderer, "skiavk", "fields after the aliased key parse too");

    CHECK_STR_EQ(g_game_cache[1].package, "com.no.res.key", "an entry without the knob still parses");
    CHECK_STR_EQ(g_game_cache[1].refresh_rate, "144", "its own fields parse");
    CHECK_STR_EQ(g_game_cache[1].resolution_downscale, "default",
                 "an absent knob reads as the declared default — the scan stops at the entry's end");

    CHECK_STR_EQ(g_game_cache[2].resolution_downscale, "0.75",
                 "when both spellings are present the canonical `resolution_downscale` wins");
    CHECK_STR_EQ(g_game_cache[2].package, "com.both.keys", "the both-keys entry parses");

    /*
     * ── ٣) نواة الوحدة المشحونة: ١١ تطبيقًا مزروعًا لا تُقرأ كصفر ─────────────────────────
     *
     * هذا هو **الملفّ الذي يشحنه المستودع** (`maxmanagerApplist.json`، ينسخه `customize.sh` إلى
     * `gamelist/`)، لا نصًّا مكتوبًا هنا. وشكله متفارج، فلو عاد القارئ إلى مطابقة شكل واحد
     * سقطت هذه الدعوى قبل أن يصل عطب إلى جهاز.
     */
    host_clear_routes();
    host_route_open(GAMELIST, REPO_DIR "/maxmanagerApplist.json");
    host_reset_capture();
    load_gamelist();

    CHECK(g_game_cache_count > 0, "the shipped seed parses to at least one entry");
    CHECK(host_log_contains("EVENT=GAMELIST_LOADED count="), "the count line is emitted for the seed too");
    CHECK(!host_log_contains("EVENT=GAMELIST_LOAD_EMPTY"),
          "the shipped seed is never reported as an unreadable document");

    bool names_present = true;
    bool curated_defaults_intact = true;
    int seen_legends = 0;
    for (int i = 0; i < g_game_cache_count; i++) {
        if (g_game_cache[i].package[0] == '\0')
            names_present = false;
        if (strcmp(g_game_cache[i].package, "com.mobile.legends") == 0) {
            seen_legends++;
            /*
             * المزروع كله `default` في مفاتيحه السبعة: فقراءة قيمة أخرى تعني أن حقلًا قُرئ من
             * حقل آخر. و`cpu_policy_controls` غائب عن المزروع فيقرأ فارغًا (عقد الافتراضي).
             */
            if (strcmp(g_game_cache[i].refresh_rate, "default") != 0 ||
                strcmp(g_game_cache[i].renderer, "default") != 0 ||
                strcmp(g_game_cache[i].resolution_downscale, "default") != 0 ||
                strcmp(g_game_cache[i].perf_lite_mode, "default") != 0 ||
                strcmp(g_game_cache[i].dnd_on_gaming, "default") != 0 ||
                strcmp(g_game_cache[i].app_priority, "default") != 0 ||
                strcmp(g_game_cache[i].game_preload, "default") != 0 ||
                g_game_cache[i].cpu_policy_controls[0] != '\0')
                curated_defaults_intact = false;
        }
    }
    CHECK(names_present, "no seed entry parses with an empty package name (the key walk-back holds)");
    CHECK(seen_legends == 1, "a curated game is found exactly once by its package name");
    CHECK(curated_defaults_intact,
          "every curated value reads as written (re-measure if the seed intentionally changes)");

    /*
     * ── ٤) الصفر المشبوك يُعلَن: صفرٌ بلا سطر يخفي عطبًا (وهو ما جرى في الميدان) ──────────
     *
     * وثانيًا: مستند `{}` — وهو مستخدم حذف كل تطبيقاته — **لا** يُحذَّر عليه، فلا يصير التحذير
     * ضجيجًا يدفع القارئ إلى تجاهله.
     */
    host_clear_routes();
    FILE* empty_object = fopen(BUILD_DIR "/gamelist_empty.json", "w");
    CHECK(empty_object != NULL, "the host can write the empty-document fixture");
    if (empty_object) {
        fputs("{}", empty_object);
        fclose(empty_object);
    }
    host_route_open(GAMELIST, BUILD_DIR "/gamelist_empty.json");
    host_reset_capture();
    load_gamelist();
    CHECK(g_game_cache_count == 0, "an empty document parses to zero entries");
    CHECK(!host_log_contains("EVENT=GAMELIST_LOAD_EMPTY"),
          "an empty document is a legitimate state, not a shape failure");

    host_clear_routes();
    FILE* members_not_objects = fopen(BUILD_DIR "/gamelist_not_objects.json", "w");
    CHECK(members_not_objects != NULL, "the host can write the non-object document fixture");
    if (members_not_objects) {
        fputs("{\"com.example.game\": [1, 2]}", members_not_objects);
        fclose(members_not_objects);
    }
    host_route_open(GAMELIST, BUILD_DIR "/gamelist_not_objects.json");
    host_reset_capture();
    load_gamelist();
    CHECK(g_game_cache_count == 0, "a document whose members are not objects yields no entry");
    CHECK(host_log_contains("EVENT=GAMELIST_LOAD_EMPTY count=0 size="),
          "a document that declares content and matches nothing is reported, not silently zeroed");

    host_clear_routes();
    host_route_open(GAMELIST, BUILD_DIR "/no_such_gamelist_here.json");
    host_reset_capture();
    load_gamelist();
    CHECK(host_log_contains("EVENT=GAMELIST_LOAD_FAILED reason=fopen_failed"),
          "an unreadable gamelist names its reason");
    CHECK(g_game_cache_count == 0,
          "an unreadable file clears the cache instead of leaving a stale one (no removed app keeps its overrides)");

    host_clear_routes();
    free_gamelist_cache();
}
