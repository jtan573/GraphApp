package com.example.graphapp.data.db

import android.content.Context
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.EventStatus
import io.objectbox.BoxStore
import io.objectbox.BoxStoreBuilder
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.converter.PropertyConverter
import io.objectbox.relation.ToMany

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

        store.boxFor(DictionaryNodeEntity::class.java).removeAll()
    }
}

/* -----------------------------------
    EVENT NODE ENTITY
------------------------------------ */
@Entity
data class EventNodeEntity(
    @Id var id: Long = 0,
    var name: String,
    var type: String,
    var description: String? = null,
    var frequency: Int? = 1,
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null,
    @Convert(converter = StatusConverter::class, dbType = Int::class)
    var status: EventStatus = EventStatus.ACTIVE,
    var cachedNodeIds: MutableMap<String, MutableList<Long>> = mutableMapOf(),
    var tags: List<String> = mutableListOf<String>()
)

@Entity
data class EventEdgeEntity(
    @Id var id: Long = 0,
    var firstNodeId: Long,
    var secondNodeId: Long,
    var edgeType: String? = null
)


/* -----------------------------------
    DICTIONARY NODE ENTITY
------------------------------------ */
@Entity
data class DictionaryNodeEntity(
    @Id var id: Long = 0,
    var type: String,
    var value: String,
    @HnswIndex(dimensions=384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null,
) {
    lateinit var events: ToMany<EventNodeEntity>
}


/* -----------------------------------
    USER/ACTION NODE ENTITY
------------------------------------ */
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
    var timestamp: Long,
)

@Entity
data class ActionEdgeEntity(
    @Id var id: Long = 0,
    var fromNodeId: Long,
    var fromNodeType: String,
    var toNodeId: Long,
    var toNodeType: String
)


/*----------------------------------
    Converters
 ----------------------------------*/
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


class StatusConverter : PropertyConverter<EventStatus?, Int?> {
    override fun convertToEntityProperty(databaseValue: Int?): EventStatus? {
        if (databaseValue == null) {
            return null
        }
        for (role in EventStatus.entries) {
            if (role.id == databaseValue) {
                return role
            }
        }
        return EventStatus.UNKNOWN
    }

    override fun convertToDatabaseValue(entityProperty: EventStatus?): Int? {
        return entityProperty?.id
    }
}

class EventTypeConverter : PropertyConverter<SchemaEventTypeNames?, String?> {
    override fun convertToDatabaseValue(entityProperty: SchemaEventTypeNames?): String {
        return entityProperty?.key ?: throw IllegalArgumentException(
            "EventTypeConverter: null enum cannot be saved"
        )
    }

    override fun convertToEntityProperty(databaseValue: String?): SchemaEventTypeNames {
        if (databaseValue == null) {
            throw IllegalArgumentException("EventTypeConverter: null value in DB")
        }
        return try {
            SchemaEventTypeNames.valueOf(databaseValue)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "EventTypeConverter: unknown value '$databaseValue' in DB",
                e
            )
        }
    }
}


