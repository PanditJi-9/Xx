#ifndef MAXMANAGER_NETWORK_QOS_H
#define MAXMANAGER_NETWORK_QOS_H

#include <stdbool.h>
#include <sys/types.h>

typedef struct {
    bool active;
    uid_t prioritized_uid;
    char prev_tcp_cc[32];
    char prev_qdisc[32];
    bool tcp_cc_modified;
    bool qdisc_modified;
    bool dscp_rule_added;
    bool bg_muted;
} NetworkQoSState;

void nqos_init(NetworkQoSState *state);

/* تفعيل BBR + fq + وسم حزم UID اللعبة بأولوية DSCP 46 (EF) */
bool nqos_enable_for_game(NetworkQoSState *state, uid_t game_uid, bool mute_background);

/* استعادة إعدادات الشبكة وإزالة القواعد بالكامل */
void nqos_disable(NetworkQoSState *state);

#endif /* MAXMANAGER_NETWORK_QOS_H */
