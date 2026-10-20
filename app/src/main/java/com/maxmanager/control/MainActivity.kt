package com.maxmanager.control

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * لوحة Max Control — مركز تحكّم المالك، وإعادة تصميمها في هذه الجولة قائمة على أربعة تبويبات.
 *
 * ## ما تغيّر ولماذا (طلب المالك: «أعد تصميم ui و ux»)
 *
 * كان الملفّ الواحد يحوي الشاشة كلها في عمود واحد طويل: تُنشئ مفتاحًا، ثم تنزل مئات الأسطر
 * لتنشر تحديثًا، وتتحقّق من الإحصاءات في الطريق. والعيوب المقيسة التي قادت التقسيم:
 *
 * ① **كل الأقسام تحت `if (loaded)`**: لو تعذّر تحميل الإعداد اختفت المفاتيح والبيانات معه —
 *    يُقرأ عطب الإعداد كعطب في كل شيء. الآن لكل تبويب حاله، والبيانات تُقرأ من `admin_*`
 *    وحدها فلا تنتظر الإعداد أصلًا.
 * ② **البيانات لم تُجلب إلا مع الإعداد**: تبويب البيانات صار يجلب عند أول فتح، ويقول بأي
 *    لحظة قُرئ، ويشرح لماذا الفراغ — بدل أن يبدو معطّلًا.
 * ③ **النشر كان زرًّا أعمى**: الآن حوار تأكيد يسرد الفروق بالعربية (من `Draft.summarise`)،
 *    وحوار نجاح يقول رقم المراجعة والإصدار.
 * ④ **قسم التحديث كان مبعثرًا**: صار تبويبًا واحدًا فيه «المنشور حاليًا»، الرقم، الرابط،
 *    **معاينة النافذة كما سيراها المستخدم**، وسجلّ التغييرات — في مسار مترابط واحد.
 *
 * والقسمة ملفّيًّا: [Widgets] للمكوّنات المشتركة · [Draft] للمسودّة · [KeysTab] · [UpdateTab]
 * (في `PublishTab.kt`) · [CustomizeTab] · [DataTab]. وهذا الملفّ يبقى: النشاط، السمة، الدخول،
 * واللوح الجامع الذي يربطها.
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AppTheme {
                var session by remember { mutableStateOf<Session?>(null) }
                var notice by remember { mutableStateOf("") }
                val current = session
                if (current == null) {
                    LoginScreen(notice) { session = it; notice = "" }
                } else {
                    DashboardScreen(current) { reason ->
                        val s = current
                        Thread { runCatching { Api.signOut(s) } }.start()
                        notice = reason
                        session = null
                    }
                }
            }
        }
    }
}

// ───────────────────────────── theme ─────────────────────────────

private val Palette = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF00255C),
    primaryContainer = Color(0xFF1B3A73),
    onPrimaryContainer = Color(0xFFD9E4FF),
    background = Color(0xFF0C0F14),
    onBackground = Color(0xFFE4E7EE),
    surface = Color(0xFF0C0F14),
    onSurface = Color(0xFFE4E7EE),
    surfaceVariant = Color(0xFF181D26),
    onSurfaceVariant = Color(0xFFA9B0BF),
    outline = Color(0xFF3A4252),
    error = Color(0xFFFF8A80)
)

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette) {
        // The whole control center is Arabic, so it is always laid out right-to-left.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
        }
    }
}

// ───────────────────────────── login ─────────────────────────────

@Composable
private fun LoginScreen(notice: String, onLoggedIn: (Session) -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = LocalContext.current.getSharedPreferences("control", Context.MODE_PRIVATE)
    var email by remember { mutableStateOf(prefs.getString("email", "").orEmpty()) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val configProblem = remember { Config.problem() }

    fun submit() {
        if (busy || configProblem != null) return
        if (email.isBlank() || password.isEmpty()) {
            error = "أدخل البريد الإلكتروني وكلمة المرور"
            return
        }
        busy = true
        error = ""
        scope.launch {
            try {
                val s = withContext(Dispatchers.IO) { Api.signIn(email.trim(), password) }
                prefs.edit().putString("email", email.trim()).apply()
                onLoggedIn(s)
            } catch (e: ApiException) {
                error = e.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("M", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(16.dp))
        Text("Max Manager", style = MaterialTheme.typography.headlineSmall)
        Text("مركز التحكم الخاص بالمالك", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        if (configProblem != null) Banner(configProblem, Tone.Error)
        else if (notice.isNotEmpty()) Banner(notice, Tone.Info)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني") },
            singleLine = true,
            textStyle = ltr(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            singleLine = true,
            textStyle = ltr(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "إخفاء" else "إظهار") }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Banner(error, Tone.Error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { submit() },
            enabled = !busy && configProblem == null,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(10.dp))
                Text("جارٍ الدخول…")
            } else {
                Text("دخول")
            }
        }
    }
}

// ───────────────────────────── dashboard (four tabs) ─────────────────────────────

/** التبويبات الأربعة — ترتيبها ترتيب العمل اليومي: مفتاح، تحديث، تخصيص، ثم أرقام. */
private enum class Tab(val label: String) {
    Keys("المفاتيح"),
    Update("التحديث"),
    Customize("التخصيص"),
    Data("البيانات"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(session: Session, onLogout: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val draft = remember { Draft() }
    val keys = remember { KeysState() }
    var tab by remember { mutableStateOf(Tab.Keys) }

    var stats by remember { mutableStateOf<List<CountryStat>>(emptyList()) }
    var statsError by remember { mutableStateOf("") }
    var compat by remember { mutableStateOf<List<CompatStat>>(emptyList()) }
    var compatError by remember { mutableStateOf("") }
    var health by remember { mutableStateOf<List<HealthStat>>(emptyList()) }
    var healthError by remember { mutableStateOf("") }
    var audit by remember { mutableStateOf<List<AuditRow>>(emptyList()) }
    var auditError by remember { mutableStateOf("") }
    var dataLoadedAt by remember { mutableStateOf("") }
    var dataLoaded by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf(Status("جارٍ التحميل…", Tone.Info)) }
    var busy by remember { mutableStateOf(false) }
    var configLoaded by remember { mutableStateOf(false) }
    var confirmPublish by remember { mutableStateOf(false) }
    var publishedResult by remember { mutableStateOf<Pair<Long, String>?>(null) }

    fun fail(prefix: String, e: ApiException) {
        if (e.authExpired) onLogout(e.message.orEmpty()) else status = Status(prefix + e.message, Tone.Error)
    }

    // أربع قراءات مستقلّة: عطبٌ في إحداها لا يُسقط الثلاث. والفراغ يبقى فارغًا معلَنًا لا
    // يُغطّى بصفرٍ كاذب — الصفر يعني «قِيس فكان صفرًا»، وهذا ليس ما يحدث هنا.
    suspend fun loadData() {
        try {
            stats = withContext(Dispatchers.IO) { Api.countryStats(session) }
            statsError = ""
        } catch (e: ApiException) {
            if (e.authExpired) { onLogout(e.message.orEmpty()); return }
            statsError = e.message.orEmpty()
        }
        try {
            compat = withContext(Dispatchers.IO) { Api.compatStats(session) }
            compatError = ""
        } catch (e: ApiException) {
            if (e.authExpired) { onLogout(e.message.orEmpty()); return }
            compatError = e.message.orEmpty()
        }
        try {
            health = withContext(Dispatchers.IO) { Api.healthStats(session) }
            healthError = ""
        } catch (e: ApiException) {
            if (e.authExpired) { onLogout(e.message.orEmpty()); return }
            healthError = e.message.orEmpty()
        }
        try {
            audit = withContext(Dispatchers.IO) { Api.auditPage(session) }
            auditError = ""
        } catch (e: ApiException) {
            if (e.authExpired) { onLogout(e.message.orEmpty()); return }
            auditError = e.message.orEmpty()
        }
        dataLoadedAt = shortStamp(Instant.now().toString())
        dataLoaded = true
    }

    fun refreshAll() {
        scope.launch {
            busy = true
            status = Status("جارٍ التحميل…", Tone.Info)
            try {
                draft.load(withContext(Dispatchers.IO) { Api.loadConfig() })
                configLoaded = true
                status = Status("تم تحميل الإعدادات والإحصاءات", Tone.Ok)
            } catch (e: ApiException) {
                fail("تعذّر تحميل الإعدادات: ", e)
            }
            loadData()
            busy = false
        }
    }

    fun refreshData() {
        scope.launch {
            busy = true
            loadData()
            busy = false
        }
    }

    fun publish() {
        val problem = draft.validate()
        if (problem != null) {
            status = Status(problem, Tone.Error)
            return
        }
        val body = draft.build()
        val version = draft.version.trim()
        scope.launch {
            busy = true
            status = Status("جارٍ النشر…", Tone.Info)
            val revision = try {
                withContext(Dispatchers.IO) { Api.publishConfig(session, body) }.optLong("revision")
            } catch (e: ApiException) {
                fail("فشل النشر: ", e)
                busy = false
                return@launch
            }
            try {
                // إعادة القراءة بعد النشر: الشاشة تعرض ما وقّعه الخادم لا ما أرسلته.
                draft.load(withContext(Dispatchers.IO) { Api.loadConfig() })
                status = Status("تم النشر موقّعًا — مراجعة $revision · v$version", Tone.Ok)
            } catch (e: ApiException) {
                status = Status("نُشر التحديث، لكن تعذّرت إعادة القراءة: ${e.message}", Tone.Info)
            }
            publishedResult = revision to version
            busy = false
        }
    }

    /**
     * إضافة إصدار إلى السجل — ويصير **هو** المنشور إن كان أحدث من الحالي.
     *
     * هذا الرابط كان مفقودًا: «أضف إلى السجل» يكتب ملاحظات، ورقم الإصدار في بطاقة أخرى
     * يُكتب يدويًّا — فيسهل أن يُضاف سجلٌّ لـ2.3 ويُنشر 2.2. فربطُهما هنا يجعل حقلًا واحدًا
     * يكفي حين يكون السجل هو مصدر الإصدار الأحدث.
     */
    fun addRelease(version: String, date: String, notesAr: List<String>, notesEn: List<String>): String? {
        val v = version.trim()
        if (!Validators.isVersion(v)) return "رقم الإصدار يجب أن يكون مثل 2.3"
        if (!Validators.isDate(date.trim())) return "التاريخ يجب أن يكون بصيغة 2026-10-14"
        if (notesAr.isEmpty() && notesEn.isEmpty()) return "أضف سطرًا واحدًا على الأقل في الملاحظات"
        if (draft.changelog.any { it.optString("version") == v }) return "هذا الإصدار موجود في السجل"
        val entry = JSONObject()
            .put("version", v)
            .put("date", date.trim())
            .put("notes_ar", JSONArray(notesAr))
            .put("notes_en", JSONArray(notesEn))
        draft.changelog.add(0, entry)
        if (Validators.compareVersions(v, draft.version.trim()) > 0) {
            draft.version = v
            draft.released = date.trim()
        }
        return null
    }

    LaunchedEffect(Unit) { refreshAll() }

    // تبويب البيانات يجلب عند أول فتح له — لا ينتظر زرًّا ولا يعتمد على نجاح تحميل الإعداد.
    LaunchedEffect(tab) {
        if (tab == Tab.Data && !dataLoaded) refreshData()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("مركز التحكم") },
                actions = {
                    TextButton(onClick = { refreshAll() }, enabled = !busy) { Text("تحديث") }
                    TextButton(onClick = { onLogout("خرجت من الجلسة") }) { Text("خروج") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            // شريط النشر يظهر فقط بعد قراءة الإعداد: النشر بلا حالة حقيقية قد يمحو ما لا نراه.
            if (configLoaded) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { confirmPublish = true },
                            enabled = draft.dirty && !busy,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(if (draft.dirty) "نشر التغييرات" else "لا توجد تغييرات")
                        }
                        if (draft.dirty) {
                            OutlinedButton(onClick = { draft.discard() }, enabled = !busy, modifier = Modifier.height(48.dp)) {
                                Text("تراجع")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).imePadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Banner(status.text, status.tone, busy)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tab.values().forEach { t ->
                    FilterChip(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.Keys -> KeysTab(session, keys, onLogout)
                    Tab.Update -> UpdateTab(
                        d = draft,
                        loaded = configLoaded,
                        busy = busy,
                        onPublish = { confirmPublish = true },
                        onAddRelease = { v, date, ar, en -> addRelease(v, date, ar, en) },
                    )
                    Tab.Customize -> CustomizeTab(draft, configLoaded)
                    Tab.Data -> DataTab(
                        stats = stats, statsError = statsError,
                        compat = compat, compatError = compatError,
                        health = health, healthError = healthError,
                        audit = audit, auditError = auditError,
                        lastLoadedAt = dataLoadedAt,
                        busy = busy,
                        onRefresh = { refreshData() },
                    )
                }
            }
        }
    }

    if (confirmPublish) {
        val changes = draft.summarise()
        AlertDialog(
            onDismissRequest = { confirmPublish = false },
            title = { Text("نشر التغييرات؟") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("ستُطبَّق فورًا على كل مستخدمي MaxManager عند فتحهم التطبيق أو فحصهم التحديثات.")
                    Spacer(Modifier.height(8.dp))
                    if (changes.isEmpty()) {
                        Text(
                            "لا فرق ملموس عن المنشور — النشر يرفع المراجعة فقط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        changes.forEach { line -> Text("• $line", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { confirmPublish = false; publish() }) { Text("نشر الآن") } },
            dismissButton = { TextButton(onClick = { confirmPublish = false }) { Text("إلغاء") } }
        )
    }

    publishedResult?.let { (revision, version) ->
        AlertDialog(
            onDismissRequest = { publishedResult = null },
            title = { Text("تم النشر") },
            text = {
                Column {
                    Text("مراجعة $revision · الإصدار v$version")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "يصل التنبيه لمستخدمي MaxManager عند فتح التطبيق أو فحص التحديثات من الإعدادات، " +
                            "وزرّ النافذة يفتح الرابط الذي كتبته في المتصفّح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { publishedResult = null }) { Text("تم") } }
        )
    }
}
