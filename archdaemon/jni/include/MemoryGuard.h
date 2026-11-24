#ifndef MAXMANAGER_MEMORY_GUARD_H
#define MAXMANAGER_MEMORY_GUARD_H

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>

bool mg_get_available_ram_mb(size_t *out_mb);
bool mg_can_preload(size_t min_required_mb);
bool mg_protect_game_pid(pid_t pid, int target_oom_adj, int *out_prev_oom_adj);
bool mg_restore_game_pid(pid_t pid, int prev_oom_adj);
bool mg_is_pid_alive(pid_t pid);

#endif /* MAXMANAGER_MEMORY_GUARD_H */
