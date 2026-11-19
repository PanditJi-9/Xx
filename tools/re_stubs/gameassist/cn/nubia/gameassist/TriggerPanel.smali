.class public final Lcn/nubia/gameassist/TriggerPanel;
.super Ljava/lang/Object;

# لماذا يوجد هذا الصنف:
# GameAssist يبني لوحته الجانبية فقط عندما يستقبل onGameStart() من نظام RedMagic،
# وهذه الإشارة لا تأتي على أجهزة أخرى. فبدل انتظار نظامٍ غائب، نطلق نفس الدالة
# التي كان النظام سيستدعيها — بعد الإقلاع بقليل، مرة واحدة.

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static arm()V
    .locals 4

    :try_start_0
    new-instance v0, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v1

    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    new-instance v1, Lcn/nubia/gameassist/TriggerPanel$1;
    invoke-direct {v1}, Lcn/nubia/gameassist/TriggerPanel$1;-><init>()V

    const-wide/16 v2, 0xbb8

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    return-void
.end method
