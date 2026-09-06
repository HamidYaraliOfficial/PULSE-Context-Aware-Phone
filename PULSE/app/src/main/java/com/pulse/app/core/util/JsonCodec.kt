package com.pulse.app.core.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room entities in PULSE store structured values (condition trees, action
 * param maps, signal contribution lists, weekly schedules) as JSON text
 * columns rather than normalized child tables where the shape is a tree or
 * a small ad-hoc bag of fields — see the design note in
 * `database/entities/ContextEntities.kt`. This object centralizes that
 * encode/decode so every repository uses the same lenient, forward-
 * compatible [Json] configuration.
 */
object JsonCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    inline fun <reified T> decode(text: String?, default: T): T =
        if (text.isNullOrBlank()) default else runCatching { json.decodeFromString<T>(text) }.getOrDefault(default)
}
