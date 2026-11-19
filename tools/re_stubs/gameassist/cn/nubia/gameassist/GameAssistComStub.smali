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
# فالبديل يسجّل نفسه بنفس المفتاح، ويعيد لكل وحدة **وكيلًا ديناميكيًّا غير فارغ**
# ينفّذ واجهتها ويردّ بقيم افتراضية (GameAssistComStub$Handler).
#
# ولماذا لم يبقَ null: قِيس 2026-11-19 13:20 أن null يقتل العملية في
# WifiTile.c عبر IModuleProxy.setListening قبل ظهور اللوحة، وأن تعطيل كل وحدة
# على حِدة مطاردة لا تنتهي (13 وحدة، ولكل واحدة عدة نداءات).

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public a(Ljava/lang/Class;)Lcom/zte/gameassist/common/IModuleProxy;
    .locals 3

    if-eqz p1, :no_proxy

    invoke-virtual {p1}, Ljava/lang/Class;->isInterface()Z

    move-result v0

    if-eqz v0, :no_proxy

    const/4 v0, 0x1

    new-array v0, v0, [Ljava/lang/Class;

    const/4 v1, 0x0

    aput-object p1, v0, v1

    invoke-virtual {p1}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;

    move-result-object v1

    new-instance v2, Lcn/nubia/gameassist/GameAssistComStub$Handler;

    invoke-direct {v2}, Lcn/nubia/gameassist/GameAssistComStub$Handler;-><init>()V

    invoke-static {v1, v0, v2}, Ljava/lang/reflect/Proxy;->newProxyInstance(Ljava/lang/ClassLoader;[Ljava/lang/Class;Ljava/lang/reflect/InvocationHandler;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/zte/gameassist/common/IModuleProxy;

    return-object v0

    :no_proxy
    const/4 v0, 0x0

    return-object v0
.end method
