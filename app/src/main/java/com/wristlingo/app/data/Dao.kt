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

    @Query(
        """
        SELECT s.*, (
            SELECT src_text FROM utterances u
            WHERE u.session_id = s.id
            ORDER BY ts ASC LIMIT 1
        ) AS first_src
        FROM sessions s
        ORDER BY started_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentWithFirst(limit: Int = 50): Flow<List<SessionWithFirst>>
}

@Dao
interface UtteranceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utterance: UtteranceEntity): Long

    @Query("SELECT * FROM utterances WHERE session_id = :sessionId ORDER BY ts ASC")
    fun observeBySession(sessionId: Long): Flow<List<UtteranceEntity>>
}

data class SessionWithFirst(
    val id: Long,
    val started_at: Long,
    val target_lang: String,
    val first_src: String?
)

