package com.example.synapse

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityHomeBinding
import com.example.synapse.databinding.TopBarBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var topBarBinding: TopBarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind the included top bar layout
        val topBarView: View = findViewById(R.id.topBar)
        topBarBinding = TopBarBinding.bind(topBarView)

        setupTopBar()
        setupFragments()
    }

    private fun setupTopBar() {
        topBarBinding.notificationBtn.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        topBarBinding.searchBtn.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, BroadGroupFragment())
            .commit()
    }
}
