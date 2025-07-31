package com.example.graphapp.data.db

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.BoxStoreBuilder
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.converter.PropertyConverter

object VectorDatabase {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {

        BoxStore.deleteAllFiles(context, BoxStoreBuilder.DEFAULT_NAME)

        store = MyObjectBox.builder()
            .androidContext(context)
            .build()

        store.boxFor(EventNodeEntity::class.java).removeAll()
        store.boxFor(EventEdgeEntity::class.java).removeAll()

        store.boxFor(UserNodeEntity::class.java).removeAll()
        store.boxFor(ActionNodeEntity::class.java).removeAll()
        store.boxFor(ActionEdgeEntity::class.java).removeAll()
    }
}

@Entity
data class EventNodeEntity(
    @Id var id: Long = 0,
    var name: String,
    var type: String,
    var description: String? = null,
    var frequency: Int? = 1,
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null,
    var cachedNodeIds: MutableMap<String, MutableList<Long>> = mutableMapOf(),
    var tags: MutableList<String> = mutableListOf<String>()
)

@Entity
data class EventEdgeEntity(
    @Id var id: Long = 0,
    var firstNodeId: Long,
    var secondNodeId: Long,
    var edgeType: String? = null
)

@Entity
data class DictionaryNodeEntity(
    @Id var id: Long = 0,
    var value: String,
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null,
)

class LongListConverter : PropertyConverter<MutableList<Long>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableList<Long> {
        return databaseValue
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { it.trim().toLong() }
            ?.toMutableList()
            ?: mutableListOf()
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<Long>?): String {
        return entityProperty?.joinToString(",") ?: ""
    }
}

@Entity
data class UserNodeEntity(
    @Id var id: Long = 0,
    var identifier: String = "",
    var role: String = "",
    var specialisation: String = "",
    var currentLocation: String = "",
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null,
    @Convert(converter = LongListConverter::class, dbType = String::class)
    var actionsTaken: MutableList<Long> = mutableListOf()
)

@Entity
data class ActionNodeEntity(
    @Id var id: Long = 0,
    var actionName: String,
    var timestamp: String,
)

@Entity
data class ActionEdgeEntity(
    @Id var id: Long = 0,
    var fromNodeId: Long,
    var fromNodeType: String,
    var toNodeId: Long,
    var toNodeType: String
)



