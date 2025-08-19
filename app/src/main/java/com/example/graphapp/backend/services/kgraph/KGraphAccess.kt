package com.example.graphapp.backend.services.kgraph

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
import kotlinx.coroutines.flow.StateFlow

@Singleton
class GraphAccess @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val readyGate = CompletableDeferred<Unit>() // for suspend callers

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
            Log.d("REPOSITORY INFO", "Embedding repository initialised.")
            dictionaryRepository.initialiseDictionaryRepository()
            Log.d("REPOSITORY INFO", "Dictionary repository initialised.")
            posTaggerRepository.initialisePosTagger()
            Log.d("REPOSITORY INFO", "POS Tagger repository initialised.")
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
