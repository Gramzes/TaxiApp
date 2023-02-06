package com.example.taxiapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.viewModels
import com.example.taxiapp.databinding.ActivityDriverSignInBinding
import com.example.taxiapp.viewmodel.DriverSignInViewModel

class DriverSignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverSignInBinding
    private var isSignInMode = true
    private val viewModel: DriverSignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()

        binding.signInBtn.setOnClickListener {
            if (isSignInMode){
                signInUser()
            }
            else{
                signUpUser()
            }
        }

        binding.signUpTextView.setOnClickListener {
            isSignInMode = !isSignInMode
            changeUI(isSignInMode)
        }
    }

    fun saveData(){
        with(binding){
            viewModel.isSignInMode = isSignInMode
            viewModel.email = emailEditText.editText!!.text.toString()
            viewModel.name = nameEditText.editText!!.text.toString()
            viewModel.password = passwordEditText.editText!!.text.toString()
            viewModel.confPass = confPasswordEditText.editText!!.text.toString()
        }
    }
    override fun onStop() {
        saveData()
        super.onStop()
    }

    fun initUI(){
        with(binding){
            emailEditText.editText?.setText(viewModel.email)
            nameEditText.editText?.setText(viewModel.name)
            passwordEditText.editText?.setText(viewModel.password)
            confPasswordEditText.editText?.setText(viewModel.confPass)
            isSignInMode = viewModel.isSignInMode
            changeUI(isSignInMode)
        }
    }

    fun changeUI(isSignInMode: Boolean){
        if (!isSignInMode) {
            with(binding) {
                signInBtn.text = resources.getString(R.string.sign_up)
                signUpTextView.text = resources.getString(R.string.sign_in)
                nameEditText.visibility = View.VISIBLE
                confPasswordEditText.visibility = View.VISIBLE
            }
        }
        else{
            with(binding) {
                signInBtn.text = resources.getString(R.string.sign_in)
                signUpTextView.text = resources.getString(R.string.sign_up)
                nameEditText.visibility = View.GONE
                confPasswordEditText.visibility = View.GONE
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