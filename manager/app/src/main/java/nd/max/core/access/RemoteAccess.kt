/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.BuildConfig
import nd.max.core.access.PromoWindow
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.core.license.DeviceIdentities
import nd.max.core.license.DeviceIdentity
import nd.max.core.license.LicenseAdmission
import nd.max.core.license.LicenseCallOutcome
import nd.max.core.license.LicenseCertificates
import nd.max.core.license.LicenseCiphers
import nd.max.core.license.LicenseClient
import nd.max.core.license.LicenseEnvelope
import nd.max.core.license.LicenseFault
import nd.max.core.license.LicenseFetchException
import nd.max.core.license.LicenseKey
import nd.max.core.license.LicenseProof
import nd.max.core.license.LicenseRecord
import nd.max.core.license.LicenseRejection
import nd.max.core.license.LicenseState
import nd.max.core.license.LicenseStates
import nd.max.core.license.LicenseStore
import nd.max.core.remote.ConfigBootGate
import nd.max.core.remote.ConfigClient
import nd.max.core.remote.ConfigFreshness
import nd.max.core.remote.ConfigOrigin
import nd.max.core.remote.ConfigStore
import nd.max.core.remote.FetchAttempt
import nd.max.core.remote.FetchOutcome
import nd.max.core.remote.RemoteConfig
import nd.max.core.remote.RemoteFetchException
import nd.max.core.remote.RemoteFlags
import nd.max.core.remote.StoredConfig
import java.io.File

/**
 * المستوى الواحد الذي يقرأ منه التطبيق: «ما حال الإعداد؟ وما حال المفتاح؟ وما يُفتح الآن؟».
 *
 * ## لماذا كائنٌ واحد لا حقنٌ في كل شاشة
 *
 * كل سطح يعمل اليوم بلا خادم وبلا مفتاح (سلوك الحزمة)، فالقيمة الوحيدة التي تحتاجها الشاشات هي
 * **حكمٌ جاهز** يُقرأ من حالة واحدة ([state]) ويُعاد رسمه عند تغيّرها. ولو فُتح لكل شاشة مسارها
 * الخاصّ (قراءة ملفّ هنا، ونداء شبكة هناك) لعددنا: عدد النتائج، وعدد الطلبات، وعدد الأماكن
 * التي يمكن أن تتباعد فيها نسخة قديمة من المنطق.
 *
 * ## ترتيب الإقلاع — وبلا شبكة في المسار
 *
 * ① قراءة الذاكرة (إعدادٌ موقّع مخزَّن + شهادة) → ② الحكم فورًا بـ`ConfigBootGate` (نقيّ، بلا
 * شبكة) → ③ تحديثٌ **خلفيّ** واحد للإعداد ثم للمفتاح. فلا شاشة تنتظر خادمًا، ولا مسار إقلاع
 * يلمس الشبكة — وهذا هو §٩ من مواصفة التحكّم حرفيًّا.
 *
 * ## ولماذا الشبكة هنا كاملة
 *
 * طلبان فقط في عمر التشغيل: `GET` واحد للإعداد (بوسم `ETag` فيصير ‏304 بلا جسم)، و`POST` واحد
 * لدالّة الترخيص — **ولا يُندى الاثنان إلّا عند الحاجة**: الإعداد إن وُجد مخزَّن طازج. والشهادة
 * تُجدَّد مرّة في كل تشغيل، فيلتقط الإلغاء بأسرع ما يمكن بلا استهلاك يُذكر.
 */
object RemoteAccess {

    /** ما تراه الشاشات: الإعداد المعتمد، من أيّ مصدر، وحال المفتاح، والنافذة السارية. */
    data class Snapshot(
        val config: RemoteConfig,
        val origin: ConfigOrigin,
        val freshness: ConfigFreshness,
        val revision: Long,
        val code: String?,
        val license: LicenseState,
        val promo: PromoWindow?,
    )

    /** دورة حياة التفعيل كما تراها الشاشة — ولا استثناءات تعبر إلى الواجهة. */
    sealed interface ActivationStatus {
        data object Idle : ActivationStatus
        data object Running : ActivationStatus
        data object Succeeded : ActivationStatus
        data class Failed(val code: String) : ActivationStatus
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(
        Snapshot(
            config = RemoteFlags.defaultConfig(),
            origin = ConfigOrigin.DEFAULT,
            freshness = ConfigFreshness.ABSENT,
            revision = 0,
            code = null,
            license = LicenseState(),
            promo = null,
        ),
    )

    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private val _activation = MutableStateFlow<ActivationStatus>(ActivationStatus.Idle)
    val activation: StateFlow<ActivationStatus> = _activation.asStateFlow()

    @Volatile private var appContext: Context? = null
    @Volatile private var configStore: ConfigStore? = null
    @Volatile private var licenseStore: LicenseStore? = null
    @Volatile private var device: DeviceIdentity? = null
    @Volatile private var record: LicenseRecord? = null
    @Volatile private var appVersionCode: Long = 0
    @Volatile private var refreshing = false
    @Volatile private var booted = false

    /** الإقلاع: قراءة الذاكرة والحكم فورًا، ثم تحديث خلفيّ واحد. ويُنادى مرّةً من التطبيق. */
    fun boot(context: Context) {
        if (booted) return
        val app = context.applicationContext
        appContext = app
        val directory = File(app.filesDir, DIRECTORY).apply { runCatching { mkdirs() } }

        val cipher = LicenseCiphers.primary()
        val storedConfig = ConfigStore(File(app.filesDir, ConfigStore.FILE_NAME))
        val storedLicense = LicenseStore(File(directory, LicenseStore.FILE_NAME), cipher)
        configStore = storedConfig
        licenseStore = storedLicense

        val identity = DeviceIdentities.loadOrCreate(File(directory, DeviceIdentities.FILE_NAME))
        device = identity
        record = storedLicense.read()
        readVersion(app)

        val now = System.currentTimeMillis()
        val decision = ConfigBootGate.decide(
            nowMillis = now,
            fetched = FetchAttempt.NotAttempted,
            stored = storedConfig.read(),
            publicKeyBase64 = BuildConfig.CONFIG_PUBLIC_KEY,
        )
        publish(decision.config, decision.origin, decision.freshness, decision.revision, decision.code)
        booted = true
        CommunityStats.attach(app)

        DiagnosticCenter.record(
            component = COMPONENT,
            message = "initialised cipher=${cipher.id} device_persisted=${identity.persisted} " +
                "config=${decision.origin.name.lowercase()} license=${if (record != null) "stored" else "none"}",
        )
        refresh("boot")
    }

    /**
     * تحديث خلفيّ واحد: الإعداد ثم المفتاح. ولا يُزحام: نداء أثناء نداء يُهمَل بهدوء
     * (المستخدم ضغط «تحقق الآن» مرتين لا يعني طلبين على الخادم المجّاني).
     */
    fun refresh(reason: String) {
        if (!booted || refreshing) return
        refreshing = true
        scope.launch {
            try {
                refreshConfig(reason)
                refreshLicense(reason)
                CommunityStats.attempt(reason)
            } finally {
                refreshing = false
            }
        }
    }

    /** حكم الوصول لسطح — يُنادى من حرّاس الشاشات، ويُحسب **الآن** لا عند آخر تحديث. */
    fun access(featureId: String): FeatureAccess {
        val snapshot = _state.value
        val now = System.currentTimeMillis()
        return FeatureAccessRules.access(
            config = snapshot.config,
            license = LicenseStates.evaluate(record, now),
            featureId = featureId,
            versionCode = appVersionCode.toInt(),
            nowMillis = now,
        )
    }

    /** حال المفتاح الآن (يعيد الحساب فلا يتجمّد عرضٌ قديم بعد تغيّر). */
    fun licenseNow(): LicenseState = LicenseStates.evaluate(record, System.currentTimeMillis())

    /**
     * عرضٌ مختصر لمعرّف هذا الجهاز (أوّل ٦ و… وآخر ٤) — يُعرض في شاشة الترخيص للدعم.
     * ولا يُعرض المعرّف كاملًا على شاشة: لقطة شاشة تُنشر في مجموعة لا يجب أن تحمل مفتاح ربطٍ.
     */
    fun devicePreview(): String = device?.id?.let(DeviceIdentities::preview) ?: ""

    /** تفعيل مفتاح كتبه المستخدم. يقع على IO، ويعود بحكمٍ جاهز للعرض. */
    suspend fun activate(rawKey: String): ActivationStatus = withContext(Dispatchers.IO) {
        if (appContext == null) return@withContext ActivationStatus.Failed(LicenseFault.NOT_CONFIGURED.code)
        val identity = device ?: return@withContext ActivationStatus.Failed(LicenseFault.NOT_CONFIGURED.code)
        if (_activation.value == ActivationStatus.Running) return@withContext _activation.value

        val canonical = LicenseKey.normalize(rawKey)
            ?: return@withContext finish(ActivationStatus.Failed(REJECT_INVALID_KEY))

        val client = licenseClient()
            ?: return@withContext finish(ActivationStatus.Failed(LicenseFault.NOT_CONFIGURED.code))
        val keyHash = LicenseKey.sha256Hex(canonical)
        val proof = proofFor("license.redeem", keyHash, identity)

        _activation.value = ActivationStatus.Running
        return@withContext try {
            when (
                val outcome = client.redeem(
                    keyHash = keyHash,
                    device = identity.id,
                    label = deviceLabel(),
                    appVersion = appVersionCode.toString(),
                    legacyDevice = proof.legacyDevice,
                    devicePublicKey = proof.publicKey,
                    deviceProof = proof.proof,
                    deviceProofAt = proof.atMillis,
                )
            ) {
                is LicenseCallOutcome.Denied -> finish(ActivationStatus.Failed(outcome.code))
                is LicenseCallOutcome.Granted -> {
                    val rejection = admitGranted(outcome.envelope)
                    if (rejection == null) {
                        finish(ActivationStatus.Succeeded)
                    } else {
                        DiagnosticCenter.record(component = COMPONENT, message = "activate rejected ${rejection.code}")
                        finish(ActivationStatus.Failed(rejection.code))
                    }
                }
            }
        } catch (fault: LicenseFetchException) {
            DiagnosticCenter.record(component = COMPONENT, message = "activate failed ${fault.fault.code}")
            finish(ActivationStatus.Failed(fault.fault.code))
        } catch (error: Exception) {
            DiagnosticCenter.record(component = COMPONENT, message = "activate error", throwable = error)
            finish(ActivationStatus.Failed(LicenseFault.NETWORK.code))
        }
    }

    // ── الداخل ────────────────────────────────────────────────────────────────────────────────

    private fun readVersion(context: Context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersionCode = info.longVersionCode
        }
    }

    private fun deviceLabel(): String = runCatching {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL?.trim().orEmpty()
        val android = Build.VERSION.RELEASE?.trim().orEmpty()
        listOf(manufacturer, model)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { "Android device" } +
            (if (android.isNotEmpty()) " · Android $android (SDK ${Build.VERSION.SDK_INT})" else "")
    }.getOrDefault("Android device").take(80)

    /**
     * حقول إثبات حيازة مفتاح الجهاز. غياب Keystore (أو فشل التوقيع) يُعيد المعرّف القديم
     * وحده — فيبقى السلوك السابق بدل أن يمنع التفعيل كليًّا.
     */
    private fun proofFor(action: String, subject: String, identity: DeviceIdentity): DeviceProofArgs {
        val legacy = identity.legacyId.orEmpty()
        val publicKey = identity.publicKeyBase64 ?: return DeviceProofArgs(legacy, "", "", 0L)
        val at = System.currentTimeMillis()
        val signature = identity.sign(LicenseProof.payload(action, identity.id, subject, at))
            ?: return DeviceProofArgs(legacy, "", "", 0L)
        return DeviceProofArgs(legacy, publicKey, signature, at)
    }

    private fun configClient(): ConfigClient? {
        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isEmpty()) return null
        return ConfigClient(baseUrl = url, apiKey = BuildConfig.SUPABASE_ANON_KEY.trim())
    }

    private fun licenseClient(): LicenseClient? {
        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isEmpty()) return null
        return LicenseClient(baseUrl = url, apiKey = BuildConfig.SUPABASE_ANON_KEY.trim())
    }

    private fun refreshConfig(reason: String) {
        val store = configStore ?: return
        val client = configClient() ?: return
        val before = store.read()
        val now = System.currentTimeMillis()
        try {
            when (val outcome = client.fetch(before?.etag)) {
                is FetchOutcome.Unchanged -> {
                    store.noteAttempt(now, "remote_unchanged")
                    DiagnosticCenter.record(component = COMPONENT, message = "config unchanged ($reason)")
                }
                is FetchOutcome.Changed -> {
                    val decision = ConfigBootGate.decide(
                        nowMillis = now,
                        fetched = FetchAttempt.Received(outcome.envelope, outcome.etag),
                        stored = before,
                        publicKeyBase64 = BuildConfig.CONFIG_PUBLIC_KEY,
                    )
                    if (decision.origin == ConfigOrigin.SERVER) {
                        store.write(
                            StoredConfig(
                                envelope = outcome.envelope,
                                storedAtMillis = now,
                                lastAttemptMillis = now,
                                lastOutcome = "remote_server",
                                etag = outcome.etag ?: "",
                            ),
                        )
                        DiagnosticCenter.record(
                            component = COMPONENT,
                            message = "config revision=${decision.revision} ($reason)",
                        )
                    } else {
                        store.noteAttempt(now, decision.code ?: "remote_rejected")
                        DiagnosticCenter.record(
                            component = COMPONENT,
                            message = "config rejected ${decision.code ?: "remote_rejected"} ($reason)",
                        )
                    }
                    publish(decision.config, decision.origin, decision.freshness, decision.revision, decision.code)
                }
            }
        } catch (fault: RemoteFetchException) {
            store.noteAttempt(now, fault.fault.code)
            publishCurrentCode(fault.fault.code)
            DiagnosticCenter.record(component = COMPONENT, message = "config fetch ${fault.fault.code} ($reason)")
        } catch (error: Exception) {
            // الشبكة لا تُسقط شيئًا: الإعداد المخزَّن يبقى الحكم.
            publishCurrentCode(LicenseFault.NETWORK.code)
            DiagnosticCenter.record(component = COMPONENT, message = "config fetch error ($reason)", throwable = error)
        }
    }

    private fun refreshLicense(reason: String) {
        val store = licenseStore ?: return
        val identity = device ?: return
        val current = record ?: return
        val client = licenseClient() ?: return
        val now = System.currentTimeMillis()
        val proof = proofFor("license.validate", current.licenseId, identity)
        try {
            when (
                val outcome = client.validate(
                    licenseId = current.licenseId,
                    device = identity.id,
                    appVersion = appVersionCode.toString(),
                    legacyDevice = proof.legacyDevice,
                    devicePublicKey = proof.publicKey,
                    deviceProof = proof.proof,
                    deviceProofAt = proof.atMillis,
                )
            ) {
                is LicenseCallOutcome.Granted -> {
                    val failure = admitGranted(outcome.envelope)
                    if (failure != null) {
                        store.noteAttempt(now, failure.code)
                        DiagnosticCenter.record(component = COMPONENT, message = "renew rejected ${failure.code} ($reason)")
                    } else {
                        DiagnosticCenter.record(component = COMPONENT, message = "renewed ($reason)")
                    }
                }
                is LicenseCallOutcome.Denied -> {
                    when (outcome.code) {
                        // حكم نهائيّ من الخادم: تُمسح الشهادة فيُقفل فورًا — ولا انتظار لانتهائها.
                        "revoked", "device_removed", "not_found" -> {
                            store.clear()
                            record = null
                            publishLicense(outcome.code)
                            DiagnosticCenter.record(component = COMPONENT, message = "license cleared ${outcome.code}")
                        }
                        else -> {
                            store.noteAttempt(now, outcome.code, outcome.serverMillis)
                            publishLicense(outcome.code)
                            DiagnosticCenter.record(component = COMPONENT, message = "renew denied ${outcome.code} ($reason)")
                        }
                    }
                }
            }
        } catch (fault: LicenseFetchException) {
            store.noteAttempt(now, fault.fault.code)
            DiagnosticCenter.record(component = COMPONENT, message = "renew fetch ${fault.fault.code} ($reason)")
        } catch (error: Exception) {
            DiagnosticCenter.record(component = COMPONENT, message = "renew error ($reason)", throwable = error)
        }
    }

    /**
     * قبول شهادة واردة من الشبكة: التحقّق الكامل ثم الحفظ. `null` = قُبلت.
     * وتُعيد سبب الرفض ليُسجَّل — ولا تُسقط شيئًا قائمًا (شهادة قديمة صالحة تبقى حتى ينقضي وقتها).
     */
    private fun admitGranted(envelope: LicenseEnvelope): LicenseRejection? {
        val identity = device ?: return LicenseRejection.NOT_CONFIGURED
        val now = System.currentTimeMillis()
        return when (
            val admitted = LicenseCertificates.admit(
                envelope = envelope,
                publicKeyBase64 = BuildConfig.CONFIG_PUBLIC_KEY,
                deviceId = identity.id,
                nowMillis = now,
            )
        ) {
            is LicenseAdmission.Rejected -> admitted.reason
            is LicenseAdmission.Accepted -> {
                val fresh = LicenseStates.recordOf(
                    payload = admitted.payload,
                    envelope = envelope,
                    storedAtMillis = now,
                    previous = record,
                )
                licenseStore?.write(fresh)
                record = fresh
                publishLicense(null)
                null
            }
        }
    }

    private fun finish(status: ActivationStatus): ActivationStatus {
        _activation.value = status
        return status
    }

    private fun publish(
        config: RemoteConfig,
        origin: ConfigOrigin,
        freshness: ConfigFreshness,
        revision: Long,
        code: String?,
    ) {
        val now = System.currentTimeMillis()
        _state.value = Snapshot(
            config = config,
            origin = origin,
            freshness = freshness,
            revision = revision,
            code = code,
            license = LicenseStates.evaluate(record, now),
            promo = config.activePromoAt(now),
        )
    }

    private fun publishLicense(code: String?) {
        val now = System.currentTimeMillis()
        val current = _state.value
        _state.value = current.copy(
            license = LicenseStates.evaluate(record, now),
            code = code ?: current.code,
            promo = current.config.activePromoAt(now),
        )
    }

    private fun publishCurrentCode(code: String) {
        val now = System.currentTimeMillis()
        val current = _state.value
        _state.value = current.copy(code = code, promo = current.config.activePromoAt(now))
    }

    /** مجلّد الذاكرة داخل `filesDir` — اسم واحد في موضع واحد. */
    private const val DIRECTORY = "license"

    /** رمز رفض محلي (لا من الخادم): المفتاح لا يجتاز محرفَي التجزيء أصلًا. */
    const val REJECT_INVALID_KEY = "license_key_invalid"

    private const val COMPONENT = "license"
}

/** حقول الإثبات التي يرسلها كل نداء ترخيص إلى `maxctl` (فارغة في بيئة بلا Keystore). */
private data class DeviceProofArgs(
    val legacyDevice: String,
    val publicKey: String,
    val proof: String,
    val atMillis: Long,
)
