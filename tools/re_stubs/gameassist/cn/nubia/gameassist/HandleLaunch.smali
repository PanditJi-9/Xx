.class public Lcn/nubia/gameassist/HandleLaunch;
.super Landroid/app/Activity;

# لماذا نشاط مصدَّر بدل بثّ: قِيس (2026-11-19 ~19:35) أن MIUI ترفض إيقاظ عملية
# GameAssist بالبثّ المحقون من GameSpace:
#   WakePathChecker: MIUILOG-AutoStart ... Reject userId=0
#     caller=cn.nubia.gamelauncher callee=cn.nubia.gameassist wakeType=2
#   BroadcastQueueInjector: Unable to launch app cn.nubia.gameassist ...
#     process is not permitted to wake path
# وضبط MIUIOP(10001/10002/10021/10053)=allow لم يُغيّر الحكم.
# startActivity من تطبيق في المقدمة (GameSpace لحظة لمس «بدء اللعبة») لا يمرّ
# بذلك الفحص، فهو الطريق الوحيد المقيس للإيقاظ.
#
# النشاط لا يرسم شيئًا: theme شفاف من المانيفست، يستدعي مقبضنا ثم ينهي نفسه.
# العملية تشغّل GameAssistApplication.onCreate (فيه TriggerPanel.arm) أصلًا،
# فالنداء هنا تأكيد لا شرط.

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/app/Activity;-><init>()V

    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 2

    :try_start_0
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    invoke-static {p0}, Lcn/nubia/gameassist/GameAssistHandle;->start(Landroid/content/Context;)V

    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v0, "HandleLaunch"

    const-string v1, "launch failed"

    invoke-static {v0, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    invoke-virtual {p0}, Landroid/app/Activity;->finish()V

    return-void
.end method
