.class public final Lcn/nubia/gameassist/GameAssistComStub;
.super Ljava/lang/Object;
.implements Lcn/nubia/componentcenter/service/GameAssistComService;

# لماذا يوجد هذا الصنف:
# كل وحدات تحكّم اللوحة تطلب خدمة componentcenter عبر
#   Router.getService("GameAssistComService")
# ثم تستدعي a(Class) عليها. وعلى أجهزة غير RedMagic لا يوجد من يسجّل هذه
# الخدمة، فتعود null، فيرمي أول invoke-interface بـNullPointerException
# ويُسقط GameAssistWindowManager.<init> كلّه (قِيس 2026-11-19 11:47).
#
# فهذا بديل يسجّل نفسه بنفس المفتاح ويعيد null من a(Class) — أي «لا وكيل لهذه
# الوحدة»، وهو ما تفهمه الوحدات كتعطيل ميزتها. الواجهة تحمل دالة واحدة فقط،
# فالسطح المقلَّد صغير وبقدر الحاجة.

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public a(Ljava/lang/Class;)Lcom/zte/gameassist/common/IModuleProxy;
    .locals 0

    const/4 p0, 0x0

    return-object p0
.end method
