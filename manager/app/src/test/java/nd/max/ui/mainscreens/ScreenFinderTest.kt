/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.R
import nd.max.ui.navigation.MaxDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * مُوجِّد الشاشات — يقاس على JVM، لأن عطبه **صامت**: بحثٌ لا يجد الشاشة لا يرمي استثناءً ولا
 * يكتب سطرًا في السجلّ، يظهر فقط على هاتف من كتب اسمًا صحيحًا في لغته.
 *
 * وثلاثة عقود تُقفل هنا:
 *
 * 1. **الفهرس هو السجلّ المرئيّ**: `MaxDestination.Visible` (كل السجلّ إلا ما أخفاه المالك)
 *    ناقصًا **الأبواب ذات الابن الواحد** (صفحتها سطرٌ واحد يقود إلى الابن، والابن نفسه في
 *    الفهرس). وما يحتاج معاملًا لا يُسقَط إن كان له أبٌ ظاهر: يبقى للبحث، ويقول «اختر التطبيق
 *    أولًا»، وضغطه يقود إلى الأب. فلا وجهة جديدة تُنسى، ولا وجهة محذوفة تبقى، ولا ضغطة تفتح
 *    تفصيل حزمة اسمها فارغ.
 * 2. **والترتيب موقعٌ لا وجود**: اسمُ الشاشة قبل سطر وصفها، وسطرُ الوصف قبل اسم مجالها —
 *    وإلا تصدّرت نتيجةٌ هامشية الشاشةَ التي اسمها هو نفسه ما كتبه المستخدم.
 * 3. **و«أين تسكن» من الشجرة**: أبُ الوجهة في السجلّ، بلا جدول ثانٍ ينحرف.
 * 4. **وما يفتحه المستخدم يُقرأ من سجلّ واحد** (الجولة ٢٠٣): العدد يأتي من `ScreenUsageStore`
 *    بالمسار، ويرتّب النتائج المتعادلة، ويُقترح في الحقل الفارغ — ووسم الخطورة من `maxRiskLabel`
 *    نفسه الذي يوسم به الإعداد ‹أدوات متقدّمة›، لا جدول ثانٍ للوسم.
 */
class ScreenFinderTest {

    private lateinit var sourceRoot: File

    @Before
    fun locateSourceRoot() {
        val found = listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max"),
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", found != null)
        sourceRoot = found!!
    }

    private fun read(path: String): String = File(sourceRoot, path).readText()

    private fun target(
        destination: MaxDestination,
        title: String,
        role: String = "",
        where: String = "",
        usage: Int = 0,
        riskNote: String? = null,
    ) = ScreenFinderTarget(destination, title, role, where, usage, riskNote)

    /**
     * **ما يجب أن يعرضه الفهرس بالحساب، لا بالعدد المكتوب.**
     *
     * الفهرس = كلّ مرئيّ، **إلّا الأبواب ذات الابن الواحد**: صفحتها سطرٌ واحد يقود إلى الابن،
     * فعرضها يعطي نقرةً مقابل صفر معلومة. والشرط يُنسخ من الإنتاج حرفًا (بما فيه «الابن يحتاج
     * معاملًا ⇒ لا إسقاط»)، فلا يفترق الحارس عن الدالّة التي يحرسها.
     *
     * **وما يحتاج معاملًا يبقى إن كان له أبٌ ظاهر يُفتح بلا معامل** — يُعرض للبحث وحده، وفعله
     * يقود إلى الأب. وما لا أب له (أو أبوه يحتاج معاملًا) يُسقَط: لا مسار صادقًا إليه.
     */
    private fun expectedFinderSize(): Int {
        val visible = MaxDestination.Visible
        val passThrough = visible.filter { it.parent != null }
            .groupBy { it.parent!! }
            .filterValues { kids -> kids.size == 1 && !kids.single().needsLaunchArgument }
            .keys
        return visible.count { destination ->
            destination !in passThrough &&
                (!destination.needsLaunchArgument ||
                    destination.parent?.let { it in visible && !it.needsLaunchArgument } == true)
        }
    }

    // ── (١) الفهرس من السجلّ ──────────────────────────────────────────────────

    @Test
    fun `the index is the registry itself, minus what cannot open bare`() {
        val targets = screenFinderTargets({ "«$it»" })

        assertTrue("فهرس فارغ — الفحص نفسه معطوب", targets.size >= 40)
        // **وما يحتاج معاملًا يبقى، لكن بمسار صادق:** إمّا بلا معامل، أو بأبٍ ظاهر يُفتح.
        assertTrue(
            "وجهة بمعرّف في مسارها بلا أبٍ ظاهر: عرضها وعدٌ بضغطة تفتح شيئًا آخر",
            targets.all { target ->
                !target.destination.needsLaunchArgument ||
                    target.opensFrom?.let { !it.needsLaunchArgument } == true
            },
        )
        assertEquals(
            "الفهرس ليس السجلّ المرئيّ (وجهة نُسيت أو زيدت بيد)",
            expectedFinderSize(),
            targets.size,
        )
        assertTrue(
            "ووجهة لا معرّف لها تُعرض فعلًا (الفحص ليس سالبًا فقط)",
            targets.any { it.destination == MaxDestination.ZramManager },
        )
    }

    @Test
    fun `a screen that needs an app is still findable, and it says where to start`() {
        val targets = screenFinderTargets(resolve = { resource ->
            if (resource == R.string.max_title_app_settings) "App settings" else "«$resource»"
        })

        val settings = targets.single { it.destination == MaxDestination.AppSettings }
        assertEquals(
            "إعدادات التطبيق تُفتح من قائمة التطبيقات، فالسطر يقود إلى هناك",
            MaxDestination.Apps,
            settings.opensFrom,
        )
        assertEquals(MaxDestination.Apps, screenFinderOpenTarget(settings))

        // والبحث بعنوانها المحلول يجدها. لا نفرض أنها النتيجة الوحيدة: قد تطابق العبارة
        // وصف شاشة أخرى، وعقد المُوجِّد هو إمكان الوصول لا احتكار عبارة البحث.
        assertTrue(
            "إعدادات التطبيق موجودة في نتائج البحث بعنوانها",
            screenFinderResults("app settings", targets)
                .any { it.destination == MaxDestination.AppSettings },
        )

        // ووجهة عادية لا تُعاد توجيهًا: فعلها هو وجهتها.
        val zram = targets.single { it.destination == MaxDestination.ZramManager }
        assertNull(zram.opensFrom)
        assertEquals(MaxDestination.ZramManager, screenFinderOpenTarget(zram))
    }

    @Test
    fun `a screen that needs an app is never suggested as if it opened itself`() {
        val targets = screenFinderTargets({ "«$it»" })
        val withUsage = targets.map {
            if (it.destination == MaxDestination.AppSettings) it.copy(usage = 99) else it
        }
        assertFalse(
            "اقتراحٌ يفتح قائمة التطبيقات بدلًا من الشاشة يُقرأ عطبًا لا اقتراحًا",
            screenFinderSuggestions(withUsage).any { it.opensFrom != null },
        )
    }

    @Test
    fun `a screen the owner hid does not come back through search`() {
        val targets = screenFinderTargets({ "«$it»" })
        assertFalse(
            "سطح أخفاه المالك من الإعدادات لا يعود من باب فهرس الشاشات",
            targets.any { it.destination.hidden },
        )
        // والفحص ليس سالبًا فقط: غير المخفيّ يظهر فعلًا.
        assertTrue(
            "الفهرس لا يعرض شيئًا — الفحص سالب بلا موجب",
            targets.any { it.destination == MaxDestination.ZramManager },
        )
    }

    @Test
    fun `where a screen lives comes from the tree, not from a second table`() {
        assertEquals(
            MaxDestination.MemoryHub.titleRes,
            screenFinderWhereRes(MaxDestination.ZramManager),
        )
        assertEquals(
            MaxDestination.Settings.titleRes,
            screenFinderWhereRes(MaxDestination.ConfigBackup),
        )
        assertEquals(
            R.string.screen_finder_where_bar,
            screenFinderWhereRes(MaxDestination.Control),
        )
        assertEquals(
            R.string.max_nav_now,
            screenFinderWhereRes(MaxDestination.DeviceInfo),
        )
    }

    // ── (٢) الترتيب: الاسم قبل الوصف قبل المجال ───────────────────────────────

    @Test
    fun `the name of a screen outranks a word in its description, which outranks its domain`() {
        val byWhere = target(MaxDestination.StorageHub, "التخزين", role = "المساحة", where = "إدارة ZRAM")
        val byRole = target(MaxDestination.StorageDetail, "تفاصيل التخزين", role = "إدارة ZRAM ومساحتها")
        val byTitle = target(MaxDestination.ZramManager, "إدارة ZRAM", role = "ضبط المبادلة")

        // والمدخل مُرتَّب بالعكس عن قصد: النتيجة لا تتبع ترتيب الإدخال.
        val ordered = screenFinderResults("ZRAM", listOf(byWhere, byRole, byTitle))

        assertEquals(
            listOf(
                MaxDestination.ZramManager,
                MaxDestination.StorageDetail,
                MaxDestination.StorageHub,
            ),
            ordered.map { it.destination },
        )
    }

    @Test
    fun `the query is folded before it is compared with the name`() {
        val targets = listOf(
            target(MaxDestination.ThermalDetail, "الحرارة", role = "الحدود والحرارة"),
        )
        assertEquals(1, screenFinderResults("الحراره", targets).size)
        assertEquals(1, screenFinderResults("الحرارة", targets).size)
        assertEquals(1, screenFinderResults("  الحرارة  ", targets).size)
    }

    @Test
    fun `the count of matches is not the count of rows, and the difference is what is hidden`() {
        // ثلاثون وجهة تُطابق العبارة نفسها، والسقف أربع وعشرون: الفرق ستٌّ تُعلَن لا تُخفى.
        val many = (1..30).map { target(MaxDestination.ZramManager, "إدارة الذاكرة $it") }
        val shown = screenFinderResults("الذاكرة", many)
        assertEquals(24, shown.size)
        assertEquals(30, screenFinderMatchCount("الذاكرة", many))
        assertEquals(30 - shown.size, screenFinderMatchCount("الذاكرة", many) - shown.size)
    }

    @Test
    fun `the count is never smaller than what the sheet shows`() {
        val targets = screenFinderTargets({ "«$it»" })
        // الخاصيّة التي تحرس الورقة: ما يُعرض لا يزيد على ما يُعلَن. ولو انعكس لقالت
        // الورقة «لا شيء آخر» وهي تعرض أكثر مما عدّت — وهو العطب الذي جاءت هذه الدالّة لمنعه.
        listOf("الصوت", "شحن", "gpu", "سجل", "شاشة", "ال").forEach { query ->
            val shown = screenFinderResults(query, targets)
            assertTrue("سقف العرض تجاوز ٢٤ لـ«$query»", shown.size <= 24)
            assertTrue(
                "المعروض أكثر من المعدود لـ«$query»: ${shown.size} > ${screenFinderMatchCount(query, targets)}",
                shown.size <= screenFinderMatchCount(query, targets),
            )
        }
    }

    @Test
    fun `the enter key opens the row that is already at the top of the list`() {
        val targets = screenFinderTargets({ "«$it»" })
        // المفتاح لا يفتح نتيجةً ثانية: هو أوّل ما تعرضه الورقة، بالترتيب نفسه.
        listOf("الصوت", "شحن", "gpu").forEach { query ->
            assertEquals(
                "مفتاح Enter لا يفتح أعلى نتيجة لـ«$query»",
                screenFinderResults(query, targets).first(),
                screenFinderTopHit(query, targets),
            )
        }
    }

    @Test
    fun `the enter key does nothing on an empty field or a query that matches nothing`() {
        // **ولا يُعتمد على السجلّ الحقيقيّ في «لا شيء»:** العبارة التي تبدو بلا معنى قد تُطابق
        // بعد إسقاط نحو الكلام، فيسقط الاختبار على افتراضٍ لا على عطب (وقع فعلًا: «zzz لا شيء
        // بهذا الاسم» أعادت نتيجةً لأنّ «الاسم» كلمة في الفهرس). فالفهرس هنا اصطناعيّ ومحدَّد.
        val only = listOf(target(MaxDestination.ZramManager, "إدارة الذاكرة"))
        assertNull(screenFinderTopHit("", only))
        assertNull(screenFinderTopHit("   ", only))
        assertNull(screenFinderTopHit("qwxzvbn", only))
    }

    @Test
    fun `an empty query claims no matches at all`() {
        val targets = screenFinderTargets({ "«$it»" })
        // لا «٠ نتيجة» تُكتب في الورقة: الفراغ ليس بحثًا فاشلًا، بل حقلًا لم يُكتب فيه بعد.
        assertEquals(0, screenFinderMatchCount("", targets))
        assertEquals(0, screenFinderMatchCount("   ", targets))
    }

    @Test
    fun `an empty query returns nothing instead of the whole app`() {
        val targets = screenFinderTargets({ "«$it»" })
        assertTrue(screenFinderResults("", targets).isEmpty())
        assertTrue(screenFinderResults("   ", targets).isEmpty())
    }

    @Test
    fun `screens of the same rank come back in an order that the registry cannot change`() {
        val first = target(MaxDestination.ZramManager, "الذاكرة")
        val second = target(MaxDestination.MemoryHub, "اختبار الذاكرة")

        assertEquals(
            screenFinderResults("الذاكرة", listOf(second, first)).map { it.destination },
            screenFinderResults("الذاكرة", listOf(first, second)).map { it.destination },
        )
    }

    @Test
    fun `the result list is capped`() {
        val many = (1..30).map { target(MaxDestination.Logs, "سجلّ رقم $it") }
        assertEquals(3, screenFinderResults("سجل", many, limit = 3).size)
    }

    // ── (٣) والرسم يستعمل لغة الصفوف نفسها ────────────────────────────────────

    @Test
    fun `the sheet reuses the app's row language instead of inventing a third one`() {
        val sheet = read("ui/mainscreens/ScreenFinderSheet.kt")
        assertTrue("الورقة من قشرة التطبيق لا من حوار مكتوب بيد", sheet.contains("CustomBottomSheet("))
        assertTrue("وحقل البحث من العنصر المشترك لا من OutlinedTextField ثانٍ", sheet.contains("MaxSearchField("))
        assertTrue("والنتيجة صفٌّ كصفوف التطبيق", sheet.contains("MaxRow("))
        assertTrue(
            "واسم المكان مرئيّ في السطر نفسه: المستخدم يعرف أين تسكن قبل الضغط",
            sheet.contains("target.where"),
        )
        assertTrue(
            "والفهرس من السجلّ لا من قائمة مكتوبة بيد",
            read("ui/mainscreens/ScreenFinderModel.kt").contains("MaxDestination.Visible"),
        )
        assertTrue(
            "و«المرئيّ» مشتقٌّ من السجلّ لا قائمة ثانية:" +
                " إخفاءٌ في السجلّ يسري على الفهرس بلا سطرٍ هنا",
            read("ui/navigation/MaxDestinations.kt").contains("All.filterNot { it.hidden }"),
        )
    }

    // ── (٤) معلومات السطر: كم فتحتها · ووسم الخطورة · وما تفتحه أكثر ───────

    @Test
    fun `among equally ranked screens, the one you open most comes first`() {
        // ورتبة المطابقة واحدة في الاثنين (بداية كلمة داخل الاسم)، فلا يفصل بينهما إلا العدد.
        val rarely = target(MaxDestination.ZramManager, "اختبار الذاكرة", usage = 1)
        val often = target(MaxDestination.MemoryHub, "ضغط الذاكرة", usage = 9)

        assertEquals(
            listOf(MaxDestination.MemoryHub, MaxDestination.ZramManager),
            screenFinderResults("الذاكرة", listOf(rarely, often)).map { it.destination },
        )
        // ولا يُقدَّم العدد على الرتبة: مطابقةُ الاسم تسبق مطابقة الوصف ولو كثر فتحُ الثانية.
        val named = target(MaxDestination.ZramManager, "الذاكرة", usage = 1)
        val described = target(MaxDestination.MemoryHub, "اختبار", role = "الذاكرة", usage = 99)
        assertEquals(
            listOf(MaxDestination.ZramManager, MaxDestination.MemoryHub),
            screenFinderResults("الذاكرة", listOf(described, named)).map { it.destination },
        )
    }

    @Test
    fun `the count is attached by route, and a screen never opened reads zero`() {
        val targets = screenFinderTargets(
            { "«$it»" },
            mapOf(
                MaxDestination.ZramManager.route to 3,
                "route_not_in_the_registry" to 7,
            ),
        )

        assertEquals(
            "العدّاد يُقرأ بمسار الوجهة لا باسم عرض يبتكره القارئ",
            3,
            targets.first { it.destination == MaxDestination.ZramManager }.usage,
        )
        assertEquals(
            "وشاشة لم تُفتح بعدُ صفرها صفر ولا يرمي",
            0,
            targets.first { it.destination == MaxDestination.MemoryHub }.usage,
        )
        assertEquals(
            "الفهرس كله لا يسقط إذا كان في السجلّ مسار خارج السجلّ",
            expectedFinderSize(),
            targets.size,
        )
    }

    @Test
    fun `what you opened most is suggested before you type, and nothing when nothing was recorded`() {
        assertTrue(
            "لا اقتراح من عدّاد فارغ: أعلى الصفر ترتيبٌ لا مقياس خلفه",
            screenFinderSuggestions(screenFinderTargets({ "«$it»" })).isEmpty(),
        )

        val targets = listOf(
            target(MaxDestination.Logs, "السجلات", usage = 0),
            target(MaxDestination.ZramManager, "الذاكرة", usage = 2),
            target(MaxDestination.MemoryHub, "اختبار الذاكرة", usage = 5),
        )
        assertEquals(
            listOf(MaxDestination.MemoryHub, MaxDestination.ZramManager),
            screenFinderSuggestions(targets).map { it.destination },
        )
        assertEquals(1, screenFinderSuggestions(targets, limit = 1).size)
        assertEquals(0, screenFinderSuggestions(targets, limit = 0).size)
        assertEquals(4, SCREEN_FINDER_SUGGESTIONS)
    }

    @Test
    fun `the screen you are standing on is not suggested to you`() {
        val targets = listOf(
            target(MaxDestination.Control, "Control", usage = 40),
            target(MaxDestination.Settings, "Settings", usage = 12),
            target(MaxDestination.ZramManager, "ZRAM", usage = 3),
        )
        assertEquals(
            "المضيف لا يُقترح لنفسه: صفٌّ لا يفعل شيئًا عند الضغط ويأكل مقعدًا من أربعة",
            listOf(MaxDestination.Settings, MaxDestination.ZramManager),
            screenFinderSuggestions(
                targets,
                exclude = setOf(MaxDestination.Control.route),
            ).map { it.destination },
        )
        assertEquals(
            "والاستبعاد للاقتراح وحده: من كتب اسم المضيف يجده في النتائج",
            listOf(MaxDestination.Control),
            screenFinderResults("control", targets).map { it.destination },
        )
    }

    @Test
    fun `excluding the host frees a seat instead of shrinking the list`() {
        val targets = listOf(
            target(MaxDestination.Control, "Control", usage = 50),
            target(MaxDestination.Settings, "Settings", usage = 40),
            target(MaxDestination.Logs, "Logs", usage = 30),
            target(MaxDestination.MemoryHub, "Memory", usage = 20),
            target(MaxDestination.ZramManager, "ZRAM", usage = 10),
        )
        val withoutHost = screenFinderSuggestions(
            targets,
            exclude = setOf(MaxDestination.Control.route),
        )
        assertEquals(
            "السقف أربعة، والمستبعَد يُحرّر مقعده للخامس ولا يُنقص القائمة",
            listOf(
                MaxDestination.Settings,
                MaxDestination.Logs,
                MaxDestination.MemoryHub,
                MaxDestination.ZramManager,
            ),
            withoutHost.map { it.destination },
        )
        assertEquals(SCREEN_FINDER_SUGGESTIONS, withoutHost.size)
    }

    @Test
    fun `a screen that carries risk says so, and an ordinary one says nothing`() {
        assertEquals(
            R.string.max_risk_advanced,
            screenFinderRiskRes(MaxDestination.SetEdit),
        )
        assertNull(
            "الشاشة العادية لا وسم لها — كتابة «عادي» في كل صفّ ضجيج",
            screenFinderRiskRes(MaxDestination.ZramManager),
        )

        val targets = screenFinderTargets({ "«$it»" })
        assertEquals(
            "وسم السطر من `maxRiskLabel` نفسه لا من جدول ثانٍ",
            "«${R.string.max_risk_advanced}»",
            targets.first { it.destination == MaxDestination.SetEdit }.riskNote,
        )
        assertNull(targets.first { it.destination == MaxDestination.ZramManager }.riskNote)
    }

    @Test
    fun `the sheet reads the one ledger and shows the four facts per row`() {
        val sheet = read("ui/mainscreens/ScreenFinderSheet.kt")
        assertTrue(
            "العدّاد من السجلّ الواحد لا من عدّاد خاصّ بالمُوجِّد",
            sheet.contains("ScreenUsageStore.of(context).counts()"),
        )
        assertTrue("ووسم الخطورة يُعرض في السطر", sheet.contains("target.riskNote"))
        assertTrue("وعدد الفتحات يُعرض من مورد واحد", sheet.contains("R.string.screen_opens_count"))
        assertTrue("وما تفتحه أكثر يُعرض قبل الكتابة", sheet.contains("screenFinderSuggestions("))
        assertTrue(
            "والمُوجِّد لا يخترع عدّادًا ثانيًا",
            !read("ui/mainscreens/ScreenFinderSheet.kt").contains("getSharedPreferences"),
        )
        assertTrue(
            "والمضيف لا يُقترح لنفسه: كل مستدعٍ يمرّر مسار شاشته",
            read("ui/mainscreens/ControlScreen.kt").contains("currentRoute = MaxDestination.Control.route") &&
                read("ui/mainscreens/SettingsScreen.kt").contains("currentRoute = MaxDestination.Settings.route"),
        )
    }

    @Test
    fun `intent words find the screen that does the job`() {
        val targets = screenFinderTargets({ "«$it»" })

        fun destinations(query: String) = screenFinderResults(query, targets).map { it.destination }

        assertTrue(
            "«شحن» ليست اسم شاشة — تُطابَق من كلمات القصد لا من الاسم",
            MaxDestination.Charging in destinations("شحن"),
        )
        assertTrue(
            "«سخونة» كلمة المستخدم لا اسم الشاشة",
            MaxDestination.ThermalDetail in destinations("سخونة"),
        )
        assertTrue(
            "والإنجليزية تعمل في الواجهة العربية — من كتب bypass لا يُترك بلا نتيجة",
            MaxDestination.BypassCharging in destinations("bypass"),
        )
        assertTrue(
            "والرقم ٦٠ يقصد الشاشة التي تضبطه",
            MaxDestination.FpsGo in destinations("٦٠"),
        )
    }

    /**
     * الكلمات لا تصنع بابًا خلفيًّا: مدخلٌ لوجهة مخفيّة أو لا تُفتح بلا معرّف كان سيُظهرها في
     * البحث وحدها — وهي حالٌ أسوأ من غيابها (ADR-07: لا وعد بضغطة تفتح شيئًا آخر).
     */
    @Test
    fun `intent words never expose a hidden or bare-unopenable destination`() {
        val openable = MaxDestination.Visible
            .filterNot { it.needsLaunchArgument }
            .toSet()

        val stray = maxDestinationKeywords.keys.filterNot { it in openable }
        assertTrue("كلمة قصد لوجهة غير معروضة: $stray", stray.isEmpty())

        // والعكس أيضًا: وجهة معروضة بلا كلمة قصد تُترك بلا بحث إلا باسمها — وهي الحال التي
        // وُلدت منها الكلمات. فالتغطية عقدٌ لا تفضّل: تُقاس كلها أو لا تُقال.
        val uncovered = openable.filterNot { it in maxDestinationKeywords }
        assertTrue("وجهة معروضة بلا كلمة قصد: $uncovered", uncovered.isEmpty())
        assertTrue(
            "مدخل بلا كلمات أو بكلمة فارغة",
            maxDestinationKeywords.values.all { words ->
                words.isNotEmpty() && words.none { it.isBlank() }
            },
        )
    }

    /**
     * الجملة الكاملة: المستخدم يكتب ما يقوله، لا اسم الشاشة (فكرة §٣-٥ «أوامر بالقصد»).
     * وما يُسقَط نحوٌ خالص فقط — كلمةٌ تحمل معنى لا تُسقَط، وإلا عملت الجملة على غير قصدها.
     */
    @Test
    fun `a whole sentence finds the screen it asks for`() {
        val targets = screenFinderTargets({ "«$it»" })

        fun destinations(query: String) = screenFinderResults(query, targets).map { it.destination }

        assertTrue(
            "«أوقف الشحن الآن»: كلمة القصد وحدها تُطابق بعد إسقاط النحو",
            MaxDestination.Charging in destinations("أوقف الشحن الآن"),
        )
        assertTrue(
            "ونحو الإنجليزية يُسقط أيضًا: how do i open bypass",
            MaxDestination.BypassCharging in destinations("how do i open bypass"),
        )
        assertTrue(
            "ونحوٌ وحده لا يُرجع شيئًا — لا نتائج من لا شيء",
            destinations("how do i the").isEmpty(),
        )
    }

    /** الاسم المطابق كاملًا يبقى قبل الجملة: الترتيب لا ينقلب لأن الاستعلام طال. */
    @Test
    fun `sentences do not outrank exact names`() {
        val targets = screenFinderTargets({ "«$it»" })

        assertEquals(
            "الاسم المطابق يبقى أوّلًا مهما طالت الجملة",
            MaxDestination.BatteryHistory,
            screenFinderResults("سجل البطارية", targets).firstOrNull()?.destination,
        )
    }

    @Test
    fun `the finder opens from the settings page too, not only from control`() {
        // العطب المقصود: الفهرس يغطّي كل الوجهات، لكن مدخله الوحيد كان شريط «التحكّم» — فمن
        // هو في الإعدادات (وهي أطول قائمة في التطبيق) كان يغادرها ليبحث. والورقة واحدة.
        val settings = read("ui/mainscreens/SettingsScreen.kt")
        assertTrue(
            "SettingsScreen does not host the shared ScreenFinderSheet",
            settings.contains("ScreenFinderSheet("),
        )
        assertTrue(
            "SettingsScreen has no state that opens the finder",
            settings.contains("showFinder"),
        )
        assertTrue(
            "ControlScreen no longer hosts the same sheet — two finders would be two truths",
            read("ui/mainscreens/ControlScreen.kt").contains("ScreenFinderSheet("),
        )
    }
}
