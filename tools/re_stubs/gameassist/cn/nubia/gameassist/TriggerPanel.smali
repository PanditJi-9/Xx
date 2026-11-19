.class public final Lcn/nubia/gameassist/TriggerPanel;
.super Ljava/lang/Object;

# لماذا يوجد هذا الصنف:
# GameAssist يبني لوحته الجانبية فقط عندما يستقبل onGameStart() من نظام RedMagic،
# وهذه الإشارة لا تأتي على أجهزة أخرى. فبدل انتظار نظامٍ غائب، نطلق نفس الدالة
# التي كان النظام سيستدعيها — بعد الإقلاع بقليل، مرة واحدة.
#
# ويُسجّل كل خطوة باسم ثابت (GameAssistTrigger) لأن الصمت لا يميّز
# «لم يُستدعَ» من «استُدعي وعاد بلا نافذة» — وقد قياس ذلك مرة (2026-11-19).

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static arm()V
    .locals 4

    :try_start_0
    const-string v0, "GameAssistTrigger"
    const-string v1, "arm()"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v0, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v1

    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    new-instance v1, Lcn/nubia/gameassist/TriggerPanel$1;
    invoke-direct {v1}, Lcn/nubia/gameassist/TriggerPanel$1;-><init>()V

    # 100ms لا 3000ms: قِيس أن العملية تُقتل بضغط الذاكرة قبل المؤقت القديم.
    const-wide/16 v2, 0x64

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistTrigger"
    const-string v2, "arm() threw"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
