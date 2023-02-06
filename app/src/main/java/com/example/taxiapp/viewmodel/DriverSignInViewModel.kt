package com.example.taxiapp.viewmodel

import androidx.lifecycle.ViewModel

class DriverSignInViewModel: ViewModel() {
    var isSignInMode = true
    var email = ""
    var name = ""
    var password = ""
    var confPass = ""
}