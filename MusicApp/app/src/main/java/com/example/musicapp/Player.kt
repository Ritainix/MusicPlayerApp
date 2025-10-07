package com.example.musicapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class Player : AppCompatActivity() {

    private lateinit var btnPlay: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnShuffle: Button
    private var Shuffle = false

    private lateinit var txtSName: TextView
    private lateinit var txtStart: TextView
    private lateinit var txtStop: TextView

    private lateinit var seekMusic: SeekBar
    private var sname: String? = null
    private var position: Int = 0
    private var mySongs: ArrayList<File>? = null
    private var updateseekbar: Thread? = null

    // Service
    private var musicService: MusicPlayerService? = null
    private var isBound = false
    private var currentSongPath: String? = null
    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    // Connection to the service
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

        // Stop previous player
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
        playSong(position)

        // Seekbar
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
                    btnPlay.setBackgroundResource(R.drawable.play)
                } else {
                    it.start()
                    btnPlay.setBackgroundResource(R.drawable.pause)
                }
            }
        }

        btnNext.setOnClickListener {
            position = if (Shuffle) {
                (0 until mySongs!!.size).random()
            } else {
                (position + 1) % mySongs!!.size
            }
            playSong(position)
        }

        btnPrev.setOnClickListener {
            position = if (Shuffle) {
                (0 until mySongs!!.size).random()
            } else {
                if (position - 1 < 0) mySongs!!.size - 1 else position - 1
            }
            playSong(position)
        }

        btnShuffle.setOnClickListener {
            Shuffle = !Shuffle
            if (Shuffle) {
                btnShuffle.setBackgroundResource(R.drawable.shuffleon)
                Toast.makeText(this,"Shuffle turned on", Toast.LENGTH_SHORT).show()
            } else {
                btnShuffle.setBackgroundResource(R.drawable.shuffleoff)
                Toast.makeText(this,"Shuffle turned off", Toast.LENGTH_SHORT).show()
            }
        }

        // Bind service
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    private fun playSong(pos: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer=null

        val file = mySongs!![pos]
        val uri = Uri.fromFile(file)
        mediaPlayer = MediaPlayer.create(applicationContext, uri)
        mediaPlayer?.start()



        currentSongPath = file.absolutePath

        sname = file.name.substringBeforeLast(".")
        txtSName.text = sname
        btnPlay.setBackgroundResource(R.drawable.pause)

        val duration = mediaPlayer?.duration ?: 0
        txtStop.text = createTime(duration)
        seekMusic.progress = 0
        seekMusic.max = duration

        mediaPlayer?.setOnCompletionListener {
            position = if (Shuffle) {
                (0 until mySongs!!.size).random()
            } else {
                (position + 1) % mySongs!!.size
            }
            playSong(position)
        }
    }

    override fun onPause() {
        super.onPause()
        // Start service only if music is playing
        if (mediaPlayer?.isPlaying == true && currentSongPath != null) {
            val serviceIntent = Intent(this, MusicPlayerService::class.java)
            serviceIntent.putExtra("songPath", currentSongPath)
            serviceIntent.putExtra("currentPosition", mediaPlayer?.currentPosition ?: 0)
            startService(serviceIntent)
        }
        mediaPlayer?.let {
            if (it.isPlaying){
                it.pause()
                btnPlay.setBackgroundResource(R.drawable.play)
            }
        }


        }

    override fun onResume() {
        super.onResume()
        // Stop service when app comes to foreground
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        stopService(serviceIntent)

        // Stop MediaPlayer dentro do service explicitamente
        musicService?.stopMusic()

        // Resume playback in the app
        mediaPlayer?.start()
        btnPlay.setBackgroundResource(R.drawable.pause)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        musicService?.stopMusic()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
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
