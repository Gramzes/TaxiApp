package com.example.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taxiapp.databinding.ActivityChooseModeBinding

class ChooseModeActivity : AppCompatActivity() {
    lateinit var binding: ActivityChooseModeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userBrn.setOnClickListener {
            val intent = Intent(this, UserSignInActivity::class.java)
            startActivity(intent)
        }
        binding.driverBtn.setOnClickListener {
            val intent = Intent(this, DriverSignInActivity::class.java)
            startActivity(intent)
        }
    }
}