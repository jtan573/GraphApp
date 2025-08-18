package com.example.graphapp.backend.services.kgraph

import android.content.Context
import com.example.graphapp.data.repository.DictionaryRepository
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.PosTaggerRepository
import com.example.graphapp.data.repository.UserActionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class GraphAccess @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val embeddingRepository = EmbeddingRepository(context)
    private val sentenceEmbedding = embeddingRepository.getSentenceEmbeddingModel()
    val dictionaryRepository = DictionaryRepository(context, sentenceEmbedding)
    val posTaggerRepository = PosTaggerRepository(context)
    val eventRepository = EventRepository(embeddingRepository, dictionaryRepository, posTaggerRepository)
    val userActionRepository = UserActionRepository(sentenceEmbedding)

    init {
        // one-time async init; or expose a suspend init() and call at app start
        CoroutineScope(Dispatchers.IO).launch {
            embeddingRepository.initializeEmbedding()
            dictionaryRepository.initialiseDictionaryRepository()
            posTaggerRepository.initialisePosTagger()
            eventRepository.initialiseEventRepository()
            userActionRepository.initialiseUserActionRepository()
        }
    }
}
