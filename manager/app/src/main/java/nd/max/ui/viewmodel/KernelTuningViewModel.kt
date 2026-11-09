/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.display.PanelColorTuning
import nd.max.core.hardware.GovernorTuning
import nd.max.core.hardware.HapticTuning
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.NodeControl
import nd.max.core.hardware.NodeTuning
import nd.max.core.hardware.NodeTuningOutcome
import nd.max.core.hardware.NodeTuningResult
import nd.max.core.hardware.StorageTuning
import nd.max.ui.util.ProfilePresetStore
import nd.max.ui.util.ProfileSharing
import nd.max.core.policy.DangerConsent

/** Manual sections share one backend and lifetime; no timer or background re-apply is introduced. */
@HiltViewModel
class KernelTuningViewModel @Inject constructor(
    arbiter: HardwareControlArbiter,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    enum class Domain { STORAGE, GOVERNOR, HAPTIC, COLOR }

    private val tuning = NodeTuning(arbiter)
    private val baselines = linkedMapOf<String, String>()
    private val lastWritten = linkedMapOf<String, String>()
    private val loadedDomains = hashSetOf<Domain>()
    private val loadingDomains = hashSetOf<Domain>()
    private var pending: Pair<NodeControl, String>? = null

    var storage by mutableStateOf<List<StorageTuning.Device>>(emptyList()); private set
    var governors by mutableStateOf<List<GovernorTuning.Group>>(emptyList()); private set
    var haptics by mutableStateOf<List<NodeControl>>(emptyList()); private set
    var colors by mutableStateOf<List<NodeControl>>(emptyList()); private set
    var busy by mutableStateOf(false); private set
    var scanning by mutableStateOf(false); private set
    var result by mutableStateOf<NodeTuningResult?>(null); private set
    var resultDomain by mutableStateOf<Domain?>(null); private set
    var snapshotSaved by mutableStateOf<Boolean?>(null); private set
    var savedGovernorProfiles by mutableStateOf<Set<String>>(emptySet()); private set

    fun load(domain: Domain, force: Boolean = false) {
        if (busy || domain in loadingDomains || (!force && domain in loadedDomains)) return
        loadingDomains += domain
        scanning = true
        viewModelScope.launch {
            try {
                when (domain) {
                    Domain.STORAGE -> storage = withContext(Dispatchers.IO) { StorageTuning.discover() }
                    Domain.GOVERNOR -> {
                        governors = withContext(Dispatchers.IO) { GovernorTuning.discover() }
                        savedGovernorProfiles = withContext(Dispatchers.IO) {
                            ProfileSharing.KNOWN_PROFILES.filter { ProfilePresetStore.governorSnapshot(context, it).isNotEmpty() }.toSet()
                        }
                    }
                    Domain.HAPTIC -> haptics = withContext(Dispatchers.IO) { HapticTuning.discover() }
                    Domain.COLOR -> colors = withContext(Dispatchers.IO) { PanelColorTuning.discover() }
                }
                controls(domain).forEach { baselines.putIfAbsent(it.key, it.value) }
                loadedDomains += domain
            } finally {
                loadingDomains -= domain
                scanning = loadingDomains.isNotEmpty()
            }
        }
    }

    fun controls(domain: Domain): List<NodeControl> = when (domain) {
        Domain.STORAGE -> storage.flatMap(StorageTuning.Device::controls)
        Domain.GOVERNOR -> governors.flatMap(GovernorTuning.Group::controls)
        Domain.HAPTIC -> haptics
        Domain.COLOR -> colors
    }

    fun prepare(control: NodeControl, value: String) {
        pending = null
        if (busy || loadingDomains.isNotEmpty() || !control.accepts(value)) return
        pending = control to value
    }

    /** Pending state is read from the ViewModel, so an already-acknowledged gate cannot lose a click. */
    fun applyPending() {
        val request = pending ?: return
        pending = null
        apply(listOf(request))
    }

    fun restore(control: NodeControl) {
        val baseline = baselines[control.key] ?: return
        val applied = lastWritten[control.key] ?: return
        if (control.value != applied) {
            result = NodeTuningResult(NodeTuningOutcome.BLOCKED, control.value)
            resultDomain = domainOf(control)
            return
        }
        apply(listOf(control to baseline))
    }

    fun canRestore(control: NodeControl): Boolean =
        lastWritten[control.key] == control.value && baselines[control.key] != control.value

    fun saveGovernorSnapshot(profile: String) {
        val expectedKeys = controls(Domain.GOVERNOR).map(NodeControl::key).toSet()
        if (expectedKeys.isEmpty() || busy || loadingDomains.isNotEmpty()) return
        busy = true
        viewModelScope.launch {
            try {
                snapshotSaved = withContext(Dispatchers.IO) {
                    val fresh = GovernorTuning.discover().flatMap(GovernorTuning.Group::controls)
                    fresh.map(NodeControl::key).toSet() == expectedKeys &&
                        ProfilePresetStore.setGovernorSnapshot(context, profile, fresh.associate { it.key to it.value })
                }
                if (snapshotSaved == true) savedGovernorProfiles = savedGovernorProfiles + profile
            } finally {
                busy = false
            }
        }
    }

    fun hasGovernorSnapshot(profile: String): Boolean = profile in savedGovernorProfiles

    fun applyGovernorSnapshot(profile: String) {
        if (busy || loadingDomains.isNotEmpty()) return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { ProfilePresetStore.governorSnapshot(context, profile) }
            val matched = GovernorTuning.matchSnapshot(controls(Domain.GOVERNOR), saved)
            if (matched == null) {
                result = NodeTuningResult(NodeTuningOutcome.INVALID)
                resultDomain = Domain.GOVERNOR
            } else apply(matched)
        }
    }

    private fun apply(requests: List<Pair<NodeControl, String>>) {
        if (busy || requests.isEmpty() || loadingDomains.isNotEmpty()) return
        busy = true
        result = null
        resultDomain = domainOf(requests.first().first)
        viewModelScope.launch {
            try {
                val verdict = withContext(Dispatchers.IO) {
                    val consent = resultDomain?.let(::consentId)
                    if (consent == null || !DangerConsent.isAcknowledged(context, consent)) {
                        NodeTuningResult(NodeTuningOutcome.BLOCKED)
                    } else {
                        tuning.applyPreset(requests)
                    }
                }
                if (verdict.outcome == NodeTuningOutcome.APPLIED) {
                    requests.forEach { (control, value) -> lastWritten[control.key] = control.format.normalise(value) ?: value }
                }
                result = verdict
            } finally {
                busy = false
                loadedDomains.toList().forEach { load(it, force = true) }
            }
        }
    }

    fun feedback(domain: Domain): NodeTuningResult? = result.takeIf { resultDomain == domain }

    private fun domainOf(control: NodeControl): Domain? = Domain.entries.firstOrNull { domain ->
        controls(domain).any { it.key == control.key }
    }

    private fun consentId(domain: Domain): String = when (domain) {
        Domain.STORAGE -> DangerConsent.STORAGE_TUNING
        Domain.GOVERNOR -> DangerConsent.GOVERNOR_TUNING
        Domain.HAPTIC -> DangerConsent.HAPTIC_TUNING
        Domain.COLOR -> DangerConsent.PANEL_COLOR_TUNING
    }
}
