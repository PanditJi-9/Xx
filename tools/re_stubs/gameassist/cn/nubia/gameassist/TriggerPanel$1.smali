.class final Lcn/nubia/gameassist/TriggerPanel$1;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

# ينفّذ الإشارة التي كان نظام RedMagic سيُرسلها: onGameStart().
#
# ولا يعتمد على GameAssistApplication.k(): قِيس على الجهاز (2026-11-19) أن
# onCreate ملفوف بـcatch-all يشمل invoke-super، فإن رمى super (عطب componentcenter)
# لم يصل التنفيذ إلى sget-object الذي يعيّن المطيح، فعاد k() فارغًا ووقف كل شيء بصمت.
# فالسياق يُؤخذ من ActivityThread.currentApplication() — غير فارغ بعد ثلاث ثوانٍ — وk()
# مجرّد تفضيل إن كان موجودًا.

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public run()V
    .locals 5

    :try_start_0
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0

    if-eqz v0, :have_ctx

    invoke-static {}, Lcn/nubia/gameassist/GameAssistApplication;->k()Lcn/nubia/gameassist/GameAssistApplication;
    move-result-object v0

    :have_ctx
    if-eqz v0, :cond_end

    const-string v2, "GameAssistTrigger"
    const-string v3, "run: context acquired"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    # سجّل بديل componentcenter قبل بناء المدير: كل وحدة تحكّم تطلبه بـ
    # getService("GameAssistComService")، وبلا بديل يرمي أول استدعاء NPE
    # ويسقط البناء كله. addService يضع في HashMap فقط، والتسجيل المسبق آمن.
    invoke-static {}, Lcn/nubia/componentcenter/router/Router;->getInstance()Lcn/nubia/componentcenter/router/Router;
    move-result-object v2

    const-string v3, "GameAssistComService"

    new-instance v4, Lcn/nubia/gameassist/GameAssistComStub;
    invoke-direct {v4}, Lcn/nubia/gameassist/GameAssistComStub;-><init>()V

    invoke-virtual {v2, v3, v4}, Lcn/nubia/componentcenter/router/Router;->addService(Ljava/lang/String;Ljava/lang/Object;)V

    const-string v2, "GameAssistTrigger"
    const-string v3, "componentcenter stub registered"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    # الترتيب مقيس من الشجرة: p0() يبني LayoutParams ثم ينشر w → g0() الذي
    # يبني GameAssistRootView (ويشغّل c0() فيصير حقل السماح D=true)، وc() ينشر
    # l0() → w0("onTouchSlideIn") الذي يُظهر النافذة. الحاجزان المتبقيان في
    # نوع النافذة 2027 ووسم PRIVATE_FLAG_TRUSTED_OVERLAY يُصلحان في الـworkflow.
    invoke-static {v0}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->O(Landroid/content/Context;)Lcn/nubia/gameassist/panel/GameAssistWindowManager;
    move-result-object v1

    if-eqz v1, :cond_end

    const-string v2, "GameAssistTrigger"
    const-string v3, "calling p0() then c()"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->p0()V

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->c()V

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->onGameStart()V

    const-string v2, "GameAssistTrigger"
    const-string v3, "show sequence posted"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    :cond_end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistTrigger"
    const-string v2, "run() threw, contained"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
