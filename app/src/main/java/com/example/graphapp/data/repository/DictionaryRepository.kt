package com.example.graphapp.data.repository

import android.content.Context
import com.example.graphapp.data.db.DictionaryDatabaseQueries
import com.example.graphapp.data.embedding.SentenceEmbedding

class DictionaryRepository(
    private val context: Context,
    private val sentenceEmbedding: SentenceEmbedding
) {
    private val queries = DictionaryDatabaseQueries()
    private val nounPhrases = loadNounPhrases(context)
    private val suspiciousPhrases = loadSuspiciousPhrases(context)

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

    fun loadSuspiciousPhrases(context: Context): Set<String> {
        return context.assets.open("dictionaries/Suspicious_Terms_Dictionary.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        }
    }
    suspend fun initialiseDictionaryRepository() {
        suspiciousPhrases.forEach { term ->
            insertWordNodeIntoDb(term)
        }
    }

    /*---------------------------
        FOR NOUN PHRASES
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