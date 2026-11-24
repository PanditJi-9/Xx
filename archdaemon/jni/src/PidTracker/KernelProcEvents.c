#include "KernelProcEvents.h"

#include <errno.h>
#include <poll.h>
#include <stdint.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#if defined(__linux__)
#include <linux/netlink.h>
#include <linux/connector.h>
#include <linux/cn_proc.h>
#endif

#if defined(NETLINK_CONNECTOR) && defined(CN_IDX_PROC)
/*
 * رأس `cn_msg` مُعاد رسمه هنا بدل تضمينه عضوًا.
 *
 * ولماذا: `struct cn_msg` في النواة ينتهي بـ`__u8 data[]` (مصفوفة مرنة)، وتضمينه
 * عضوًا داخل struct آخر **مخالف للمعيار** ويُسقط `ndk-build` بـ`-pedantic-errors`:
 * `invalid use of structure with flexible array member [-Wpedantic]` (قِيس بـgcc 15).
 * والحقول أدناه هي حقول `cn_msg` قبل `data[]` بالترتيب والحجم نفسيهما، فيبقى
 * التخطيط بايتًا ببايت كما تتوقّعه النواة، ويأتي الحدث (`proc_event`) في محلّه.
 */
struct ml_cn_msg {
    struct cb_id id;
    __u32 seq;
    __u32 ack;
    __u16 len;
    __u16 flags;
};

struct nl_cn_ctrl_msg {
    struct nlmsghdr nl_hdr;
    struct ml_cn_msg cn_hdr;
    enum proc_cn_mcast_op op;
};

struct nl_cn_event_msg {
    struct nlmsghdr nl_hdr;
    struct ml_cn_msg cn_hdr;
    struct proc_event ev;
};
#endif

bool kpe_open(KernelProcMonitor *mon) {
    if (!mon) return false;
    mon->sock_fd = -1;
    mon->supported = false;

#if defined(NETLINK_CONNECTOR) && defined(CN_IDX_PROC)
    int fd = socket(PF_NETLINK, SOCK_DGRAM | SOCK_CLOEXEC, NETLINK_CONNECTOR);
    if (fd < 0) return false;

    struct sockaddr_nl addr;
    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_groups = CN_IDX_PROC;
    addr.nl_pid = (uint32_t)getpid();

    if (bind(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        close(fd);
        return false;
    }

    struct nl_cn_ctrl_msg msg;
    memset(&msg, 0, sizeof(msg));
    msg.nl_hdr.nlmsg_len = sizeof(msg);
    msg.nl_hdr.nlmsg_pid = (uint32_t)getpid();
    msg.nl_hdr.nlmsg_type = NLMSG_DONE;
    msg.cn_hdr.id.idx = CN_IDX_PROC;
    msg.cn_hdr.id.val = CN_VAL_PROC;
    msg.cn_hdr.len = sizeof(msg.op);
    msg.op = PROC_CN_MCAST_LISTEN;

    if (send(fd, &msg, sizeof(msg), 0) < 0) {
        close(fd);
        return false;
    }

    mon->sock_fd = fd;
    mon->supported = true;
    return true;
#else
    return false;
#endif
}

void kpe_close(KernelProcMonitor *mon) {
    if (!mon) return;
    if (mon->sock_fd >= 0) {
        close(mon->sock_fd);
        mon->sock_fd = -1;
    }
    mon->supported = false;
}

bool kpe_wait_event(KernelProcMonitor *mon, int timeout_ms, KernelProcEvent *out_event) {
    if (!mon || !out_event) return false;
    memset(out_event, 0, sizeof(*out_event));

#if defined(NETLINK_CONNECTOR) && defined(CN_IDX_PROC)
    if (!mon->supported || mon->sock_fd < 0) return false;

    struct pollfd pfd;
    pfd.fd = mon->sock_fd;
    pfd.events = POLLIN;
    pfd.revents = 0;

    int rc = poll(&pfd, 1, timeout_ms);
    if (rc <= 0 || !(pfd.revents & POLLIN)) return false;

    struct nl_cn_event_msg msg;
    ssize_t len = recv(mon->sock_fd, &msg, sizeof(msg), 0);
    if (len < (ssize_t)sizeof(msg.nl_hdr)) return false;

    if (msg.ev.what == PROC_EVENT_EXEC) {
        out_event->type = KPE_EXEC;
        out_event->pid = msg.ev.event_data.exec.process_pid;
        out_event->tgid = msg.ev.event_data.exec.process_tgid;
        return true;
    }
    if (msg.ev.what == PROC_EVENT_EXIT) {
        out_event->type = KPE_EXIT;
        out_event->pid = msg.ev.event_data.exit.process_pid;
        out_event->tgid = msg.ev.event_data.exit.process_tgid;
        return true;
    }
    return false;
#else
    (void)timeout_ms;
    return false;
#endif
}
