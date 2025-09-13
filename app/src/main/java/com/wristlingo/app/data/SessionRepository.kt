package com.wristlingo.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(private val database: AppDatabase) {

    private val sessionDao: SessionDao = database.sessionDao()
    private val utteranceDao: UtteranceDao = database.utteranceDao()

    suspend fun createSession(
        targetLang: String,
        startedAtEpochMs: Long = System.currentTimeMillis()
    ): Long {
        val entity = SessionEntity(
            startedAtEpochMs = startedAtEpochMs,
            targetLang = targetLang
        )
        return sessionDao.insert(entity)
    }

    fun observeRecentSessions(limit: Int = 50): Flow<List<SessionEntity>> {
        return sessionDao.observeRecent(limit)
    }

    data class SessionPreview(
        val id: Long,
        val startedAtEpochMs: Long,
        val targetLang: String,
        val title: String?
    )

    fun observeRecentSessionPreviews(limit: Int = 50): Flow<List<SessionPreview>> {
        return sessionDao.observeRecentWithFirst(limit).map { list ->
            list.map { s ->
                SessionPreview(
                    id = s.id,
                    startedAtEpochMs = s.started_at,
                    targetLang = s.target_lang,
                    title = s.first_src?.lineSequence()?.firstOrNull()?.take(80)
                )
            }
        }
    }

    suspend fun getSessionById(id: Long): SessionEntity? {
        return sessionDao.getById(id)
    }

    fun observeUtterances(sessionId: Long): Flow<List<UtteranceEntity>> {
        return utteranceDao.observeBySession(sessionId)
    }

    suspend fun insertUtterance(
        sessionId: Long,
        timestampEpochMs: Long,
        srcText: String,
        dstText: String,
        srcLang: String?,
        dstLang: String
    ): Long {
        val entity = UtteranceEntity(
            sessionId = sessionId,
            timestampEpochMs = timestampEpochMs,
            srcText = srcText,
            dstText = dstText,
            srcLang = srcLang,
            dstLang = dstLang
        )
        return utteranceDao.insert(entity)
    }
}

