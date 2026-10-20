package com.thoughtflow.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtflow.ai.data.model.DashboardResponse
import com.thoughtflow.ai.data.model.ThoughtResponse
import com.thoughtflow.ai.data.repository.ThoughtFlowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val dashboard: DashboardResponse,
        val thoughts: List<ThoughtResponse>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

class DashboardViewModel(
    private val repository: ThoughtFlowRepository = ThoughtFlowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getDashboard().collect { dashResult ->
                dashResult.onSuccess { dashData ->
                    repository.getThoughts().collect { thoughtsResult ->
                        val thoughts = thoughtsResult.getOrDefault(emptyList())
                        _uiState.value = DashboardUiState.Success(dashData, thoughts)
                    }
                }.onFailure { err ->
                    _uiState.value = DashboardUiState.Error(err.message ?: "Failed to connect to ThoughtFlow backend")
                }
            }
            _isRefreshing.value = false
        }
    }

    fun runPipeline(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.runPipeline().collect { res ->
                res.onSuccess {
                    loadData()
                    onComplete(true, "Pipeline completed successfully!")
                }.onFailure {
                    onComplete(false, it.message ?: "Pipeline execution failed")
                }
            }
        }
    }
}
