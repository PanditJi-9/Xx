/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import nd.max.core.privilege.ExecutionModeRules
import nd.max.core.privilege.ExecutionModeStore

/**
 * القناة الوحيدة إلى [RootNodeService].
 *
 * **عقد الاستعمال:** [service] قد يكون `null` في أي لحظة، وهذا **ليس عطبًا**: الربط غير
 * متزامن، ويبدأ من `MainActivity` وينتهي بعدها. فكل مستدعٍ يعتبر `null` = «لا قناة الآن»
 * ويسلك بالطريق الاحتياطي الذي يعرفه (`RootFileAccess` يمرّ إلى الملف ثم الصدفة) — وهذا
 * الترتيب هو ما كان يعمل قبل وجود الخدمة، فيبقى شبكة أمان لا مسارًا ميتًا.
 *
 * ولا تنتظار هنا: لا `CountDownLatch` ولا مهلة. الانتظار في مسار واجهة يعني تجميد إطار
 * لعملية قد لا تُربط أبدًا (جهاز بلا جذر، أو صلاحية مرفوضة) — والبديل الاحتياطي أسرع.
 */
object RootNodeChannel {

    private const val TAG = "MaxRootNode"

    /**
     * الخدمة المربوطة أو `null`، تُقرأ من خيوط متعددة (مسح الحرارة يعمل خارج الخيط الرئيسي).
     * `@Volatile` لأن الكتابة تحدث في خيط الربط والقراءة في خيوط العمل.
     */
    @Volatile
    private var connectedService: IRootNodeService? = null

    val service: IRootNodeService?
        get() = connectedService.takeIf { rootAllowed() }

    private fun rootAllowed() = ExecutionModeRules.mayUseRoot(ExecutionModeStore.current())

    // الحارس يبقى مع مرجع الخدمة المحتفَظ به؛ تغيير الوضع لا يُتجاوز بنسخة قديمة منه.
    private fun guarded(remote: IRootNodeService): IRootNodeService = object : IRootNodeService.Stub() {
        override fun readText(path: String): String = if (rootAllowed()) remote.readText(path) else ""
        override fun readTexts(paths: MutableList<String>): MutableList<String> =
            if (rootAllowed()) remote.readTexts(paths) else paths.mapTo(mutableListOf()) { "" }
        override fun exists(path: String): Boolean = rootAllowed() && remote.exists(path)
        override fun writeText(path: String, value: String): Boolean = rootAllowed() && remote.writeText(path, value)
        override fun listNames(path: String, directoriesOnly: Boolean): MutableList<String> =
            if (rootAllowed()) remote.listNames(path, directoriesOnly) else mutableListOf()
    }

    @Volatile
    private var binding = false

    /** هل القناة مربوطة الآن؟ قراءة حالة فقط، لا تُستدعى منها أي خدمة. */
    val isBound: Boolean
        get() = service != null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            binding = false
            if (owners.isEmpty() || !rootAllowed()) {
                runCatching { RootService.unbind(this) }
                connectedService = null
                return
            }
            connectedService = guarded(IRootNodeService.Stub.asInterface(binder))
            log("connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binding = false
            connectedService = null
            log("disconnected")
        }
    }

    /**
     * أصحاب القناة الحاليّون (النشاط، لوحة الألعاب…). **لا يُفكّ الربط إلا حين يغادر آخرُهم** —
     * وإلا أسقط إغلاقُ النشاط القناةَ من تحت لوحة الألعاب وهي تقرأ ترددات اللعبة، فتعود كل
     * قراءة إلى الصدفة وتُظهر العدّادات «—».
     */
    private val owners = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    const val OWNER_ACTIVITY = "activity"
    const val OWNER_GAME_PANEL = "game-panel"

    /** يبدأ الربط مرة واحدة؛ النداء المتكرّر أثناء الربط لا يفتح طلبًا ثانيًا. */
    fun connect(context: Context, owner: String = OWNER_ACTIVITY) {
        if (!rootAllowed()) return
        owners.add(owner)
        if (connectedService != null || binding) return
        binding = true
        val intent = Intent(context.applicationContext, RootNodeService::class.java)
        runCatching { RootService.bind(intent, connection) }.onFailure {
            binding = false
            log("bind failed: ${it.javaClass.simpleName}")
        }
    }

    /** يُسقط صاحبًا؛ ولا ينهي الربط ما دام لأحدٍ غيره حاجة إليه. النداء بلا ربط قائم لا يفعل شيئًا. */
    fun disconnect(owner: String = OWNER_ACTIVITY) {
        owners.remove(owner)
        if (owners.isNotEmpty()) return
        if (connectedService == null && !binding) return
        runCatching { RootService.unbind(connection) }
        binding = false
        connectedService = null
    }

    private fun log(message: String) {
        android.util.Log.d(TAG, message)
    }
}
