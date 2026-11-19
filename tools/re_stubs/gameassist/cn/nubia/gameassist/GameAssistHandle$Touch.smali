.class final Lcn/nubia/gameassist/GameAssistHandle$Touch;
.super Ljava/lang/Object;
.implements Landroid/view/View$OnTouchListener;

# مستقبِل اللمس على شريط السحب. في المرجع يُسحب الشريط من الحافة فتُفتح اللوحة،
# فالإفلات (ACTION_UP) — سواء أكان لمسًا قصيرًا أم سحبًا — ينادي مسار الإظهار.
# لا نُفرّق بالمسافة: شريط بعرض 24px لا يُسحب عرضًا، وكل إفلات عليه قصدٌ للمالك.

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z
    .locals 2

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v0

    const/4 v1, 0x1

    if-ne v0, v1, :end

    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    move-result-object v0

    invoke-static {v0}, Lcn/nubia/gameassist/GameAssistHandle;->show(Landroid/content/Context;)V

    :end
    const/4 v0, 0x1

    return v0
.end method
