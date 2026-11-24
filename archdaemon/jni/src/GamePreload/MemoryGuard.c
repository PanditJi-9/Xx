#include "MemoryGuard.h"
#include "SysfsDirect.h"

#include <stdio.h>

bool mg_is_pid_alive(pid_t pid) {
    if (pid <= 1) return false;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/stat", (int)pid);
    return sysfs_path_exists(path);
}

bool mg_get_available_ram_mb(size_t *out_mb) {
    if (!out_mb) return false;
    FILE *fp = fopen("/proc/meminfo", "r");
    if (!fp) return false;
    char line[128];
    unsigned long mem_available_kb = 0, mem_free_kb = 0, cached_kb = 0;
    bool has_available = false;
    while (fgets(line, sizeof(line), fp)) {
        if (sscanf(line, "MemAvailable: %lu kB", &mem_available_kb) == 1) {
            has_available = true;
            break;
        }
        sscanf(line, "MemFree: %lu kB", &mem_free_kb);
        sscanf(line, "Cached: %lu kB", &cached_kb);
    }
    fclose(fp);
    unsigned long effective_kb = has_available ? mem_available_kb : (mem_free_kb + cached_kb);
    *out_mb = (size_t)(effective_kb / 1024UL);
    return true;
}

bool mg_can_preload(size_t min_required_mb) {
    size_t avail_mb = 0;
    if (!mg_get_available_ram_mb(&avail_mb)) return false;
    return avail_mb >= min_required_mb;
}

bool mg_protect_game_pid(pid_t pid, int target_oom_adj, int *out_prev_oom_adj) {
    if (pid <= 1) return false;
    if (target_oom_adj < -500) target_oom_adj = -500;
    if (target_oom_adj > 0) target_oom_adj = 0;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/oom_score_adj", (int)pid);
    int prev = 0;
    if (!sysfs_read_int(path, &prev)) return false;
    if (out_prev_oom_adj) *out_prev_oom_adj = prev;
    return sysfs_write_int(path, target_oom_adj);
}

bool mg_restore_game_pid(pid_t pid, int prev_oom_adj) {
    if (pid <= 1) return false;
    if (prev_oom_adj < -1000) prev_oom_adj = -1000;
    if (prev_oom_adj > 1000) prev_oom_adj = 1000;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/oom_score_adj", (int)pid);
    return sysfs_write_int(path, prev_oom_adj);
}
