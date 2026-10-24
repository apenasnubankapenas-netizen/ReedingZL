package com.example.data.parser

import com.example.data.model.SubtitleCue
import com.example.data.model.WordTiming
import java.util.regex.Pattern

object SrtParser {
    private val TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

    fun parse(srtContent: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = srtContent.trim().split(Regex("\r?\n\r?\n+"))

        var indexCounter = 1
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            var lineIdx = 0
            var id = indexCounter
            if (lines[0].toIntOrNull() != null) {
                id = lines[0].toInt()
                lineIdx++
            }

            if (lineIdx >= lines.size) continue

            val timeMatcher = TIME_PATTERN.matcher(lines[lineIdx])
            if (timeMatcher.find()) {
                val startMs = parseTimeToMs(timeMatcher.group(1)!!, timeMatcher.group(2)!!, timeMatcher.group(3)!!, timeMatcher.group(4)!!)
                val endMs = parseTimeToMs(timeMatcher.group(5)!!, timeMatcher.group(6)!!, timeMatcher.group(7)!!, timeMatcher.group(8)!!)
                lineIdx++

                val textLines = lines.subList(lineIdx, lines.size)
                val fullText = textLines.joinToString(" ")

                val words = generateWordTimings(fullText, startMs, endMs)

                cues.add(
                    SubtitleCue(
                        index = id,
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        text = fullText,
                        words = words
                    )
                )
                indexCounter++
            }
        }
        return cues
    }

    fun parseSrt(srtContent: String): List<SubtitleCue> = parse(srtContent)

    fun exportSrt(cues: List<SubtitleCue>, offsetMs: Long = 0L): String {
        val sb = StringBuilder()
        cues.forEachIndexed { index, cue ->
            sb.append("${index + 1}\n")
            val adjStart = (cue.startTimeMs + offsetMs).coerceAtLeast(0L)
            val adjEnd = (cue.endTimeMs + offsetMs).coerceAtLeast(0L)
            sb.append("${formatTime(adjStart)} --> ${formatTime(adjEnd)}\n")
            sb.append("${cue.text}\n\n")
        }
        return sb.toString().trimEnd()
    }

    fun exportCapCutFormat(cues: List<SubtitleCue>, offsetMs: Long = 0L): String {
        val sb = StringBuilder()
        cues.forEachIndexed { _, cue ->
            val adjStart = (cue.startTimeMs + offsetMs).coerceAtLeast(0L)
            sb.append("[${formatTime(adjStart)}] ${cue.text}\n")
        }
        return sb.toString().trimEnd()
    }

    fun formatTimestamp(millis: Long): String = formatTime(millis)

    fun formatTime(millis: Long): String {
        val hours = millis / 3600_000
        val minutes = (millis % 3600_000) / 60_000
        val seconds = (millis % 60_000) / 1000
        val ms = millis % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, ms)
    }

    private fun parseTimeToMs(hours: String, minutes: String, seconds: String, millis: String): Long {
        return (hours.toLong() * 3600_000L) +
                (minutes.toLong() * 60_000L) +
                (seconds.toLong() * 1000L) +
                millis.toLong()
    }

    fun generateWordTimings(text: String, startMs: Long, endMs: Long): List<WordTiming> {
        val wordList = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (wordList.isEmpty()) return emptyList()

        val totalDuration = maxOf(endMs - startMs, 100L)
        val durationPerWord = totalDuration / wordList.size

        return wordList.mapIndexed { idx, word ->
            val wStart = startMs + (idx * durationPerWord)
            val wEnd = if (idx == wordList.lastIndex) endMs else wStart + durationPerWord
            WordTiming(word = word, startTimeMs = wStart, endTimeMs = wEnd)
        }
    }
}
