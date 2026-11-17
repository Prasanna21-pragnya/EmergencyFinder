
package com.example.emergencyfinder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(favourite: Favourite)

    @Query("SELECT * FROM favourites ORDER BY name ASC")
    suspend fun getAllFavourites(): List<Favourite>

    @Query("DELETE FROM favourites WHERE id = :placeId")
    suspend fun deleteFavourite(placeId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE id = :placeId)")
    suspend fun isFavourite(placeId: String): Boolean
}
