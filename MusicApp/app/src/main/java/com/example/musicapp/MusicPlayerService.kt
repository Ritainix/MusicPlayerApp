package com.example.musicapp

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

import java.io.File

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()
    private var songList: ArrayList<File> = arrayListOf()
    private var position: Int = 0

    //static class
    companion object {
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
    }

    //the binder for the activity to get the service
    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    //return the binder
    override fun onBind(intent: Intent?): IBinder = binder

    //create de service
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    //start the service, in this case, foreground
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val action = intent?.action
        val songPath = intent?.getStringExtra("songPath")
        //gets the current position, if it's not exist gets the first song (0), even if it's null the currentPost will still be 0 (?:0)
        val currentPos = intent?.getIntExtra("currentPosition", 0) ?: 0
        val playlistPaths = intent?.getStringArrayListExtra("playlist")


        if (songPath != null) {
            if (playlistPaths != null) {
                songList = ArrayList(playlistPaths.map { File(it) })
            }
            playMusic(File(songPath), currentPos)
            return START_STICKY
        }

        when (action) {
            "ACTION_PLAY_PAUSE" -> togglePlayPause()
            "ACTION_NEXT" -> playNext()
            "ACTION_PREV" -> playPrev()
            else -> {
                //in case the action is not recognized
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Music Player")
                    .setContentText("Starting...")
                    .setSmallIcon(R.drawable.musiclogo)
                    .setOngoing(true)
                    .build()
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        return START_STICKY
    }



    fun setPlaylist(list: ArrayList<File>, pos: Int) {
        songList = list
        position = pos
    }

    private fun playMusic(file: File, startPos: Int = 0) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, android.net.Uri.fromFile(file))
        mediaPlayer?.isLooping = false
        mediaPlayer?.seekTo(startPos)
        mediaPlayer?.start()

        startForeground(NOTIFICATION_ID, createNotification(file.nameWithoutExtension))

        mediaPlayer?.setOnCompletionListener {
            playNext()
        }
    }

    private fun createNotification(songName: String): Notification {
        //open intent
        val openAppIntent = Intent(this, Player::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //next intent
        val nextIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_NEXT" }
        val nextPending = PendingIntent.getService(this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        //previous intent
        val prevIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_PREV" }
        val prevPending = PendingIntent.getService(this, 3, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        //playpause intent
        val playPauseIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_PLAY_PAUSE" }
        val playPausePending = PendingIntent.getService(this, 4, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        //buld the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            //.setContentTitle("Playing")
            .setContentText(songList.getOrNull(position)?.nameWithoutExtension ?: "")
            .setSmallIcon(R.drawable.musiclogo)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.previous_notification, "Previous", prevPending)
            .addAction(
                if (mediaPlayer?.isPlaying == true) R.drawable.pause_notification else R.drawable.play_notification,
                "Play/Pause",
                playPausePending
            )
            .addAction(R.drawable.next_notification, "Next", nextPending)
            .setStyle(MediaStyle().setShowActionsInCompactView(0,1,2))

            .setOngoing(true)
            .build()

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }

            startForeground(
                NOTIFICATION_ID,
                createNotification(songList.getOrNull(position)?.nameWithoutExtension ?: "")
            )
        }
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
        stopSelf()
    }

    fun playNext() {
        if (songList.isEmpty()) return
        position = (position + 1) % songList.size
        playMusic(songList[position])
    }

    fun playPrev() {
        if (songList.isEmpty()) return
        position = if(position - 1 < 0) songList.size - 1 else position - 1
        playMusic(songList[position])
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

//https://medium.com/@dugguRK/kotlin-music-foreground-service-play-on-android-4b57b10fe583