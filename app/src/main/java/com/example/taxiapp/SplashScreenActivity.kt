package com.example.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val thread = object :Thread(){
            override fun run() {
                try{
                    sleep(2000)
                }
                catch (ex: Exception){
                    ex.printStackTrace()
                }
                finally {
                    val intent = Intent(this@SplashScreenActivity, ChooseModeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
        thread.start()
    }
}