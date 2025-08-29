package com.example.graphapp.core.services.kgraph

import android.content.Context
import com.example.graphapp.data.repository.DictionaryRepository
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.PosTaggerRepository
import com.example.graphapp.data.repository.UserActionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class GraphAccess @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _ready = MutableStateFlow(false)
    private val readyGate = CompletableDeferred<Unit>()

    val embeddingRepository = EmbeddingRepository(context)
    private val sentenceEmbedding = embeddingRepository.getSentenceEmbeddingModel()
    val posTaggerRepository = PosTaggerRepository(context)
    val dictionaryRepository = DictionaryRepository(context, sentenceEmbedding, posTaggerRepository)
    val eventRepository = EventRepository(embeddingRepository, dictionaryRepository)
    val userActionRepository = UserActionRepository(sentenceEmbedding)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            embeddingRepository.initializeEmbedding()
            Log.d("REPOSITORY INFO", "Embedding repository initialised.")
            posTaggerRepository.initialisePosTagger()
            Log.d("REPOSITORY INFO", "POS Tagger repository initialised.")
            dictionaryRepository.initialiseDictionaryRepository()
            Log.d("REPOSITORY INFO", "Dictionary repository initialised.")
            eventRepository.initialiseEventRepository()
            Log.d("REPOSITORY INFO", "Event repository initialised.")
            userActionRepository.initialiseUserActionRepository()
            Log.d("REPOSITORY INFO", "UserAction repository initialised.")

            _ready.value = true
            readyGate.complete(Unit)
        }
    }

    /** Suspend until all repositories are ready */
    suspend fun awaitReady() = readyGate.await()
}
