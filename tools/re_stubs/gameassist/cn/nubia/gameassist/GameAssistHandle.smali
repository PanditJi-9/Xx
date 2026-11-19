.class public final Lcn/nubia/gameassist/GameAssistHandle;
.super Ljava/lang/Object;

# شريط السحب الذي يعيد إظهار اللوحة — الطلب الحرفيّ للمالك:
#   «لا توجد شريط سحب لكي اعيد اظهارها كما اريد»
#
# لماذا نافذة مستقلة: اللوحة تُضاف ثم تبقى، وبلا نافذة حيّة تُقتل العملية بضغط
# الذاكرة (قِيس 2026-11-19: adj 905 normal_mem_pressure). والشريط نافذة دائمة
# صغيرة، فتُبقي العملية في com.android.server.am الحيّة، ويُلمس متى شاء المالك.
#
# تاريخ القياس (لا يُعاد اختباره من الصفر):
#   15:26  لا سطر GameAssistHandle إطلاقًا — الحارس كان if-eqz على sView، وهو نفس
#          اختبار if-nez على مرجع، فكان أول نداء (sView=null) يعود فورًا.
#   15:54  نفس النتيجة بعد تصحيح الحارس بعلامة صريحة.
#   16:15  صار السطر «start entered» يظهر مرتين (من TriggerPanel$1 ومن
#          UpdatePluginConfigReceiver.boot)، لكن لا «handle added» ولا
#          «handle start failed» — و`Killing …(adj 905): normal_mem_pressure`
#          بعد 1.6 ثانية. أي أن إنشاء النافذة يحجب الخيط الرئيسي قبل أن تبني
#          لوحة GameTurbo نافذتها، فيموت التطبيق بلا نافذة.
#   ⇒ الفصل التالي: start() تُجدوِل على الخيط الرئيسي بعد 3 ثوانٍ (تبني اللوحة
#     أولًا)، وmake() تبني الشريط بسجلات تقسيم قبل/بعد addView.

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

    if-eqz v0, :have_ctx

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
    .locals 6

    :try_start_0
    const-string v0, "GameAssistHandle"
    const-string v1, "start entered"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    sget-object v2, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    if-nez v2, :create

    return-void

    :create
    if-nez p0, :done

    # لا نبني هنا: البناء على الخيط الرئيسي قبل لوحة GameTurbo يحجبها (قِيس
    # 16:15). نُجدوِل بعد 3 ثوانٍ — اللوحة تكون قد أضافت نافذتها فتبقى العملية.
    new-instance v2, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v3

    invoke-direct {v2, v3}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    new-instance v3, Lcn/nubia/gameassist/GameAssistHandle$Create;

    invoke-direct {v3, p0}, Lcn/nubia/gameassist/GameAssistHandle$Create;-><init>(Landroid/content/Context;)V

    const-wide/16 v4, 0xbb8    # 3000 ms

    invoke-virtual {v2, v3, v4, v5}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    const-string v2, "GameAssistHandle"
    const-string v3, "create posted"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    :done
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
    sget-object v0, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    if-nez v0, :go

    return-void

    :go
    if-nez p0, :end

    const-string v2, "GameAssistHandle"
    const-string v3, "make begin"
    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v0, Landroid/widget/TextView;

    invoke-direct {v0, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V

    const-string v2, "G"

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    const/4 v2, -0x1

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setTextColor(I)V

    const/high16 v2, 0x41800000    # 16.0f

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setTextSize(F)V

    const/16 v2, 0x11

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setGravity(I)V

    const v2, -0x1000000

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setBackgroundColor(I)V

    new-instance v2, Lcn/nubia/gameassist/GameAssistHandle$Touch;

    invoke-direct {v2}, Lcn/nubia/gameassist/GameAssistHandle$Touch;-><init>()V

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setOnTouchListener(Landroid/view/View$OnTouchListener;)V

    new-instance v1, Landroid/view/WindowManager$LayoutParams;

    const/16 v2, 0x3c      # 60 px

    const/16 v3, 0x2bc     # 700 px

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

.method public static show(Landroid/content/Context;)V
    .locals 4

    :try_start_0
    new-instance v0, Landroid/content/Intent;

    const-string v1, "cn.nubia.gameassist.CONFIG_UPDATE_ACTION"

    invoke-direct {v0, v1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    const-string v1, "cn.nubia.gameassist"

    const-string v2, "cn.nubia.gameassist.plugin.config.UpdatePluginConfigReceiver"

    invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string v1, "dispatcher:cn.nubia.gameassist.config.UPDATE_PLUGIN_CONFIG"

    invoke-static {v1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;

    invoke-virtual {p0, v0}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V

    const-string v1, "GameAssistHandle"
    const-string v2, "handle tapped: show requested"
    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
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
