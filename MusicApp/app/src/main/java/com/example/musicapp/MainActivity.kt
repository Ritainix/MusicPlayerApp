package com.example.musicapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

import java.io.File

//REFERENCE FOR THIS PROJECT:   Coding with Evan on Youtube

class MainActivity : AppCompatActivity() {

    lateinit var listMusic: ListView
    var items: Array<String> = emptyArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listMusic = findViewById(R.id.listViewMusics)

        permissions()
    }

    private fun permissions() {
        Dexter.withContext(this)
            .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    listSongs()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
    }

    private fun findSong(file: File): List<File> {
        return file.walkTopDown()
            .filter { it.isFile && (it.extension == "mp3" || it.extension == "wav") }
            .toList()
    }

    private fun listSongs() {
        val mySongs = findSong(Environment.getExternalStorageDirectory())
        items = Array(mySongs.size) { i ->
            mySongs[i].name.replace(".mp3", "").replace(".wav", "")
        }

        val customAdapter = CustomAdapter(this, items)
        listMusic.adapter = customAdapter

        val songList = ArrayList(mySongs.map { it.absolutePath })
        listMusic.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(applicationContext, Player::class.java).apply {
                putStringArrayListExtra("songs", songList)
                putExtra("songname", items[position])
                putExtra("pos", position)
            }
            startActivity(intent)
        }


    }


    class CustomAdapter(
        private val context: Context,
        private val songs: Array<String>
    ) : BaseAdapter() {

        override fun getCount(): Int = songs.size

        override fun getItem(position: Int): Any = songs[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

            val textSong = view.findViewById<TextView>(R.id.SongName)
            textSong.isSelected = true
            textSong.text = songs[position]

            return view
        }
    }

}


