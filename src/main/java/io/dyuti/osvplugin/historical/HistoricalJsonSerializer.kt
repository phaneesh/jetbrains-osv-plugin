package io.dyuti.osvplugin.historical

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * Simple JSON serializer/deserializer for [HistoricalScanRecord].
 */
object HistoricalJsonSerializer {
    private val gson: Gson =
        GsonBuilder()
            .registerTypeAdapter(OsVSeverity::class.java, SeverityAdapter())
            .create()

    fun serialize(record: HistoricalScanRecord): String = gson.toJson(record)

    fun deserialize(json: String): HistoricalScanRecord? =
        try {
            gson.fromJson(json, HistoricalScanRecord::class.java)
        } catch (_: Exception) {
            null
        }

    class SeverityAdapter : TypeAdapter<OsVSeverity>() {
        override fun write(
            out: JsonWriter,
            value: OsVSeverity?,
        ) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value.name)
            }
        }

        override fun read(reader: JsonReader): OsVSeverity? =
            try {
                val name = reader.nextString()
                OsVSeverity.entries.find { it.name.equals(name, ignoreCase = true) }
            } catch (_: Exception) {
                reader.skipValue()
                null
            }
    }
}
