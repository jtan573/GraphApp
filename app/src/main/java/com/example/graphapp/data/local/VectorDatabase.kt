package com.example.graphapp.data.local

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.BoxStoreBuilder
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType

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
    var embedding: FloatArray? = null,
    var cachedNodeIds: MutableMap<String, MutableList<Long>> = mutableMapOf()
)

@Entity
data class EdgeEntity(
    @Id
    var id: Long = 0,
    var firstNodeId: Long,
    var secondNodeId: Long,
    var edgeType: String? = null
)
