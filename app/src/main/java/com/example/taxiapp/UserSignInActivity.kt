package com.example.taxiapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taxiapp.databinding.ActivityUserSignInBinding
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.taxiapp.model.User
import com.example.taxiapp.viewmodel.UserSignInViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserSignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSignInBinding
    private var isSignInMode = true
    private val viewModel: UserSignInViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSignInBinding.inflate(layoutInflater)
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

        auth = Firebase.auth
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
        val email = binding.emailEditText.editText!!.text.toString()
        val password = binding.passwordEditText.editText!!.text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                } else {
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signUpUser(){
        val isMailVal = validateEmail()
        val isNameVal = validateUserName()
        val isPasVal = validatePassword()

        if (isMailVal && isNameVal && isPasVal){
            val name = binding.nameEditText.editText!!.text.toString()
            val email = binding.emailEditText.editText!!.text.toString()
            val password = binding.passwordEditText.editText!!.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        viewModel.addUser(User(name))
                    } else {
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
