package com.example.graphapp.data.repository

import android.app.Application
import android.content.Context
import com.example.graphapp.backend.dto.GraphSchema.SchemaPosTags
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import java.io.File

data class PosTaggerRepository(private val context: Context) {

    private lateinit var tagger: MaxentTagger

    fun initialisePosTagger() {
        val taggerFile = copyTaggerToCache(context)
        tagger = MaxentTagger(taggerFile.absolutePath)
    }

    fun copyTaggerToCache(context: Context): File {
        val inputStream = context.assets.open("models/english-left3words-distsim.tagger")
        val outFile = File(context.cacheDir, "tagger.tagger")

        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outFile
    }

    fun tagText(inputText: String): String {
        return tagger.tagString(inputText)
    }

    fun extractTaggedWords(taggedSentence: String): List<String> {
        val taggedTokens = taggedSentence.split(" ")

        return taggedTokens.mapNotNull { token ->
            val parts = token.split("_")
            if (parts.size == 2 && parts[1] in SchemaPosTags) {
                parts[0]
            } else {
                null
            }
        }
    }

}
