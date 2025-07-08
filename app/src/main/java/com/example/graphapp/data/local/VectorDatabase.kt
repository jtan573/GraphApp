package com.example.graphapp.data.local

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType

object VectorDatabase {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context)
            .build()
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
