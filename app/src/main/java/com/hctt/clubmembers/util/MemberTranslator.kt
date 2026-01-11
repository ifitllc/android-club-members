package com.hctt.clubmembers.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberTranslator @Inject constructor() {
    
    suspend fun translateChineseToEnglish(text: String): String {
        if (text.isBlank()) return ""
        
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)
        
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
            
        // Ensure model is downloaded
        translator.downloadModelIfNeeded(conditions).await()
        
        val result = translator.translate(text).await()
        translator.close()
        return result
    }
}
