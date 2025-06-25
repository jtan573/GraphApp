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

    fun getNodeTypes(): List<String> {
        return queries.getNodeTypes().executeAsList()
    }

    fun getEdgeTypes(): List<String> {
        return queries.getEdgeTypes().executeAsList()
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
        queries.insertEdge(queries.findNodeByName("Emily").executeAsOne(), queries.findNodeByName("Spotify").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Liam").executeAsOne(), queries.findNodeByName("Mint").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Olivia").executeAsOne(), queries.findNodeByName("Nike Training Club").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Noah").executeAsOne(), queries.findNodeByName("Clash of Clans").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Ava").executeAsOne(), queries.findNodeByName("Amazon").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Sophia").executeAsOne(), queries.findNodeByName("Notion").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Emily").executeAsOne(), queries.findNodeByName("Calm").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Emily").executeAsOne(), queries.findNodeByName("YouTube").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Emily").executeAsOne(), queries.findNodeByName("Nike Training Club").executeAsOne(), "Who")  // add if not already
        queries.insertEdge(queries.findNodeByName("Daniel").executeAsOne(), queries.findNodeByName("Snapchat").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Daniel").executeAsOne(), queries.findNodeByName("YouTube").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Grace").executeAsOne(), queries.findNodeByName("Google Fit").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Grace").executeAsOne(), queries.findNodeByName("Trello").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Grace").executeAsOne(), queries.findNodeByName("Headspace").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByName("Ava").executeAsOne(), queries.findNodeByName("Zara").executeAsOne(), "Who")

        // How → DeviceType
        queries.insertEdge(queries.findNodeByName("Spotify").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Mint").executeAsOne(), queries.findNodeByName("Laptop").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Nike Training Club").executeAsOne(), queries.findNodeByName("Smartwatch").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Clash of Clans").executeAsOne(), queries.findNodeByName("Tablet").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Amazon").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Notion").executeAsOne(), queries.findNodeByName("Laptop").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Calm").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("YouTube").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Google Fit").executeAsOne(), queries.findNodeByName("Smartwatch").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Trello").executeAsOne(), queries.findNodeByName("Laptop").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Snapchat").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Zara").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByName("Headspace").executeAsOne(), queries.findNodeByName("Smartphone").executeAsOne(), "How")

        // When → Duration
        queries.insertEdge(queries.findNodeByName("Spotify").executeAsOne(), queries.findNodeByName("30–60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Mint").executeAsOne(), queries.findNodeByName("<15 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Nike Training Club").executeAsOne(), queries.findNodeByName(">60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Clash of Clans").executeAsOne(), queries.findNodeByName("30–60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Amazon").executeAsOne(), queries.findNodeByName("15–30 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Notion").executeAsOne(), queries.findNodeByName("30–60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Calm").executeAsOne(), queries.findNodeByName("15–30 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("YouTube").executeAsOne(), queries.findNodeByName("30–60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Google Fit").executeAsOne(), queries.findNodeByName(">60 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Trello").executeAsOne(), queries.findNodeByName("15–30 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Snapchat").executeAsOne(), queries.findNodeByName("15–30 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Zara").executeAsOne(), queries.findNodeByName("<15 mins").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByName("Headspace").executeAsOne(), queries.findNodeByName("15–30 mins").executeAsOne(), "When")

        // Where → Country
        queries.insertEdge(queries.findNodeByName("Spotify").executeAsOne(), queries.findNodeByName("Brazil").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Mint").executeAsOne(), queries.findNodeByName("United States").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Nike Training Club").executeAsOne(), queries.findNodeByName("Germany").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Clash of Clans").executeAsOne(), queries.findNodeByName("Japan").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Amazon").executeAsOne(), queries.findNodeByName("India").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Notion").executeAsOne(), queries.findNodeByName("Germany").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Calm").executeAsOne(), queries.findNodeByName("India").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("YouTube").executeAsOne(), queries.findNodeByName("India").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Google Fit").executeAsOne(), queries.findNodeByName("United States").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Trello").executeAsOne(), queries.findNodeByName("Germany").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Snapchat").executeAsOne(), queries.findNodeByName("Japan").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Zara").executeAsOne(), queries.findNodeByName("Brazil").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByName("Headspace").executeAsOne(), queries.findNodeByName("Germany").executeAsOne(), "Where")

        // Why → Category
        queries.insertEdge(queries.findNodeByName("Spotify").executeAsOne(), queries.findNodeByName("Music").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Mint").executeAsOne(), queries.findNodeByName("Finance").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Nike Training Club").executeAsOne(), queries.findNodeByName("Fitness").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Clash of Clans").executeAsOne(), queries.findNodeByName("Games").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Amazon").executeAsOne(), queries.findNodeByName("Shopping").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Notion").executeAsOne(), queries.findNodeByName("Productivity").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Calm").executeAsOne(), queries.findNodeByName("Lifestyle").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("YouTube").executeAsOne(), queries.findNodeByName("Lifestyle").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Google Fit").executeAsOne(), queries.findNodeByName("Fitness").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Trello").executeAsOne(), queries.findNodeByName("Productivity").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Snapchat").executeAsOne(), queries.findNodeByName("Lifestyle").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Zara").executeAsOne(), queries.findNodeByName("Shopping").executeAsOne(), "Why")
        queries.insertEdge(queries.findNodeByName("Headspace").executeAsOne(), queries.findNodeByName("Lifestyle").executeAsOne(), "Why")

    }
}