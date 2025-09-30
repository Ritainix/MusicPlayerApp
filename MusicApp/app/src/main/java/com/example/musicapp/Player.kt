package com.example.musicapp

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

//REFERENCE FOR THIS PROJECT:   Coding with Evan on Youtube
class Player : AppCompatActivity() {

    private lateinit var btnPlay: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnFf: Button
    private lateinit var btnFr: Button

    private lateinit var txtSName: TextView
    private lateinit var txtStart: TextView
    private lateinit var txtStop: TextView

    private lateinit var seekMusic: SeekBar
    //private lateinit var visualizer: BarVisualizer

    private var sname: String? = null
    private var position: Int = 0
    private var mySongs: ArrayList<File>? = null

    companion object {
        var mediaPlayer: MediaPlayer? = null
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
        btnFf = findViewById(R.id.fastforwardButton)
        btnFr = findViewById(R.id.previousforwardButton)

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


        val uri = Uri.parse(mySongs?.get(position)?.toString())
        mediaPlayer = MediaPlayer.create(applicationContext, uri)
        mediaPlayer?.start()
        btnPlay.setBackgroundResource(R.drawable.pause)


        btnPlay.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                btnPlay.setBackgroundResource(R.drawable.play)
                mediaPlayer?.pause()
            } else {
                btnPlay.setBackgroundResource(R.drawable.pause)
                mediaPlayer?.start()
            }
        }
    }
}
