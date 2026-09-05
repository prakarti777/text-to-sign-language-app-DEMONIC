package com.example.demonic.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demonic.data.AssetJsonGestureRepository
import com.example.demonic.data.Gesture
import com.example.demonic.domain.PhraseMatcher
import com.example.demonic.speech.SpeechState
import com.example.demonic.speech.VoskSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val inputText: String = "",
    val speechState: SpeechState = SpeechState.NotInitialized,
    val partialSpeechText: String = "",
    val resolvedGestures: List<Gesture> = emptyList(),
    val unresolvedWords: List<String> = emptyList(),
    val currentPlayingIndex: Int = -1,
    val isPlaying: Boolean = false,
    val permissionGranted: Boolean = false
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AssetJsonGestureRepository(application)
    private val phraseMatcher = PhraseMatcher(repository)
    private val speechManager = VoskSpeechManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            speechManager.state.collect { state ->
                _uiState.value = _uiState.value.copy(speechState = state)
            }
        }
        viewModelScope.launch {
            speechManager.partialText.collect { partial ->
                _uiState.value = _uiState.value.copy(partialSpeechText = partial)
            }
        }
        viewModelScope.launch {
            speechManager.recognizedText.collect { recognized ->
                _uiState.value = _uiState.value.copy(inputText = recognized)
                processText(recognized)
            }
        }

        speechManager.initModel()
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun processText(text: String) {
        val result = phraseMatcher.matchPhrases(text)
        val gestures = result.resolved
        val unresolved = result.unresolved
        Log.d("MainScreenViewModel", "Text '$text' matched to ${gestures.size} gestures: ${gestures.map { it.id }}, unresolved: $unresolved")
        _uiState.value = _uiState.value.copy(
            resolvedGestures = gestures,
            unresolvedWords = unresolved,
            currentPlayingIndex = if (gestures.isNotEmpty()) 0 else -1,
            isPlaying = gestures.isNotEmpty()
        )
    }

    fun toggleSpeechRecognition() {
        val state = _uiState.value.speechState
        if (state == SpeechState.Listening) {
            speechManager.stopListening()
        } else if (state == SpeechState.Ready) {
            if (_uiState.value.permissionGranted) {
                speechManager.startListening()
            }
        }
    }

    fun setCurrentPlayingIndex(index: Int) {
        _uiState.value = _uiState.value.copy(
            currentPlayingIndex = index,
            isPlaying = index >= 0 && index < _uiState.value.resolvedGestures.size
        )
    }

    fun setPlaybackPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
