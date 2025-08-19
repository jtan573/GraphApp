package com.example.graphapp.backend.services.kgraph.nlp

/* FUTURE USE

import android.content.Context
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class NlpManager(private val context: Context): NlpService {

    private val modelFileName = "mobilebert.tflite"
    private val vocabFileName = "vocab.txt"
    private val interpreter: Interpreter
    private val vocab: Map<String, Int>

    init {
        interpreter = Interpreter(loadModelFile(modelFileName))
        vocab = loadVocabFromAssets(vocabFileName)
    }

    override fun runQuery(query: String): Map<String, Float> {
        // Tokenize and convert to IDs
        val tokens = simpleWhitespaceTokenizer(query)
        val tokenIds = convertTokensToIds(tokens)

        // Prepare inputs with padding
        val maxSeqLen = 128
        val inputIds = IntArray(maxSeqLen) { 0 }
        val attentionMask = IntArray(maxSeqLen) { 0 }

        val length = tokenIds.size.coerceAtMost(maxSeqLen)
        for (i in 0 until length) {
            inputIds[i] = tokenIds[i]
            attentionMask[i] = 1
        }

        // Prepare input buffers
        val inputIdsTensor = TensorBuffer.createFixedSize(intArrayOf(1, maxSeqLen), DataType.FLOAT32)
        val attentionMaskTensor = TensorBuffer.createFixedSize(intArrayOf(1, maxSeqLen), DataType.FLOAT32)

        inputIdsTensor.loadArray(inputIds)
        attentionMaskTensor.loadArray(attentionMask)

        // Prepare output buffer (adjust output shape & size to your model)
        val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32) // example: 2 classes

        // Run inference
        val inputs = arrayOf(inputIdsTensor.buffer, attentionMaskTensor.buffer)
        val outputs = mutableMapOf<Int, Any>(0 to outputTensor.buffer)
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // Map output scores to labels (adjust labels as per your model)
        val labels = listOf("Negative", "Positive")
        return labels.zip(outputTensor.floatArray.toList()).toMap()
    }

    override fun close() {
        interpreter.close()
    }

    private fun simpleWhitespaceTokenizer(text: String): List<String> {
        return text.lowercase()
            .split(" ", ",", ".", "!", "?", ";", ":", "\"", "'")
            .filter { it.isNotBlank() }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocabFromAssets(fileName: String): Map<String, Int> {
        val vocabMap = mutableMapOf<String, Int>()
        context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                vocabMap[line.trim()] = index
            }
        }
        return vocabMap
    }

    // Convert tokens to IDs using vocab; use [UNK] token ID if token not found
    private fun convertTokensToIds(tokens: List<String>): IntArray {
        val unkId = vocab["[UNK]"] ?: 100  // default to 100 if not found
        return tokens.map { token -> vocab[token] ?: unkId }.toIntArray()
    }
}

 */