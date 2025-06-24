package com.example.graphapp.data

import android.content.Context
import com.example.graphapp.data.local.createDriver
import com.example.graphdb.Edge
import com.example.graphdb.GraphDatabase
import com.example.graphdb.Node

class GraphRepository (context: Context) {
    private val database = GraphDatabase(createDriver(context))
    private val queries = database.graphDatabaseQueries

    fun getAllNodes(): List<Node> {
        return queries.selectAllNodes().executeAsList()
    }

    fun getAllEdges(): List<Edge> {
        return queries.selectAllEdges().executeAsList()
    }

    fun insertNode(name: String, nodeType: String) {
        queries.insertNode(name, nodeType)
    }

    fun insertEdge(fromNode: String, toNode: String, relationType: String) {
        queries.insertEdge(
            queries.findNodeByName(fromNode).executeAsOne(),
            queries.findNodeByName(toNode).executeAsOne(),
            relationType
        )
    }

    fun initialiseDatabase() {
        queries.insertNode("John", "Person")
        queries.insertNode("Alice", "Person")
        queries.insertNode("Bob", "Person")
        queries.insertNode("Visited Museum", "Activity")
        queries.insertNode("Watched Movie", "Activity")
        queries.insertNode("Attended Concert", "Activity")
        queries.insertNode("2025-06-01", "Time")
        queries.insertNode("2025-06-02", "Time")
        queries.insertNode("2025-06-03", "Time")
        queries.insertNode("New York", "Location")
        queries.insertNode("Tokyo", "Location")
        queries.insertNode("London", "Location")
        queries.insertNode("Smartphone", "Device")
        queries.insertNode("Laptop", "Device")
        queries.insertNode("Smartwatch", "Device")

        queries.insertEdge(queries.findNodeByName("John").executeAsOne(), queries.findNodeByName("Visited Museum").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Alice").executeAsOne(), queries.findNodeByName("Visited Museum").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Bob").executeAsOne(), queries.findNodeByName("Watched Movie").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Alice").executeAsOne(), queries.findNodeByName("Watched Movie").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("John").executeAsOne(), queries.findNodeByName("Attended Concert").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Visited Museum").executeAsOne(), queries.findNodeByName("2025-06-01").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Watched Movie").executeAsOne(), queries.findNodeByName("2025-06-02").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Attended Concert").executeAsOne(), queries.findNodeByName("2025-06-03").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Visited Museum").executeAsOne(), queries.findNodeByName("New York").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Watched Movie").executeAsOne(), queries.findNodeByName("Tokyo").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Attended Concert").executeAsOne(), queries.findNodeByName("London").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Visited Museum").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Watched Movie").executeAsOne(), queries.findNodeByName("Laptop").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Attended Concert").executeAsOne(), queries.findNodeByName("Smartwatch").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Alice").executeAsOne(), queries.findNodeByName("Attended Concert").executeAsOne(), "Who")
    }
}