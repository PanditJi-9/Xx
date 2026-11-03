/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#include <MaxManager.h>
#include <ForegroundObserver.h>

#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#define FG_POLL_SECONDS 1
#define FG_MAX_PIDS 256
#define FG_APP_UID_MIN 10000

/**
 * @brief اسم الحزمة من أول سطر `/proc/<pid>/cmdline`: يُقطع عند `:` (عمليّة فرعيّة `pkg:remote`) ويُتحقَّق من الشكل.
 *
 * يرفض ما لا نقطة فيه (عمليّات النظام بلا نقطة مثل `zygote`) وما فيه حرف خارج `[A-Za-z0-9._]` — فلا يصير اسمٌ
 * مخترع «تطبيقًا في المقدّمة» (العطب الذي وُثّق في الرفيق: `0.85`)؛ فالقاعدة ألّا يبدأ الاسم بنقطة/رقم.
 */
bool fg_package_from_cmdline(const char* cmdline, char* out, size_t out_size) {
    if (!cmdline || !out || out_size < 4) {
        return false;
    }
    size_t length = 0;
    bool has_dot = false;
    for (; cmdline[length] != '\0' && cmdline[length] != ':'; length++) {
        const char c = cmdline[length];
        if (!(isalnum((unsigned char)c) || c == '.' || c == '_')) {
            return false;
        }
        if (c == '.') {
            has_dot = true;
        }
        if (length + 1 >= out_size) {
            return false;
        }
    }
    if (length == 0 || !has_dot || !isalpha((unsigned char)cmdline[0]) || cmdline[length - 1] == '.') {
        return false;
    }
    memcpy(out, cmdline, length);
    out[length] = '\0';
    return true;
}

/**
 * @brief هل الحزمة مفتاح في قائمة التطبيقات؟ مسح نصّيّ بنفس تسامح الخادم: `"pkg"` ثم فراغ اختياريّ ثم `:`.
 */
bool fg_gamelist_contains(const char* json, const char* package) {
    if (!json || !package || package[0] == '\0') {
        return false;
    }
    const size_t length = strlen(package);
    for (const char* cursor = json; (cursor = strchr(cursor, '"')) != NULL; cursor++) {
        if (strncmp(cursor + 1, package, length) != 0 || cursor[1 + length] != '"') {
            continue;
        }
        const char* after = cursor + 2 + length;
        while (*after == ' ' || *after == '\t' || *after == '\n' || *after == '\r') {
            after++;
        }
        if (*after == ':') {
            return true;
        }
    }
    return false;
}

/**
 * @brief نصّ `app_status` بصيغة `StatusMonitor.c::read_app_status` حرفًا بحرف.
 */
int fg_format_status(char* out, size_t size, const char* package, int pid, int uid, int screen_awake,
                     int battery_level, int is_charging, const char* switch_id) {
    return snprintf(out, size,
                    "focused_app %s %d %d\n"
                    "screen_awake %d\n"
                    "battery_saver 0\n"
                    "zen_mode 0\n"
                    "app_name %s\n"
                    "battery_level %d\n"
                    "is_charging %d\n"
                    "switch_id %s\n",
                    package, pid, uid, screen_awake, package, battery_level, is_charging, switch_id);
}

static int read_int_file(const char* path, int fallback) {
    FILE* file = fopen(path, "r");
    if (!file) {
        return fallback;
    }
    int value = fallback;
    if (fscanf(file, "%d", &value) != 1) {
        value = fallback;
    }
    fclose(file);
    return value;
}

static int read_top_app_pids(int* pids, int max) {
    static const char* const FILES[] = {
        "/dev/cpuset/top-app/cgroup.procs", "/dev/cpuset/top-app/tasks", "/dev/cpuctl/top-app/cgroup.procs",
        "/dev/cpuctl/top-app/tasks",
    };
    for (size_t index = 0; index < sizeof(FILES) / sizeof(FILES[0]); index++) {
        FILE* file = fopen(FILES[index], "r");
        if (!file) {
            continue;
        }
        int count = 0;
        int pid = 0;
        while (count < max && fscanf(file, "%d", &pid) == 1) {
            pids[count++] = pid;
        }
        fclose(file);
        return count;
    }
    return 0;
}

static int screen_awake_now(void) {
    static const char* const NODES[] = {
        "/sys/class/leds/lcd-backlight/brightness", "/sys/class/backlight/panel0-backlight/brightness",
        "/sys/class/backlight/backlight/brightness",
    };
    for (size_t index = 0; index < sizeof(NODES) / sizeof(NODES[0]); index++) {
        const int value = read_int_file(NODES[index], -1);
        if (value >= 0) {
            return value > 0 ? 1 : 0;
        }
    }
    return 1; /* لا عقدة معروفة: لا ندّعي أنّ الشاشة مطفأة (ذلك يوقف إدارة التطبيق كلها) */
}

static int is_charging_now(void) {
    FILE* file = fopen("/sys/class/power_supply/battery/status", "r");
    if (!file) {
        return 0;
    }
    char status[32] = {0};
    const bool got = fgets(status, sizeof(status), file) != NULL;
    fclose(file);
    return got && strncmp(status, "Charging", 8) == 0;
}

static char* read_whole_file(const char* path, time_t* mtime) {
    struct stat info;
    if (stat(path, &info) != 0 || info.st_size <= 0 || info.st_size > (1 << 20)) {
        return NULL;
    }
    FILE* file = fopen(path, "r");
    if (!file) {
        return NULL;
    }
    char* text = malloc((size_t)info.st_size + 1);
    if (!text) {
        fclose(file);
        return NULL;
    }
    const size_t got = fread(text, 1, (size_t)info.st_size, file);
    fclose(file);
    text[got] = '\0';
    *mtime = info.st_mtime;
    return text;
}

static void write_status_atomically(const char* text) {
    const char* tmp = APP_MONITOR_FILE ".tmp";
    FILE* file = fopen(tmp, "w");
    if (!file) {
        return;
    }
    fputs(text, file);
    fclose(file);
    if (rename(tmp, APP_MONITOR_FILE) != 0) {
        unlink(tmp);
    }
}

static void* observer_thread(void* arg) {
    const char* lock_path = (const char*)arg;
    char* gamelist = NULL;
    time_t gamelist_mtime = 0;
    char last_key[256] = {0};
    char switch_id[32] = "sw-0";
    char last_pkg[128] = "unknown";

    for (;;) {
        sleep(FG_POLL_SECONDS);
        if (lock_path && is_java_lock_held(lock_path)) {
            continue; /* الرفيق حيّ (جذر متاح): هو صاحب الملفّ */
        }

        struct stat info;
        if (stat(GAMELIST, &info) == 0 && (gamelist == NULL || info.st_mtime != gamelist_mtime)) {
            char* fresh = read_whole_file(GAMELIST, &gamelist_mtime);
            if (fresh) {
                free(gamelist);
                gamelist = fresh;
            }
        }

        char package[128] = "unknown";
        int pid = 0;
        int uid = 0;
        int pids[FG_MAX_PIDS];
        const int count = read_top_app_pids(pids, FG_MAX_PIDS);
        for (int index = 0; index < count && gamelist; index++) {
            char path[64];
            snprintf(path, sizeof(path), "/proc/%d/cmdline", pids[index]);
            FILE* file = fopen(path, "r");
            if (!file) {
                continue;
            }
            char cmdline[160] = {0};
            const size_t got = fread(cmdline, 1, sizeof(cmdline) - 1, file);
            fclose(file);
            cmdline[got] = '\0';
            char candidate[128];
            if (!fg_package_from_cmdline(cmdline, candidate, sizeof(candidate)) || !fg_gamelist_contains(gamelist, candidate)) {
                continue;
            }
            snprintf(path, sizeof(path), "/proc/%d", pids[index]);
            struct stat proc_info;
            if (stat(path, &proc_info) != 0 || (int)proc_info.st_uid < FG_APP_UID_MIN) {
                continue;
            }
            snprintf(package, sizeof(package), "%s", candidate);
            pid = pids[index];
            uid = (int)proc_info.st_uid;
            break;
        }

        const int screen = screen_awake_now();
        const int level = read_int_file("/sys/class/power_supply/battery/capacity", -1);
        const int charging = is_charging_now();

        char key[256];
        snprintf(key, sizeof(key), "%s|%d|%d|%d", package, screen, level, charging);
        if (strcmp(key, last_key) == 0) {
            continue;
        }
        if (strcmp(package, last_pkg) != 0) {
            snprintf(switch_id, sizeof(switch_id), "sw-%lld", (long long)time(NULL) * 1000LL);
            snprintf(last_pkg, sizeof(last_pkg), "%s", package);
            log_zenith(LOG_INFO, "EVENT=FOREGROUND_SOURCE src=native pkg=%s", package);
        }
        char text[512];
        fg_format_status(text, sizeof(text), package, pid, uid, screen, level, charging, switch_id);
        write_status_atomically(text);
        snprintf(last_key, sizeof(last_key), "%s", key);
    }
    return NULL;
}

/**
 * @brief يشغّل خيط المراقبة. فشله غير قاتل: يبقى الخادم يخدم القناة، ويبقى الرفيق (إن وُجد جذر) طريقًا.
 */
void start_foreground_observer(const char* java_lock_path) {
    pthread_t thread;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&thread, &attr, observer_thread, (void*)java_lock_path) != 0) {
        log_zenith(LOG_WARN, "EVENT=FOREGROUND_OBSERVER_FAILED errno=%d action=continue", errno);
    } else {
        log_zenith(LOG_INFO, "EVENT=FOREGROUND_OBSERVER_STARTED mode=native");
    }
    pthread_attr_destroy(&attr);
}
