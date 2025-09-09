package com.wristlingo.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions"
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "started_at")
    val startedAtEpochMs: Long,
    @ColumnInfo(name = "target_lang")
    val targetLang: String
)

@Entity(
    tableName = "utterances",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["session_id"])
    ]
)
data class UtteranceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "ts")
    val timestampEpochMs: Long,
    @ColumnInfo(name = "src_text")
    val srcText: String,
    @ColumnInfo(name = "dst_text")
    val dstText: String,
    @ColumnInfo(name = "src_lang")
    val srcLang: String?,
    @ColumnInfo(name = "dst_lang")
    val dstLang: String
)

