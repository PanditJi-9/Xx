/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#ifndef MAXMANAGER_IPC_SERVER_H
#define MAXMANAGER_IPC_SERVER_H

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>

/*
 * قناة الاتصال بين التطبيق (بلا روت) والخادم (روت، بنطاق SELinux خاصّ به) — المرحلة الأولى.
 *
 * سوكيت من نوع unix stream في **الفضاء المجرّد** (`@maxmanager.sock`): لا ملفّ على القرص فلا يحتاج التطبيق
 * إذنًا على أيّ مجلّد، ويحكم SELinux بـ`connectto` على نطاق الخادم وحده. ثم الخادم يتحقّق بنفسه من
 * هويّة الطرف الآخر (`SO_PEERCRED`) — فلا يكفي أن يمرّ SELinux.
 *
 * البروتوكول سطر طلب واحد ثم ردّ ثم إغلاق. **قائمة بيضاء مغلقة** — لا أمر صدفة ولا نصّ حرّ:
 *   PING                     ->  OK 1
 *   STATUS                   ->  OK state=<running|stopped|...> ai=<0|1>
 *   PROFILE <1|2|3> [AI]     ->  OK | ERR exit=<n>
 *   EXISTS <path>            ->  OK 1 | OK 0                (مسار من القائمة البيضاء فقط)
 *   READ <path>              ->  OK <len>\n<len bytes>       (قراءة فقط، ≤ IPC_READ_MAX)
 *   SETPROP <key> <value>    ->  OK | ERR invalid            (مفتاح تحت persist.sys.maxmanagerconf. فقط)
 *   WRITE <path> <value>     ->  OK | ERR write=<errno>      (عقدة من قائمة الكتابة الضيّقة، قيمة ≤ ٦٤ حرفًا)
 *   CANWRITE <path>          ->  OK 1 | OK 0                (في قائمة الكتابة **وموجودة**)
 */

#define IPC_SOCKET_NAME "maxmanager.sock"
#define IPC_PACKAGE_NAME "nd.max"
#define IPC_PACKAGES_LIST "/data/system/packages.list"
#define IPC_LINE_MAX 224
#define IPC_PATH_MAX 192
#define IPC_KEY_MAX 96
#define IPC_VALUE_MAX 92   /* PROP_VALUE_MAX */
#define IPC_WRITE_VALUE_MAX 64
#define IPC_READ_MAX 4096
#define IPC_REPLY_MAX (IPC_READ_MAX + 32)
#define IPC_PROP_PREFIX "persist.sys.maxmanagerconf."

typedef enum {
    IPC_CMD_INVALID = 0,
    IPC_CMD_PING,
    IPC_CMD_STATUS,
    IPC_CMD_PROFILE,
    IPC_CMD_EXISTS,
    IPC_CMD_READ,
    IPC_CMD_SETPROP,
    IPC_CMD_WRITE,
    IPC_CMD_CANWRITE,
} IpcCommandKind;

typedef struct {
    IpcCommandKind kind;
    char profile;  /* '1' | '2' | '3' عند IPC_CMD_PROFILE */
    bool from_ai;  /* علم `--from-ai` الذي يضعه محرّك MAX AI */
    char path[IPC_PATH_MAX];
    char key[IPC_KEY_MAX];
    char value[IPC_VALUE_MAX];
} IpcCommand;

/* مُنفِّذ تبديل الملف — يُستبدل في الاختبارات. يعيد رمز خروج العملية (0 = نجاح). */
typedef int (*IpcProfileRunner)(char profile, bool from_ai);
/* كاتب الخصائص — يُستبدل في الاختبارات. يعيد 0 عند النجاح. */
typedef int (*IpcPropertySetter)(const char* key, const char* value);

typedef struct {
    IpcProfileRunner profile;
    IpcPropertySetter setprop;
} IpcBackend;

extern const IpcBackend IPC_REAL_BACKEND;

bool ipc_parse_command(const char* line, IpcCommand* out);
bool ipc_app_uid_from_text(const char* text, const char* package, uid_t* uid);
bool ipc_uid_allowed(uid_t peer, bool app_known, uid_t app_uid);
bool ipc_path_allowed(const char* path);
bool ipc_write_path_allowed(const char* path);
bool ipc_write_value_allowed(const char* value);
int ipc_write_node(const char* resolved, const char* value);
bool ipc_resolved_allowed(const char* resolved);
bool ipc_prop_allowed(const char* key, const char* value);
size_t ipc_execute(const IpcCommand* command, const IpcBackend* backend, char* reply, size_t reply_size);
void ipc_handle_connection(int fd, uid_t peer_uid, const IpcBackend* backend);
int ipc_spawn_self_profile(char profile, bool from_ai);
/* يعدّد أهداف الفحص من القوائم البيضاء نفسها (مصدر واحد): ملفّ محدّد (walk=false) أو مجلّد يُنزَل فيه (walk=true). */
typedef void (*IpcScanEmit)(const char* path, bool walk, void* context);
void ipc_scan_targets(IpcScanEmit emit, void* context);
void start_ipc_server(void);

#endif /* MAXMANAGER_IPC_SERVER_H */
