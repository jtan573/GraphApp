package com.example.graphapp.data.local

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore
import io.objectbox.BoxStoreBuilder
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType
import java.io.File

object VectorDatabase {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {

        BoxStore.deleteAllFiles(context, BoxStoreBuilder.DEFAULT_NAME)

        store = MyObjectBox.builder()
            .androidContext(context)
            .build()

        store.boxFor(NodeEntity::class.java).removeAll()
        store.boxFor(EdgeEntity::class.java).removeAll()
    }
}

@Entity
data class NodeEntity(
    @Id var id: Long = 0,
    var name: String,
    var type: String,
    var description: String? = null,
    var frequency: Int? = 1,
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null
)

@Entity
data class EdgeEntity(
    @Id
    var id: Long = 0,
    var fromId: Long,
    var toId: Long,
    var edgeType: String? = null
)

data class NodeWithoutEmbedding(
    val id: Long,
    val name: String?,
    val type: String?,
    val description: String?,
    val frequency: Int?
)
