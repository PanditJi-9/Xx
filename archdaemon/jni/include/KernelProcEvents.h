#ifndef MAXMANAGER_KERNEL_PROC_EVENTS_H
#define MAXMANAGER_KERNEL_PROC_EVENTS_H

#include <stdbool.h>
#include <sys/types.h>

typedef enum {
    KPE_NONE = 0,
    KPE_EXEC,
    KPE_EXIT
} KernelProcEventType;

typedef struct {
    KernelProcEventType type;
    pid_t pid;
    pid_t tgid;
} KernelProcEvent;

typedef struct {
    int sock_fd;
    bool supported;
} KernelProcMonitor;

bool kpe_open(KernelProcMonitor *mon);
void kpe_close(KernelProcMonitor *mon);

/* انتظار صفري الاستهلاك باستخدام poll() حتى حدوث Exec أو Exit أو انتهاء المهلة */
bool kpe_wait_event(KernelProcMonitor *mon, int timeout_ms, KernelProcEvent *out_event);

#endif /* MAXMANAGER_KERNEL_PROC_EVENTS_H */
