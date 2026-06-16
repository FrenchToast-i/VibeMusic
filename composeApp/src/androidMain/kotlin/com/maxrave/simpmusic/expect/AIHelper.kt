package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.ai.AIService
import com.maxrave.simpmusic.ai.ModelDownloader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File

private val aiService = AIService()
private val httpClient = HttpClient(CIO)
private val modelDownloader = ModelDownloader(httpClient)

actual fun getAIRecommendation(prompt: String): String {
    // Get model directory
    val modelDir = File(System.getProperty("java.io.tmpdir"), "ai_models")
    
    // Check if model is downloaded
    val modelPath = modelDownloader.getModelPath(modelDir)
    
    if (modelPath == null) {
        return "AI model not downloaded. Please download the model first."
    }
    
    // Load model if not already loaded
    if (!aiService.isModelReady()) {
        return "AI model not loaded. Please load the model first."
    }
    
    // Generate recommendation
    return try {
        val params = mapOf(
            "temperature" to 0.7f,
            "maxTokens" to 512,
            "topP" to 0.95f,
            "topK" to 40,
            "repeatPenalty" to 1.1f,
        )
        aiService.generate(prompt, params)
    } catch (e: Exception) {
        "Error generating recommendation: ${e.message}"
    }
}
