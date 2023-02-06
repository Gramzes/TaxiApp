package com.example.taxiapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.taxiapp.model.User
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UserSignInViewModel: ViewModel() {
    private val database = Firebase.database

    var isSignInMode = true
    var email = ""
    var name = ""
    var password = ""
    var confPass = ""

    fun addUser(user: User){
        val usersRef = database.getReference("users")
        usersRef.push().setValue(user)
    }
}