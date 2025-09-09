package com.wristlingo.app.data

import kotlinx.coroutines.flow.Flow

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

