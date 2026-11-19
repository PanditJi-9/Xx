.class public Lcom/zte/feature/Feature;
.super Ljava/lang/Object;

# بديل صنف ZTE للأعلام — يُصلح السقوط الحقيقي المقيس:
#   11-19 16:37:02.445 AndroidRuntime: java.lang.NoClassDefFoundError:
#     Failed resolution of: Lcom/zte/feature/Feature;
#     at com.zte.shared.wrapper.DisplayManagerWrapper.<clinit>
#     at cn.nubia.systemwrapper.ActivityManagerWrapper.e
#     at cn.nubia.gameassist.dessert.tiles.RecordTile.w
#     at com.zte.gameassist.common.ObserverManager.b
#     at cn.nubia.gameassist.dessert.tiles.RecordTile.c
#     at cn.nubia.gameassist.dessert.panel.DessertTilesAdapter.O
#     … RecyclerView/StaggeredGridLayoutManager → موت العملية (am_proc_died adj 905)
#
# لماذا صنف بديل لا لفّ لكل مُستدعٍ: الصنف الغائب لا يوجد في أي APK من حزمتنا
# (قِيس: المراجع كلها ملفان فقط)، وDisplayManagerWrapper يقرأه في <clinit> —
# أي أن أول لمس لأي غلاف عرض يُسقط <clinit> ويُسمّم الصنف كله (NoClassDefFoundError)
# لكل من يستخدمه لاحقًا. فالعلاج في الجذر: صنف واحد يُعيد القيمة الافتراضية،
# فينجح <clinit> مرة واحدة ويعمل كل مُستدعٍ بلا لفّ ولا مسار ثانٍ (ADR-18).
#
# واجهته مقيسة من موضعَي الاستخدام فقط:
#   1) DisplayManagerWrapper.<clinit>  → getBoolean(String;Z)Z  (مرتين)
#   2) com/zte/mifavor/widget/Utils    → getCtsRunningStatus()Z
# ولا نُضيف ما لم يُرجع إليه أحد: الواجهة الزائدة تُوهم بسلوك غير موجود (ADR-07).

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

# أعلام الميزات على هذا الجهاز غير موجودة ⇒ القيمة الافتراضية التي يطلبها المُستدعي.
.method public static getBoolean(Ljava/lang/String;Z)Z
    .locals 0

    return p1
.end method

# «هل يعمل CTS؟» — على جهاز مستخدم عادي: لا. القيمة الافتراضية false.
.method public static getCtsRunningStatus()Z
    .locals 1

    const/4 v0, 0x0

    return v0
.end method
