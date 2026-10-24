package com.example.data.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PdfBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

import android.media.MediaPlayer
import android.net.Uri

data class StudyAlarmEvent(
    val book: PdfBook,
    val triggeredAt: Long = System.currentTimeMillis()
)

data class StudyAlarmSettings(
    val isEnabled: Boolean = false,
    val intervalMinutes: Int = 30,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val customAudioUri: String? = null,
    val customAudioName: String? = null,
    val nextTriggerAt: Long = 0L
)

class StudyAlarmManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("study_alarm_preferences", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var previewMediaPlayer: MediaPlayer? = null

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<StudyAlarmSettings> = _settings.asStateFlow()

    private val _activeAlarmEvent = MutableStateFlow<StudyAlarmEvent?>(null)
    val activeAlarmEvent: StateFlow<StudyAlarmEvent?> = _activeAlarmEvent.asStateFlow()

    private var availableBooksSupplier: (() -> List<PdfBook>)? = null

    companion object {
        const val CHANNEL_ID = "study_reminder_channel"
        const val NOTIFICATION_ID = 9182
    }

    init {
        createNotificationChannel()
        startPeriodicTicker()
    }

    fun setBooksSupplier(supplier: () -> List<PdfBook>) {
        this.availableBooksSupplier = supplier
    }

    private fun loadSettings(): StudyAlarmSettings {
        val enabled = prefs.getBoolean("alarm_enabled", false)
        val interval = prefs.getInt("interval_minutes", 30).coerceAtLeast(1)
        val sound = prefs.getBoolean("sound_enabled", true)
        val vibration = prefs.getBoolean("vibration_enabled", true)
        val customUri = prefs.getString("custom_audio_uri", null)?.takeIf { it.isNotBlank() }
        val customName = prefs.getString("custom_audio_name", null)?.takeIf { it.isNotBlank() }
        val nextTrigger = prefs.getLong("next_trigger_at", 0L)
        return StudyAlarmSettings(
            isEnabled = enabled,
            intervalMinutes = interval,
            soundEnabled = sound,
            vibrationEnabled = vibration,
            customAudioUri = customUri,
            customAudioName = customName,
            nextTriggerAt = nextTrigger
        )
    }

    fun updateSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        customAudioUri: String? = _settings.value.customAudioUri,
        customAudioName: String? = _settings.value.customAudioName
    ) {
        val validInterval = intervalMinutes.coerceAtLeast(1)
        val now = System.currentTimeMillis()
        val nextTrigger = if (enabled) {
            now + (validInterval * 60 * 1000L)
        } else {
            0L
        }

        prefs.edit()
            .putBoolean("alarm_enabled", enabled)
            .putInt("interval_minutes", validInterval)
            .putBoolean("sound_enabled", soundEnabled)
            .putBoolean("vibration_enabled", vibrationEnabled)
            .putString("custom_audio_uri", customAudioUri)
            .putString("custom_audio_name", customAudioName)
            .putLong("next_trigger_at", nextTrigger)
            .apply()

        _settings.value = StudyAlarmSettings(
            isEnabled = enabled,
            intervalMinutes = validInterval,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            customAudioUri = customAudioUri,
            customAudioName = customAudioName,
            nextTriggerAt = nextTrigger
        )

        if (enabled) {
            startPeriodicTicker()
        }
    }

    private fun startPeriodicTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(15_000L) // Checagem a cada 15 segundos
                val current = _settings.value
                if (current.isEnabled && current.nextTriggerAt > 0L) {
                    val now = System.currentTimeMillis()
                    if (now >= current.nextTriggerAt) {
                        val books = availableBooksSupplier?.invoke() ?: emptyList()
                        triggerAlarm(books)
                        // Agenda próximo
                        val next = now + (current.intervalMinutes * 60 * 1000L)
                        prefs.edit().putLong("next_trigger_at", next).apply()
                        _settings.value = current.copy(nextTriggerAt = next)
                    }
                }
            }
        }
    }

    fun triggerAlarm(books: List<PdfBook>) {
        if (books.isEmpty()) return

        // Escolhe livro aleatório, dando preferência aos que possuem leitura em andamento
        val chosenBook = selectRandomBook(books) ?: return

        // Toca som se configurado
        if (_settings.value.soundEnabled) {
            playAlarmSound()
        }

        // Vibra se configurado
        if (_settings.value.vibrationEnabled) {
            triggerVibration()
        }

        // Exibe Notificação do Android
        showSystemNotification(chosenBook)

        // Aciona evento em tela
        _activeAlarmEvent.value = StudyAlarmEvent(
            book = chosenBook,
            triggeredAt = System.currentTimeMillis()
        )
    }

    fun testTriggerNow(books: List<PdfBook>) {
        triggerAlarm(books)
    }

    private fun selectRandomBook(books: List<PdfBook>): PdfBook? {
        if (books.isEmpty()) return null
        // Filtrar livros não vazios
        val validBooks = books.filter { it.title.isNotBlank() }
        if (validBooks.isEmpty()) return books.first()

        // 70% de chance de pegar um livro já iniciado (onde parou na página > 1)
        val inProgress = validBooks.filter { it.currentPage > 1 }
        return if (inProgress.isNotEmpty() && Random.nextFloat() < 0.7f) {
            inProgress.random()
        } else {
            validBooks.random()
        }
    }

    private fun playAlarmSound() {
        try {
            stopSound()
            val customUriString = _settings.value.customAudioUri
            if (!customUriString.isNullOrBlank()) {
                try {
                    val parsedUri = Uri.parse(customUriString)
                    val mp = MediaPlayer().apply {
                        setDataSource(context, parsedUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                        }
                        isLooping = true
                        prepare()
                        start()
                    }
                    activeMediaPlayer = mp
                    return
                } catch (e: Exception) {
                    android.util.Log.e("StudyAlarmManager", "Failed to play custom alarm audio: ${e.message}")
                }
            }

            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            activeRingtone = RingtoneManager.getRingtone(context, alertUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200, 700)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(1000)
                }
            }
        } catch (_: Exception) {}
    }

    private fun showSystemNotification(book: PdfBook) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_OPEN_BOOK_ID", book.id)
                putExtra("EXTRA_OPEN_BOOK_PAGE", book.currentPage)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                book.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val pageInfo = if (book.currentPage > 1) {
                "Você parou na página ${book.currentPage} de ${book.totalPages}."
            } else {
                "Página ${book.currentPage} de ${book.totalPages}."
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Hora de Estudar: ${book.title}")
                .setContentText("Livro aleatório sorteado! $pageInfo Toque para continuar.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Livro selecionado: '${book.title}' (${book.author}). $pageInfo Mantenha seu ritmo de estudo consistente!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager?.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmes de Estudo Periódico",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações e alarmes periódicos para estudo de livros"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun dismissActiveAlarm() {
        stopSound()
        _activeAlarmEvent.value = null
    }

    fun testPlayAudio(uriString: String, onFinished: () -> Unit = {}) {
        stopAudioPreview()
        try {
            val parsedUri = Uri.parse(uriString)
            val mp = MediaPlayer().apply {
                setDataSource(context, parsedUri)
                setOnCompletionListener {
                    onFinished()
                    stopAudioPreview()
                }
                prepare()
                start()
            }
            previewMediaPlayer = mp
        } catch (e: Exception) {
            onFinished()
        }
    }

    fun stopAudioPreview() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
            previewMediaPlayer = null
        } catch (_: Exception) {}
    }

    private fun stopSound() {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (_: Exception) {}
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (_: Exception) {}
    }
}
