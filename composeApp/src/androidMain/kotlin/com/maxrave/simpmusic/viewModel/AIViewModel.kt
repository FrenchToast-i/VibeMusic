package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.ai.AIService
import com.maxrave.simpmusic.ai.ModelDownloader
import com.maxrave.simpmusic.ai.ModelDownloader.DownloadState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for managing AI model and inference
 */
class AIViewModel : ViewModel() {
    private val httpClient = HttpClient(CIO)
    private val modelDownloader = ModelDownloader(httpClient)
    private val aiService = AIService()
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
    
    private val _generatedText = MutableStateFlow("")
    val generatedText: StateFlow<String> = _generatedText.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _modelInfo = MutableStateFlow<Map<String, Any>>(emptyMap())
    val modelInfo: StateFlow<Map<String, Any>> = _modelInfo.asStateFlow()
    
    /**
     * Get model storage directory
     */
    private fun getModelDir(): File {
        return File(System.getProperty("java.io.tmpdir"), "ai_models")
    }
    
    /**
     * Download the AI model
     */
    fun downloadModel() {
        viewModelScope.launch {
            modelDownloader.downloadModel(getModelDir()).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Success) {
                    // Auto-load model after download
                    loadModel(state.filePath)
                }
            }
        }
    }
    
    /**
     * Load the AI model
     */
    fun loadModel(modelPath: String? = null) {
        viewModelScope.launch {
            val path = modelPath ?: modelDownloader.getModelPath(getModelDir())
            if (path != null) {
                val success = aiService.loadModel(path)
                _isModelLoaded.value = success
                _modelInfo.value = aiService.getModelInfo()
            }
        }
    }
    
    /**
     * Generate text
     */
    fun generateText(prompt: String, params: Map<String, Any>? = null) {
        viewModelScope.launch {
            if (!aiService.isModelReady()) {
                _generatedText.value = "Model not loaded. Please download and load the model first."
                return@launch
            }
            
            _isGenerating.value = true
            try {
                val result = aiService.generate(prompt, params)
                _generatedText.value = result
            } catch (e: Exception) {
                _generatedText.value = "Error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Get default generation parameters
     */
    private fun getDefaultParams(): Map<String, Any> {
        return mapOf(
            "temperature" to 0.7f,
            "maxTokens" to 512,
            "topP" to 0.95f,
            "topK" to 40,
            "repeatPenalty" to 1.1f,
            "contextLength" to 4096,
            "numThreads" to 4,
            "useMmap" to true,
            "flashAttention" to false,
            "gpuLayers" to 99,
        )
    }
    
    /**
     * Check if model is downloaded
     */
    fun checkModelDownloaded() {
        viewModelScope.launch {
            val downloaded = modelDownloader.isModelDownloaded(getModelDir())
            if (downloaded) {
                _downloadState.value = DownloadState.Success(
                    modelDownloader.getModelPath(getModelDir()) ?: ""
                )
            }
        }
    }
    
    /**
     * Unload model
     */
    fun unloadModel() {
        aiService.unloadModel()
        _isModelLoaded.value = false
        _modelInfo.value = emptyMap()
    }
    
    /**
     * Delete downloaded model
     */
    fun deleteModel() {
        viewModelScope.launch {
            unloadModel()
            modelDownloader.deleteModel(getModelDir())
            _downloadState.value = DownloadState.Idle
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        aiService.unloadModel()
        httpClient.close()
    }
}
