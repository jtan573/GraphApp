package com.example.graphapp.data.db.queries

import com.example.graphapp.data.db.DictionaryNodeEntity
import com.example.graphapp.data.db.DictionaryNodeEntity_
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.VectorDatabase
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
        type: String = "POS",
        numTagsToFind: Int = 5,
        threshold: Float = 0.5f
    ) : List<String> {
        val simWords = mutableListOf<String>()
        dictBox.query(
            DictionaryNodeEntity_.type.equal(type).and
            (DictionaryNodeEntity_.embedding.nearestNeighbors(inputEmbedding, numTagsToFind))
        ).build().findWithScores().forEach { result ->
            if (result.score < threshold) {
                simWords.add(result.get().value)
            }
        }
        return simWords
    }

    /**
     * Adds POS tag to dictionary.
     * Checks if there are any existing similar tags before adding.
     */
    fun addPosTagIntoDbQuery(
        inputValue: String,
        inputEmbedding: FloatArray,
        type: String = "POS"
    ) {
        val simWordsFound = mutableListOf<String>()
        dictBox.query(
            DictionaryNodeEntity_.type.equal(type).and
                (DictionaryNodeEntity_.embedding.nearestNeighbors(inputEmbedding, 1))
        ).build().findWithScores().forEach { result ->
            if (result.score < 0.2f) {
                simWordsFound.add(result.get().value)
            }
        }
        if (simWordsFound.isEmpty()) {
            dictBox.put(
                DictionaryNodeEntity(
                    type = type,
                    value = inputValue,
                    embedding = inputEmbedding
                )
            )
        }
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

    fun getSuspiciousEvents() : List<EventNodeEntity> {
        val suspiciousWordsOnly = dictBox.query(
            DictionaryNodeEntity_.type.equal("SUSPICIOUS")
        ).build().find()

        val suspiciousEvents = mutableListOf<EventNodeEntity>()
        suspiciousWordsOnly.forEach { word ->
            word.events.forEach { event ->
                if (!suspiciousEvents.contains(event)) {
                    suspiciousEvents.add(event)
                }
            }
        }
        return suspiciousEvents
    }

    fun resetDictionaryDbQuery() {
        dictBox.removeAll()
    }
}