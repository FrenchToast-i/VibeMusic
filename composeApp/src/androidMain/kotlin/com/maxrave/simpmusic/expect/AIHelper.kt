package com.maxrave.simpmusic.expect

import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.ai.AIService
import com.maxrave.simpmusic.ai.ModelDownloader
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.io.File

private val aiService = AIService()
private val httpClient = HttpClient(CIO)
private val modelDownloader = ModelDownloader(httpClient)

actual suspend fun getAIRecommendation(prompt: String, aiProvider: String, apiKey: String, customBaseUrl: String?): String {
    return withContext(Dispatchers.IO) {
        when (aiProvider) {
            DataStoreManager.AI_PROVIDER_GEMINI -> {
                if (apiKey.isEmpty()) {
                    return@withContext "Please set your Gemini API key in settings"
                }
                try {
                    callGeminiAPI(prompt, apiKey)
                } catch (e: Exception) {
                    "Error calling Gemini API: ${e.message}"
                }
            }
            DataStoreManager.AI_PROVIDER_OPENAI, DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI -> {
                if (apiKey.isEmpty()) {
                    return@withContext "Please set your OpenAI API key in settings"
                }
                try {
                    callOpenAIAPI(prompt, apiKey, customBaseUrl)
                } catch (e: Exception) {
                    "Error calling OpenAI API: ${e.message}"
                }
            }
            else -> {
                // Local model
                val modelDir = File(System.getProperty("java.io.tmpdir"), "ai_models")
                val modelPath = modelDownloader.getModelPath(modelDir)
                
                if (modelPath == null) {
                    return@withContext "AI model not downloaded. Please download the model first."
                }
                
                if (!aiService.isModelReady()) {
                    val loaded = aiService.loadModel(modelPath)
                    if (!loaded) {
                        return@withContext "Failed to load AI model."
                    }
                }
                
                try {
                    aiService.generate(prompt)
                } catch (e: Exception) {
                    "Error generating recommendation: ${e.message}"
                }
            }
        }
    }
}

private suspend fun callGeminiAPI(prompt: String, apiKey: String): String {
    val requestBody = buildJsonObject {
        put("contents", buildJsonArray {
            add(buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("text", JsonPrimitive(prompt))
                    })
                })
            })
        })
    }
    
    val response = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(requestBody.toString())
    }
    val responseBody = response.body<String>()
    // Parse the response to extract the actual text
    try {
        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text")
            }
        }
        return responseBody
    } catch (e: Exception) {
        return responseBody
    }
}

private suspend fun callOpenAIAPI(prompt: String, apiKey: String, customBaseUrl: String?): String {
    val baseUrl = if (!customBaseUrl.isNullOrEmpty()) customBaseUrl else "https://api.openai.com/v1/chat/completions"
    val requestBody = buildJsonObject {
        put("model", JsonPrimitive("gpt-3.5-turbo"))
        put("messages", buildJsonArray {
            add(buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", JsonPrimitive(prompt))
            })
        })
    }
    
    val response = httpClient.post(baseUrl) {
        contentType(ContentType.Application.Json)
        headers {
            append("Authorization", "Bearer $apiKey")
        }
        setBody(requestBody.toString())
    }
    val responseBody = response.body<String>()
    // Parse the response to extract the actual text
    try {
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content")
        }
        return responseBody
    } catch (e: Exception) {
        return responseBody
    }
}
