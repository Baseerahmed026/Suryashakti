package com.example.suryashakti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.suryashakti.databinding.ActivityMainBinding
import com.example.suryashakti.ui.DashboardFragment
import com.example.suryashakti.ui.InsightsFragment
import com.example.suryashakti.ui.LogFragment
import com.example.suryashakti.ui.ReportFragment
import com.example.suryashakti.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            binding.bottomNav.selectedItemId = R.id.nav_dashboard
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_log -> loadFragment(LogFragment())
                R.id.nav_report -> loadFragment(ReportFragment())
                R.id.nav_insights -> loadFragment(InsightsFragment())
                R.id.nav_settings -> loadFragment(SettingsFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}