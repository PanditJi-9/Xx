/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#include <MaxManager.h>
#include <IpcServer.h>

#include <errno.h>
#include <limits.h>
#include <fcntl.h>
#include <pthread.h>
#include <spawn.h>
#include <stdatomic.h>
#include <sys/stat.h>
#include <time.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <unistd.h>

/* bionic يعرّف `struct ucred` دائمًا؛ glibc (مضيف الاختبار) لا يعرّفه بلا `_GNU_SOURCE` والـprelude سبقنا إليه. */
#if defined(__GLIBC__) && !defined(__USE_GNU)
struct ucred {
    pid_t pid;
    uid_t uid;
    gid_t gid;
};
#endif

#define IPC_MAX_CLIENTS 8
#define IPC_READ_TIMEOUT_SEC 2

extern char** environ;

static bool char_in(char c, const char* extra) {
    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        return true;
    }
    return c != '\0' && strchr(extra, c) != NULL;
}

static bool all_chars_in(const char* text, const char* extra) {
    for (const char* p = text; *p; p++) {
        if (!char_in(*p, extra)) {
            return false;
        }
    }
    return true;
}

/**
 * @brief يحلّل سطر أمر ويقبله فقط إن طابق القائمة البيضاء حرفًا بحرف.
 *
 * كل فعل له مجموعة حروفه الضيّقة، والتحقّق قبل أيّ تفسير: فلا تصل إلى `posix_spawn` إلا رقم الملف
 * (`1`/`2`/`3`) الذي اختاره هذا الملفّ بنفسه، ولا إلى `open` إلا مسار اجتاز [ipc_path_allowed]،
 * ولا إلى `__system_property_set` إلا زوج اجتاز [ipc_prop_allowed] — لا نصّ من العميل يُنفَّذ.
 */
bool ipc_parse_command(const char* line, IpcCommand* out) {
    if (!line || !out) {
        return false;
    }
    memset(out, 0, sizeof(*out));

    char text[IPC_LINE_MAX];
    size_t length = strnlen(line, sizeof(text));
    if (length == 0 || length >= sizeof(text)) {
        return false;
    }
    memcpy(text, line, length);
    text[length] = '\0';
    while (length > 0 && (text[length - 1] == '\n' || text[length - 1] == '\r' || text[length - 1] == ' ')) {
        text[--length] = '\0';
    }
    if (length == 0 || !all_chars_in(text, " /._:@+,=-")) {
        return false;
    }

    char* save = NULL;
    const char* verb = strtok_r(text, " ", &save);
    if (!verb) {
        return false;
    }
    if (strcmp(verb, "PING") == 0 || strcmp(verb, "STATUS") == 0) {
        if (strtok_r(NULL, " ", &save) != NULL) {
            return false;
        }
        out->kind = (verb[0] == 'P') ? IPC_CMD_PING : IPC_CMD_STATUS;
        return true;
    }
    if (strcmp(verb, "PROFILE") == 0) {
        const char* arg = strtok_r(NULL, " ", &save);
        if (!arg || arg[1] != '\0' || (arg[0] != '1' && arg[0] != '2' && arg[0] != '3')) {
            return false;
        }
        const char* flag = strtok_r(NULL, " ", &save);
        if (flag) {
            if (strcmp(flag, "AI") != 0 || strtok_r(NULL, " ", &save) != NULL) {
                return false;
            }
            out->from_ai = true;
        }
        out->kind = IPC_CMD_PROFILE;
        out->profile = arg[0];
        return true;
    }
    if (strcmp(verb, "READ") == 0 || strcmp(verb, "EXISTS") == 0) {
        const char* path = strtok_r(NULL, " ", &save);
        if (!path || strtok_r(NULL, " ", &save) != NULL || strlen(path) >= sizeof(out->path) ||
            !(ipc_path_allowed(path) || ipc_write_path_allowed(path))) {
            return false;
        }
        snprintf(out->path, sizeof(out->path), "%s", path);
        out->kind = (verb[0] == 'R') ? IPC_CMD_READ : IPC_CMD_EXISTS;
        return true;
    }
    if (strcmp(verb, "CANWRITE") == 0) {
        const char* path = strtok_r(NULL, " ", &save);
        if (!path || strtok_r(NULL, " ", &save) != NULL || strlen(path) >= sizeof(out->path) || !ipc_write_path_allowed(path)) {
            return false;
        }
        snprintf(out->path, sizeof(out->path), "%s", path);
        out->kind = IPC_CMD_CANWRITE;
        return true;
    }
    if (strcmp(verb, "WRITE") == 0) {
        const char* path = strtok_r(NULL, " ", &save);
        const char* value = strtok_r(NULL, "", &save); /* باقي السطر: القيمة قد تحمل مسافة ("480000 1400000") */
        if (!path || !value || strlen(path) >= sizeof(out->path) || !ipc_write_path_allowed(path) ||
            !ipc_write_value_allowed(value)) {
            return false;
        }
        snprintf(out->path, sizeof(out->path), "%s", path);
        snprintf(out->value, sizeof(out->value), "%s", value);
        out->kind = IPC_CMD_WRITE;
        return true;
    }
    if (strcmp(verb, "SETPROP") == 0) {
        const char* key = strtok_r(NULL, " ", &save);
        const char* value = strtok_r(NULL, " ", &save);
        if (!key || !value || strtok_r(NULL, " ", &save) != NULL || !ipc_prop_allowed(key, value)) {
            return false;
        }
        snprintf(out->key, sizeof(out->key), "%s", key);
        snprintf(out->value, sizeof(out->value), "%s", value);
        out->kind = IPC_CMD_SETPROP;
        return true;
    }
    return false;
}

/*
 * القراءة المسموحة — **مأخوذة من قياس ما يقرؤه التطبيق فعلًا** (٣٠٣ موضعًا في ٥٩ ملفًّا): عقد المعالج
 * والـGPU والحرارة والشحن وذاكرة الاستبدال، ومؤشّرات `/proc` الإحصائية. قراءة فقط، وبلا `debugfs`
 * ولا `selinux` ولا `firmware` ولا أيّ `/data`. وهي **قائمة تُضيَّق** لا تُوسَّع: ما لا يُقرأ هنا يبقى
 * يُقرأ بالجذر في التطبيق كما كان.
 */
static const char* const READ_PREFIXES[] = {
    "/sys/devices/system/", "/sys/class/power_supply/", "/sys/class/devfreq/", "/sys/class/kgsl/",
    "/sys/class/thermal/",  "/sys/class/graphics/",     "/sys/module/ged/",    "/sys/kernel/ged/",
    "/sys/kernel/gpu/",     "/sys/block/zram0/",        "/proc/pressure/",     "/proc/sys/vm/",
};
static const char* const READ_FILES[] = {
    "/proc/stat", "/proc/meminfo", "/proc/cpuinfo", "/proc/uptime", "/proc/swaps", "/proc/loadavg",
};

/**
 * @brief المسار مطلوبًا: حروف ضيّقة، مطلق، بلا `..` ولا `//`، وتحت بادئة مسموحة أو ملفّ مسموح بعينه.
 */
bool ipc_path_allowed(const char* path) {
    if (!path || path[0] != '/' || strlen(path) >= IPC_PATH_MAX || !all_chars_in(path, "/._:@+-") ||
        strstr(path, "..") != NULL || strstr(path, "//") != NULL) {
        return false;
    }
    for (size_t index = 0; index < sizeof(READ_FILES) / sizeof(READ_FILES[0]); index++) {
        if (strcmp(path, READ_FILES[index]) == 0) {
            return true;
        }
    }
    for (size_t index = 0; index < sizeof(READ_PREFIXES) / sizeof(READ_PREFIXES[0]); index++) {
        const size_t prefix_length = strlen(READ_PREFIXES[index]);
        if (strncmp(path, READ_PREFIXES[index], prefix_length) == 0 && path[prefix_length] != '\0') {
            return true;
        }
    }
    return false;
}

/*
 * الكتابة المسموحة — **أضيق من القراءة بكثير**، ومأخوذة من قياس ما يكتبه التطبيق فعلًا (٣٥ موضعًا في ١٦ ملفًّا
 * + عقد الشحن في الخادم). كل قاعدة = بادئة + لاحقات: المسار يبدأ بالبادئة وينتهي بإحدى اللاحقات بالضبط، فلا
 * يكفي أن يقع المسار تحت مجلّد مسموح. وما ليس هنا (مثل `core_pattern` و`modprobe` و`/proc/sys/kernel/` كلّه)
 * يبقى مرفوضًا — فالتطبيق المخترَق لا يملك بهذه القناة تنفيذ شيفرة بصلاحية الجذر.
 */
typedef struct {
    const char* prefix;
    bool exact; /* true: المسار = البادئة + اللاحقة بلا وسط. false: وسط غير فارغ (cpu0/cpufreq، اسم قرص…) */
    const char* suffixes[14];
} WriteRule;

static const WriteRule WRITE_RULES[] = {
    {"/sys/devices/system/cpu/", false, {"/scaling_governor", "/scaling_min_freq", "/scaling_max_freq", "/online", "/boost", NULL}},
    {"/sys/class/devfreq/", false, {"/governor", "/min_freq", "/max_freq", NULL}},
    {"/sys/class/kgsl/", false, {"/max_gpuclk", "/min_clock_mhz", "/max_clock_mhz", "/min_pwrlevel", "/max_pwrlevel", "/thermal_pwrlevel", "/idle_timer", "/governor", "/min_freq", "/max_freq", NULL}},
    {"/sys/module/ged/parameters/", true, {"/gpu_cust_boost_freq", "/gpu_cust_upbound_freq", "/gpu_dvfs_enable", NULL}},
    {"/sys/kernel/ged/hal/", true, {"/custom_upbound_gpu_freq", "/custom_boost_gpu_freq", NULL}},
    {"/sys/devices/virtual/thermal/thermal_message/", true, {"/cpu_limits", "/sconfig", NULL}},
    {"/proc/powerhal_cpu_ctrl/", true, {"/perfserv_freq", NULL}},
    {"/proc/sys/vm/", true, {"/swappiness", "/compact_memory", NULL}},
    {"/proc/sys/kernel/", true, {"/printk", "/sched_autogroup_enabled", "/sched_bore", "/sched_child_runs_first", "/sched_cstate_aware", "/sched_schedstats", "/sched_tunable_scaling", "/sched_util_clamp_max", "/sched_util_clamp_min", NULL}},
    {"/proc/sys/net/ipv4/", true, {"/tcp_congestion_control", "/tcp_ecn", "/tcp_fastopen", "/tcp_sack", "/tcp_syncookies", "/tcp_tw_reuse", NULL}},
    {"/sys/block/", false, {"/queue/scheduler", "/disksize", "/reset", "/comp_algorithm", "/mem_limit", NULL}},
    {"/proc/", true, {"/dolby_vision/enable", "/mtk_video_enhance/enable", "/silky_brightness/enable", "/touch_boost/enable", "/touchpanel/double_tap_enable", "/touchpanel/game_switch_enable", NULL}},
    {"/sys/class/graphics/", true, {"/fb0/aod_brightness_smooth", "/fb0/hdr_enable", "/fb0/video_enhance", NULL}},
    {"/sys/class/leds/", true, {"/lcd-backlight/hbm", NULL}},
    {"/sys/devices/platform/", true, {"/mtk-hbm/hbm_enable", NULL}},
    {"/sys/android_touch/", true, {"/double_tap_enable", "/doubletap2wake", "/game_switch_enable", NULL}},
    {"/sys/touchpanel/", true, {"/double_tap_enable", "/game_switch_enable", NULL}},
    {"/sys/class/touch/", true, {"/touch_dev/double_tap_enable", "/touch_dev/game_switch_enable", NULL}},
    {"/sys/class/touchscreen/", true, {"/double_tap_enable", "/game_switch_enable", NULL}},
    {"/sys/devices/virtual/touch/", true, {"/touch_dev/game_switch_enable", NULL}},
    {"/sys/class/power_supply/", false, {"/constant_charge_current_max", "/fast_charge_current_max", "/input_current_limit", "/charge_control_limit", "/sic_mode", "/input_suspend", "/battery_input_suspend", "/charging_enabled", "/battery_charging_enabled", "/charge_enabled", "/charge_disable", "/stop_charge", NULL}},
    {"/sys/class/qcom-battery/", true, {"/sic_mode", NULL}},
    {"/proc/mtk_battery_cmd/", true, {"/sic_mode", NULL}},
};

static bool ends_with(const char* text, const char* suffix) {
    const size_t text_length = strlen(text);
    const size_t suffix_length = strlen(suffix);
    return text_length >= suffix_length && strcmp(text + text_length - suffix_length, suffix) == 0;
}

/**
 * @brief مسار كتابة مسموح: حروف ضيّقة، مطلق، بلا `..`/`//`، وبادئة + لاحقة من [WRITE_RULES] **معًا**، ووسطٌ غير فارغ
 * بينهما إلا حين تكون البادئة نفسها مجلّد العقدة (مثل `/sys/touchpanel/`).
 */
bool ipc_write_path_allowed(const char* path) {
    if (!path || path[0] != '/' || strlen(path) >= IPC_PATH_MAX || !all_chars_in(path, "/._:@+-") ||
        strstr(path, "..") != NULL || strstr(path, "//") != NULL) {
        return false;
    }
    for (size_t rule = 0; rule < sizeof(WRITE_RULES) / sizeof(WRITE_RULES[0]); rule++) {
        const size_t prefix_length = strlen(WRITE_RULES[rule].prefix);
        if (strncmp(path, WRITE_RULES[rule].prefix, prefix_length) != 0) {
            continue;
        }
        const size_t path_length = strlen(path);
        for (size_t index = 0; WRITE_RULES[rule].suffixes[index] != NULL; index++) {
            const char* suffix = WRITE_RULES[rule].suffixes[index];
            const size_t suffix_length = strlen(suffix); /* تبدأ بـ'/'، والبادئة تنتهي بـ'/' */
            if (WRITE_RULES[rule].exact) {
                if (path_length == prefix_length + suffix_length - 1 && strcmp(path + prefix_length, suffix + 1) == 0) {
                    return true;
                }
            } else if (path_length > prefix_length + suffix_length && ends_with(path, suffix)) {
                return true; /* وسط غير فارغ بين البادئة واللاحقة */
            }
        }
    }
    return false;
}

/**
 * @brief قيمة كتابة مسموحة: ١..٦٤ حرفًا من `[A-Za-z0-9 _.:,+-]` بلا مسافة في الطرفين (تُمرَّر إلى عقدة، لا إلى صدفة).
 */
bool ipc_write_value_allowed(const char* value) {
    if (!value) {
        return false;
    }
    const size_t length = strlen(value);
    if (length == 0 || length > IPC_WRITE_VALUE_MAX || value[0] == ' ' || value[length - 1] == ' ') {
        return false;
    }
    return all_chars_in(value, " _.:,+-");
}

/**
 * @brief يكتب القيمة في عقدة **حُسم مسارها** — ويعيد `0` أو `errno`.
 *
 * رقصة الصلاحيّات (`chmod`): عقد HyperOS 3 تأتي `0444` فترفض الكتابة حتى من الجذر (EACCES)، فإن لم تكن العقدة
 * قابلة للكتابة تُجعل `0644` ثم تُكتب ثم **تُعاد صلاحيّتها الأصليّة** — نفس ما كان يفعله التطبيق في الصدفة.
 */
int ipc_write_node(const char* resolved, const char* value) {
    struct stat info;
    if (stat(resolved, &info) != 0) {
        return errno;
    }
    if (!S_ISREG(info.st_mode)) {
        return EINVAL;
    }
    const mode_t original = info.st_mode & 07777;
    const bool relax = (original & 0200) == 0;
    if (relax && chmod(resolved, 0644) != 0) {
        return errno;
    }
    int result = 0;
    const int fd = open(resolved, O_WRONLY | O_CLOEXEC);
    if (fd < 0) {
        result = errno;
    } else {
        char line[IPC_WRITE_VALUE_MAX + 2];
        const int length = snprintf(line, sizeof(line), "%s\n", value);
        if (length < 0 || write(fd, line, (size_t)length) != (ssize_t)length) {
            result = errno ? errno : EIO;
        }
        close(fd);
    }
    if (relax) {
        (void)chmod(resolved, original);
    }
    return result;
}

/**
 * @brief يعدّد ما يجب فحص نوعه في SELinux لتوليد القواعد: كل ما تسمح به القائمتان (قراءة وكتابة) — فلا تنحرف
 * قواعد السياسة عن ما تسمح به القناة فعلًا.
 */
void ipc_scan_targets(IpcScanEmit emit, void* context) {
    for (size_t index = 0; index < sizeof(READ_FILES) / sizeof(READ_FILES[0]); index++) {
        emit(READ_FILES[index], false, context);
    }
    for (size_t index = 0; index < sizeof(READ_PREFIXES) / sizeof(READ_PREFIXES[0]); index++) {
        emit(READ_PREFIXES[index], true, context);
    }
    for (size_t rule = 0; rule < sizeof(WRITE_RULES) / sizeof(WRITE_RULES[0]); rule++) {
        if (!WRITE_RULES[rule].exact) {
            emit(WRITE_RULES[rule].prefix, true, context);
            continue;
        }
        for (size_t index = 0; WRITE_RULES[rule].suffixes[index] != NULL; index++) {
            char path[IPC_PATH_MAX];
            snprintf(path, sizeof(path), "%s%s", WRITE_RULES[rule].prefix, WRITE_RULES[rule].suffixes[index] + 1);
            emit(path, false, context);
        }
    }
}

/**
 * @brief المسار **بعد حلّ الروابط**: يبقى داخل `/sys` أو `/proc` ولا يدخل ما يُحجَب.
 *
 * عقد sysfs كلّها روابط (`/sys/class/power_supply/battery` ← `/sys/devices/platform/...`) فلا يصحّ
 * إعادة فرض البادئة نفسها؛ لكن لا رابط مسموح أن يخرج بالقارئ من نظام ملفّات النواة الوهميّ.
 */
bool ipc_resolved_allowed(const char* resolved) {
    if (!resolved) {
        return false;
    }
    const bool in_sys = strncmp(resolved, "/sys/", 5) == 0;
    const bool in_proc = strncmp(resolved, "/proc/", 6) == 0;
    if (!in_sys && !in_proc) {
        return false;
    }
    static const char* const BLOCKED[] = {"/sys/kernel/debug", "/sys/fs/", "/sys/firmware/", "/proc/self/", "/proc/kcore", "/proc/kmsg"};
    for (size_t index = 0; index < sizeof(BLOCKED) / sizeof(BLOCKED[0]); index++) {
        if (strncmp(resolved, BLOCKED[index], strlen(BLOCKED[index])) == 0) {
            return false;
        }
    }
    return true;
}

/**
 * @brief الكتابة المسموحة: مفاتيح إعداد MaxManager وحدها، وقيم ضيّقة الحروف.
 *
 * `persist.sys.maxmanager.*` (بلا `conf`) **خارج** القائمة عن قصد: فيها حالة الخادم نفسها، فلا يكتبها التطبيق.
 */
bool ipc_prop_allowed(const char* key, const char* value) {
    if (!key || !value) {
        return false;
    }
    const size_t prefix_length = strlen(IPC_PROP_PREFIX);
    if (strncmp(key, IPC_PROP_PREFIX, prefix_length) != 0 || key[prefix_length] == '\0' || strlen(key) >= IPC_KEY_MAX ||
        !all_chars_in(key, "._")) {
        return false;
    }
    return strlen(value) < IPC_VALUE_MAX && value[0] != '\0' && all_chars_in(value, "._:+,-");
}

/**
 * @brief يستخرج uid التطبيق من نصّ `packages.list` (`<package> <uid> <debuggable> <dataDir> ...`).
 */
bool ipc_app_uid_from_text(const char* text, const char* package, uid_t* uid) {
    if (!text || !package || !uid) {
        return false;
    }
    const size_t package_length = strlen(package);
    const char* cursor = text;
    while (*cursor) {
        const char* end = strchr(cursor, '\n');
        const size_t line_length = end ? (size_t)(end - cursor) : strlen(cursor);
        if (line_length > package_length + 1 && strncmp(cursor, package, package_length) == 0 &&
            cursor[package_length] == ' ') {
            char* parse_end = NULL;
            const char* number = cursor + package_length + 1;
            const unsigned long value = strtoul(number, &parse_end, 10);
            if (parse_end != number && (*parse_end == ' ' || *parse_end == '\n' || *parse_end == '\0')) {
                *uid = (uid_t)value;
                return true;
            }
        }
        if (!end) {
            break;
        }
        cursor = end + 1;
    }
    return false;
}

/**
 * @brief من يُسمح له: الجذر (الأدوات المحلية) أو uid التطبيق المسجَّل نفسه تمامًا (المستخدم الأول).
 *
 * إن تعذّرت معرفة uid التطبيق فالجواب **رفض** لا سماح — الفشل لا يفتح الباب.
 */
bool ipc_uid_allowed(uid_t peer, bool app_known, uid_t app_uid) {
    if (peer == 0) {
        return true;
    }
    return app_known && peer == app_uid;
}

static bool read_app_uid(uid_t* uid) {
    FILE* file = fopen(IPC_PACKAGES_LIST, "r");
    if (!file) {
        return false;
    }
    char buffer[16384];
    const size_t got = fread(buffer, 1, sizeof(buffer) - 1, file);
    fclose(file);
    buffer[got] = '\0';
    return ipc_app_uid_from_text(buffer, IPC_PACKAGE_NAME, uid);
}

/*
 * uid التطبيق مخبّأ: القراءة كانت تُعاد (١٦ كيلوبايت) عند **كل** اتصال، والقراءات تأتي بالعشرات في الثانية.
 * والتخبئة لا تُضعف الفحص: uid التطبيق لا يتغيّر ما دام مثبّتًا، ومن لا يطابق المخبَّأ يُعاد قياسه من
 * الملفّ مرّةً قبل أن يُرفض (فمن أُعيد تثبيته بـuid جديد لا يُحرم).
 */
static atomic_long cached_app_uid = -1;

static bool peer_allowed(uid_t peer) {
    if (peer == 0) {
        return true;
    }
    const long cached = atomic_load(&cached_app_uid);
    if (cached >= 0 && ipc_uid_allowed(peer, true, (uid_t)cached)) {
        return true;
    }
    uid_t fresh = 0;
    const bool known = read_app_uid(&fresh);
    if (known) {
        atomic_store(&cached_app_uid, (long)fresh);
    }
    return ipc_uid_allowed(peer, known, fresh);
}

static size_t read_node(const char* path, char* reply, size_t reply_size) {
    char resolved[PATH_MAX];
    if (!realpath(path, resolved)) {
        return (size_t)snprintf(reply, reply_size, "ERR notfound\n");
    }
    if (!ipc_resolved_allowed(resolved)) {
        return (size_t)snprintf(reply, reply_size, "ERR denied\n");
    }
    const int fd = open(resolved, O_RDONLY | O_CLOEXEC | O_NONBLOCK);
    if (fd < 0) {
        return (size_t)snprintf(reply, reply_size, "ERR open=%d\n", errno);
    }
    struct stat info;
    if (fstat(fd, &info) != 0 || !S_ISREG(info.st_mode)) {
        close(fd);
        return (size_t)snprintf(reply, reply_size, "ERR notfile\n");
    }
    char body[IPC_READ_MAX];
    const ssize_t got = read(fd, body, sizeof(body));
    close(fd);
    if (got < 0) {
        return (size_t)snprintf(reply, reply_size, "ERR read=%d\n", errno);
    }
    const int header = snprintf(reply, reply_size, "OK %zd\n", got);
    if (header < 0 || (size_t)header + (size_t)got > reply_size) {
        return (size_t)snprintf(reply, reply_size, "ERR toolarge\n");
    }
    memcpy(reply + header, body, (size_t)got);
    return (size_t)header + (size_t)got;
}

/**
 * @brief ينفّذ أمرًا محلَّلًا ويكتب الردّ، ويعيد عدد البايتات الواجب إرسالها (الردّ قد يحمل جسمًا ثنائيًّا).
 */
size_t ipc_execute(const IpcCommand* command, const IpcBackend* backend, char* reply, size_t reply_size) {
    switch (command->kind) {
        case IPC_CMD_PING:
            return (size_t)snprintf(reply, reply_size, "OK 1\n");
        case IPC_CMD_STATUS: {
            char state[PROP_VALUE_MAX] = {0};
            char ai[PROP_VALUE_MAX] = {0};
            __system_property_get("persist.sys.maxmanager.state", state);
            __system_property_get("persist.sys.maxmanagerconf.AIenabled", ai);
            return (size_t)snprintf(reply, reply_size, "OK state=%s ai=%s\n", state[0] ? state : "unknown", ai[0] ? ai : "0");
        }
        case IPC_CMD_PROFILE: {
            const int rc = (backend && backend->profile) ? backend->profile(command->profile, command->from_ai) : -1;
            return (size_t)(rc == 0 ? snprintf(reply, reply_size, "OK\n") : snprintf(reply, reply_size, "ERR exit=%d\n", rc));
        }
        case IPC_CMD_EXISTS: {
            char resolved[PATH_MAX];
            const bool present = realpath(command->path, resolved) != NULL && ipc_resolved_allowed(resolved);
            return (size_t)snprintf(reply, reply_size, "OK %d\n", present ? 1 : 0);
        }
        case IPC_CMD_READ:
            return read_node(command->path, reply, reply_size);
        case IPC_CMD_CANWRITE: {
            char resolved[PATH_MAX];
            const char* want = strrchr(command->path, '/');
            const bool present = realpath(command->path, resolved) != NULL && ipc_resolved_allowed(resolved) && want &&
                                 strrchr(resolved, '/') != NULL && strcmp(want, strrchr(resolved, '/')) == 0;
            return (size_t)snprintf(reply, reply_size, "OK %d\n", present ? 1 : 0);
        }
        case IPC_CMD_WRITE: {
            char resolved[PATH_MAX];
            if (!realpath(command->path, resolved)) {
                return (size_t)snprintf(reply, reply_size, "ERR notfound\n");
            }
            /* الرابط لا يبدّل العقدة: يبقى داخل /sys أو /proc، وباسم السمة نفسه (يمنع تحويل `governor` إلى سمة أخرى). */
            const char* want = strrchr(command->path, '/');
            const char* got = strrchr(resolved, '/');
            if (!ipc_resolved_allowed(resolved) || !want || !got || strcmp(want, got) != 0) {
                return (size_t)snprintf(reply, reply_size, "ERR denied\n");
            }
            const int rc = ipc_write_node(resolved, command->value);
            return (size_t)(rc == 0 ? snprintf(reply, reply_size, "OK\n") : snprintf(reply, reply_size, "ERR write=%d\n", rc));
        }
        case IPC_CMD_SETPROP: {
            const int rc = (backend && backend->setprop) ? backend->setprop(command->key, command->value) : -1;
            return (size_t)(rc == 0 ? snprintf(reply, reply_size, "OK\n") : snprintf(reply, reply_size, "ERR setprop=%d\n", rc));
        }
        default:
            return (size_t)snprintf(reply, reply_size, "ERR invalid\n");
    }
}

/**
 * @brief الطريق الحقيقيّ لتبديل الملف: يُشغِّل الثنائيّ نفسه بـ`--profile N` — عملية منفصلة تمامًا كما
 * كان التطبيق يفعل بالجذر، فلا يتغيّر أيّ سلوك (بوّابة AI، حفظ النيّة، الإشعار) ولا يتقاسم الخيط
 * الرئيسيّ حالةً. ومتسلسل بقفل: تبديلان معًا لا يكتبان العقدة نفسها.
 */
int ipc_spawn_self_profile(char profile, bool from_ai) {
    static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
    char name[] = "sys.maxmanager-service";
    char flag_profile[] = "--profile";
    char value[2] = {profile, '\0'};
    char flag_ai[] = "--from-ai";
    char* argv[] = {name, flag_profile, value, from_ai ? flag_ai : NULL, NULL};

    pthread_mutex_lock(&lock);
    pid_t pid = 0;
    int rc = posix_spawn(&pid, "/proc/self/exe", NULL, NULL, argv, environ);
    if (rc != 0) {
        pthread_mutex_unlock(&lock);
        return rc > 0 ? -rc : rc;
    }
    int status = 0;
    while (waitpid(pid, &status, 0) < 0) {
        if (errno != EINTR) {
            pthread_mutex_unlock(&lock);
            return -ECHILD;
        }
    }
    pthread_mutex_unlock(&lock);
    return WIFEXITED(status) ? WEXITSTATUS(status) : 128;
}

static int real_setprop(const char* key, const char* value) {
    return __system_property_set(key, value);
}

const IpcBackend IPC_REAL_BACKEND = {.profile = ipc_spawn_self_profile, .setprop = real_setprop};

static void write_bytes(int fd, const char* data, size_t length) {
    while (length > 0) {
        const ssize_t sent = send(fd, data, length, MSG_NOSIGNAL);
        if (sent <= 0) {
            return;
        }
        data += sent;
        length -= (size_t)sent;
    }
}

static void write_reply(int fd, const char* reply) {
    write_bytes(fd, reply, strlen(reply));
}

/**
 * @brief يخدم اتصالًا واحدًا: يقرأ سطرًا، يتحقّق من الهويّة **قبل** أيّ تحليل، ينفّذ، يردّ.
 */
void ipc_handle_connection(int fd, uid_t peer_uid, const IpcBackend* backend) {
    struct timeval timeout = {.tv_sec = IPC_READ_TIMEOUT_SEC, .tv_usec = 0};
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));

    if (!peer_allowed(peer_uid)) {
        log_zenith(LOG_WARN, "EVENT=IPC_DENIED peer_uid=%u", (unsigned)peer_uid);
        write_reply(fd, "ERR denied\n");
        return;
    }

    char line[IPC_LINE_MAX];
    size_t length = 0;
    while (length < sizeof(line) - 1) {
        const ssize_t got = recv(fd, line + length, 1, 0);
        if (got <= 0) {
            break;
        }
        if (line[length] == '\n') {
            break;
        }
        length++;
    }
    line[length] = '\0';

    IpcCommand command;
    if (!ipc_parse_command(line, &command)) {
        write_reply(fd, "ERR invalid\n");
        return;
    }
    char reply[IPC_REPLY_MAX];
    const size_t reply_length = ipc_execute(&command, backend, reply, sizeof(reply));
    write_bytes(fd, reply, reply_length < sizeof(reply) ? reply_length : sizeof(reply));
}

typedef struct {
    int fd;
    uid_t uid;
} IpcClient;

static atomic_int active_clients = 0;

static void* client_thread(void* arg) {
    IpcClient* client = (IpcClient*)arg;
    ipc_handle_connection(client->fd, client->uid, &IPC_REAL_BACKEND);
    close(client->fd);
    free(client);
    atomic_fetch_sub(&active_clients, 1);
    return NULL;
}

static int open_listener(void) {
    const int fd = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        return -1;
    }
    struct sockaddr_un address;
    memset(&address, 0, sizeof(address));
    address.sun_family = AF_UNIX;
    const size_t name_length = strlen(IPC_SOCKET_NAME);
    memcpy(address.sun_path + 1, IPC_SOCKET_NAME, name_length); /* sun_path[0] == '\0' => abstract */
    const socklen_t address_length = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + name_length);
    if (bind(fd, (struct sockaddr*)&address, address_length) != 0 || listen(fd, IPC_MAX_CLIENTS) != 0) {
        close(fd);
        return -1;
    }
    return fd;
}

static void* accept_thread(void* arg) {
    const int listener = (int)(intptr_t)arg;
    for (;;) {
        const int fd = accept(listener, NULL, NULL);
        if (fd >= 0) {
            (void)fcntl(fd, F_SETFD, FD_CLOEXEC);
        }
        if (fd < 0) {
            if (errno == EINTR) {
                continue;
            }
            log_zenith(LOG_ERROR, "EVENT=IPC_ACCEPT_FAILED errno=%d action=stop", errno);
            return NULL;
        }
        struct ucred cred;
        socklen_t cred_length = sizeof(cred);
        if (getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &cred, &cred_length) != 0 ||
            atomic_load(&active_clients) >= IPC_MAX_CLIENTS) {
            write_reply(fd, "ERR busy\n");
            close(fd);
            continue;
        }
        IpcClient* client = malloc(sizeof(*client));
        pthread_t thread;
        pthread_attr_t attr;
        if (!client) {
            close(fd);
            continue;
        }
        client->fd = fd;
        client->uid = cred.uid;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        atomic_fetch_add(&active_clients, 1);
        if (pthread_create(&thread, &attr, client_thread, client) != 0) {
            atomic_fetch_sub(&active_clients, 1);
            close(fd);
            free(client);
        }
        pthread_attr_destroy(&attr);
    }
}

/**
 * @brief يفتح القناة ويشغّل خيط القبول. فشلها **ليس قاتلًا**: الخادم يبقى يعمل، ويبقى طريق الجذر في التطبيق.
 */
void start_ipc_server(void) {
    const int listener = open_listener();
    if (listener < 0) {
        log_zenith(LOG_WARN, "EVENT=IPC_LISTEN_FAILED errno=%d action=continue_without_ipc", errno);
        return;
    }
    pthread_t thread;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&thread, &attr, accept_thread, (void*)(intptr_t)listener) != 0) {
        close(listener);
        log_zenith(LOG_WARN, "EVENT=IPC_THREAD_FAILED action=continue_without_ipc");
    } else {
        log_zenith(LOG_INFO, "EVENT=IPC_LISTENING name=@%s", IPC_SOCKET_NAME);
    }
    pthread_attr_destroy(&attr);
}
