package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.local.Event
import com.example.graphapp.data.schema.graphCompletion
import com.example.graphapp.data.schema.GraphSchema.edgeLabels
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.schema.GraphScoringStrategy
import com.example.graphapp.data.schema.SimRankScoring
import com.example.graphapp.data.schema.createCompleteGraph
import com.example.graphdb.Edge
import com.example.graphdb.Node
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GraphRepository(application)

    private val _graphData = MutableStateFlow<String?>(null)
    val graphData: StateFlow<String?> = _graphData

    private val _createdEvents = MutableStateFlow<List<Event>>(emptyList())
    val createdEvents: StateFlow<List<Event>> = _createdEvents

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initialiseDatabase()
            val nodes = repository.getAllNodes()
            val edges = repository.getAllEdges()
            val json = convertToJson(nodes, edges)
            _graphData.value = json
        }
    }

    fun getNodeTypes(): List<String> {
        return repository.getNodeTypes()
    }

    private fun convertToJson(nodes: List<Node>, edges: List<Edge>): String {
        val gson = Gson()
        val nodeList = nodes.map { mapOf("id" to it.name, "type" to it.type) }
        val edgeList = edges.map { edge ->
            val source = nodes.find { it.id == edge.fromNode }?.name
            val target = nodes.find { it.id == edge.toNode }?.name
            mapOf("source" to source, "target" to target, "label" to edge.relationType)
        }
        val json = mapOf("nodes" to nodeList, "links" to edgeList)
        return gson.toJson(json)
    }

    private fun reloadGraphData() {
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges()
        val json = convertToJson(nodes, edges)
        _graphData.value = json
    }

    fun createEvent(map: Map<String, String>) {
        val normalizedMap = map.filterValues { it.isNotBlank() }

        if (normalizedMap.isEmpty()) { return }

        for ((type, value) in normalizedMap) {
            repository.insertNode(value, type)
        }

        for ((type1, value1) in normalizedMap) {
            for ((type2, value2) in normalizedMap) {
                if (type1 != type2) {
                    val edgeType = edgeLabels["$type1-$type2"]
                    if (edgeType != null) {
                        repository.insertEdge(fromNodeName = value1, fromNodeType = type1,
                            toNodeName = value2, toNodeType = type2, relationType = edgeType)
                    }
                }
            }
        }

        // Refresh Main Screen Graph
        reloadGraphData()

        // Add to event logs
        val currentList = _createdEvents.value.toMutableList()
        currentList.add(Event(normalizedMap))
        _createdEvents.value = currentList

        // Predict possible new links
        val predictions = predictTopLinks(normalizedMap, SimRankScoring)

        val eventKeyIds = normalizedMap.entries.mapNotNull { (type, value) ->
            if (type in keyNodes) {
                repository.findNodeByNameAndType(value, type)
            } else {
                null
            }
        }
        createFilteredGraph(predictions, eventKeyIds)
    }

    fun fillMissingLinks() {
        val completions = graphCompletion(repository)
        val newEdges = createCompleteGraph(completions)
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges() + newEdges
        val json = convertToJson(nodes, edges)
        _graphData.value = json
        return
    }

    private fun predictTopLinks(
        map: Map<String, String>,
        scoringAlgo: GraphScoringStrategy
    ): Map<String, List<Pair<Long, Float>>> {

        val predictions = scoringAlgo.score(map, repository)

        // For debugging
        for ((type, predictionsForType) in predictions) {
            for ((nodeId, similarity) in predictionsForType) {
                val node = repository.findNodeById(nodeId)
                if (node != null) {
                    Log.d(
                        "Prediction",
                        "Type=$type Predicted: ${node.name}, Similarity=$similarity"
                    )
                }
            }
        }

        return predictions
    }

    private fun createFilteredGraph(
        predictions: Map<String, List<Pair<Long, Float>>>,
        eventKeyIds: List<Long>
    ) {
        val neighborNodes = mutableListOf<Node>()
        val neighborEdges = mutableListOf<Edge>()

        val allPredictedNodes: List<Long> = predictions.values.flatten().map { it.first }

        for (id in (allPredictedNodes + eventKeyIds)) {
            val nodes = repository.getNeighborsOfNodeById(id)
            for (n in nodes) {
                val edges = repository.getEdgeBetweenNodes(id, n.id)
                neighborEdges.addAll(edges.filter { it !in neighborEdges })
            }
            neighborNodes.addAll(nodes.filter { it !in neighborNodes })
            repository.findNodeById(id)?.let { node ->
                if (node !in neighborNodes) {
                    neighborNodes.add(node)
                }
            }
        }

        for (id in eventKeyIds) {
            for ((type, nodeList) in predictions) {
                for ((predictedId, _) in nodeList) {
                    val relationType = "Suggest-$type"
                    val newEdge = Edge(
                        id = -1L,
                        fromNode = id,
                        toNode = predictedId,
                        relationType = relationType
                    )
                    neighborEdges.add(newEdge)
                    Log.d(
                        "New Edges",
                        "Type=$type Predicted: $newEdge"
                    )
                }
            }
        }

        val json = convertToJson(neighborNodes, neighborEdges)
        _filteredGraphData.value = json
    }
}