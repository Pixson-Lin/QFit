package com.pixsonlin.qfit.service

import com.pixsonlin.qfit.data.IntensityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveRunUi(
    val runId: String,
    val intensity: IntensityLevel,
    val durationMinutes: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val stepsWritten: Int = 0,
    val finished: Boolean = false,
    val errorMessage: String? = null,
)

object RunSessionState {
    private val _active = MutableStateFlow<ActiveRunUi?>(null)
    val active: StateFlow<ActiveRunUi?> = _active.asStateFlow()

    fun setActive(run: ActiveRunUi?) {
        _active.value = run
    }

    fun update(transform: (ActiveRunUi) -> ActiveRunUi) {
        val current = _active.value ?: return
        _active.value = transform(current)
    }
}
