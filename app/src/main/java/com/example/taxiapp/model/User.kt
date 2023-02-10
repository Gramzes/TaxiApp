package com.example.taxiapp.model

data class User(var name: String? = null,
                var id: String? = null,
                var latitude: Double? = null,
                var longitude: Double? = null,
                var clientId: String? = null)