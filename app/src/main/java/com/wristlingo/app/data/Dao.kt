package com.wristlingo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY started_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?
}

@Dao
interface UtteranceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utterance: UtteranceEntity): Long

    @Query("SELECT * FROM utterances WHERE session_id = :sessionId ORDER BY ts ASC")
    fun observeBySession(sessionId: Long): Flow<List<UtteranceEntity>>
}

