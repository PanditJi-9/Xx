.class final Lcn/nubia/gameassist/TriggerPanel$1;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

# ينفّذ الإشارة التي كان نظام RedMagic سيُرسلها: onGameStart().
# لا يفترض أن مدير النافذة جاهز: إن لم يكن، يبنيه من مطيعه العام O(Context).

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public run()V
    .locals 4

    :try_start_0
    invoke-static {}, Lcn/nubia/gameassist/GameAssistApplication;->k()Lcn/nubia/gameassist/GameAssistApplication;
    move-result-object v0

    if-eqz v0, :cond_end

    invoke-static {}, Lcn/nubia/gameassist/GameAssistApplication;->j()Lcn/nubia/gameassist/panel/GameAssistWindowManager;
    move-result-object v1

    if-eqz v1, :cond_call

    invoke-static {v0}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->O(Landroid/content/Context;)Lcn/nubia/gameassist/panel/GameAssistWindowManager;
    move-result-object v1

    :cond_call
    if-eqz v1, :cond_end

    const-string v2, "GameAssistTrigger"
    const-string v3, "calling onGameStart()"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->onGameStart()V

    :cond_end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistTrigger"
    const-string v2, "onGameStart() threw, contained"
    invoke-static {v1, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    return-void
.end method
