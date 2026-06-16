package com.maxrave.simpmusic.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Model downloader for downloading GGUF models from HuggingFace
 */
class ModelDownloader(private val httpClient: HttpClient) {
    
    companion object {
        // Qwen 3 1.7B GGUF model URL (Q4_K_M quantization for mobile)
        private const val QWEN_MODEL_URL = "https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/qwen3-1.7b-instruct-q4_k_m.gguf"
        private const val MODEL_FILE_NAME = "qwen3-1.7b-instruct-q4_k_m.gguf"
        private const val MODEL_SIZE_BYTES = 1_200_000_000L // ~1.2GB
    }
    
    /**
     * Download progress state
     */
    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Float, val downloadedBytes: Long) : DownloadState()
        data class Success(val filePath: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
    
    /**
     * Download Qwen 3 1.7B model to specified directory
     * @param targetDir Directory to save the model
     * @return Flow of download states
     */
    fun downloadModel(targetDir: File): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)
        
        try {
            val modelFile = File(targetDir, MODEL_FILE_NAME)
            
            // Check if model already exists
            if (modelFile.exists() && modelFile.length() > 0) {
                emit(DownloadState.Success(modelFile.absolutePath))
                return@flow
            }
            
            // Create target directory if it doesn't exist
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // Download with progress tracking
            val response: HttpResponse = httpClient.get(QWEN_MODEL_URL)
            
            val contentLength = response.headers["Content-Length"]?.firstOrNull()?.toString()
            val totalBytes = contentLength?.toLongOrNull() ?: MODEL_SIZE_BYTES
            
            response.body<ByteArray>().let { data ->
                val tempFile = File(targetDir, "$MODEL_FILE_NAME.tmp")
                
                // Write to temp file first
                tempFile.writeBytes(data)
                
                // Rename to final file
                tempFile.renameTo(modelFile)
                
                emit(DownloadState.Success(modelFile.absolutePath))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error("Failed to download model: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if model is already downloaded
     * @param targetDir Directory where model would be stored
     * @return true if model exists and is valid
     */
    suspend fun isModelDownloaded(targetDir: File): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(targetDir, MODEL_FILE_NAME)
        modelFile.exists() && modelFile.length() > 100_000_000 // At least 100MB
    }
    
    /**
     * Get model file path
     * @param targetDir Directory where model is stored
     * @return Model file path or null if not found
     */
    fun getModelPath(targetDir: File): String? {
        val modelFile = File(targetDir, MODEL_FILE_NAME)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    /**
     * Delete downloaded model
     * @param targetDir Directory where model is stored
     * @return true if deletion successful
     */
    suspend fun deleteModel(targetDir: File): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(targetDir, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }
}
