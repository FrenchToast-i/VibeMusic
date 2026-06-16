package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.ai.AIService
import com.maxrave.simpmusic.ai.ModelDownloader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val aiService = AIService()
private val httpClient = HttpClient(CIO)
private val modelDownloader = ModelDownloader(httpClient)

actual suspend fun getAIRecommendation(prompt: String): String {
    return withContext(Dispatchers.IO) {
        // Get model directory
        val modelDir = File(System.getProperty("java.io.tmpdir"), "ai_models")
        
        // Check if model is downloaded
        val modelPath = modelDownloader.getModelPath(modelDir)
        
        if (modelPath == null) {
            return@withContext "AI model not downloaded. Please download the model first."
        }
        
        // Load model if not already loaded
        if (!aiService.isModelReady()) {
            val loaded = aiService.loadModel(modelPath)
            if (!loaded) {
                return@withContext "Failed to load AI model."
            }
        }
        
        // Generate recommendation
        try {
            aiService.generate(prompt)
        } catch (e: Exception) {
            "Error generating recommendation: ${e.message}"
        }
    }
}
