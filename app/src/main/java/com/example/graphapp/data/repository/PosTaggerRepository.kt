package com.example.graphapp.data.repository

import android.content.Context
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import java.io.File

data class PosTaggerRepository(private val context: Context) {

    private lateinit var tagger: MaxentTagger
    private lateinit var taggerFile: File

    fun initialisePosTagger() {
        taggerFile = copyTaggerToCache(context)
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
        return tagger.tagString(inputText.lowercase())
    }

    fun getTaggerFilepath(): String {
        return taggerFile.absolutePath
    }
}
