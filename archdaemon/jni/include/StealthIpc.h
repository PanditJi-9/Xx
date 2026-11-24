#ifndef MAXMANAGER_STEALTH_IPC_H
#define MAXMANAGER_STEALTH_IPC_H

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>

#define STEALTH_IPC_DEFAULT_NAME "maxmanager_stealth_ipc"

typedef struct {
    pid_t pid;
    uid_t uid;
    gid_t gid;
    char secontext[128];
} StealthPeerIdentity;

/* فتح مقبس Abstract Unix Domain Socket لا يترك أي ملف مادي على القرص */
int sipc_bind_abstract_server(const char *abstract_name, int backlog);

/* قبول اتصال والتحقق من هوية المتصل عبر SO_PEERCRED و SO_PEERSEC */
int sipc_accept_authenticated(int server_fd,
                              uid_t allowed_control_uid,
                              StealthPeerIdentity *out_peer);

#endif /* MAXMANAGER_STEALTH_IPC_H */
