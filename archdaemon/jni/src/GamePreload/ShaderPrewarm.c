#include "ShaderPrewarm.h"
#include "MemoryGuard.h"
#include "SysfsDirect.h"

#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

static bool has_warmable_ext(const char *name) {
    if (!name) return false;
    const char *dot = strrchr(name, '.');
    if (!dot) return false;
    return (strcmp(dot, ".pak") == 0 ||
            strcmp(dot, ".obb") == 0 ||
            strcmp(dot, ".so") == 0 ||
            strcmp(dot, ".vkcache") == 0 ||
            strcmp(dot, ".bin") == 0 ||
            strcmp(dot, ".shader") == 0);
}

static void warm_file(const char *path, size_t max_chunk, size_t *budget_left, PrewarmReport *rep) {
    if (*budget_left == 0) return;

    struct stat st;
    if (stat(path, &st) != 0 || !S_ISREG(st.st_mode) || st.st_size <= 0) return;

    size_t to_warm = (size_t)st.st_size;
    if (to_warm > max_chunk) to_warm = max_chunk;
    if (to_warm > *budget_left) to_warm = *budget_left;

    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return;

#ifdef POSIX_FADV_WILLNEED
    if (posix_fadvise(fd, 0, (off_t)to_warm, POSIX_FADV_WILLNEED) == 0) {
        *budget_left -= to_warm;
        rep->files_warmed++;
        rep->bytes_advised += to_warm;
    }
#else
    (void)to_warm;
#endif
    close(fd);
}

static void scan_dir_limited(const char *dir_path, int depth, size_t *budget_left, PrewarmReport *rep) {
    if (depth <= 0 || *budget_left == 0) return;

    DIR *dir = opendir(dir_path);
    if (!dir) return;

    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL && *budget_left > 0) {
        if (strcmp(ent->d_name, ".") == 0 || strcmp(ent->d_name, "..") == 0) continue;

        char full[512];
        int n = snprintf(full, sizeof(full), "%s/%s", dir_path, ent->d_name);
        if (n <= 0 || (size_t)n >= sizeof(full)) continue;

        struct stat st;
        if (lstat(full, &st) != 0) continue;

        if (S_ISDIR(st.st_mode)) {
            scan_dir_limited(full, depth - 1, budget_left, rep);
        } else if (S_ISREG(st.st_mode) && has_warmable_ext(ent->d_name)) {
            /* حد أقصى 32MB لكل ملف لتجنب طرد صفحات النظام النشطة */
            warm_file(full, 32UL * 1024UL * 1024UL, budget_left, rep);
        }
    }
    closedir(dir);
}

bool sp_prewarm_package(const char *package_name,
                        size_t min_avail_ram_mb,
                        size_t max_budget_mb,
                        const char *block_readahead_node,
                        PrewarmReport *out_report) {
    PrewarmReport local;
    memset(&local, 0, sizeof(local));

    if (!package_name || !package_name[0]) {
        if (out_report) *out_report = local;
        return false;
    }

    if (!mg_can_preload(min_avail_ram_mb)) {
        local.skipped_low_ram = true;
        if (out_report) *out_report = local;
        return false;
    }

    int prev_ra = 0;
    bool ra_boosted = false;
    if (block_readahead_node && block_readahead_node[0] &&
        sysfs_read_int(block_readahead_node, &prev_ra)) {
        if (prev_ra < 1024 && sysfs_write_int(block_readahead_node, 1024)) {
            ra_boosted = true;
        }
    }

    size_t budget_bytes = (max_budget_mb > 0 ? max_budget_mb : 192UL) * 1024UL * 1024UL;

    char dirs[4][256];
    snprintf(dirs[0], sizeof(dirs[0]), "/data/user/0/%s/code_cache", package_name);
    snprintf(dirs[1], sizeof(dirs[1]), "/data/user/0/%s/cache", package_name);
    snprintf(dirs[2], sizeof(dirs[2]), "/sdcard/Android/obb/%s", package_name);
    snprintf(dirs[3], sizeof(dirs[3]), "/sdcard/Android/data/%s/files", package_name);

    for (int i = 0; i < 4 && budget_bytes > 0; ++i) {
        scan_dir_limited(dirs[i], 3, &budget_bytes, &local);
    }

    if (ra_boosted) {
        sysfs_write_int(block_readahead_node, prev_ra);
    }

    if (out_report) *out_report = local;
    return (local.files_warmed > 0);
}
