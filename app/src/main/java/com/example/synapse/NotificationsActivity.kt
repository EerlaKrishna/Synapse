package com.example.synapse


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.synapse.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup notifications view logic here
    }
}
