package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CircuitDiagnosticRecord
import com.example.data.model.McuProjectRecord
import com.example.data.repository.McuWorkspaceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class WorkspaceViewModel(
    private val repository: McuWorkspaceRepository,
    private val currentUserId: String
) : ViewModel() {

    val projectsState: StateFlow<CloudUiState<List<McuProjectRecord>>> =
        repository.observeProjects(currentUserId)
            .map<List<McuProjectRecord>, CloudUiState<List<McuProjectRecord>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing projects", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "خطا در بارگذاری پروژه‌ها"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val diagnosticsState: StateFlow<CloudUiState<List<CircuitDiagnosticRecord>>> =
        repository.observeDiagnostics(currentUserId)
            .map<List<CircuitDiagnosticRecord>, CloudUiState<List<CircuitDiagnosticRecord>>> {
                CloudUiState.Success(it)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing diagnostics", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "خطا در بارگذاری گزارش‌های مدار"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun saveProject(
        title: String,
        mcuFamily: String,
        mcuModel: String,
        category: String,
        promptText: String,
        codeOrResult: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.saveProject(
                title = title,
                mcuFamily = mcuFamily,
                mcuModel = mcuModel,
                category = category,
                promptText = promptText,
                codeOrResult = codeOrResult,
                notes = notes
            )
            _toastMessage.value = if (result.isSuccess) {
                "پروژه با موفقیت در فضای ابری ذخیره شد."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "ذخیره‌سازی پروژه ناموفق بود."
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val result = repository.deleteProject(projectId)
            if (result.isFailure) {
                _toastMessage.value = result.exceptionOrNull()?.localizedMessage ?: "حذف پروژه ناموفق بود."
            }
        }
    }

    fun saveDiagnostic(
        title: String,
        mcuFamily: String,
        diagnosticType: String,
        queryText: String,
        analysisResult: String
    ) {
        viewModelScope.launch {
            val result = repository.saveDiagnostic(
                title = title,
                mcuFamily = mcuFamily,
                diagnosticType = diagnosticType,
                queryText = queryText,
                analysisResult = analysisResult
            )
            _toastMessage.value = if (result.isSuccess) {
                "گزارش مهندسی در آرشیو ابری ذخیره شد."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "ذخیره گزارش ناموفق بود."
            }
        }
    }

    fun deleteDiagnostic(diagnosticId: String) {
        viewModelScope.launch {
            val result = repository.deleteDiagnostic(diagnosticId)
            if (result.isFailure) {
                _toastMessage.value = result.exceptionOrNull()?.localizedMessage ?: "حذف گزارش ناموفق بود."
            }
        }
    }

    private companion object {
        const val TAG = "WorkspaceVM"
    }
}
