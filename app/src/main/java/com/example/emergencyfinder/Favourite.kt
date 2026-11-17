
package com.example.emergencyfinder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class Favourite(

    @PrimaryKey
    val id: String,

    val name: String,

    val latitude: Double,

    val longitude: Double,

    val phone: String?,

    val category: String
)
