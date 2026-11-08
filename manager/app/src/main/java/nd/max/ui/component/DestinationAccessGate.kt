/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.navigation.rememberDestinationAccess
import nd.max.ui.navigation.requiredPrivilege

/** حارس التنفيذ قبل تركيب الشاشة؛ الروابط والويدجت لا تتجاوز اختيار الوضع. */
@Composable
fun DestinationAccessGate(destination: MaxDestination?, content: @Composable () -> Unit) {
    val access = rememberDestinationAccess()
    var dialog by remember { mutableStateOf(false) }
    if (destination == null || access.supported(destination)) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().padding(MaxSpace.lg),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(destination.titleRes))
            Text(stringResource(R.string.access_requires_level, stringResource(destination.requiredPrivilege.labelRes)))
            Button(onClick = { dialog = true }) { Text(stringResource(R.string.access_open)) }
        }
    }
    if (dialog) AccessModeDialog(onDismiss = { dialog = false })
}
