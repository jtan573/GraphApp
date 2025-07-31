package com.example.graphapp.data.db

import android.util.Log
import io.objectbox.kotlin.boxFor

class DictionaryDatabaseQueries() {

    private val dictBox = VectorDatabase.store.boxFor(DictionaryNodeEntity::class)

    fun addNodeIntoDbQuery(
        inputValue: String,
        inputEmbedding: FloatArray
    ) {
        dictBox.put(
            DictionaryNodeEntity(
                value = inputValue,
                embedding = inputEmbedding
            )
        )
        return
    }

    fun findNodeByNameTypeQuery(name: String) : DictionaryNodeEntity? {
        val nodeFound = dictBox
            .query(DictionaryNodeEntity_.value.equal(name))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findSimilarWord(inputEmbedding: FloatArray, inputValue: String) : Boolean {
        dictBox.query(
            DictionaryNodeEntity_.embedding.nearestNeighbors(inputEmbedding, 1)
        ).build().findWithScores().forEach { result ->
            if (result.score < 0.5f) {
                return true
            }
        }
        return false
    }
}
