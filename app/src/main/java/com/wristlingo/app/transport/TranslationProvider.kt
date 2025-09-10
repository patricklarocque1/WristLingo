package com.wristlingo.app.transport

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.common.model.RemoteModelManager
import com.wristlingo.core.settings.Settings
import kotlinx.coroutines.tasks.await

class TranslationProvider(private val context: Context, private val settings: Settings) {

    private var translator: Translator? = null
    private var currentSource: String? = null
    private var currentTarget: String? = null

    suspend fun ensureModel(source: String?, target: String) {
        val srcLang = source ?: TranslateLanguage.ENGLISH
        val dstLang = target
        if (translator == null || currentSource != srcLang || currentTarget != dstLang) {
            translator?.close()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcLang)
                .setTargetLanguage(dstLang)
                .build()
            translator = Translation.getClient(options)
            currentSource = srcLang
            currentTarget = dstLang
        }
        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)?.await()
    }

    suspend fun translate(text: String, source: String?, target: String): String {
        ensureModel(source, target)
        return translator?.translate(text)?.await() ?: text
    }

    fun defaultTarget(): String = settings.defaultTargetLanguage

    suspend fun isModelDownloaded(language: String): Boolean {
        val langCode = TranslateLanguage.fromLanguageTag(language) ?: language
        val model = TranslateRemoteModel.Builder(langCode).build()
        return try {
            RemoteModelManager.getInstance().isModelDownloaded(model).await()
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun downloadTargetModelIfNeeded(target: String) {
        ensureModel(null, target)
    }
}

