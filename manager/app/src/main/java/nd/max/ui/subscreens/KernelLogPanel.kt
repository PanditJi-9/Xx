/*
 * MaxManager — سجلّ النواة (dmesg)
 *
 * هذا السطح يأخذ من `fkm` فكرةً لا نصًّا: `system_health/DmesgFragment` هناك يعرض سجلّ النواة
 * بجانب سجلّ التطبيق، و`devcheck`'s `LogcatReader` لا يعرضه أصلًا. وحين يغيب السجلّ في شاشتنا
 * يبقى المستخدم بلا المصدر الذي يشرح *لماذا* سقط المحرّك أو رُفض إعداد.
 *
 * والحدّ المعلن: `dmesg` في أندرويد الحديثة يحتاج صلاحية الجذر (أو `CAP_SYSLOG`). فالشاشة
 * **تقرأ لقطة** لا تبثّ، وتُرجع «غير مُتاح» نصًّا حين يرفض النظام الأمر — ولا صفرًا ولا جدولًا
 * فارغًا يُقرأ «لا شيء حدث». والقصر مقصود: آخر `KernelLogMaxLines` سطرًا، لأن سجلّ نواة كاملًا
 * لا يُقرأ بالعين ولا يُمرَّر بلا نهاية.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.privilege.RootCommands
import nd.max.ui.design.MaxSpace

/** آخر ما يُحتفظ به: سجلّ نواة كامل لا يُقرأ، ولا يُمرّر بلا نهاية في عمود واحد. */
private const val KernelLogMaxLines = 400

/** 12sp هو أصغر حجم مسموح في هذا المستودع، ولا حاجة لتصغيره: الشاشة أفقيّة وواسعة. */
private val KernelLogTextSize = 12.sp

/**
 * لقطة سجلّ النواة. تُقرأ مرّة عند الظهور، وتُعاد بضغطة صريحة — لا مؤقّت خلفيّ يستهلك بطارية
 * لعرض نصّ لا يتغيّر بسرعة تفيد العين.
 */
@Composable
internal fun KernelLogPanel(modifier: Modifier = Modifier) {
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }

    val unavailable = stringResource(R.string.logsviewer_kernel_unavailable)
    val emptyText = stringResource(R.string.logsviewer_kernel_empty)

    LaunchedEffect(attempt) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { RootCommands.cmd("dmesg").exec() }.getOrNull()
        }
        val read = result?.out?.filter { it.isNotBlank() }.orEmpty()
        lines = if (read.size > KernelLogMaxLines) read.takeLast(KernelLogMaxLines) else read
        problem = when {
            result == null -> unavailable
            !result.isSuccess || lines.isEmpty() ->
                result.err.firstOrNull { it.isNotBlank() } ?: unavailable
            else -> null
        }
        loading = false
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaxSpace.xs),
        ) {
            Text(
                text = stringResource(R.string.logsviewer_kernel_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { attempt += 1 }) {
                Text(stringResource(R.string.logsviewer_kernel_refresh))
            }
        }

        when {
            loading && lines.isEmpty() -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            problem != null -> Text(
                text = problem.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaxSpace.md),
            )
            lines.isEmpty() -> Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaxSpace.md),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(lines) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = KernelLogTextSize,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}
