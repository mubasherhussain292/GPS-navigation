package com.example.gpsnavigation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.gpsnavigation.R
import com.example.gpsnavigation.databinding.ActivityOnBoardingBinding
import com.example.gpsnavigation.models.OnboardingItem

class OnBoardingActivity : AppCompatActivity() {
    lateinit var binding : ActivityOnBoardingBinding
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val items = listOf(
            OnboardingItem(R.drawable.ic_launcher_background, "Title 1", "Description 1"),
            OnboardingItem(R.drawable.ic_launcher_background, "Title 2", "Description 2"),
            OnboardingItem(R.drawable.ic_launcher_background, "Title 3", "Description 3")
        )

        viewPager.adapter = OnboardingAdapter(items)

        binding.btnNext.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) {
                viewPager.currentItem += 1
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
