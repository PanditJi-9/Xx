/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
 * CLI contract suite — the Control channel of `docs/ai/ARCHITECTURE-AUDIT.md` §12.1.
 *
 * What it measures
 * ----------------
 * `BinaryCLI/CLIUtility.c::handle_profile` decides whether a `--profile` request is applied,
 * rejected, or blocked because MAX AI is driving. That decision is the only brake between a
 * user tap and a full system-wide profile rewrite, and it was reachable only through argv on a
 * rooted device — so it had no test at all.
 *
 * Here the real function is called with the real argv shapes, and the **reference table**
 * `fixtures/contracts/cli_profile.tsv` supplies every case. The table is data, not a second
 * copy of the logic: adding a row adds a falsifiable case without touching this file.
 *
 * The limit: this proves the decision, not the outcome. `run_profiler` and `notify` are
 * recorded, not executed — no sysfs write happens, no notification is posted, and the actual
 * profile application still needs a device.
 */
#include <unistd.h>

#include <MaxManager.h>

#include "host/host.h"

#ifndef FIXTURE_DIR
#error "FIXTURE_DIR must be defined by the harness Makefile (the path to fixtures/contracts/)"
#endif

/* الرقم المثبَّت: عدد صفوف الحالة في الجدول المرجعي (يُعاد قياسه عند تغيير مقصود). */
#define CLI_TABLE_ROWS 16
#define CLI_COLUMNS 8

static bool is_dash(const char* value) {
    return value && value[0] == '-' && value[1] == '\0';
}

/*
 * `handle_profile` narrates its decision on stdout (`printf`) and its refusals on stderr
 * (`fprintf`). Both are the function's real behaviour and are asserted through the captured log
 * and notification channels instead — printing them here would interleave with the PASS/FAIL
 * lines and make the table unreadable. So both streams are parked on /dev/null for the single
 * call and restored immediately after.
 */
static void silence_streams_begin(int saved[2]) {
    saved[0] = -1;
    saved[1] = -1;
    fflush(stdout);
    fflush(stderr);
    FILE* sink = fopen("/dev/null", "w");
    if (!sink)
        return;
    saved[0] = dup(fileno(stdout));
    saved[1] = dup(fileno(stderr));
    if (saved[0] >= 0)
        dup2(fileno(sink), fileno(stdout));
    if (saved[1] >= 0)
        dup2(fileno(sink), fileno(stderr));
    fclose(sink);
}

static void silence_streams_end(int saved[2]) {
    fflush(stdout);
    fflush(stderr);
    if (saved[0] >= 0) {
        dup2(saved[0], fileno(stdout));
        close(saved[0]);
    }
    if (saved[1] >= 0) {
        dup2(saved[1], fileno(stderr));
        close(saved[1]);
    }
}

void suite_cli_profile(void) {
    printf("\n\033[1m[cli_profile] --profile / --from-ai decision table\033[0m\n");

    char path[512];
    snprintf(path, sizeof(path), "%s/cli_profile.tsv", FIXTURE_DIR);

    host_clear_routes();

    /*
     * `run_profiler` تُنادي `is_kanged()` فعلًا — حارس الهوية يُعاد التحقّق منه عند كل تطبيق
     * ملفّ — فالمسار الحقيقي هنا يقرأ `module.prop`. و**هذا الموضع نفسه كان عطبًا مقيسًا** على
     * جهاز: الملفّ المشحون لم يطابق نصّ الحارس القديم، فخرج الخادم من داخل هذا النداء وأسكت كل
     * ما يملكه. فيُوجَّه المسار إلى الملفّ المشحون (لا إلى نصّ مكتوب في الاختبار)، ويُثبَّت الحكم
     * صريحًا **قبل** التشغيل: فلو انحرف الاسم مرّة أخرى ظهر سطر FAIL برسالته، ولم يختفِ نصف
     * المجموعة بصمت. وغياب هذا التوجيه يوقف المجموعة كلها عند `exit` — وهو سلوك مقصود ومُعلَن.
     */
    host_route_open(MODULE_PROP, REPO_DIR "/mainfiles/module.prop");
    CHECK(!module_identity_violated(),
          "run_profiler re-checks module identity, and the shipped module.prop passes it");

    FILE* fp = fopen(path, "r");
    CHECK(fp != NULL, "the reference table opens (fixtures/contracts/cli_profile.tsv)");
    if (!fp)
        return;

    char line[512];
    int rows = 0;

    while (fgets(line, sizeof(line), fp)) {
        char* newline = strchr(line, '\n');
        if (newline)
            *newline = '\0';
        if (line[0] == '\0' || line[0] == '#')
            continue;

        char* field[CLI_COLUMNS] = {0};
        int columns = 1;
        field[0] = line;
        for (char* p = line; *p != '\0' && columns < CLI_COLUMNS; p++) {
            if (*p == '\t') {
                *p = '\0';
                field[columns++] = p + 1;
            }
        }
        /* The table's own header row documents the column order; it is not a case. */
        if (columns == CLI_COLUMNS && strcmp(field[0], "label") == 0)
            continue;
        if (columns != CLI_COLUMNS) {
            char bad[160];
            snprintf(bad, sizeof(bad), "row %d has %d columns, expected %d", rows + 1, columns, CLI_COLUMNS);
            CHECK(false, bad);
            continue;
        }
        rows++;

        const char* ai = is_dash(field[1]) ? "" : field[1];
        const char* profile_arg = is_dash(field[2]) ? "" : field[2];
        const char* extra = is_dash(field[3]) ? "" : field[3];
        int expected_exit = atoi(field[4]);

        host_reset_capture();
        host_prop_reset();
        host_prop_set("persist.sys.maxmanagerconf.AIenabled", ai[0] ? ai : "0");

        /*
         * `extra` قد يحمل **أكثر من عَلَم** مفصولة بفراغ (`--verbose --from-ai`)، لأن الدلالة
         * المُثبَّتة أن `--from-ai` يُقبل في **أي فهرس ≥ ٣** — وحالة بعَلَم واحد لا تقيس ذلك أبدًا.
         */
        char extra_copy[192];
        snprintf(extra_copy, sizeof(extra_copy), "%s", extra);

        const char* argv[8];
        int argc = 0;
        argv[argc++] = "sys.maxmanager-service";
        argv[argc++] = "--profile";
        argv[argc++] = profile_arg;
        for (char* token = strtok(extra_copy, " "); token != NULL && argc < 7; token = strtok(NULL, " "))
            argv[argc++] = token;

        char label[192];
        int saved[2];
        silence_streams_begin(saved);
        int rc = handle_profile(argc, (char**)argv);
        silence_streams_end(saved);

        snprintf(label, sizeof(label), "[%s] exit code", field[0]);
        CHECK(rc == expected_exit, label);

        snprintf(label, sizeof(label), "[%s] log line", field[0]);
        if (is_dash(field[5]))
            CHECK(!host_log_contains("CLI_PROFILE_"), label);
        else
            CHECK(host_log_contains(field[5]), label);

        snprintf(label, sizeof(label), "[%s] notification", field[0]);
        if (is_dash(field[6]))
            CHECK(host_notify_count() == 0, label);
        else
            CHECK(host_notify_contains(field[6]), label);

        /*
         * الإثبات على الأثر الحقيقي: `run_profiler` **لا يُلتَفّ حولها** في هذا الـharness، فهي
         * تُنفَّذ فعلًا ويرصد `systemv` أمر الثنائية الذي تُصدره. فيُقاس الطريق كاملًا من argv إلى
         * الأمر، لا سجلّ يقف مكان الدالة.
         */
        snprintf(label, sizeof(label), "[%s] profiler target", field[0]);
        if (is_dash(field[7]))
            CHECK(!host_saw_systemv("sys.maxmanager-profilesettings"), label);
        else {
            char expected_cmd[64];
            snprintf(expected_cmd, sizeof(expected_cmd), "sys.maxmanager-profilesettings %s", field[7]);
            CHECK(host_saw_systemv(expected_cmd), label);
        }

        /*
         * ── كتابة ملفّي الملف العام — الفجوة الأولى في §١٢.٦ ────────────────────────────
         * كان `write2file` بديلًا صامتًا، فلم يكن أحد يرى **أين يُكتب أيُّ رقم**، وسجّلته الوثيقة
         * فجوة. الآن يُسجّل النداء بمساره وصيغته وقيمته، ويُشترط أن الملفّين (`PROFILE_MODE`
         * و`PROFILE_MODE_APP`) تلقّيا **نفس الرقم الذي طلبه المستخدم**. وما لا يُقاس هنا: أن البايتات
         * بلغت قرص هاتف — وهذا يبقى ادّعاءً يحتاج جهازًا.
         */
        snprintf(label, sizeof(label), "[%s] profile file writes", field[0]);
        if (is_dash(field[7])) {
            CHECK(host_write_count() == 0, label);
        } else {
            int wanted = atoi(field[7]);
            int matched = 0;
            for (int i = 0; i < host_write_count(); i++) {
                const char* path = host_write_path(i);
                bool is_profile_file = strcmp(path, PROFILE_MODE) == 0 || strcmp(path, PROFILE_MODE_APP) == 0;
                if (is_profile_file && host_write_has_int(i) && host_write_int(i) == wanted)
                    matched++;
            }
            CHECK(matched == 2, label);
        }

        /*
         * الاختيار اليدوي صار نيّة المستخدم الواحدة: يُكتب `MAX_AI_MODE` مرة واحدة
         * بنفس الرقم. ومسار `--from-ai` لا يكتبها — المحرك ينفّذ نيّة كتبها
         * التطبيق مسبقًا، وكل خطوة AI ليست اختيارًا جديدًا من المستخدم.
         */
        snprintf(label, sizeof(label), "[%s] max-ai mode write", field[0]);
        int mode_writes = 0;
        int wanted_mode = is_dash(field[7]) ? -1 : atoi(field[7]);
        for (int i = 0; i < host_write_count(); i++) {
            if (strcmp(host_write_path(i), MAX_AI_MODE) == 0 && host_write_has_int(i) &&
                host_write_int(i) == wanted_mode)
                mode_writes++;
        }
        bool from_ai = strstr(extra, "--from-ai") != NULL;
        bool should_write_mode = wanted_mode > 0 && !from_ai;
        CHECK(mode_writes == (should_write_mode ? 1 : 0), label);
    }

    fclose(fp);

    /*
     * ── بوّابة التوفّر: `require_daemon_running()` ────────────────────────────────────────
     * وهي أوّل بوّابة في `Main.c` قبل كل أمر بعد `--rerun`، وكانت في الطبعة الأولى من هذا
     * الـharness **مفترضة** لا مقيسة (stub يعلن الخادم عاملًا دائمًا). تُقاس الآن في الفرعين.
     */
    int saved_gate[2];

    host_set_daemon_running(false);
    silence_streams_begin(saved_gate);
    int refused = require_daemon_running();
    silence_streams_end(saved_gate);
    CHECK(refused == 0, "a stopped daemon makes require_daemon_running refuse (0)");

    host_set_daemon_running(true);
    silence_streams_begin(saved_gate);
    int allowed = require_daemon_running();
    silence_streams_end(saved_gate);
    CHECK(allowed == 1, "a running daemon makes require_daemon_running allow (1)");

    host_set_daemon_running(true);

    /*
     * ── عزل الحاكم بين العمليتين: `--profile` لا يملك جلسة Per-App ────────────────────────
     *
     * **العطب الذي تُثبِّته هذه الكتلة:** مسار `--profile` يعمل في **عملية أخرى** غير الخادم،
     * فـ`gamestart` و`opts` فيها فارغان دائمًا ولو كانت لعبة مُدارة في المقدّمة. وكان
     * `run_profiler()` يعيد حساب علم العزل من هذين الفارغين فينشر `0`، فيرى `binprofiles` أن
     * لا مالك للتجاوز ويكتب حاكم/سقف الـGPU العام فوق سقف التطبيق — وهو «البروفايل يرتدّ بعد
     * لمسة في لوحة اللعبة» بعينه (ثم يعيده إصلاح الانحراف بعد ١٠ ثوانٍ فيرتدّ ثانيًا).
     *
     * والقياس هنا على **الخاصية نفسها** بعد نداء `handle_profile` الحقيقي — لا على سطر سجلّ
     * فقط — ولذلك ثلاث حالات متقابلة:
     *   ١. علم مرفوع + CLI  ⇒ يبقى مرفوعًا (ولا يستطيع الخادم أن يفقده من عملية لا تعرف حاله).
     *   ٢. لا جلسة حيّة (علم `0`) ⇒ يبقى `0` ولا سطر «preserved» كاذب.
     *   ٣. الخادم نفسه (هدف محلي بلا أي تجاوز) ⇒ **يُنزل** العلم، وإلا صار الحرّاس صمّاء للأبد.
     */
    static const char* cli_profiles[] = {"1", "2", "3"};
    for (size_t p = 0; p < sizeof(cli_profiles) / sizeof(cli_profiles[0]); p++) {
        host_reset_capture();
        host_prop_reset();
        host_prop_set("persist.sys.maxmanagerconf.AIenabled", "0");
        host_prop_set("sys.maxmanager.perapp.governor_isolation", "1");

        const char* argv[] = {"sys.maxmanager-service", "--profile", cli_profiles[p]};
        int saved_iso[2];
        silence_streams_begin(saved_iso);
        int rc = handle_profile(3, (char**)argv);
        silence_streams_end(saved_iso);

        char live[PROP_VALUE_MAX] = {0};
        __system_property_get("sys.maxmanager.perapp.governor_isolation", live);

        char iso_label[192];
        snprintf(iso_label, sizeof(iso_label),
                 "[isolation] CLI profile %s leaves a live per-app flag raised", cli_profiles[p]);
        CHECK(rc == 0 && strcmp(live, "1") == 0, iso_label);

        snprintf(iso_label, sizeof(iso_label),
                 "[isolation] CLI profile %s records the preserve decision", cli_profiles[p]);
        CHECK(host_log_contains("EVENT=PERAPP_ISOLATION_PRESERVED"), iso_label);
    }

    host_reset_capture();
    host_prop_reset();
    host_prop_set("persist.sys.maxmanagerconf.AIenabled", "0");
    host_prop_set("sys.maxmanager.perapp.governor_isolation", "0");
    {
        const char* argv[] = {"sys.maxmanager-service", "--profile", "2"};
        int saved_iso[2];
        silence_streams_begin(saved_iso);
        int rc = handle_profile(3, (char**)argv);
        silence_streams_end(saved_iso);
        char live[PROP_VALUE_MAX] = {0};
        __system_property_get("sys.maxmanager.perapp.governor_isolation", live);
        CHECK(rc == 0 && live[0] == '0', "[isolation] an idle system stays cleared (no session invented)");
        CHECK(!host_log_contains("EVENT=PERAPP_ISOLATION_PRESERVED"),
              "[isolation] no false 'preserved' line without a live flag");
    }

    /*
     * الحالة الثالثة: الخادم نفسه. `gamestart` هدف محلي، وكل حقول `opts` صريحة `default`،
     * فلا تجاوز ⇒ العلم يُنزل. وهذه هي الدعوى التي تمنع "الحل" من أن يصير قفلًا دائمًا.
     */
    {
        char* saved_gamestart = gamestart;
        GameConfig saved_opts = opts;

        gamestart = strdup("com.example.hostgame");
        memset(&opts, 0, sizeof(opts));
        strcpy(opts.gpu_profile, "default");
        strcpy(opts.thermal_profile, "default");
        strcpy(opts.cpu_governor, "default");
        strcpy(opts.gpu_governor, "default");
        strcpy(opts.gpu_max_freq, "default");
        host_reset_capture();
        host_prop_reset();
        host_prop_set("sys.maxmanager.perapp.governor_isolation", "1");
        run_profiler(PERFORMANCE_PROFILE);
        char live[PROP_VALUE_MAX] = {0};
        __system_property_get("sys.maxmanager.perapp.governor_isolation", live);
        CHECK(live[0] == '0', "[isolation] a local target with no overrides lowers the flag");
        CHECK(!host_log_contains("EVENT=PERAPP_ISOLATION_PRESERVED"),
              "[isolation] a local target never takes the preserve branch");

        /* والعكس: تجاوز صريح محلي ⇒ العلم مرفوع من نفس العملية بلا سطر preserved. */
        strcpy(opts.gpu_profile, "gaming");
        host_prop_set("sys.maxmanager.perapp.governor_isolation", "0");
        run_profiler(PERFORMANCE_PROFILE);
        __system_property_get("sys.maxmanager.perapp.governor_isolation", live);
        CHECK(strcmp(live, "1") == 0, "[isolation] a local per-app override raises the flag");

        free(gamestart);
        gamestart = saved_gamestart;
        opts = saved_opts;
    }

    /*
     * A table silently truncated by a bad edit would make this suite assert fewer things while
     * still printing "all checks passed" — the failure mode this pin exists to catch.
     */
    CHECK(rows == CLI_TABLE_ROWS, "the reference table still carries every row (re-measure on intentional change)");
}
