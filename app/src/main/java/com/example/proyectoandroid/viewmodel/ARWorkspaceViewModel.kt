package com.example.proyectoandroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoandroid.data.model.AssemblyModel
import com.example.proyectoandroid.data.model.AssemblyStep
import com.example.proyectoandroid.data.model.NodeHighlightInfo
import com.example.proyectoandroid.data.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ARWorkspaceUiState(
    val currentModel: AssemblyModel? = null,
    val currentStepIndex: Int = 0,
    val isPlaneDetected: Boolean = false,
    val isModelAnchored: Boolean = false,
    val highlightedNodeInfo: NodeHighlightInfo? = null,
    val trackingFailureMessage: String? = null,
    val isLoadingModel: Boolean = false,
    val errorMessage: String? = null,
    val isExplodedView: Boolean = false
) {
    val currentStep: AssemblyStep?
        get() = currentModel?.steps?.getOrNull(currentStepIndex)

    val totalSteps: Int
        get() = currentModel?.steps?.size ?: 0

    val progress: Float
        get() = if (totalSteps > 0) (currentStepIndex + 1).toFloat() / totalSteps.toFloat() else 0f
}

class ARWorkspaceViewModel(
    private val repository: ModelRepository = ModelRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ARWorkspaceUiState())
    val uiState: StateFlow<ARWorkspaceUiState> = _uiState.asStateFlow()

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModel = true, errorMessage = null) }
            val model = repository.getModelById(modelId)
            if (model != null) {
                _uiState.update {
                    it.copy(
                        currentModel = model,
                        currentStepIndex = 0,
                        isLoadingModel = false,
                        highlightedNodeInfo = null,
                        isModelAnchored = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        errorMessage = "No se pudo cargar el modelo 3D ($modelId). Verifica los archivos assets."
                    )
                }
            }
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            val total = state.totalSteps
            if (state.currentStepIndex < total - 1) {
                state.copy(
                    currentStepIndex = state.currentStepIndex + 1,
                    highlightedNodeInfo = null
                )
            } else state
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStepIndex > 0) {
                state.copy(
                    currentStepIndex = state.currentStepIndex - 1,
                    highlightedNodeInfo = null
                )
            } else state
        }
    }

    fun onPlaneDetected(detected: Boolean) {
        _uiState.update { it.copy(isPlaneDetected = detected) }
    }

    fun onModelAnchored(anchored: Boolean) {
        _uiState.update { it.copy(isModelAnchored = anchored) }
    }

    fun onNodeTouched(nodeName: String) {
        val info = repository.getNodeInfo(nodeName)
        _uiState.update { it.copy(highlightedNodeInfo = info) }
    }

    fun clearHighlight() {
        _uiState.update { it.copy(highlightedNodeInfo = null) }
    }

    fun resetAnchor() {
        _uiState.update {
            it.copy(
                isModelAnchored = false,
                currentStepIndex = 0,
                highlightedNodeInfo = null
            )
        }
    }

    fun setTrackingFailureMessage(message: String?) {
        _uiState.update { it.copy(trackingFailureMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleExplodedView() {
        _uiState.update { it.copy(isExplodedView = !it.isExplodedView) }
    }
}
