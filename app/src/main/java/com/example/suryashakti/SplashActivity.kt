package com.example.suryashakti

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.suryashakti.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var progress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sun pulse animation
        animateSun()

        // Smooth progress bar
        animateProgress()

        // Navigate after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }

    private fun animateSun() {
        binding.ivSun.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(800)
            .withEndAction {
                binding.ivSun.animate()
                    .scaleX(1.0f).scaleY(1.0f)
                    .setDuration(800)
                    .withEndAction { animateSun() }
                    .start()
            }.start()
    }

    private fun animateProgress() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    binding.progressBar.progress = progress
                    progress += 2
                    handler.postDelayed(this, 55)
                }
            }
        }
        handler.post(runnable)
    }
}