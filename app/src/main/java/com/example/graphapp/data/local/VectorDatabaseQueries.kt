package com.example.graphapp.data.local

import android.util.Log
import io.objectbox.kotlin.boxFor

class VectorDBQueries() {

    private val nodesBox = VectorDatabase.store.boxFor(NodeEntity::class)
    private val edgesBox = VectorDatabase.store.boxFor(EdgeEntity::class)

    fun addNodeIntoDbQuery(
        name: String,
        type: String,
        description: String? = null,
        frequency: Int? = 1,
        embedding: FloatArray
    ) {
        nodesBox.put(
            NodeEntity(
                name = name,
                type = type,
                description = description,
                frequency = frequency,
                embedding = embedding
            )
        )

        Log.d("ADDED NODE", "Inserted new node: $name")
        return
    }

    fun addEdgeIntoDbQuery(fromId: Long, toId: Long, edgeType: String){
        edgesBox.put(
            EdgeEntity(
                fromId = fromId, toId = toId, edgeType = edgeType
            )
        )
        Log.d("ADDED EDGE", "Inserted new edge")
        return
    }

    fun findNodeByNameTypeQuery(name: String, type: String) : NodeEntity? {
        val nodeFound = nodesBox
            .query(NodeEntity_.name.equal(name).and(NodeEntity_.type.equal(type)))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findNodeByIdQuery(inputId: Long) : NodeEntity? {
        val nodeFound = nodesBox
            .query(NodeEntity_.id.equal(inputId))
            .build()
            .findFirst()

        return nodeFound
    }

    fun incrementFreqOfNodeQuery(node: NodeEntity) {
        node.frequency = node.frequency?.plus(1)
        nodesBox.put(node)
        println("Node value incremented: $node")
    }

    fun findAllNodesQuery() : List<NodeEntity> {
        return nodesBox.query().build().find()
    }

    fun findAllEdgesQuery() : List<EdgeEntity> {
        return edgesBox.query().build().find()
    }

    fun findAllNodesWithoutEmbeddingQuery() : List<NodeWithoutEmbedding> {
        return nodesBox.query().build().find()
            .map { node ->
                NodeWithoutEmbedding(
                    id = node.id,
                    name = node.name,
                    type = node.type,
                    description = node.description,
                    frequency = node.frequency
                )
            }
    }

    fun findAllNodeFrequenciesQuery() : Map<Long, Int> {
        return nodesBox.query().build().find()
            .associate { node -> node.id to (node.frequency ?: 0) }
    }

    fun findAllEdgesAroundNodeIdQuery(id: Long): List<EdgeEntity> {
        return edgesBox.query(
            EdgeEntity_.fromId.equal(id).or(EdgeEntity_.toId.equal(id))
        ).build().find()
    }
}
