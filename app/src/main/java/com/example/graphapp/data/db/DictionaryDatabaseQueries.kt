package com.example.graphapp.data.db

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import io.objectbox.kotlin.boxFor

class DictionaryDatabaseQueries() {

    private val dictBox = VectorDatabase.store.boxFor(DictionaryNodeEntity::class)

    /*---------------------------
        FOR POS TAG SEARCH
    --------------------------- */
    fun updateEventRelationOfTerm(
        eventNode: EventNodeEntity,
        tag: String,
        type: String = "POS"
    ) {
        val nodeFound = dictBox
            .query(DictionaryNodeEntity_.type.equal(type).and(DictionaryNodeEntity_.value.equal(tag)))
            .build()
            .findFirst()
        if (nodeFound != null) {
            nodeFound.events.add(eventNode)
            dictBox.put(nodeFound)
        }
    }

    fun findDictionaryTermQuery(
        value: String,
    ) : DictionaryNodeEntity? {
        return dictBox
            .query(DictionaryNodeEntity_.value.equal(value))
            .build()
            .findFirst()
    }

    fun findSimilarTagsQuery(
        inputEmbedding: FloatArray,
        type: String = "POS"
    ) : List<String> {
        val simWords = mutableListOf<String>()
        dictBox.query(
            DictionaryNodeEntity_.type.equal(type).and
            (DictionaryNodeEntity_.embedding.nearestNeighbors(inputEmbedding, 5))
        ).build().findWithScores().forEach { result ->
            if (result.score < 0.5f) {
                simWords.add(result.get().value)
            }
        }
        return simWords
    }

    fun addPosTagIntoDbQuery(
        inputValue: String,
        inputEmbedding: FloatArray,
        type: String = "POS"
    ) {
        dictBox.put(
            DictionaryNodeEntity(
                type = type,
                value = inputValue,
                embedding = inputEmbedding
            )
        )
        return
    }

    /*---------------------------
        FOR SUSPICIOUS PHRASES
    --------------------------- */
    fun addSuspiciousNodeIntoDbQuery(
        inputValue: String,
        inputEmbedding: FloatArray,
        type: String = "SUSPICIOUS"
    ) {
        dictBox.put(
            DictionaryNodeEntity(
                type = type,
                value = inputValue,
                embedding = inputEmbedding
            )
        )
        return
    }

    fun findSuspiciousTermsQuery(
        inputEmbedding: FloatArray,
        type: String = "SUSPICIOUS"
    ) : List<String> {
        val simWords = mutableListOf<String>()
        dictBox.query(
            DictionaryNodeEntity_.type.equal(type).and
            (DictionaryNodeEntity_.embedding.nearestNeighbors(inputEmbedding, 3))
        ).build().findWithScores().forEach { result ->
            if (result.score < 0.5f) {
                simWords.add(result.get().value)
            }
        }
        return simWords
    }

}
