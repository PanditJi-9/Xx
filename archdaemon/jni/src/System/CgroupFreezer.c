#include "CgroupFreezer.h"
#include "SysfsDirect.h"

#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char *kCgroupRoots[] = {
    "/sys/fs/cgroup",
    "/acct",
    NULL
};

static bool is_uid_whitelisted(uid_t uid,
                               uid_t active_game_uid,
                               const uid_t *whitelist,
                               size_t count) {
    /* عدم لمس خدمات النظام أو root أو shell مطلقاً */
    if (uid < 10000) return true;
    if (uid == active_game_uid) return true;
    for (size_t i = 0; i < count; ++i) {
        if (whitelist && whitelist[i] == uid) return true;
    }
    return false;
}

bool cf_init(CgroupFreezerState *state) {
    if (!state) return false;
    memset(state, 0, sizeof(*state));

    for (int i = 0; kCgroupRoots[i] != NULL; ++i) {
        char probe[192];
        snprintf(probe, sizeof(probe), "%s/cgroup.controllers", kCgroupRoots[i]);
        if (sysfs_path_exists(probe)) {
            snprintf(state->cgroup_root, sizeof(state->cgroup_root), "%s", kCgroupRoots[i]);
            return true;
        }
    }
    return false;
}

int cf_freeze_background_uids(CgroupFreezerState *state,
                              uid_t active_game_uid,
                              const uid_t *whitelist_uids,
                              size_t whitelist_count) {
    if (!state || state->cgroup_root[0] == '\0') return 0;

    DIR *dir = opendir(state->cgroup_root);
    if (!dir) return 0;

    int newly_frozen = 0;
    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (strncmp(ent->d_name, "uid_", 4) != 0) continue;
        uid_t uid = (uid_t)strtoul(ent->d_name + 4, NULL, 10);
        if (is_uid_whitelisted(uid, active_game_uid, whitelist_uids, whitelist_count)) {
            continue;
        }
        if (state->frozen_count >= CF_MAX_FROZEN) break;

        char freeze_node[512];
        int fn_len = snprintf(freeze_node, sizeof(freeze_node), "%s/%s/cgroup.freeze",
                 state->cgroup_root, ent->d_name);
        if (fn_len <= 0 || (size_t)fn_len >= sizeof(freeze_node)) continue;
        if (!sysfs_path_exists(freeze_node)) continue;

        if (sysfs_write_int(freeze_node, 1)) {
            state->frozen_uids[state->frozen_count++] = uid;
            newly_frozen++;
        }
    }
    closedir(dir);
    return newly_frozen;
}

int cf_unfreeze_all(CgroupFreezerState *state) {
    if (!state || state->cgroup_root[0] == '\0') return 0;

    int unfrozen = 0;
    for (size_t i = 0; i < state->frozen_count; ++i) {
        char freeze_node[256];
        snprintf(freeze_node, sizeof(freeze_node), "%s/uid_%u/cgroup.freeze",
                 state->cgroup_root, (unsigned int)state->frozen_uids[i]);
        if (sysfs_write_int(freeze_node, 0)) {
            unfrozen++;
        }
    }
    state->frozen_count = 0;
    return unfrozen;
}
