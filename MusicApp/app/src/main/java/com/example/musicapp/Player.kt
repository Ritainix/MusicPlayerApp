package com.example.musicapp

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.newSingleThreadContext
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

    private var updateseekbar: Thread? = null


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

        //udapte the song and bar
        mediaPlayer?.setOnCompletionListener {
            btnNext.performClick()
        }
        updateseekbar = Thread {
            try {
                val totalDuration = mediaPlayer?.duration ?: 0
                var currentPosition = 0

                while (mediaPlayer != null && currentPosition < totalDuration) {
                    try {
                        Thread.sleep(500) //0,5s
                        currentPosition = mediaPlayer!!.currentPosition
                        runOnUiThread {
                            seekMusic.progress = currentPosition
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        break
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        seekMusic.max = mediaPlayer?.duration ?: 0
        updateseekbar?.start()
        seekMusic.progressDrawable.setColorFilter(
            resources.getColor(R.color.primarycolor, null),
            android.graphics.PorterDuff.Mode.MULTIPLY
        )
        seekMusic.thumb.setColorFilter(
            resources.getColor(R.color.primarycolor, null),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //the user "updates" the song moving the bar
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.seekTo(seekBar?.progress ?: 0)

            }
        })

        val endTime = createTime(mediaPlayer?.duration ?: 0)
        txtStop.text = endTime

        val handler = Handler()
        val delay: Long = 1000

        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = createTime(mediaPlayer?.currentPosition ?: 0)
                txtStart.text = currentTime
                handler.postDelayed(this, delay)
            }
        }, delay)


        btnPlay.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                btnPlay.setBackgroundResource(R.drawable.play)
                mediaPlayer?.pause()
            } else {
                btnPlay.setBackgroundResource(R.drawable.pause)
                mediaPlayer?.start()
            }
        }

        btnNext.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            position = (position + 1) % mySongs!!.size
            val uri2= Uri.parse(mySongs?.get(position)?.toString())
            mediaPlayer = MediaPlayer.create(applicationContext, uri2)
            //remove the extensions name
            sname = mySongs!![position].name.substringBeforeLast(".")
            txtSName.text = sname

            txtSName.text = sname
            mediaPlayer?.start()
            btnPlay.setBackgroundResource(R.drawable.pause)
        }

        btnPrev.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            //to prevent -1
            position = if (position - 1 < 0) {
                mySongs!!.size - 1
            } else {
                position - 1
            }
            val uri2= Uri.parse(mySongs?.get(position)?.toString())
            mediaPlayer = MediaPlayer.create(applicationContext, uri2)
            //remove the extensions name
            sname = mySongs!![position].name.substringBeforeLast(".")
            txtSName.text = sname

            txtSName.text = sname
            mediaPlayer?.start()
            btnPlay.setBackgroundResource(R.drawable.pause)
        }

    }
    fun createTime(duration: Int): String {
        val min = duration / 1000 / 60
        val sec = duration / 1000 % 60
        return String.format("%d:%02d", min, sec)
    }


}
