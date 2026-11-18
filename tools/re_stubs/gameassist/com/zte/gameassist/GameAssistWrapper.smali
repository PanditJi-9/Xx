.class public Lcom/zte/gameassist/GameAssistWrapper;
.super Ljava/lang/Object;
.source "GameAssistWrapper.java"

# Stub for a class the ZTE firmware supplies and this device does not.
#
# Measured cause: cn.nubia.gameassist dies in Application.onCreate with
#
#   NoClassDefFoundError: Failed resolution of:
#       Lcom/zte/gameassist/GameAssistWrapper$Callback;
#     at cn.nubia.gameassist.utils.Utils.<clinit>
#
# because Utils declares `private static e:Lcom/zte/shared/wrapper/
# GameAssistControllerWrapper$Callback;`, and that class declares a field typed
# Lcom/zte/gameassist/GameAssistWrapper$Callback;. Resolving a field type is
# enough to make the whole class fail, so one absent class stops every service
# in the package — including the overlay.
#
# The dex of the app itself defines AbsGameAssistToken and
# AbsGameAssistToken$GameAssistControllerWrapper but never GameAssistWrapper, so
# the type must come from the system image. Every method here answers with the
# "no system controller" result, which is exactly what the callers already test
# (if-eqz / null checks), so features that need the real controller stay off
# instead of the process dying.

.field private static final sInstance:Lcom/zte/gameassist/GameAssistWrapper;


.method static constructor <clinit>()V
    .locals 1

    new-instance v0, Lcom/zte/gameassist/GameAssistWrapper;

    invoke-direct {v0}, Lcom/zte/gameassist/GameAssistWrapper;-><init>()V

    sput-object v0, Lcom/zte/gameassist/GameAssistWrapper;->sInstance:Lcom/zte/gameassist/GameAssistWrapper;

    return-void
.end method


.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


.method public static getInstance()Lcom/zte/gameassist/GameAssistWrapper;
    .locals 1

    sget-object v0, Lcom/zte/gameassist/GameAssistWrapper;->sInstance:Lcom/zte/gameassist/GameAssistWrapper;

    return-object v0
.end method


.method public static getGameAssistController()Lcom/zte/gameassist/aidl/IGameAssistController;
    .locals 1

    const/4 v0, 0x0

    return-object v0
.end method


.method public static getMutexTags(Ljava/lang/String;)Ljava/util/List;
    .locals 1

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    return-object v0
.end method


.method public invake(Ljava/lang/String;Landroid/os/Bundle;Lcom/zte/gameassist/GameAssistWrapper$Callback;)V
    .locals 0

    return-void
.end method


.method public sendToGameAssistServices(Ljava/lang/String;Landroid/os/Bundle;)Z
    .locals 1

    const/4 v0, 0x0

    return v0
.end method


.method public sendToGameAssistServices(Ljava/lang/String;Landroid/os/Bundle;Lcom/zte/gameassist/GameAssistWrapper$Callback;)Z
    .locals 1

    const/4 v0, 0x0

    return v0
.end method


.method public mutexTag(ZLjava/lang/String;Ljava/lang/String;)V
    .locals 0

    return-void
.end method


.method public monitorMutexTag(ZLjava/lang/String;Lcom/zte/gameassist/GameAssistWrapper$MutexCallback;)V
    .locals 0

    return-void
.end method
