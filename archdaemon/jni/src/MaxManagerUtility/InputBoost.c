#include "InputBoost.h"
#include "SysfsDirect.h"

#include <dirent.h>
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static pid_t find_system_server_pid(void) {
    DIR *proc = opendir("/proc");
    if (!proc) return -1;

    pid_t found = -1;
    struct dirent *ent;
    while ((ent = readdir(proc)) != NULL) {
        if (ent->d_name[0] < '0' || ent->d_name[0] > '9') continue;
        pid_t pid = (pid_t)atoi(ent->d_name);
        char cmd_path[96];
        char cmdline[128];
        snprintf(cmd_path, sizeof(cmd_path), "/proc/%d/cmdline", (int)pid);
        if (sysfs_read_str(cmd_path, cmdline, sizeof(cmdline))) {
            if (strcmp(cmdline, "system_server") == 0) {
                found = pid;
                break;
            }
        }
    }
    closedir(proc);
    return found;
}

void ib_init(InputBoostState *state, const char *discovered_touch_node) {
    if (!state) return;
    memset(state, 0, sizeof(*state));
    if (discovered_touch_node && discovered_touch_node[0]) {
        snprintf(state->touch_node_path, sizeof(state->touch_node_path), "%s", discovered_touch_node);
    }
}

bool ib_enable(InputBoostState *state) {
    if (!state) return false;
    if (state->active) return true;

    pid_t ss_pid = find_system_server_pid();
    if (ss_pid > 1) {
        char task_dir[64];
        snprintf(task_dir, sizeof(task_dir), "/proc/%d/task", (int)ss_pid);
        DIR *dir = opendir(task_dir);
        if (dir) {
            struct dirent *ent;
            while ((ent = readdir(dir)) != NULL && state->thread_count < IB_MAX_TRACKED_TIDS) {
                if (ent->d_name[0] < '0' || ent->d_name[0] > '9') continue;
                pid_t tid = (pid_t)atoi(ent->d_name);
                char comm_path[128];
                char comm[64];
                snprintf(comm_path, sizeof(comm_path), "/proc/%d/task/%d/comm", (int)ss_pid, (int)tid);
                if (!sysfs_read_str(comm_path, comm, sizeof(comm))) continue;

                if (strcmp(comm, "InputReader") == 0 || strcmp(comm, "InputDispatcher") == 0) {
                    int prev_pol = sched_getscheduler(tid);
                    struct sched_param prev_param;
                    memset(&prev_param, 0, sizeof(prev_param));
                    if (prev_pol >= 0 && sched_getparam(tid, &prev_param) == 0) {
                        struct sched_param boost_param;
                        boost_param.sched_priority = 2; /* SCHED_FIFO معتدل وآمن */
                        if (sched_setscheduler(tid, SCHED_FIFO, &boost_param) == 0) {
                            SavedInputThread *slot = &state->threads[state->thread_count++];
                            slot->tid = tid;
                            slot->prev_policy = prev_pol;
                            slot->prev_priority = prev_param.sched_priority;
                        }
                    }
                }
            }
            closedir(dir);
        }
    }

    if (state->touch_node_path[0] && sysfs_path_exists(state->touch_node_path)) {
        if (sysfs_read_str(state->touch_node_path, state->prev_touch_val, sizeof(state->prev_touch_val))) {
            if (sysfs_write_int(state->touch_node_path, 1)) {
                state->touch_node_modified = true;
            }
        }
    }

    state->active = (state->thread_count > 0 || state->touch_node_modified);
    return state->active;
}

void ib_disable(InputBoostState *state) {
    if (!state || !state->active) return;

    for (int i = 0; i < state->thread_count; ++i) {
        struct sched_param restore_param;
        restore_param.sched_priority = state->threads[i].prev_priority;
        sched_setscheduler(state->threads[i].tid, state->threads[i].prev_policy, &restore_param);
    }
    state->thread_count = 0;

    if (state->touch_node_modified && state->touch_node_path[0]) {
        sysfs_write_str(state->touch_node_path,
                        state->prev_touch_val[0] ? state->prev_touch_val : "0");
        state->touch_node_modified = false;
    }

    state->active = false;
}
