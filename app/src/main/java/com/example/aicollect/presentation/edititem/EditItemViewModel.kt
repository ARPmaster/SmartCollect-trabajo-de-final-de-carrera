/** ViewModel de edición de un ítem: carga sus datos, valida el formulario y
 * guarda solo los campos editables (nombre, descripción, deporte, estado), dejando el resto intacto.*/
package com.example.aicollect.presentation.edititem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EditItemUiState {
    data object Loading : EditItemUiState
    data class Content(val nombre: String, val descripcion: String?, val deporte: String, val estado: String) : EditItemUiState
    data class Error(val message: UiText) : EditItemUiState
}

sealed interface SaveEditUiState {
    data object Idle : SaveEditUiState
    data object Saving : SaveEditUiState
    data object Success : SaveEditUiState
    data class Error(val message: UiText) : SaveEditUiState
    data class ValidationError(val field: EditRequiredField) : SaveEditUiState
}

enum class EditRequiredField { NAME, SPORT, CONDITION }

@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow<EditItemUiState>(EditItemUiState.Loading)
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveEditUiState>(SaveEditUiState.Idle)
    val saveState: StateFlow<SaveEditUiState> = _saveState.asStateFlow()

    private var loadedItem: Item? = null

    init {
        viewModelScope.launch {
            itemRepository.getItem(itemId)
                .onSuccess { item ->
                    loadedItem = item
                    _uiState.value = EditItemUiState.Content(
                        nombre = item.nombre,
                        descripcion = item.descripcion,
                        deporte = item.deporte,
                        estado = item.estado,
                    )
                }
                .onFailure {
                    _uiState.value = EditItemUiState.Error(
                        it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.error_item_load_generic),
                    )
                }
        }
    }

    fun save(name: String, description: String?, sport: String?, condition: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _saveState.value = SaveEditUiState.ValidationError(EditRequiredField.NAME)
            return
        }
        if (sport == null) {
            _saveState.value = SaveEditUiState.ValidationError(EditRequiredField.SPORT)
            return
        }
        if (condition == null) {
            _saveState.value = SaveEditUiState.ValidationError(EditRequiredField.CONDITION)
            return
        }

        val original = loadedItem
        if (original == null) {
            _saveState.value = SaveEditUiState.Error(UiText.StringResource(R.string.error_edit_item_not_loaded))
            return
        }
        val edited = original.copy(
            nombre = trimmedName,
            descripcion = description?.trim()?.takeIf { it.isNotEmpty() },
            deporte = sport,
            estado = condition,
        )

        _saveState.value = SaveEditUiState.Saving
        viewModelScope.launch {
            itemRepository.updateItem(itemId, edited)
                .onSuccess { _saveState.value = SaveEditUiState.Success }
                .onFailure {
                    _saveState.value = SaveEditUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_edit_item_save_generic),
                    )
                }
        }
    }
}
