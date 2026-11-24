#ifndef MAXMANAGER_CGROUP_FREEZER_H
#define MAXMANAGER_CGROUP_FREEZER_H

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>

#define CF_MAX_FROZEN 128

typedef struct {
    char cgroup_root[128];
    uid_t frozen_uids[CF_MAX_FROZEN];
    size_t frozen_count;
} CgroupFreezerState;

bool cf_init(CgroupFreezerState *state);

/* تجميد جميع التطبيقات غير المستثناة (uid >= 10000) ما عدا اللعبة والقائمة البيضاء */
int cf_freeze_background_uids(CgroupFreezerState *state,
                              uid_t active_game_uid,
                              const uid_t *whitelist_uids,
                              size_t whitelist_count);

/* فك تجميد جميع التطبيقات المجمدة فوراً وبأمان */
int cf_unfreeze_all(CgroupFreezerState *state);

#endif /* MAXMANAGER_CGROUP_FREEZER_H */
