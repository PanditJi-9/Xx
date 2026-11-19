.class final Lcn/nubia/gameassist/TriggerPanel$2;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

# محاولة إظهار ثانية بعد أن يفرغ W() من بناء الجذر والـLayoutParams ويضع D=true.
# سببه مقيس: p0() ينشر على Handler الحقل n، وc() على Handler الحقل o — مختلفان،
# فترتيب النشر لا يضمن الترتيب، وقد رُصد showWindow() يسبق انتهاء W().

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public run()V
    .locals 4

    :try_start_0
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0

    if-eqz v0, :have_ctx

    invoke-static {}, Lcn/nubia/gameassist/GameAssistApplication;->k()Lcn/nubia/gameassist/GameAssistApplication;
    move-result-object v0

    :have_ctx
    if-eqz v0, :cond_end

    invoke-static {v0}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->O(Landroid/content/Context;)Lcn/nubia/gameassist/panel/GameAssistWindowManager;
    move-result-object v1

    if-eqz v1, :cond_end

    const-string v2, "GameAssistTrigger"
    const-string v3, "retry showWindow()"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    const-string v2, "retryTrigger"
    invoke-virtual {v1, v2}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->w0(Ljava/lang/String;)V

    :cond_end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistTrigger"
    const-string v2, "retry threw, contained"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
