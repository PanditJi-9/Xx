/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.spoof

import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.SharedHardwareOwnershipStore
import nd.max.core.privilege.PrivilegeManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** One file per transaction, no boot script execution and no claim of cross-engine atomicity. */
@Singleton
class SpoofConfigTransaction @Inject constructor(
    private val arbiter: HardwareControlArbiter,
    private val recovery: SpoofRecoveryStore,
) {
    private val transaction = SpoofFileTransaction(
        io = object : SpoofFileTransaction.Io {
            override fun eligibility(engine: String, requestRoot: Boolean) = this@SpoofConfigTransaction.eligibility(engine, requestRoot)
            override fun read(engine: String) = RootFileAccess.read(path(engine)) ?: if (!RootFileAccess.exists(path(engine))) "{}" else null
            override fun write(engine: String, text: String): Boolean {
                val file = path(engine)
                val parent = java.io.File(file).parent
                if (!parent.isNullOrBlank()) RootFileAccess.exec("mkdir -p $parent")
                // COPG's own WebUI rewrites COPG.json in place; the per-app engine gets the same write.
                val wrote = if (engine == SpoofCopgContract.MODULE_ID) RootFileAccess.writeTextInPlace(file, text)
                    else RootFileAccess.atomicWriteText(file, text)
                if (engine == SpoofCopgContract.MODULE_ID && wrote) {
                    syncCopgList(text)
                }
                return wrote && RootFileAccess.exec("chmod 644 $file") == 0 &&
                    RootFileAccess.exec("chcon u:object_r:system_file:s0 $file 2>/dev/null") == 0
            }
        },
        journal = object : SpoofFileTransaction.Journal {
            override fun record(engine: String) = recovery.record(engine)
            override fun put(record: SpoofRecoveryRecord) = recovery.put(record)
        },
        control = object : SpoofFileTransaction.Control {
            override fun release(engine: String) {
                arbiter.release(HardwareControlKey.spoofEngine(engine), "identity-file-$engine", restore = false)
            }
            override fun submit(engine: String, desired: String, apply: () -> Boolean, read: () -> String?,
                restore: () -> Boolean, verify: (String, String?) -> Boolean): SpoofFileTransaction.Result {
                val result = arbiter.submit(HardwareControlKey.spoofEngine(engine), ControlOwnership.Owner.GLOBAL_PROFILE,
                    "identity-file-$engine", desired, apply = { apply() }, read = read, restore = { restore() },
                    realized = { _, _ -> false }, verify = verify)
                return SpoofFileTransaction.Result(result.applied, result.verified, result.blocked, result.actual,
                    result.rollbackAttempted, result.rollbackVerified)
            }
        },
    )

    private fun path(engine: String): String = when (engine) {
        SpoofCopgContract.MODULE_ID -> SpoofCopgContract.CONFIG_PATH
        SpoofGlobalContract.MODULE_ID -> SpoofGlobalContract.CONFIG_PATH
        else -> error("unknown-engine")
    }

    /**
     * تهيئة المحرّك: فحص الصلاحيات والعلامات فقط، دون الاعتماد على module.prop.
     */
    fun eligibility(engine: String, requestRoot: Boolean = true): SpoofEngineReason? {
        path(engine) // reject any non-allowlisted target before root access
        if (!SharedHardwareOwnershipStore.isConfigured()) return SpoofEngineReason.STORE_UNCONFIGURED
        if (!PrivilegeManager.cachedRootGranted() && (!requestRoot || !PrivilegeManager.requestRoot())) return SpoofEngineReason.ROOT_REQUIRED
        val staged = stagedDir(engine)
        if (RootFileAccess.exists(staged)) return SpoofEngineReason.ENGINE_UPDATE_PENDING
        val dir = moduleDir(engine)
        if (RootFileAccess.exists(dir)) {
            val markers = SpoofEngineGate.markers.filterTo(mutableSetOf()) { RootFileAccess.exists("$dir/$it") }
            val markerReason = SpoofEngineGate.markerReason(markers)
            if (markerReason != null) return markerReason
        }
        return null
    }

    /**
     * قياس التشخيص — **قراءة فقط**، تُستدعى عند عرض الحالة لا عند كل كتابة.
     *
     * وحدّها: `module.prop` يُقرأ مرّة أخرى بعد [eligibility]، فالتكلفة قراءتان إضافيتان في حالة
     * الفشل وحدهما. وهذا مقصود: السطر يجب أن يحمل ما قيس لا ما استُنتج من الرمز.
     */
    internal fun probe(engine: String, reason: SpoofEngineReason): EngineProbe {
        path(engine)
        val dir = moduleDir(engine)
        val staged = stagedDir(engine)
        val moduleProp = RootFileAccess.read("$dir/module.prop")
        val stagedProp = RootFileAccess.read("$staged/module.prop")
        return EngineProbe(
            engine = engine,
            reason = reason,
            rootGranted = PrivilegeManager.cachedRootGranted(),
            moduleDirPresent = RootFileAccess.exists(dir),
            modulePropReadable = moduleProp != null,
            moduleIdValue = SpoofEngineGate.idValue(moduleProp),
            stagedDirPresent = RootFileAccess.exists(staged),
            stagedPropReadable = stagedProp != null,
            // «لم يُقرأ» ليس «مطابق»: `identityReason(null, …)` تعيد سبب الغياب، فتُقلب إلى
            // `true` لو كُتبت مباشرة — والسطر التشخيصيّ كان يقول `staged_id=1` عن ملفّ لم يُقرأ.
            stagedIdMatches = stagedProp != null && SpoofEngineGate.identityReason(stagedProp, engine) == null,
        )
    }

    private fun moduleDir(engine: String): String = "/data/adb/modules/$engine"
    private fun stagedDir(engine: String): String = "/data/adb/modules_update/$engine"

    fun apply(engine: String, original: String, target: String): SpoofEngineWrite = transaction.apply(engine, original, target)
    fun restore(engine: String): SpoofEngineWrite = transaction.restore(engine)

    private fun syncCopgList(configJson: String) {
        runCatching {
            val listPath = "${SpoofCopgContract.MODULE_DIR}/list.json"
            val existingText = RootFileAccess.read(listPath)
            val existing = if (existingText.isNullOrBlank()) mutableMapOf<String, String>()
            else {
                val elem = runCatching { Json.parseToJsonElement(existingText) as? JsonObject }.getOrNull()
                elem?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content.orEmpty() }?.toMutableMap() ?: mutableMapOf()
            }
            val configElem = runCatching { Json.parseToJsonElement(configJson) as? JsonObject }.getOrNull() ?: return
            configElem.forEach { (key, value) ->
                if (key.startsWith("PACKAGES_") && !key.endsWith(SpoofCopgContract.DEVICE_SUFFIX) && value is JsonArray) {
                    value.forEach { item ->
                        val raw = (item as? JsonPrimitive)?.content.orEmpty()
                        val pkg = raw.substringBefore(':').trim()
                        if (pkg.isNotEmpty() && !existing.containsKey(pkg)) {
                            existing[pkg] = pkg
                        }
                    }
                }
            }
            val updatedJson = buildJsonObject {
                existing.toSortedMap().forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            }.toString()
            RootFileAccess.writeTextInPlace(listPath, updatedJson)
            RootFileAccess.exec("chmod 644 $listPath")
            RootFileAccess.exec("chcon u:object_r:system_file:s0 $listPath 2>/dev/null")
        }
    }
}
