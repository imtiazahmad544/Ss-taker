package com.floatingscreen.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import com.floatingscreen.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryFilter { ALL, RECORDINGS, SCREENSHOTS }

data class HistoryUiState(
    val media: List<MediaRecord> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filter: HistoryFilter = HistoryFilter.ALL,
    val selectedIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val totalStorageUsed: Long = 0L
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllMediaUseCase: GetAllMediaUseCase,
    private val getMediaByTypeUseCase: GetMediaByTypeUseCase,
    private val searchMediaUseCase: SearchMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val renameMediaUseCase: RenameMediaUseCase,
    private val getStorageUsedUseCase: GetStorageUsedUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isLoading = MutableStateFlow(false)
    private val _totalStorage = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        _filter,
        _searchQuery,
        _selectedIds,
        _isLoading,
        _totalStorage
    ) { filter, query, selectedIds, isLoading, storage ->
        HistoryUiState(
            filter = filter,
            searchQuery = query,
            selectedIds = selectedIds,
            isMultiSelectMode = selectedIds.isNotEmpty(),
            isLoading = isLoading,
            totalStorageUsed = storage
        )
    }.flatMapLatest { partialState ->
        val query = partialState.searchQuery
        val filter = partialState.filter

        val mediaFlow = when {
            query.isNotBlank() -> searchMediaUseCase(query)
            filter == HistoryFilter.RECORDINGS -> getMediaByTypeUseCase(MediaType.RECORDING)
            filter == HistoryFilter.SCREENSHOTS -> getMediaByTypeUseCase(MediaType.SCREENSHOT)
            else -> getAllMediaUseCase()
        }

        mediaFlow.map { media ->
            partialState.copy(media = media)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState(isLoading = true)
    )

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            _totalStorage.value = getStorageUsedUseCase()
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
        clearSelection()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.media.map { it.id }.toSet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _isLoading.value = true
            val ids = _selectedIds.value.toList()
            ids.forEach { id -> deleteMediaUseCase(id) }
            clearSelection()
            _totalStorage.value = getStorageUsedUseCase()
            _isLoading.value = false
        }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            deleteMediaUseCase(id)
            _selectedIds.update { it - id }
            _totalStorage.value = getStorageUsedUseCase()
        }
    }

    fun renameMedia(id: Long, newName: String) {
        viewModelScope.launch {
            renameMediaUseCase(id, newName)
        }
    }
}
