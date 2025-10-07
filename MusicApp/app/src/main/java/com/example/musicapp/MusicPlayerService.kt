package com.example.musicapp

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()

    companion object {
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songPath = intent?.getStringExtra("songPath")
        if (songPath != null) {
            playMusic(File(songPath))
        }
        return START_STICKY
    }

    private fun playMusic(file: File) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, android.net.Uri.fromFile(file))
        mediaPlayer?.isLooping = false
        mediaPlayer?.start()

        startForeground(NOTIFICATION_ID, createNotification(file.nameWithoutExtension))
    }

    private fun createNotification(songName: String): Notification {
        val notificationIntent = Intent(this, Player::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("A reproduzir música")
            .setContentText(songName)
            .setSmallIcon(R.drawable.musiclogo) // usa um ícone que tenhas
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Música",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        mediaPlayer?.start()
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

//https://medium.com/@dugguRK/kotlin-music-foreground-service-play-on-android-4b57b10fe583