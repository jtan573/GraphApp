package com.example.graphapp.data.repository

import android.content.Context
import com.example.graphapp.backend.core.GraphSchema.SchemaPosTags
import com.example.graphapp.data.db.queries.DictionaryDatabaseQueries
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.embedding.SentenceEmbedding
import edu.stanford.nlp.pipeline.*;
import java.util.Properties

class DictionaryRepository(
    private val context: Context,
    private val sentenceEmbedding: SentenceEmbedding,
    private val posTaggerRepository: PosTaggerRepository
) {
    private val queries = DictionaryDatabaseQueries()
    private val nounPhrases = loadNounPhrases(context)
    private val vagueWords = loadVaguePhrases(context)
    private val suspiciousPhrases = loadSuspiciousPhrases(context)

    /*---------------------------
        GENERAL USE
    --------------------------- */
    fun resetDictionaryDb() {
        queries.resetDictionaryDbQuery()
    }

    suspend fun processInputName(inputName: String): List<String> {
        val (matchedPhrases, cleanedSentence) = extractAndRenamePhrases(inputName)
        val taggedSentence = posTaggerRepository.tagText(cleanedSentence.lowercase())
        val lemmas = lemmatiseText(cleanedSentence.lowercase())
        val lemmatisedText = replaceOriginalWithLemma(taggedSentence, lemmas)
        val filteredTags = replaceSimilarTags(lemmatisedText)
        return filteredTags + matchedPhrases
    }

    fun lemmatiseText(text: String): List<String> {
        val props = Properties().apply {
            setProperty("annotators", "tokenize,ssplit,pos,lemma")
            setProperty("pos.model", posTaggerRepository.getTaggerFilepath())
        }
        val pipeline = StanfordCoreNLP(props)

        val doc = CoreDocument(text)
        pipeline.annotate(doc)

        return doc.tokens().map { it.lemma() }
    }

    fun replaceOriginalWithLemma(taggedText: String, lemmas: List<String>): List<String> {
        val taggedTokens = taggedText.split(" ")

        val updatedText = mutableListOf<String>()
        taggedTokens.forEachIndexed { idx, token ->
            val parts = token.split("_")
            if (parts.size == 2 && parts[1] in SchemaPosTags) {
                updatedText.add(lemmas[idx])
            }
        }
        return updatedText
    }


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


    suspend fun replaceSimilarTags(posTags: List<String>): List<String> {
        val newTagList = mutableListOf<String>()
        posTags.forEach { tag ->
            if (tag in vagueWords) {
                return@forEach
            }
            val simTagsFound = queries.findSimilarTagsQuery(
                inputEmbedding = sentenceEmbedding.encode(tag),
                numTagsToFind = 1,
                threshold = 0.2f
            )
            if (simTagsFound.isNotEmpty()) {
                newTagList.add(simTagsFound.first())
            } else {
                newTagList.add(tag)
            }
        }
        return newTagList
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

    fun loadVaguePhrases(context: Context): Set<String> {
        return context.assets.open("dictionaries/Vague_Words_Dictionary.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    fun extractAndRenamePhrases(input: String): Pair<List<String>, String> {
        var workingSentence = input
        val matchedPhrases = mutableListOf<String>()

        for (phrase in nounPhrases) {
            val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(workingSentence)) {
                matchedPhrases.add(phrase)
                workingSentence = workingSentence.replace(regex, "something") // remove the phrase
            }
        }

        return matchedPhrases to workingSentence.trim().replace("\\s+".toRegex(), " ")
    }


}