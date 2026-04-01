package com.gondroid.quoteanime.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteQuoteDao {

    @Query("SELECT * FROM favorite_quotes ORDER BY savedAt DESC")
    fun getFavorites(): Flow<List<FavoriteQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: FavoriteQuoteEntity)

    @Query("DELETE FROM favorite_quotes WHERE id = :quoteId")
    suspend fun delete(quoteId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_quotes WHERE id = :quoteId)")
    fun isFavorite(quoteId: String): Flow<Boolean>

    @Query("SELECT id FROM favorite_quotes")
    fun getFavoriteIds(): Flow<Set<String>>
}
