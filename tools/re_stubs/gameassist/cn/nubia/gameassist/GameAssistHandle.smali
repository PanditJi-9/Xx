.class public final Lcn/nubia/gameassist/GameAssistHandle;
.super Ljava/lang/Object;

# شريط السحب الذي يعيد إظهار اللوحة — الطلب الحرفيّ للمالك:
#   «لا توجد شريط سحب لكي اعيد اظهارها كما اريد»
#
# لماذا نافذة مستقلة: اللوحة تُضاف ثم تبقى، وبلا نافذة حيّة تُقتل العملية بضغط
# الذاكرة (قِيس 2026-11-19: adj 905 normal_mem_pressure). والشريط نافذة دائمة
# صغيرة، فتُبقي العملية في com.android.server.am الحيّة، ويُلمس متى شاء المالك.
#
# وليس فيه اختراع لمسار إظهار جديد: اللمس يُرسل نفس البثّ الذي قِيس أنه يُظهر
# اللوحة (UpdatePluginConfigReceiver + dispatcher:…UPDATE_PLUGIN_CONFIG)،
# فالمسار واحد لا اثنان (ADR-18).

.field private static sView:Landroid/view/View;

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static start(Landroid/content/Context;)V
    .locals 7

    :try_start_0
    sget-object v0, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    # القفز يجب أن يقع حين تكون النافذة مضافة أصلًا (v0 != null).
    # كان مقلوبًا (if-nez) فكان أول استدعاء — وsView ما زال null — يقفز
    # إلى نهاية الدالة ولا يضيف شيئًا: قِيس 2026-11-19 15:26 أن البثّ يعمل
    # واللوحة تُبنى، ولا يظهر سطر GameAssistHandle إطلاقًا، مع أن استدعاء
    # start في classes2.dex موجود (باكسمالي: TriggerPanel$1 سطر 60).
    if-eqz v0, :try_end_0

    if-nez p0, :try_end_0

    new-instance v0, Landroid/widget/TextView;

    invoke-direct {v0, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V

    const-string v1, "G"

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    const/4 v1, -0x1

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V

    const/high16 v1, 0x41800000    # 16.0f

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextSize(F)V

    const/16 v1, 0x11

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setGravity(I)V

    const v1, -0x1000000

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setBackgroundColor(I)V

    new-instance v1, Lcn/nubia/gameassist/GameAssistHandle$Touch;

    invoke-direct {v1}, Lcn/nubia/gameassist/GameAssistHandle$Touch;-><init>()V

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setOnTouchListener(Landroid/view/View$OnTouchListener;)V

    # العرض 60px والارتفاع 700px على شاشة 1220x2712 — شريط رقيق على الحافة.
    new-instance v1, Landroid/view/WindowManager$LayoutParams;

    const/16 v2, 0x3c

    const/16 v3, 0x2bc

    const/16 v4, 0x7f6     # TYPE_APPLICATION_OVERLAY

    const/16 v5, 0x108     # NOT_FOCUSABLE | LAYOUT_IN_SCREEN

    const/4 v6, -0x3       # PixelFormat.TRANSLUCENT

    invoke-direct/range {v1 .. v6}, Landroid/view/WindowManager$LayoutParams;-><init>(IIIII)V

    const/16 v2, 0x15      # RIGHT | CENTER_VERTICAL

    iput v2, v1, Landroid/view/WindowManager$LayoutParams;->gravity:I

    const-string v2, "window"

    invoke-virtual {p0, v2}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/WindowManager;

    invoke-interface {v2, v0, v1}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    sput-object v0, Lcn/nubia/gameassist/GameAssistHandle;->sView:Landroid/view/View;

    const-string v2, "GameAssistHandle"

    const-string v3, "handle added at right edge"

    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "GameAssistHandle"

    const-string v2, "handle start failed"

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
