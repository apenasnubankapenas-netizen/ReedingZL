package com.example.data.model

data class WordTiming(
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    val startMs: Long get() = startTimeMs
    val endMs: Long get() = endTimeMs

    fun isActiveAt(timeMs: Long, offsetMs: Long = 0L): Boolean {
        val actualStart = startTimeMs + offsetMs
        val actualEnd = endTimeMs + offsetMs
        return timeMs in actualStart..actualEnd
    }
}

data class SubtitleCue(
    val index: Int = 1,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 5000L,
    val text: String = "",
    val words: List<WordTiming> = emptyList(),
    val id: Long = index.toLong(),
    val translation: String? = null,
    val highlightColor: String? = null,
    val note: String? = null
) {
    val cueIndex: Int get() = index
    val startMs: Long get() = startTimeMs
    val endMs: Long get() = endTimeMs

    fun isActiveAt(timeMs: Long, offsetMs: Long = 0L): Boolean {
        val actualStart = startTimeMs + offsetMs
        val actualEnd = endTimeMs + offsetMs
        return timeMs in actualStart..actualEnd
    }

    fun getAdjustedStart(offsetMs: Long = 0L): Long = startTimeMs + offsetMs

    fun getAdjustedEnd(offsetMs: Long = 0L): Long = endTimeMs + offsetMs

    fun get3WordChunks(): List<List<WordTiming>> {
        if (words.isEmpty()) {
            val generated = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (generated.isEmpty()) return emptyList()
            val dur = maxOf(endTimeMs - startTimeMs, 100L) / generated.size
            val timings = generated.mapIndexed { i, w ->
                WordTiming(w, startTimeMs + (i * dur), startTimeMs + ((i + 1) * dur))
            }
            return timings.chunked(3)
        }
        return words.chunked(3)
    }

    fun getActive3WordChunk(currentPositionMs: Long, offsetMs: Long = 0L): List<WordTiming>? {
        val chunks = get3WordChunks()
        return chunks.firstOrNull { chunk ->
            val chunkStart = (chunk.firstOrNull()?.startTimeMs ?: startTimeMs) + offsetMs
            val chunkEnd = (chunk.lastOrNull()?.endTimeMs ?: endTimeMs) + offsetMs
            currentPositionMs in chunkStart..chunkEnd
        } ?: chunks.firstOrNull()
    }

    fun formatTimestamp(timestampMs: Long = startTimeMs): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (timestampMs % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, millis)
    }

    companion object {
        fun formatTimestamp(timestampMs: Long): String {
            val totalSeconds = timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (timestampMs % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, millis)
        }
    }
}
