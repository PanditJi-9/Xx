#include "NetworkQoS.h"
#include "SysfsDirect.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

#define TCP_CC_NODE       "/proc/sys/net/ipv4/tcp_congestion_control"
#define TCP_AVAIL_CC_NODE "/proc/sys/net/ipv4/tcp_available_congestion_control"
#define DEFAULT_QDISC     "/proc/sys/net/core/default_qdisc"

static bool run_argv_quiet(char *const argv[]) {
    pid_t pid = fork();
    if (pid < 0) return false;
    if (pid == 0) {
        execvp(argv[0], argv);
        _exit(127);
    }
    int status = 0;
    if (waitpid(pid, &status, 0) < 0) return false;
    return (WIFEXITED(status) && WEXITSTATUS(status) == 0);
}

static bool apply_dscp_rule(uid_t uid, bool add) {
    char uid_str[32];
    snprintf(uid_str, sizeof(uid_str), "%u", (unsigned int)uid);
    char *flag = add ? "-A" : "-D";

    char *argv_v4[] = {
        "iptables", "-t", "mangle", flag, "OUTPUT",
        "-m", "owner", "--uid-owner", uid_str,
        "-j", "DSCP", "--set-dscp", "46", NULL
    };
    bool ok4 = run_argv_quiet(argv_v4);

    char *argv_v6[] = {
        "ip6tables", "-t", "mangle", flag, "OUTPUT",
        "-m", "owner", "--uid-owner", uid_str,
        "-j", "DSCP", "--set-dscp", "46", NULL
    };
    run_argv_quiet(argv_v6);
    return ok4;
}

static bool apply_bg_mute_chain(uid_t game_uid, bool enable) {
    char uid_str[32];
    snprintf(uid_str, sizeof(uid_str), "%u", (unsigned int)game_uid);

    if (enable) {
        char *mk[] = {"iptables", "-N", "MAXMGR_BGMUTE", NULL};
        char *fl[] = {"iptables", "-F", "MAXMGR_BGMUTE", NULL};
        char *allow_sys[] = {"iptables", "-A", "MAXMGR_BGMUTE", "-m", "owner", "--uid-owner", "0-9999", "-j", "RETURN", NULL};
        char *allow_game[] = {"iptables", "-A", "MAXMGR_BGMUTE", "-m", "owner", "--uid-owner", uid_str, "-j", "RETURN", NULL};
        char *limit_bg[] = {"iptables", "-A", "MAXMGR_BGMUTE", "-m", "limit", "--limit", "15/sec", "-j", "RETURN", NULL};
        char *drop_bg[] = {"iptables", "-A", "MAXMGR_BGMUTE", "-j", "DROP", NULL};
        char *hook[] = {"iptables", "-I", "OUTPUT", "1", "-j", "MAXMGR_BGMUTE", NULL};

        run_argv_quiet(mk);
        run_argv_quiet(fl);
        run_argv_quiet(allow_sys);
        run_argv_quiet(allow_game);
        run_argv_quiet(limit_bg);
        run_argv_quiet(drop_bg);
        return run_argv_quiet(hook);
    } else {
        char *unhook[] = {"iptables", "-D", "OUTPUT", "-j", "MAXMGR_BGMUTE", NULL};
        char *fl[] = {"iptables", "-F", "MAXMGR_BGMUTE", NULL};
        char *del[] = {"iptables", "-X", "MAXMGR_BGMUTE", NULL};
        run_argv_quiet(unhook);
        run_argv_quiet(fl);
        run_argv_quiet(del);
        return true;
    }
}

void nqos_init(NetworkQoSState *state) {
    if (!state) return;
    memset(state, 0, sizeof(*state));
}

bool nqos_enable_for_game(NetworkQoSState *state, uid_t game_uid, bool mute_background) {
    if (!state || game_uid < 10000) return false;
    if (state->active) nqos_disable(state);

    char avail[256];
    if (sysfs_read_str(TCP_AVAIL_CC_NODE, avail, sizeof(avail)) && strstr(avail, "bbr") != NULL) {
        if (sysfs_read_str(TCP_CC_NODE, state->prev_tcp_cc, sizeof(state->prev_tcp_cc))) {
            if (strcmp(state->prev_tcp_cc, "bbr") != 0 && sysfs_write_str(TCP_CC_NODE, "bbr")) {
                state->tcp_cc_modified = true;
            }
        }
    }

    if (sysfs_read_str(DEFAULT_QDISC, state->prev_qdisc, sizeof(state->prev_qdisc))) {
        if (strcmp(state->prev_qdisc, "fq") != 0 && sysfs_write_str(DEFAULT_QDISC, "fq")) {
            state->qdisc_modified = true;
        }
    }

    state->dscp_rule_added = apply_dscp_rule(game_uid, true);
    if (mute_background) {
        state->bg_muted = apply_bg_mute_chain(game_uid, true);
    }

    state->prioritized_uid = game_uid;
    state->active = true;
    return true;
}

void nqos_disable(NetworkQoSState *state) {
    if (!state || !state->active) return;

    if (state->bg_muted) {
        apply_bg_mute_chain(state->prioritized_uid, false);
        state->bg_muted = false;
    }
    if (state->dscp_rule_added) {
        apply_dscp_rule(state->prioritized_uid, false);
        state->dscp_rule_added = false;
    }
    if (state->tcp_cc_modified && state->prev_tcp_cc[0]) {
        sysfs_write_str(TCP_CC_NODE, state->prev_tcp_cc);
        state->tcp_cc_modified = false;
    }
    if (state->qdisc_modified && state->prev_qdisc[0]) {
        sysfs_write_str(DEFAULT_QDISC, state->prev_qdisc);
        state->qdisc_modified = false;
    }

    state->active = false;
    state->prioritized_uid = 0;
}
