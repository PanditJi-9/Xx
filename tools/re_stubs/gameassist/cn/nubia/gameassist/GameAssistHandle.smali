.class public final Lcn/nubia/gameassist/GameAssistHandle;
.super Ljava/lang/Object;

# شريط السحب الذي يعيد إظهار اللوحة — ومطابقته للمرجع الذي أرسله المالك:
#   «يفترض ان يبدو الامر هكذا …/Screenrecorder-2026-11-19-18-12-45-128.mp4»
# في الفيديو (هاتف RedMagic حقيقي): شريط **رفيع جدًا شبه شفاف ملتصق بالحافة**،
# يُسحب/يُلمس فتُفتح لوحة GameTurbo كاملة فوق اللعبة. لا حرف ولا صندوق أسود سميك.
#
# تاريخ القياس (لا يُعاد اختباره من الصفر):
#   15:26  لا سطر GameAssistHandle إطلاقًا — الحارس كان if-eqz على sView، وهو نفس
#          اختبار if-nez على مرجع، فكان أول نداء (sView=null) يعود فورًا.
#   17:11  إصلاح دلالة الدالڤيك: if-eqz يقفز عند == 0، وif-nez يقفز عند != 0.
#   17:41  أول نجاح: «view added» ثم نافذتان مرئيتان (اللوحة fillxfill + الشريط
#          60×700). لكن الشكل لم يكن كالمرجع.
#   17:42  BadTokenException على النوع 2003 قتل العملية (أُصلح في الـworkflow).
#   18:22  MIUI قتل العملية بـ`am_kill … normal_mem_pressure` بعد ~11 ثانية لأن
#          GamePredictor يصنّفها nonGame — ولذلك صار الإظهار عند اللمس فقط،
#          فلا نافذة ملء الشاشة تبتلع لمس اللعبة قبل أن يطلبها المالك.
#
# القرار: النافذة الدائمة شريط رفيع (24×1200px) على الحافة اليمنى، بلون أبيض
# شبه شفاف، ونصّ فارغ — يقارب مرجع المالك ويُبقي العملية حيّة. واللمس أو السحب
# ينادي مسار الإظهار نفسه الذي كان يستعمله TriggerPanel$2 (w0 = showWindow).

.field private static sView:Landroid/view/View;

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

# نقطة دخول بلا وسائط لـre_inject_call (لا يقبل إلا توقيعًا بلا بارامترات).
.method public static boot()V
    .locals 3

    :try_start_0
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0

    if-nez v0, :have_ctx

    invoke-static {}, Lcn/nubia/gameassist/GameAssistApplication;->k()Lcn/nubia/gameassist/GameAssistApplication;
    move-result-object v0

    :have_ctx
    if-eqz v0, :done

    const-string v1, "GameAssistHandle"
    const-string v2, "boot: context acquired"
    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {v0}, Lcn/nubia/gameassist/GameAssistHandle;->start(Landroid/content/Context;)V

    :done
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistHandle"
    const-string v2, "boot failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method

.method public static start(Landroid/content/Context;)V
    .locals 3

    :try_start_0
    const-string v0, "GameAssistHandle"
    const-string v1, "start entered"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {p0}, Lcn/nubia/gameassist/GameAssistHandle;->make(Landroid/content/Context;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistHandle"
    const-string v2, "start failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method

.method public static make(Landroid/content/Context;)V
    .locals 8

    :try_start_0
    const-string v2, "GameAssistHandle"
    const-string v3, "make entered"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    sget-object v0, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    # دلالة الدالڤيك — وهي التي أوقعتنا خمس مرات:
    #   if-eqz  يقفز عندما تكون القيمة **صفرًا** (== 0)
    #   if-nez  يقفز عندما تكون القيمة **غير صفر** (!= 0)
    if-eqz v0, :go

    const-string v2, "GameAssistHandle"
    const-string v3, "already added"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    return-void

    :go
    if-eqz p0, :end

    # View عارية بلا نصّ: الشريط في المرجع خطّ لا حرف.
    new-instance v0, Landroid/view/View;

    invoke-direct {v0, p0}, Landroid/view/View;-><init>(Landroid/content/Context;)V

    const v2, 0x66ffffff    # 40% أبيض — مرئي فوق الألعاب الداكنة وغير مزعج

    invoke-virtual {v0, v2}, Landroid/view/View;->setBackgroundColor(I)V

    new-instance v2, Lcn/nubia/gameassist/GameAssistHandle$Touch;

    invoke-direct {v2}, Lcn/nubia/gameassist/GameAssistHandle$Touch;-><init>()V

    invoke-virtual {v0, v2}, Landroid/view/View;->setOnTouchListener(Landroid/view/View$OnTouchListener;)V

    new-instance v1, Landroid/view/WindowManager$LayoutParams;

    const/16 v2, 0x18      # 24 px عرضًا — رفيع كالمرجع

    const/16 v3, 0x4b0     # 1200 px ارتفاعًا (وسط الشاشة)

    const/16 v4, 0x7f6     # TYPE_APPLICATION_OVERLAY

    const/16 v5, 0x108     # NOT_FOCUSABLE | LAYOUT_IN_SCREEN

    const/4 v6, -0x3       # PixelFormat.TRANSLUCENT

    invoke-direct/range {v1 .. v6}, Landroid/view/WindowManager$LayoutParams;-><init>(IIIII)V

    const/16 v2, 0x15      # RIGHT | CENTER_VERTICAL

    iput v2, v1, Landroid/view/WindowManager$LayoutParams;->gravity:I

    const-string v2, "GameAssistHandle"
    const-string v3, "before addView"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    const-string v2, "window"

    invoke-virtual {p0, v2}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v2

    check-cast v2, Landroid/view/WindowManager;

    invoke-interface {v2, v0, v1}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    sput-object v0, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    const-string v2, "GameAssistHandle"
    const-string v3, "view added"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    :end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistHandle"
    const-string v2, "make failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method

# مسار الإظهار: نفس تسلسل النظام المقيس (p0 يبني الجذر، c ينشر l0،
# وw0 = showWindow هو ما كان TriggerPanel$2 يكرّره بعد البناء). لا بثّ هنا:
# الشريط يعيش داخل العملية، فالنداء المباشر هو نفس نقطة التحوّل لا مسار ثانٍ.
# وTriggerPanel$2 نفسه يُعاد استعماله كمحاولة ثانية بعد 300ms — لا صنف جديد.
.method public static show(Landroid/content/Context;)V
    .locals 6

    :try_start_0
    const-string v0, "GameAssistHandle"
    const-string v1, "handle tapped: show requested"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {p0}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->O(Landroid/content/Context;)Lcn/nubia/gameassist/panel/GameAssistWindowManager;
    move-result-object v1

    if-eqz v1, :end

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->p0()V

    invoke-virtual {v1}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->c()V

    const-string v2, "handleTap"

    invoke-virtual {v1, v2}, Lcn/nubia/gameassist/panel/GameAssistWindowManager;->w0(Ljava/lang/String;)V

    new-instance v2, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v3

    invoke-direct {v2, v3}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    new-instance v3, Lcn/nubia/gameassist/TriggerPanel$2;

    invoke-direct {v3}, Lcn/nubia/gameassist/TriggerPanel$2;-><init>()V

    const-wide/16 v4, 0x12c

    invoke-virtual {v2, v3, v4, v5}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    :end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistHandle"
    const-string v2, "handle show failed"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
