package com.example.graphapp.data.repository

import com.example.graphapp.data.db.DictionaryDatabaseQueries
import com.example.graphapp.data.embedding.SentenceEmbedding
import com.example.graphapp.backend.core.suspiciousDict

class DictionaryRepository(
    private val sentenceEmbedding: SentenceEmbedding
) {
    private val queries = DictionaryDatabaseQueries()

    suspend fun insertWordNodeIntoDb(
        inputValue: String,
    ) {
        val nodeFound = queries.findNodeByNameTypeQuery(inputValue)

        if (nodeFound == null) {
            queries.addNodeIntoDbQuery(
                inputValue = inputValue,
                inputEmbedding = sentenceEmbedding.encode(inputValue),
            )
        }
    }

    suspend fun checkIfSuspicious(inputValue: String) : Boolean {
        val embedding = sentenceEmbedding.encode(inputValue)
        return queries.findSimilarWord(embedding, inputValue)
    }

    suspend fun initialiseDictionaryRepository() {
        suspiciousDict.forEach { term ->
            insertWordNodeIntoDb(term)
        }
    }
}