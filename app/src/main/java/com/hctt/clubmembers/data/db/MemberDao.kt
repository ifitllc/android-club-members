package com.hctt.clubmembers.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isDeleted = 0 ORDER BY expiration ASC")
    fun observeActive(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE isDeleted = 0 AND (name LIKE :query OR email LIKE :query OR phone LIKE :query) ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemberEntity?

    @Query("SELECT * FROM members")
    suspend fun getAll(): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: MemberEntity)

    @Insert
    suspend fun insertAndReturn(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Query("UPDATE members SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM members WHERE id NOT IN(:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Query("DELETE FROM members")
    suspend fun deleteAll()
}
