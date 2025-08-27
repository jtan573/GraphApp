package com.example.graphapp.data.db.queries

import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventEdgeEntity_
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.EventNodeEntity_
import com.example.graphapp.data.db.VectorDatabase
import io.objectbox.kotlin.boxFor

class EventDatabaseQueries() {

    private val nodesBox = VectorDatabase.store.boxFor(EventNodeEntity::class)
    private val edgesBox = VectorDatabase.store.boxFor(EventEdgeEntity::class)

    fun addNodeIntoDbQuery(
        name: String,
        type: String,
        description: String? = null,
        frequency: Int? = 1,
        embedding: FloatArray,
        tags: List<String> = mutableListOf<String>()
    ): Long {
        val id = nodesBox.put(
            EventNodeEntity(
                name = name,
                type = type,
                description = description,
                frequency = frequency,
                embedding = embedding,
                tags = tags
            )
        )
        return id
    }

    fun addEdgeIntoDbQuery(fromId: Long, toId: Long, edgeType: String){
        edgesBox.put(
            EventEdgeEntity(
                firstNodeId = fromId, secondNodeId = toId, edgeType = edgeType
            )
        )
        return
    }

    fun findNodeByNameTypeQuery(name: String, type: String) : EventNodeEntity? {
        val nodeFound = nodesBox
            .query(EventNodeEntity_.name.equal(name).and(EventNodeEntity_.type.equal(type)))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findNodeByIdQuery(inputId: Long) : EventNodeEntity? {
        val nodeFound = nodesBox
            .query(EventNodeEntity_.id.equal(inputId))
            .build()
            .findFirst()

        return nodeFound
    }

    fun deleteNodesAndEdges(nodeIdsToDelete: List<Long>, edgeIdsToDelete: List<Long>) {
        nodesBox.removeByIds(nodeIdsToDelete)
        edgesBox.removeByIds(edgeIdsToDelete)
    }

    fun incrementFreqOfNodeQuery(node: EventNodeEntity): Long {
        node.frequency = node.frequency?.plus(1)
        val id = nodesBox.put(node)
        println("Node value incremented: $node")
        return id
    }

    fun findAllNodesQuery() : List<EventNodeEntity> {
        return nodesBox.query().build().find()
    }

    fun findAllEdgesQuery() : List<EventEdgeEntity> {
        return edgesBox.query().build().find()
    }

    fun findAllNodesWithoutEmbeddingQuery() : List<EventNodeEntity> {
        return nodesBox.query().build().find()
            .map { node ->
                EventNodeEntity(
                    id = node.id,
                    name = node.name,
                    type = node.type,
                    description = node.description,
                    frequency = node.frequency
                )
            }
    }

    fun findNodesByTypeQuery(type: String) : List<EventNodeEntity> {
        return nodesBox
            .query(EventNodeEntity_.type.equal(type))
            .build()
            .find()
    }

    fun findAllNodeFrequenciesQuery() : Map<Long, Int> {
        return nodesBox.query().build().find()
            .associate { node -> node.id to (node.frequency ?: 0) }
    }

    fun findNodeFrequencyOfNodeId(inputId: Long) : Int? {
        return nodesBox.query(EventNodeEntity_.id.equal(inputId)).build()
            .findFirst()!!.frequency
    }

    fun findAllEdgesAroundNodeIdQuery(id: Long): List<EventEdgeEntity> {
        return edgesBox.query(
            EventEdgeEntity_.firstNodeId.equal(id).or(EventEdgeEntity_.secondNodeId.equal(id))
        ).build().find()
    }

    fun findEdgeBetweenNodeIdsQuery(first: Long, second: Long): EventEdgeEntity? {
        return edgesBox.query(
            (EventEdgeEntity_.firstNodeId.equal(first).and(EventEdgeEntity_.secondNodeId.equal(second)))
                .or(EventEdgeEntity_.firstNodeId.equal(second).and(EventEdgeEntity_.secondNodeId.equal(first)))
        ).build().findFirst()
    }

    fun resetEventDbQuery() {
        nodesBox.removeAll()
        edgesBox.removeAll()
    }

}