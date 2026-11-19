.class final Lcn/nubia/gameassist/GameAssistComStub$Handler;
.super Ljava/lang/Object;
.implements Ljava/lang/reflect/InvocationHandler;

# لماذا يوجد هذا الصنف:
# بديل componentcenter في GameAssistComStub يُنشئ وكيلًا ديناميكيًّا (java.lang.reflect.Proxy)
# لكل واجهة وحدة يطلبها التطبيق، وهذا الصنف هو الـInvocationHandler الذي يجيب على كل نداء
# بقيمة افتراضية من نوع الإرجاع بدل رمي NullPointerException.
#
# السبب مقيس على الجهاز (2026-11-19 13:20): الوكيل كان null، فرمى أول نداء
#   IModuleProxy.setListening(Z, ICallback)V
# في WifiTile.c وأسقط الخيط فماتت العملية كلها قبل إظهار اللوحة.
# والقيم الافتراضية (false/0/null) تعني «لا شيء يُعرف» — وهي لغة التطبيق نفسها في
# ADR-07 (المجهول لا يُدّعى)، لا نجاحًا مزعومًا.

.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public invoke(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;
    .locals 3

    invoke-virtual {p1}, Ljava/lang/reflect/Method;->getReturnType()Ljava/lang/Class;

    move-result-object v0

    sget-object v1, Ljava/lang/Void;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_bool

    const/4 v0, 0x0

    return-object v0

    :ret_bool
    sget-object v1, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_int

    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    return-object v0

    :ret_int
    sget-object v1, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_short

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    return-object v0

    :ret_short
    sget-object v1, Ljava/lang/Short;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_byte

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    move-result-object v0

    return-object v0

    :ret_byte
    sget-object v1, Ljava/lang/Byte;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_char

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v0

    return-object v0

    :ret_char
    sget-object v1, Ljava/lang/Character;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_long

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Character;->valueOf(C)Ljava/lang/Character;

    move-result-object v0

    return-object v0

    :ret_long
    sget-object v1, Ljava/lang/Long;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_float

    const-wide/16 v0, 0x0

    invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v0

    return-object v0

    :ret_float
    sget-object v1, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_double

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v0

    return-object v0

    :ret_double
    sget-object v1, Ljava/lang/Double;->TYPE:Ljava/lang/Class;

    if-eq v0, v1, :ret_null

    const-wide/16 v0, 0x0

    invoke-static {v0, v1}, Ljava/lang/Double;->valueOf(D)Ljava/lang/Double;

    move-result-object v0

    return-object v0

    :ret_null
    const/4 v0, 0x0

    return-object v0
.end method
