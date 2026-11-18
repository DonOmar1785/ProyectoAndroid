package com.example.proyectoandroid.viewmodel

import androidx.lifecycle.ViewModel
import com.example.proyectoandroid.data.model.AssemblyModel
import com.example.proyectoandroid.data.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogViewModel(
    private val repository: ModelRepository = ModelRepository()
) : ViewModel() {

    private val _models = MutableStateFlow<List<AssemblyModel>>(emptyList())
    val models: StateFlow<List<AssemblyModel>> = _models.asStateFlow()

    private val _selectedModel = MutableStateFlow<AssemblyModel?>(null)
    val selectedModel: StateFlow<AssemblyModel?> = _selectedModel.asStateFlow()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        val list = repository.getAvailableModels()
        _models.value = list
        if (list.isNotEmpty()) {
            _selectedModel.value = list.first()
        }
    }

    fun selectModel(model: AssemblyModel) {
        _selectedModel.value = model
    }
}
