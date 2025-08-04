package com.example.graphapp.backend

import android.app.Application
import android.util.Log
import com.example.graphapp.data.repository.DictionaryRepository
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.PosTaggerRepository
import com.example.graphapp.data.repository.UserActionRepository

class AppBackend(
    application: Application
) {
    private val context = application.applicationContext

    val embeddingRepository = EmbeddingRepository(context)
    val sentenceEmbedding = embeddingRepository.getSentenceEmbeddingModel()
    val dictionaryRepository = DictionaryRepository(sentenceEmbedding)
    val posTaggerRepository = PosTaggerRepository(context)
    val eventRepository = EventRepository(sentenceEmbedding, embeddingRepository, dictionaryRepository, posTaggerRepository)
    val userActionRepository = UserActionRepository(sentenceEmbedding)


    suspend fun initialiseBackend() {
        embeddingRepository.initializeEmbedding()
        Log.d("REPOSITORY", "EMBEDDING REPO INITIALISED.")
        dictionaryRepository.initialiseDictionaryRepository()
        Log.d("REPOSITORY", "DICTIONARY REPO INITIALISED.")
        posTaggerRepository.initialisePosTagger()
        Log.d("REPOSITORY", "POS TAGGER REPO INITIALISED.")
        eventRepository.initialiseEventRepository()
        Log.d("REPOSITORY", "EVENT REPO INITIALISED.")
        userActionRepository.initialiseUserActionRepository()
        Log.d("REPOSITORY", "USER-ACTION REPO INITIALISED.")
    }
}