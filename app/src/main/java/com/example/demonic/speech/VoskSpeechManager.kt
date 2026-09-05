package com.example.demonic.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import com.google.gson.Gson

sealed interface SpeechState {
    object NotInitialized : SpeechState
    object Initializing : SpeechState
    object Ready : SpeechState
    object Listening : SpeechState
    data class Error(val message: String) : SpeechState
}

class VoskSpeechManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : RecognitionListener {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.NotInitialized)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>()
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private val gson = Gson()

    fun initModel() {
        if (_state.value != SpeechState.NotInitialized) return
        _state.value = SpeechState.Initializing

        Log.d("VoskSpeechManager", "Unpacking model...")
        StorageService.unpack(context, "vosk-model-en", "model",
            { loadedModel ->
                Log.d("VoskSpeechManager", "Model unpacked successfully")
                model = loadedModel
                _state.value = SpeechState.Ready
            },
            { exception ->
                Log.e("VoskSpeechManager", "Failed to unpack model", exception)
                _state.value = SpeechState.Error("Failed to load offline speech model: ${exception.localizedMessage}")
            }
        )
    }

    fun startListening() {
        val currentModel = model
        if (currentModel == null) {
            _state.value = SpeechState.Error("Model not initialized")
            return
        }

        if (_state.value == SpeechState.Listening) return

        try {
            val recognizer = Recognizer(currentModel, 16000.0f)
            val service = SpeechService(recognizer, 16000.0f)
            speechService = service
            _partialText.value = ""
            service.startListening(this)
            _state.value = SpeechState.Listening
            Log.d("VoskSpeechManager", "Speech recognition started")
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Failed to start speech service", e)
            _state.value = SpeechState.Error("Failed to start listening: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        if (_state.value != SpeechState.Listening) return

        speechService?.let {
            it.stop()
            speechService = null
            _state.value = SpeechState.Ready
            Log.d("VoskSpeechManager", "Speech recognition stopped")
        }
    }

    fun destroy() {
        stopListening()
        model?.close()
        model = null
        _state.value = SpeechState.NotInitialized
    }

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis == null) return
        try {
            val result = gson.fromJson(hypothesis, VoskPartialResult::class.java)
            _partialText.value = result.partial
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error parsing partial result", e)
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis == null) return
        try {
            val result = gson.fromJson(hypothesis, VoskResult::class.java)
            if (result.text.isNotBlank()) {
                scope.launch {
                    _recognizedText.emit(result.text)
                }
            }
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error parsing result", e)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (hypothesis == null) return
        try {
            val result = gson.fromJson(hypothesis, VoskResult::class.java)
            if (result.text.isNotBlank()) {
                scope.launch {
                    _recognizedText.emit(result.text)
                }
            }
            _partialText.value = ""
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error parsing final result", e)
        }
    }

    override fun onError(exception: Exception?) {
        Log.e("VoskSpeechManager", "Vosk Error", exception)
        _state.value = SpeechState.Error(exception?.localizedMessage ?: "Unknown speech error")
    }

    override fun onTimeout() {
        Log.d("VoskSpeechManager", "Speech recognition timeout")
        stopListening()
    }

    private data class VoskPartialResult(val partial: String)
    private data class VoskResult(val text: String)
}
