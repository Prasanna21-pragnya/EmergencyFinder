
package com.example.emergencyfinder

data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val category: String,
    var distanceKm: Double = 0.0
)
