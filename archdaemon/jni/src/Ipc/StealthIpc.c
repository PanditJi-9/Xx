#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include "StealthIpc.h"

#include <stddef.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

int sipc_bind_abstract_server(const char *abstract_name, int backlog) {
    if (!abstract_name || !abstract_name[0]) abstract_name = STEALTH_IPC_DEFAULT_NAME;

    int fd = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) return -1;

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;

    size_t name_len = strlen(abstract_name);
    if (name_len >= sizeof(addr.sun_path) - 1) {
        close(fd);
        return -1;
    }

    /* البايت الأول '\0' يضع المقبس في Abstract Namespace دون أي ملف على القرص */
    addr.sun_path[0] = '\0';
    memcpy(addr.sun_path + 1, abstract_name, name_len);

    socklen_t addr_len = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + name_len);
    if (bind(fd, (struct sockaddr *)&addr, addr_len) < 0) {
        close(fd);
        return -1;
    }

    if (listen(fd, backlog > 0 ? backlog : 8) < 0) {
        close(fd);
        return -1;
    }

    return fd;
}

int sipc_accept_authenticated(int server_fd,
                              uid_t allowed_control_uid,
                              StealthPeerIdentity *out_peer) {
    if (server_fd < 0) return -1;

    int client_fd = accept(server_fd, NULL, NULL);
    if (client_fd < 0) return -1;

    struct ucred cred;
    socklen_t cred_len = sizeof(cred);
    if (getsockopt(client_fd, SOL_SOCKET, SO_PEERCRED, &cred, &cred_len) != 0) {
        close(client_fd);
        return -1;
    }

    /* السماح فقط لـ root (0) أو لتطبيق التحكم المصرّح به */
    bool uid_ok = (cred.uid == 0) ||
                  (allowed_control_uid >= 10000 && cred.uid == allowed_control_uid);
    if (!uid_ok) {
        close(client_fd);
        return -1;
    }

    if (out_peer) {
        memset(out_peer, 0, sizeof(*out_peer));
        out_peer->pid = cred.pid;
        out_peer->uid = cred.uid;
        out_peer->gid = cred.gid;
#ifdef SO_PEERSEC
        socklen_t sec_len = (socklen_t)(sizeof(out_peer->secontext) - 1);
        if (getsockopt(client_fd, SOL_SOCKET, SO_PEERSEC, out_peer->secontext, &sec_len) == 0) {
            out_peer->secontext[sec_len] = '\0';
        }
#endif
    }

    return client_fd;
}
