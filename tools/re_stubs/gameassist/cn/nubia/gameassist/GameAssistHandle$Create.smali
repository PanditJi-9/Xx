.class final Lcn/nubia/gameassist/GameAssistHandle$Create;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

# مؤجِّل بناء الشريط: يُنفَّذ على الخيط الرئيسي بعد 3 ثوانٍ من الطلب، فتكون لوحة
# GameTurbo قد أضافت نافذتها (فلا يحجبها البناء) وتكون العملية قد صارت مرئية
# فلا تُقتل بضغط الذاكرة. قِيس 16:15 أن البناء المباشر قتل العملية قبل بناء اللوحة.

.field private final ctx:Landroid/content/Context;

.method constructor <init>(Landroid/content/Context;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcn/nubia/gameassist/GameAssistHandle$Create;->ctx:Landroid/content/Context;

    return-void
.end method

.method public run()V
    .locals 2

    :try_start_0
    iget-object v0, p0, Lcn/nubia/gameassist/GameAssistHandle$Create;->ctx:Landroid/content/Context;

    invoke-static {v0}, Lcn/nubia/gameassist/GameAssistHandle;->make(Landroid/content/Context;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v0, "GameAssistHandle"
    const-string v1, "create runnable failed"
    invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    return-void
.end method
