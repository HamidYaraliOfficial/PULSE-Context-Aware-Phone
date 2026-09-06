package com.pulse.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.context.ContextEngine
import com.pulse.app.context.signals.BatterySignal
import com.pulse.app.context.signals.BatterySignalProvider
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.ModeRepository
import com.pulse.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val currentContext: ContextState? = null,
    val activeMode: PulseMode? = null,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val recommendations: List<Recommendation> = emptyList(),
    val allModes: List<PulseMode> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    val contextEngine: ContextEngine,
    private val modeRepository: ModeRepository,
    private val recommendationRepository: RecommendationRepository,
    batterySignalProvider: BatterySignalProvider,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        contextEngine.currentContext,
        modeRepository.activeMode,
        modeRepository.modes,
        recommendationRepository.pending,
        batterySignalProvider.observe().catch { emit(BatterySignal(100, false, false)) },
    ) { context, activeMode, allModes, recs, battery ->
        HomeUiState(context, activeMode, battery.level, battery.isCharging, recs, allModes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun confirmContext() = viewModelScope.launch { contextEngine.applyUserFeedback(ContextFeedback.CONFIRMED) }
    fun rejectContext() = viewModelScope.launch { contextEngine.applyUserFeedback(ContextFeedback.REJECTED) }
    fun correctContext() = viewModelScope.launch { contextEngine.applyUserFeedback(ContextFeedback.CORRECTED) }

    fun activateMode(modeId: String) = viewModelScope.launch { modeRepository.activate(modeId, "manual") }
    fun deactivateMode(modeId: String) = viewModelScope.launch { modeRepository.deactivate(modeId) }

    fun resolveRecommendation(id: String, decision: RecommendationDecision) =
        viewModelScope.launch { recommendationRepository.resolve(id, decision) }
}
