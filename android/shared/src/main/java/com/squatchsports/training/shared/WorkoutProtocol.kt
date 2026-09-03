package com.squatchsports.training.shared

import org.json.JSONObject

object WorkoutPaths {
    const val WORKOUT_STARTED = "/squatch/workout/started"
    const val WORKOUT_STOPPED = "/squatch/workout/stopped"
    const val SHOT = "/squatch/workout/shot"
    const val POSITION = "/squatch/workout/position"
    const val DRILL_INFO = "/squatch/workout/drill"
}

object WorkoutPayloads {
    fun shot(code: Int): ByteArray = code.toString().encodeToByteArray()

    fun decodeShot(data: ByteArray): Int? = data.decodeToString().toIntOrNull()

    fun drill(name: String): ByteArray = name.encodeToByteArray()

    fun decodeDrill(data: ByteArray): String = data.decodeToString()

    fun position(position: CourtPosition): ByteArray = JSONObject()
        .put("row", position.row)
        .put("column", position.column)
        .put("name", position.name ?: JSONObject.NULL)
        .put("rowPercent", position.rowPercent)
        .put("columnPercent", position.columnPercent)
        .toString()
        .encodeToByteArray()

    fun decodePosition(data: ByteArray): CourtPosition? = runCatching {
        val json = JSONObject(data.decodeToString())
        CourtPosition(
            row = json.getInt("row"),
            column = json.getInt("column"),
            name = if (json.isNull("name")) null else json.getString("name"),
            rowPercent = json.getDouble("rowPercent"),
            columnPercent = json.getDouble("columnPercent"),
        )
    }.getOrNull()
}
