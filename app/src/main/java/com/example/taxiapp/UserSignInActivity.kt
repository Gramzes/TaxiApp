package com.example.taxiapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taxiapp.databinding.ActivityUserSignInBinding
import android.util.Patterns
import android.view.View

class UserSignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSignInBinding
    private var isSignInMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInBtn.setOnClickListener {
            if (isSignInMode){
                signInUser()
            }
            else{
                signUpUser()
            }
        }

        binding.signUpTextView.setOnClickListener {
            if (isSignInMode) {
                isSignInMode = false
                with(binding) {
                    signInBtn.text = resources.getString(R.string.sign_up)
                    signUpTextView.text = resources.getString(R.string.sign_in)
                    nameEditText.visibility = View.VISIBLE
                    confPasswordEditText.visibility = View.VISIBLE
                }
            }
            else{
                isSignInMode = true
                with(binding) {
                    signInBtn.text = resources.getString(R.string.sign_in)
                    signUpTextView.text = resources.getString(R.string.sign_up)
                    nameEditText.visibility = View.GONE
                    confPasswordEditText.visibility = View.GONE
                }
            }
        }
    }


    private fun validateEmail(): Boolean{
        val email = binding.emailEditText.editText?.text.toString().trim()
        return if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.emailEditText.apply {
                isErrorEnabled = true
                error = "Incorrect email format"
            }
            false
        } else{
            binding.emailEditText.apply {
                isErrorEnabled = false
            }
            true
        }
    }

    private fun validatePassword(): Boolean{
        val pass = binding.passwordEditText.editText?.text.toString().trim()
        val confPass = binding.confPasswordEditText.editText?.text.toString().trim()
        return if (pass.length < 6){
            binding.passwordEditText.apply {
                isErrorEnabled = true
                error = "Password must be >5 characters"
            }
            false
        }
        else if (pass != confPass){
            binding.passwordEditText.apply {
                isErrorEnabled = true
                error = "Password mismatch"
            }
            binding.confPasswordEditText.apply {
                isErrorEnabled = true
                error = "Password mismatch"
            }
            false
        }
        else{
            binding.confPasswordEditText.isErrorEnabled = false
            binding.passwordEditText.isErrorEnabled = false
            true
        }
    }

    private fun validateUserName(): Boolean{
        val userName = binding.nameEditText.editText?.text.toString().trim()
        return if(userName.isEmpty()){
            binding.nameEditText.apply {
                isErrorEnabled = true
                error = "Input your name"
            }
            false
        }
        else{
            binding.nameEditText.isErrorEnabled = false
            true
        }
    }

    private fun signInUser() {

    }

    private fun signUpUser(){
        val isMailVal = validateEmail()
        val isNameVal = validateUserName()
        val isPasVal = validatePassword()

        if (isMailVal && isNameVal && isPasVal){

        }
    }
}