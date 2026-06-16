package com.maxrave.simpmusic.expect

/**
 * Expect/actual interface for AI service integration
 * This allows common code to access platform-specific AI functionality
 */
expect fun getAIRecommendation(prompt: String): String
