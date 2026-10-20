/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasControlGoal
import nd.max.core.atlas.AtlasControlIntent
import nd.max.core.atlas.AtlasControlTarget
import nd.max.core.atlas.AtlasControlTransport
import nd.max.core.atlas.AtlasRouteCandidate
import nd.max.core.atlas.AtlasRouteEvidence
import nd.max.core.atlas.AtlasSafetyPolicy
import nd.max.core.atlas.AtlasSafetyVerdict

/**
 * مُلاءِم اللمس في أطلس: يحوّل عقدة تعزيز **مقروءة وقابلة للكتابة** إلى مسار معاملة كامل.
 *
 * وما الذي يجعله آمنًا رغم أنه يكتب في `proc`/`sys`؟
 *
 * 1. **لا عقدة بلا قراءة.** العقدة لا تدخل الخطة إلا إذا أجابَت بقيمة `0` أو `1` **الآن**، وقبلت
 *    `test -w`. عقدة موجودة لا تُقرأ ليست مقبضًا، ولو كان اسمها معروفًا.
 * 2. **لا قيمة مخمَّنة.** المعنى الوحيد الذي يثبته المستودع لعقد اللمس هو التبديل الثنائي
 *    (`onValue`/`offValue`). أمّا معدّل العيّنات فبلا وحدة مثبَتة، فلا يُبنى له مسار كتابة —
 *    ويُعرض للقراءة فقط في الشاشة.
 * 3. **المعاملة تمرّ من المُحكِّم.** `apply`/`restore` يستعملان `writeVerified` القائم، ثم يقرأ
 *    [HardwareRepairExecutor] العقدة في نافذة تأكيد bounded، ويرحّع خط الأساس عند الانحراف.
 *    فالمُلاءِم لا يملك كاتبًا ثانيًا.
 * 4. **قائمة عدم اللمس أولًا.** العقدة الممنوعة تُسقَط برمز `never-touch:<rule>` قبل أن تُبنى.
 *
 * وحدّه المعلن: هذا المُلاءِم لا يجعل عتادًا لا يملك عقدةً يملكها. غياب المسار يعني «لا مقبض
 * مقروء/قابل للكتابة على هذا الجهاز»، لا «فشل» — ويُعلن ذلك في خريطة أطلس.
 */
class TouchSysfsAdapter : AtlasControlAdapter {

    override val id: String = "touch-sysfs-reviewed"
    override val target: AtlasControlTarget = AtlasControlTarget.TOUCH
    override val transport: AtlasControlTransport = AtlasControlTransport.ARBITER_SYSFS

    override fun assess(
        context: AtlasAdapterContext,
        request: AtlasControlRequest?,
    ): AtlasAdapterAssessment {
        val access = context.touchAccess
            ?: return AtlasAdapterAssessment.NotApplicable(SKIP_NO_SEAM)
        val nodes = context.touchNodes.filter { it.kind in BOOST_KINDS }
        if (nodes.isEmpty()) return AtlasAdapterAssessment.NotApplicable(SKIP_NO_NODE)

        val denied = nodes.firstOrNull { node ->
            AtlasSafetyPolicy.verdictFor(name = node.kind.token, path = node.path) is AtlasSafetyVerdict.Denied
        }
        if (denied != null) {
            val rule = AtlasSafetyPolicy.verdictFor(
                name = denied.kind.token,
                path = denied.path,
            ) as? AtlasSafetyVerdict.Denied
            return AtlasAdapterAssessment.Denied(rule?.ruleId ?: "unknown")
        }

        val usable = nodes.any { node -> isUsable(node, access) }
        return if (usable) {
            AtlasAdapterAssessment.Applicable(id, "reviewed-touch-sysfs-node")
        } else {
            AtlasAdapterAssessment.NotApplicable(SKIP_NO_BOOLEAN_NODE)
        }
    }

    override fun plan(context: AtlasAdapterContext, request: AtlasControlRequest): AtlasAdapterPlan {
        val boost = request as? AtlasControlRequest.TouchBoost
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_WRONG_REQUEST))
        val access = context.touchAccess
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_NO_SEAM))

        val bindings = mutableListOf<AtlasRouteBinding>()
        val skipped = mutableListOf<Pair<String, String>>()
        val usedLabels = mutableSetOf<String>()

        context.touchNodes
            .filter { it.kind in BOOST_KINDS }
            .forEach { node ->
                val key = HardwareControlKey.touchBoost(node.path)
                val verdict = AtlasSafetyPolicy.verdictFor(name = node.kind.token, path = node.path)
                if (verdict is AtlasSafetyVerdict.Denied) {
                    skipped += key to "never-touch:${verdict.ruleId}"
                    return@forEach
                }
                if (!isUsable(node, access)) {
                    skipped += key to SKIP_NO_BOOLEAN_NODE
                    return@forEach
                }

                val baseline = access.read(node.path) ?: run {
                    skipped += key to SKIP_UNREADABLE_NODE
                    return@forEach
                }
                val routeId = uniqueRouteId(key, usedLabels)
                val desired = if (boost.enabled) node.onValue else node.offValue

                bindings += AtlasRouteBinding(
                    candidate = AtlasRouteCandidate(
                        id = routeId,
                        priority = bindings.size,
                        evidence = AtlasRouteEvidence(
                            providerId = PROVIDER_ID,
                            transport = transport,
                            target = target,
                            readable = true,
                            privilegeAvailable = true,
                            unitProven = true,
                            baselineReadable = true,
                            rollbackProven = true,
                            reviewed = true,
                            reason = "reviewed-touch-sysfs-node",
                        ),
                    ),
                    request = HardwareRepairRequest(
                        routeId = routeId,
                        key = key,
                        owner = boost.owner,
                        token = boost.token,
                        desired = desired,
                        apply = { value -> write(access, node.path, value) },
                        read = { access.read(node.path) },
                        restore = { value -> write(access, node.path, value) },
                        baseline = baseline,
                        verify = { wanted, actual ->
                            WriteVerification.compare(wanted, actual) == WriteVerification.Outcome.MATCHED
                        },
                        realized = { wanted, actual ->
                            WriteVerification.compare(wanted, actual) == WriteVerification.Outcome.MATCHED
                        },
                        stabilitySamples = STABILITY_SAMPLES,
                        stabilityIntervalMs = STABILITY_INTERVAL_MS,
                    ),
                )
            }

        return AtlasAdapterPlan(id, bindings, skipped)
    }

    /**
     * مسار تمثيلي للخريطة: يبني طلب «تعزيز مفعّل» **ولا ينفّذه**. القراءة الوحيدة فيه هي خط
     * الأساس، وهي مسموحة صراحةً في عقد [AtlasControlAdapter.probe].
     */
    override fun probe(context: AtlasAdapterContext): AtlasAdapterPlan =
        plan(
            context,
            AtlasControlRequest.TouchBoost(
                intent = AtlasControlIntent(
                    target = AtlasControlTarget.TOUCH,
                    goal = AtlasControlGoal.PERFORMANCE,
                    desired = "1",
                ),
                owner = ControlOwnership.Owner.MAX_AI,
                token = PROBE_TOKEN,
                enabled = true,
            ),
        )

    private fun isUsable(node: TouchControlBackend.Node, access: TouchControlBackend.Access): Boolean {
        val live = access.read(node.path) ?: return false
        if (!isBoolean(live)) return false
        return access.writable(node.path)
    }

    private fun isBoolean(raw: String): Boolean =
        raw.trim().toIntOrNull()?.let { it == 0 || it == 1 } == true

    private fun write(
        access: TouchControlBackend.Access,
        path: String,
        value: String,
    ): Boolean = runCatching { access.writeVerified(path, value) }
        .getOrDefault(WriteVerification.Outcome.WRITE_FAILED) == WriteVerification.Outcome.MATCHED

    /**
     * معرّف المسار يُشتق من المفتاح القياسي، ويُفصل بلاحقة قصيرة عند تصادم البتر (٦٣ حرفًا).
     * والتصادم ممكن لأن المسار الطويل يُبتر في `labelFor`؛ ولو بقي مكرّرًا لرفض المُنفِّذ
     * الدفعة كلها (`route ids must be unique`).
     */
    private fun uniqueRouteId(key: String, used: MutableSet<String>): String {
        var label = HardwareRepairExecutor.labelFor(key)
        if (used.add(label)) return label
        var suffix = 2
        while (!used.add(label)) {
            label = HardwareRepairExecutor.labelFor("$key#$suffix")
            suffix += 1
        }
        return label
    }

    private companion object {
        const val KEY_ANY: String = "touch-boost"
        const val PROVIDER_ID: String = "touch-sysfs-reviewed"
        const val PROBE_TOKEN: String = "atlas-map-probe"
        const val SKIP_WRONG_REQUEST: String = "wrong-request-shape"
        const val SKIP_NO_SEAM: String = "no-touch-write-seam"
        const val SKIP_NO_NODE: String = "no-touch-node-seen"
        const val SKIP_NO_BOOLEAN_NODE: String = "touch-node-not-boolean-writable"
        const val SKIP_UNREADABLE_NODE: String = "touch-node-unreadable"

        const val STABILITY_SAMPLES: Int = 3
        const val STABILITY_INTERVAL_MS: Long = 40L

        val BOOST_KINDS: Set<TouchControlBackend.Kind> = setOf(
            TouchControlBackend.Kind.GAME_MODE,
            TouchControlBackend.Kind.SAMPLE_RATE,
            TouchControlBackend.Kind.INPUT_BOOST,
        )
    }
}
