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

    fun insertNode(newNodeName: String, newNodeType: String) {
        val count = queries.checkDuplicateNode(newNodeName, newNodeType).executeAsOne()
        if (count > 0) {
            return
        } else {
            queries.insertNode(newNodeName, newNodeType)
        }
    }

    fun insertEdge(fromNodeName: String, fromNodeType: String,
                   toNodeName: String, toNodeType: String,
                   relationType: String) {
        val fromNodeId = queries.findNodeByNameAndType(fromNodeName, fromNodeType).executeAsOne()
        val toNodeId = queries.findNodeByNameAndType(toNodeName, toNodeType).executeAsOne()

        val count = queries.checkDuplicateEdge(fromNodeId, toNodeId, relationType).executeAsOne()
        if (count > 0) {
            return
        } else {
            queries.insertEdge(fromNodeId, toNodeId, relationType)
        }
    }

    fun findNodeById(id: Long): Node? {
        return queries.findNodeById(id).executeAsOneOrNull()
    }

    fun findNodeByNameAndType(name: String, type: String): Long {
        return queries.findNodeByNameAndType(name, type).executeAsOne()
    }

    fun getNodeTypes(): List<String> {
        return queries.getNodeTypes().executeAsList()
    }

    fun getEdgeTypes(): List<String> {
        return queries.getEdgeTypes().executeAsList()
    }

    fun getNeighborsOfNodeById(id: Long): List<Node> {
        return queries.getNeighborsOfNodeById(id, id, id, id).executeAsList()
    }

    fun findFromNodeByToNode(id: Long): List<Long> {
        return queries.findFromNodeByToNode(id).executeAsList()
    }

    fun getEdgeBetweenNodes(id1: Long, id2: Long): List<Edge> {
        return queries.getEdgeBetweenNodes(id1, id2, id1, id2).executeAsList()
    }

    fun initialiseDatabase() {
        // Users
        queries.insertNode("Emily", "User")
        queries.insertNode("Liam", "User")
        queries.insertNode("Olivia", "User")
        queries.insertNode("Noah", "User")
        queries.insertNode("Ava", "User")
        queries.insertNode("Sophia", "User")
        queries.insertNode("Daniel", "User")
        queries.insertNode("Grace", "User")

        // Apps
        queries.insertNode("Spotify", "App")
        queries.insertNode("Nike Training Club", "App")
        queries.insertNode("Mint", "App")
        queries.insertNode("Clash of Clans", "App")
        queries.insertNode("Amazon", "App")
        queries.insertNode("Notion", "App")
        queries.insertNode("Calm", "App")
        queries.insertNode("YouTube", "App")
        queries.insertNode("Google Fit", "App")
        queries.insertNode("Trello", "App")
        queries.insertNode("Snapchat", "App")
        queries.insertNode("Zara", "App")
        queries.insertNode("Headspace", "App")

        // Categories (Why)
        queries.insertNode("Music", "Category")
        queries.insertNode("Fitness", "Category")
        queries.insertNode("Finance", "Category")
        queries.insertNode("Games", "Category")
        queries.insertNode("Shopping", "Category")
        queries.insertNode("Productivity", "Category")
        queries.insertNode("Lifestyle", "Category")

        // Countries (Where)
        queries.insertNode("United States", "Country")
        queries.insertNode("Japan", "Country")
        queries.insertNode("Germany", "Country")
        queries.insertNode("India", "Country")
        queries.insertNode("Brazil", "Country")

        // Duration (When)
        queries.insertNode("<15 mins", "Duration")
        queries.insertNode("15–30 mins", "Duration")
        queries.insertNode("30–60 mins", "Duration")
        queries.insertNode(">60 mins", "Duration")

        // Device Types (How)
        queries.insertNode("Smartphone", "DeviceType")
        queries.insertNode("Laptop", "DeviceType")
        queries.insertNode("Tablet", "DeviceType")
        queries.insertNode("Smartwatch", "DeviceType")

        // Who → App
        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Liam", "User").executeAsOne(), queries.findNodeByNameAndType("Mint", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Olivia", "User").executeAsOne(), queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Noah", "User").executeAsOne(), queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Ava", "User").executeAsOne(), queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Sophia", "User").executeAsOne(), queries.findNodeByNameAndType("Notion", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Calm", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), "Who")  // add if not already
        queries.insertEdge(queries.findNodeByNameAndType("Daniel", "User").executeAsOne(), queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Daniel", "User").executeAsOne(), queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Trello", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Ava", "User").executeAsOne(), queries.findNodeByNameAndType("Zara", "App").executeAsOne(), "Who")

        // How → DeviceType
        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Smartwatch", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Tablet", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("Smartwatch", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")

        // When → Duration
        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("<15 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType(">60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType(">60 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("<15 mins", "Duration").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")

        // Where → Country
        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Brazil", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("United States", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Japan", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("United States", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Japan", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Brazil", "Country").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")

        // Why → Category
        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Music", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("Finance", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Fitness", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Games", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("Shopping", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Productivity", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("Fitness", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Productivity", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Shopping", "Category").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")

    }
}