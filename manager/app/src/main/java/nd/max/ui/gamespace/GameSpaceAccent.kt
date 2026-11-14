/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.gamespace

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import nd.max.R
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/*
 * تشكيلة ألوان Game Space — يختار المستخدم لونها، فتتبدّل الشاشة كلها لا زخرفةٌ فيها.
 *
 * **لماذا هنا ولماذا بهذا الشكل:** Monolith يبني الأسطح من خمس درجات محايدة ويكتب أنّ «لون
 * اللعبة هو الشيء الوحيد المسموح له أن يلمع». وهذه التشكيلة لا تنقض ذلك: السطح يبقى Monolith،
 * والدرجة الملوّنة **واحدة** تُمرَّر إلى `monolithAccent` فتصبغ الشعار والبلاطة والزرّ والحلقة
 * معًا. فليست لوحةً ثانية للتطبيق، بل **اختيار المستخدم للدرجة الملوّنة الوحيدة**.
 *
 * و`Game` (الأوّل) ليس «بلا لون»: هو القائم قبل هذا الملف — اللون يُشتقّ من أيقونة اللعبة —
 * فيبقى الافتراض كما كان ولا يتغيّر شكل أحد إن لم يختر شيئًا.
 */
enum class GameSpaceScheme(val id: String, val labelRes: Int, val accent: Color?) {
    Game("game", R.string.gs_scheme_game, null),
    Ember("ember", R.string.gs_scheme_ember, Monolith.Ember),
    Violet("violet", R.string.gs_scheme_violet, Color(0xFF8B5CF6)),
    Azure("azure", R.string.gs_scheme_azure, Color(0xFF3B82F6)),
    Emerald("emerald", R.string.gs_scheme_emerald, Color(0xFF10B981)),
    Rose("rose", R.string.gs_scheme_rose, Color(0xFFF43F5E)),
    Amber("amber", R.string.gs_scheme_amber, Monolith.Amber);

    companion object {
        /** ما لم يُقرأ معرّف معروف يُعاد إلى الافتراض — لا استثناء ولا شاشة بلا لون. */
        fun fromId(id: String?): GameSpaceScheme = entries.firstOrNull { it.id == id } ?: Game
    }
}

/** تخزين الاختيار في تفضيلات مستقلّة عن `game_lobby`، فلا تُمسّ بنية خيارات اللوبي القائمة. */
object GameSpaceSkin {
    private const val PREFS = "game_space_skin"
    private const val KEY = "scheme"

    fun read(context: Context): GameSpaceScheme = GameSpaceScheme.fromId(prefs(context).getString(KEY, null))

    fun write(context: Context, scheme: GameSpaceScheme) {
        prefs(context).edit().putString(KEY, scheme.id).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * صفّ الدرجات: كل درجة عيّنة لون + اسمها، وهدفها اللمسي ٤٨dp لا نقطةً صغيرة، وتعلن نفسها
 * اختيارًا من مجموعة ([Role.RadioButton]) فيقرأها قارئ الشاشة كاختيار لا كزرّ تنفيذ.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSpaceSchemeRow(
    selected: GameSpaceScheme,
    onSelect: (GameSpaceScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        GameSpaceScheme.entries.forEach { scheme ->
            val isSelected = scheme == selected
            Row(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .clip(shape)
                    .background(if (isSelected) Monolith.GlassRaised else Color.Transparent)
                    .border(
                        MaxSize.hairlineBorder,
                        if (isSelected) Monolith.HairlineStrong else Monolith.Hairline,
                        shape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(scheme) }
                    )
                    .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(MaxSize.iconGlyphSmall)
                        .clip(CircleShape)
                        .background(scheme.accent ?: Monolith.Bone)
                )
                Text(
                    text = stringResource(scheme.labelRes),
                    style = Monolith.label,
                    color = if (isSelected) Monolith.Bone else Monolith.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
