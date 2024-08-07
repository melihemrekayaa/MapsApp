package com.example.mapsapp.webrtc.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.mapsapp.databinding.ActivityLoginBinding
import com.example.mapsapp.webrtc.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var  views: ActivityLoginBinding
    @Inject lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }


    private fun init(){
        views.apply {
            btn.setOnClickListener {
                mainRepository.login(
                    usernameEt.text.toString(),passwordEt.text.toString()
                ){ isDone, reason ->
                    if (!isDone){
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    }else{
                        //start moving to our main activity
                        startActivity(Intent(this@LoginActivity, WebRTCMainActivity::class.java).apply {
                            putExtra("username",usernameEt.text.toString())
                        })
                    }
                }
            }
        }
    }
}