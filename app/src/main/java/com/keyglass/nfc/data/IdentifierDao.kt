package com.keyglass.nfc.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface IdentifierDao {

    @Query("SELECT * FROM identifiers ORDER BY position ASC, id ASC")
    fun getAll(): LiveData<List<Identifier>>

    @Query("SELECT * FROM identifiers ORDER BY position ASC, id ASC")
    suspend fun getAllOnce(): List<Identifier>

    @Query("SELECT COUNT(*) FROM identifiers")
    suspend fun count(): Int

    @Insert
    suspend fun insert(identifier: Identifier): Long

    @Update
    suspend fun update(identifier: Identifier)

    @Delete
    suspend fun delete(identifier: Identifier)
}
