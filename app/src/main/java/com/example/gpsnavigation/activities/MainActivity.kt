package com.example.gpsnavigation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.R
import com.example.gpsnavigation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var biniding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biniding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(biniding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        biniding.compass.setOnClickListener {
            val intent = Intent(this, CompassActivity::class.java)
            startActivity(intent)
        }
    }
}