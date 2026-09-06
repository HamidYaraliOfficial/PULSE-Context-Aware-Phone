package com.pulse.app.core.di

/**
 * PULSE's core Context Inference Engine is 100% on-device and needs no AI
 * provider at all (see [com.pulse.app.context.ContextInferenceEngine]). This
 * interface exists purely as the extension point the spec asks for so a
 * future advanced feature (e.g. natural-language rule authoring) can plug
 * in either an on-device model or a Cloud API — without ever becoming a
 * dependency of the core detection pipeline. No implementation ships by
 * default; the default is [NoopAiProvider].
 */
interface AiProvider {
    suspend fun isAvailable(): Boolean
    suspend fun complete(prompt: String): Result<String>
}

class NoopAiProvider : AiProvider {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun complete(prompt: String): Result<String> =
        Result.failure(UnsupportedOperationException("No AI provider configured"))
}
