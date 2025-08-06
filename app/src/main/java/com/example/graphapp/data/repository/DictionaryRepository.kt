package com.example.graphapp.data.repository

import android.content.Context
import android.media.metrics.Event
import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.db.DictionaryDatabaseQueries
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.embedding.SentenceEmbedding

class DictionaryRepository(
    private val context: Context,
    private val sentenceEmbedding: SentenceEmbedding
) {
    private val queries = DictionaryDatabaseQueries()
    private val nounPhrases = loadNounPhrases(context)
    private val suspiciousPhrases = loadSuspiciousPhrases(context)

    /*---------------------------
        FOR POS TAG SEARCH
    --------------------------- */
    suspend fun getEventsWithSimilarTags(
        allTags: List<String>,
        eventType: String? = null
    ) : List<EventNodeEntity> {

        val simTerms = mutableListOf<String>()
        allTags.forEach { tag ->
            simTerms.addAll(queries.findSimilarTagsQuery(sentenceEmbedding.encode(tag)))
        }

        val eventNodes = mutableListOf<EventNodeEntity>()
        val seenIds = mutableSetOf<Long>()
        simTerms.forEach {
            val node = queries.findDictionaryTermQuery(it)
            node?.events?.filter { event ->
                (eventType == null || event.type == eventType) && seenIds.add(event.id)
            }?.let {
                eventNodes.addAll(it)
            }
        }

        return eventNodes
    }

    suspend fun insertPosTagIntoDb(
        inputValue: String,
        eventNode: EventNodeEntity
    ) {
        val nodeFound = queries.findDictionaryTermQuery(inputValue)

        if (nodeFound == null) {
            queries.addPosTagIntoDbQuery(
                inputValue = inputValue,
                inputEmbedding = sentenceEmbedding.encode(inputValue),
            )
        }
        queries.updateEventRelationOfTerm(eventNode, inputValue)
    }

    /*---------------------------
        FOR SUSPICIOUS PHRASES
    --------------------------- */
    suspend fun insertSuspiciousWordIntoDb(
        inputValue: String,
    ) {
        queries.addSuspiciousNodeIntoDbQuery(
            inputValue = inputValue,
            inputEmbedding = sentenceEmbedding.encode(inputValue),
        )
    }

    suspend fun checkIfSuspicious(
        inputValue: String
    ) : List<String> {
        val embedding = sentenceEmbedding.encode(inputValue)
        return queries.findSuspiciousTermsQuery(embedding)
    }

    fun loadSuspiciousPhrases(context: Context): Set<String> {
        return context.assets.open("dictionaries/Suspicious_Terms_Dictionary.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        }
    }
    suspend fun initialiseDictionaryRepository() {
        suspiciousPhrases.forEach { term ->
            insertSuspiciousWordIntoDb(term)
        }
    }

    /*---------------------------
        FOR SG NOUN PHRASES
    --------------------------- */
    fun loadNounPhrases(context: Context): Set<String> {
        return context.assets.open("dictionaries/SG_Nouns_Dictionary.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    fun extractAndRemovePhrases(input: String): Pair<List<String>, String> {
        var workingSentence = input
        val matchedPhrases = mutableListOf<String>()

        for (phrase in nounPhrases) {
            val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(workingSentence)) {
                matchedPhrases.add(phrase)
                workingSentence = workingSentence.replace(regex, "") // remove the phrase
            }
        }

        return matchedPhrases to workingSentence.trim().replace("\\s+".toRegex(), " ")
    }


}