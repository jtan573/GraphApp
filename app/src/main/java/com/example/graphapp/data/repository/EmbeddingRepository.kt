package com.example.graphapp.data.repository

import android.content.Context
import com.example.graphapp.data.embedding.SentenceEmbedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class EmbeddingRepository(private val context: Context) {

    private val sentenceEmbedding = SentenceEmbedding()

    suspend fun initializeEmbedding() = withContext(Dispatchers.IO) {

        // Copy model file to filesDir
        val modelFile = File(context.filesDir, "sentence_transformer.onnx")
        if (!modelFile.exists()) {
            context.assets.open("models/sentence_transformer.onnx").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val tokenizerBytes = context.assets.open("models/tokenizer.json").readBytes()

        sentenceEmbedding.init(
            modelFilepath = modelFile.absolutePath,
            tokenizerBytes = tokenizerBytes,
            useTokenTypeIds = true,
            outputTensorName = "sentence_embedding",
            useFP16 = false,
            useXNNPack = false,
            normalizeEmbeddings = true
        )
    }

    fun getSentenceEmbeddingModel(): SentenceEmbedding {
        return sentenceEmbedding
    }

            suspend fun getTextEmbeddings(inputString: String): FloatArray {
        return sentenceEmbedding.encode(inputString)
    }

    // Function to calculate cosine similarity between nodes
    fun cosineDistance(
        x1: FloatArray,
        x2: FloatArray
    ): Float {
        var mag1 = 0.0f
        var mag2 = 0.0f
        var product = 0.0f
        for (i in x1.indices) {
            mag1 += x1[i].pow(2)
            mag2 += x2[i].pow(2)
            product += x1[i] * x2[i]
        }
        mag1 = sqrt(mag1)
        mag2 = sqrt(mag2)
        return product / (mag1 * mag2)
    }
}
