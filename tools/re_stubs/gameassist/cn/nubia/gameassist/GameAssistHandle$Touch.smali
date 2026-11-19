.class final Lcn/nubia/gameassist/GameAssistHandle$Touch;
.super Ljava/lang/Object;
.implements Landroid/view/View$OnTouchListener;

# مستقبِل اللمس على شريط السحب: يفرّق بين الضغط القصير (إظهار) والسحب (لا شيء
# الآن، فلا نُزيح الشريط عرضًا). وحدّ التمييز 100px — أكبر من رجفة الإصبع
# وأصغر من سحب مقصود على شاشة 1220px.

.field private downX:F

.field private downY:F

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z
    .locals 6

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v0

    if-nez v0, :up

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F

    move-result v1

    iput v1, p0, Lcn/nubia/gameassist/GameAssistHandle$Touch;->downX:F

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F

    move-result v1

    iput v1, p0, Lcn/nubia/gameassist/GameAssistHandle$Touch;->downY:F

    const/4 v0, 0x1

    return v0

    :up
    const/4 v1, 0x1

    if-ne v0, v1, :end

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F

    move-result v1

    iget v2, p0, Lcn/nubia/gameassist/GameAssistHandle$Touch;->downX:F

    sub-float v1, v1, v2

    invoke-static {v1}, Ljava/lang/Math;->abs(F)F

    move-result v1

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F

    move-result v2

    iget v3, p0, Lcn/nubia/gameassist/GameAssistHandle$Touch;->downY:F

    sub-float v2, v2, v3

    invoke-static {v2}, Ljava/lang/Math;->abs(F)F

    move-result v2

    add-float v1, v1, v2

    const/high16 v2, 0x42c80000    # 100.0f

    cmpg-float v3, v1, v2

    if-gtz v3, :end

    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    move-result-object v3

    invoke-static {v3}, Lcn/nubia/gameassist/GameAssistHandle;->show(Landroid/content/Context;)V

    :end
    const/4 v0, 0x1

    return v0
.end method
