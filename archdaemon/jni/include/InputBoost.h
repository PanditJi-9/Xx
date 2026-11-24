#ifndef MAXMANAGER_INPUT_BOOST_H
#define MAXMANAGER_INPUT_BOOST_H

#include <stdbool.h>
#include <sys/types.h>

#define IB_MAX_TRACKED_TIDS 4

typedef struct {
    pid_t tid;
    int prev_policy;
    int prev_priority;
} SavedInputThread;

typedef struct {
    bool active;
    char touch_node_path[192];
    char prev_touch_val[32];
    bool touch_node_modified;
    SavedInputThread threads[IB_MAX_TRACKED_TIDS];
    int thread_count;
} InputBoostState;

void ib_init(InputBoostState *state, const char *discovered_touch_node);
bool ib_enable(InputBoostState *state);
void ib_disable(InputBoostState *state);

#endif /* MAXMANAGER_INPUT_BOOST_H */
