.class public Lcn/nubia/gameassist/MultiSubScreenHijack;
.super Ljava/lang/Object;

# لماذا هذا الصنف: MIUI ترفض إيقاظ عملية GameAssist بالبثّ/الخدمة/المزوّد
# (قِيس 2026-11-19: WakePathChecker MIUILOG-AutoStart Reject، ثم
#  BroadcastQueueInjector "process is not permitted to wake path")،
# وضبط MIUIOP(10001/10002/10021/10053)=allow لم يُغيّر الحكم.
# startActivity من تطبيق في المقدمة مسموح، فاستُعمل نشاط GameAssist المصدَّر
# الوحيد المتاح (MultiSubScreenSourceActivity) — ولا تُعدّل المانيفست لأن
# خطّ البناء ينسخ الأصل بـ--copy-original ويقيس تطابقه.
#
# النشاط المصدَّر يُستدعى ببثّ extra اسمه nfw_handle؛ وهذه الدالة تُنادى في
# نهاية MultiSubScreenBaseActivity.onCreate (بعد إنشاء LifecycleRegistry كي
# لا تنهار onPause/onResume)، فتظهر المقبض ثم تُنهي النشاط فورًا. أي استعمال
# حقيقي للميزة (بلا ذلك الـextra) لا يتغيّر سلوكه إطلاقًا.

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static maybeFinish(Landroid/app/Activity;)V
    .locals 3

    :try_start_0
    invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;

    move-result-object v0

    if-eqz v0, :cond_0

    const-string v1, "nfw_handle"

    invoke-virtual {v0, v1}, Landroid/content/Intent;->hasExtra(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    const-string v0, "MultiSubScreenHijack"

    const-string v1, "wake by handle launch"

    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {p0}, Lcn/nubia/gameassist/GameAssistHandle;->start(Landroid/content/Context;)V

    invoke-virtual {p0}, Landroid/app/Activity;->finish()V

    :cond_0
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    return-void
.end method
