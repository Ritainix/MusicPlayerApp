package com.example.musicapp

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
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
        val currentPos = intent?.getIntExtra("currentPosition", 0) ?: 0
        val playlistPaths = intent?.getStringArrayListExtra("playlist")
        val isPaused = intent?.getBooleanExtra("paused", false) ?: false


        //if there is a song coming from the player
        if (songPath != null) {
            if (playlistPaths != null) {
                songList = ArrayList(playlistPaths.map { File(it) })
            }

            //gets the song position
            position = songList.indexOfFirst { it.absolutePath == songPath }
            if (position == -1) position = 0
            if (isPaused) {
                // criar notificação mas não tocar
                mediaPlayer = MediaPlayer.create(this, android.net.Uri.fromFile(File(songPath)))
                mediaPlayer?.seekTo(currentPos)
                // NÃO FAZ start()
                startForeground(NOTIFICATION_ID, createNotification(File(songPath).nameWithoutExtension))
            } else {
                playMusic(File(songPath), currentPos) // toca normalmente
            }

            return START_STICKY //START_Sticky is from service, it tells to recreate the service after it has enough memory and call onStartCommand() again with a null intent.
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
                    .setSmallIcon(R.drawable.musicicone_ritainix)
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
        mediaPlayer?.setOnCompletionListener { playNext() }
    }

    private fun createNotification(songName: String): Notification {

        val currentFile = songList.getOrNull(position) //in case the action is not recognized
        val openAppIntent = Intent(this, Player::class.java).apply {
            putExtra("songPath", currentFile?.absolutePath)
            putExtra("currentPosition", mediaPlayer?.currentPosition ?: 0)
            putExtra("pos", position)
            putStringArrayListExtra("songs", ArrayList(songList.map { it.absolutePath }))
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val controls = RemoteViews(packageName, R.layout.notification_controls)
        controls.setTextViewText(R.id.txtTitle, songName)

        //previous intent
        controls.setOnClickPendingIntent(
            R.id.btnPrev,
            PendingIntent.getService(this, 3, Intent(this, MusicPlayerService::class.java).apply {
                action = "ACTION_PREV"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // Play/Pause
        controls.setOnClickPendingIntent(
            R.id.btnPlayPause,
            PendingIntent.getService(this, 4, Intent(this, MusicPlayerService::class.java).apply {
                action = "ACTION_PLAY_PAUSE"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        //next intent
        controls.setOnClickPendingIntent(
            R.id.btnNext,
            PendingIntent.getService(this, 2, Intent(this, MusicPlayerService::class.java).apply {
                action = "ACTION_NEXT"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val playIcon = if (mediaPlayer?.isPlaying == true)
            R.drawable.pausenotification_ritainix
        else
            R.drawable.playnotification_ritainix

        controls.setImageViewResource(R.id.btnPlayPause, playIcon)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.musicicone_ritainix)
            .setContentTitle("")
            .setCustomContentView(controls)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }




    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //only creates the channel if it's >=Android 8
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Channel", NotificationManager.IMPORTANCE_LOW //IMPORTANCE_LOW is used because if's not an alert notification, so it doesn't make a sound or vibrate
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            startForeground(NOTIFICATION_ID, createNotification(songList.getOrNull(position)?.nameWithoutExtension ?: ""))
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
        startForeground(NOTIFICATION_ID, createNotification(songList[position].nameWithoutExtension))
    }

    fun playPrev() {
        if (songList.isEmpty()) return
        position = if (position - 1 < 0) songList.size - 1 else position - 1
        playMusic(songList[position])
        startForeground(NOTIFICATION_ID, createNotification(songList[position].nameWithoutExtension))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun getSongList(): ArrayList<File> = songList

    fun getCurrentPosition(): Int = position

    fun getMediaPlayer(): MediaPlayer? = mediaPlayer

}


//https://medium.com/@dugguRK/kotlin-music-foreground-service-play-on-android-4b57b10fe583