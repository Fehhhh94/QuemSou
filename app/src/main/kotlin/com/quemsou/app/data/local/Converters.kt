package com.quemsou.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Conversores de tipo do Room. `List<String>` (as dicas de um card) é
 * persistida como JSON string via kotlinx.serialization.
 */
class Converters {

    @TypeConverter
    fun deListaDeStrings(lista: List<String>): String = Json.encodeToString(lista)

    @TypeConverter
    fun paraListaDeStrings(json: String): List<String> = Json.decodeFromString(json)
}
