package com.example.musicapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class Player : AppCompatActivity() {

    private lateinit var btnPlay: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnShuffle: ImageButton
    private var Shuffle = false
    private lateinit var txtSName: TextView
    private lateinit var txtStart: TextView
    private lateinit var txtStop: TextView
    private lateinit var seekMusic: SeekBar
    private var sname: String? = null
    private var position: Int = 0
    private var mySongs: ArrayList<File>? = null
    private var updateseekbar: Thread? = null

    private var musicService: MusicPlayerService? = null
    private var isBound = false
    private var currentSongPath: String? = null

    companion object { var mediaPlayer: MediaPlayer? = null }

    //connect/disconnect the service
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnPrev = findViewById(R.id.previousButton)
        btnNext = findViewById(R.id.nextButton)
        btnPlay = findViewById(R.id.playButton)
        btnShuffle = findViewById(R.id.shuffleButton)
        txtSName = findViewById(R.id.txtsn)
        txtStart = findViewById(R.id.txtsstart)
        txtStop = findViewById(R.id.txtsend)
        seekMusic = findViewById(R.id.seekbar)

        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        val intent = intent
        val songList = intent.getStringArrayListExtra("songs")
        val songName = intent.getStringExtra("songname")
        position = intent.getIntExtra("pos", 0)
        mySongs = songList?.map { File(it) } as ArrayList<File>?
        sname = songName
        txtSName.text = sname
        txtSName.isSelected = true

        if (intent.hasExtra("songPath")) {
            val restoredPath = intent.getStringExtra("songPath")
            val restoredPos = intent.getIntExtra("currentPosition", 0)
            val songsList = intent.getStringArrayListExtra("songs")
            if (restoredPath != null && songsList != null) {
                mySongs = songsList.map { File(it) } as ArrayList<File>
                val index = mySongs!!.indexOfFirst { it.absolutePath == restoredPath }
                if (index != -1) {
                    position = index
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(applicationContext, Uri.fromFile(mySongs!![position]))
                    mediaPlayer?.seekTo(restoredPos)
                    mediaPlayer?.start()
                    sname = mySongs!![position].name.substringBeforeLast(".")
                    txtSName.text = sname
                    val duration = mediaPlayer?.duration ?: 0
                    txtStop.text = createTime(duration)
                    seekMusic.max = duration
                }
                return
            }
        }

        playSong(position)

        //Seekbar update
        val handler = Handler()
        val delay: Long = 1000
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = createTime(mediaPlayer?.currentPosition ?: 0)
                txtStart.text = currentTime
                seekMusic.progress = mediaPlayer?.currentPosition ?: 0
                handler.postDelayed(this, delay)
            }
        }, delay)

        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.seekTo(seekBar?.progress ?: 0)
            }
        })

        btnPlay.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlay.setBackgroundResource(R.drawable.play_ritainix)
                } else {
                    it.start()
                    btnPlay.setBackgroundResource(R.drawable.pause_ritainix)
                }
            }
        }

        btnNext.setOnClickListener {
            position = if (Shuffle) (0 until mySongs!!.size).random() else (position + 1) % mySongs!!.size
            playSong(position)
        }

        btnPrev.setOnClickListener {
            position = if (Shuffle) (0 until mySongs!!.size).random() else if (position - 1 < 0) mySongs!!.size - 1 else position - 1
            playSong(position)
        }

        btnShuffle.setOnClickListener {
            Shuffle = !Shuffle
            if (Shuffle) {
                btnShuffle.setBackgroundResource(R.drawable.shuffleon_ritainix)
                Toast.makeText(this,"Shuffle turned on", Toast.LENGTH_SHORT).show()
            } else {
                btnShuffle.setBackgroundResource(R.drawable.shuffleoff_ritainix)
                Toast.makeText(this,"Shuffle turned off", Toast.LENGTH_SHORT).show()
            }
        }

        //Bind service
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    private fun playSong(pos: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        val file = mySongs!![pos]
        mediaPlayer = MediaPlayer.create(applicationContext, Uri.fromFile(file))
        mediaPlayer?.start()
        currentSongPath = file.absolutePath
        sname = file.name.substringBeforeLast(".")
        txtSName.text = sname
        btnPlay.setBackgroundResource(R.drawable.pause_ritainix)
        val duration = mediaPlayer?.duration ?: 0 //gets the total time of the music in ms
        txtStop.text = createTime(duration)
        seekMusic.progress = 0 //initial
        seekMusic.max = duration //defines the duration as max (end)
        mediaPlayer?.setOnCompletionListener {
            position = if (Shuffle) (0 until mySongs!!.size).random() else (position + 1) % mySongs!!.size
            playSong(position)
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.let {
            if (it.isPlaying) {
                //pause the activity
                it.pause()
                btnPlay.setBackgroundResource(R.drawable.play_ritainix)

                //pass the music to the service
                val serviceIntent = Intent(this, MusicPlayerService::class.java)
                serviceIntent.putExtra("songPath", currentSongPath)
                serviceIntent.putExtra("currentPosition", it.currentPosition)
                serviceIntent.putExtra("pos", position)
                serviceIntent.putStringArrayListExtra(
                    "playlist",
                    ArrayList(mySongs?.map { it.absolutePath } ?: emptyList())
                )


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        musicService?.let { service ->

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            // Usa os getters públicos do Service
            val currentFile = service.getSongList().getOrNull(service.getCurrentPosition())
            if (currentFile != null) {
                mediaPlayer = MediaPlayer.create(applicationContext, Uri.fromFile(currentFile))
                mediaPlayer?.seekTo(service.getMediaPlayer()?.currentPosition ?: 0)
                mediaPlayer?.start()

                sname = currentFile.name.substringBeforeLast(".")
                txtSName.text = sname
                txtStart.text = createTime(mediaPlayer?.currentPosition ?: 0)
                val duration = mediaPlayer?.duration ?: 0
                txtStop.text = createTime(duration)
                seekMusic.max = duration
                seekMusic.progress = mediaPlayer?.currentPosition ?: 0
                btnPlay.setBackgroundResource(R.drawable.pause_ritainix)

                position = service.getCurrentPosition()
                currentSongPath = currentFile.absolutePath
            }

            service.stopMusic()
        }

        stopService(Intent(this, MusicPlayerService::class.java))
    }



    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        musicService?.stopMusic()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
            mediaPlayer = null
        }
        stopService(Intent(this, MusicPlayerService::class.java))
    }

    private fun createTime(duration: Int): String {
        val min = duration / 1000 / 60
        val sec = duration / 1000 % 60
        return String.format("%d:%02d", min, sec)
    }
}
