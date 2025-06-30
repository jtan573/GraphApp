package com.example.graphapp.data

import android.content.Context
import android.util.Log
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

    fun getEdgeBetweenNodes(id1: Long, id2: Long): List<Edge> {
        return queries.getEdgeBetweenNodes(id1, id2, id1, id2).executeAsList()
    }

    fun initialiseDatabase() {
        // Article 1
        queries.insertNode("Bombing", "Article")
        queries.insertNode("Group Alpha", "Entity")
        queries.insertNode("Explosives", "Method")
        queries.insertNode("2022-06-12", "Date")
        queries.insertNode("Market District", "Location")
        queries.insertNode("Intimidation", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Alpha", "Entity").executeAsOne(), queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Explosives", "Method").executeAsOne(), queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2022-06-12", "Date").executeAsOne(), queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Market District", "Location").executeAsOne(), queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Intimidation", "Motive").executeAsOne(), queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(), "Why")

        // Article 2
        queries.insertNode("Shooting", "Article")
        queries.insertNode("Individual X", "Entity")
        queries.insertNode("Firearm", "Method")
        queries.insertNode("2021-03-04", "Date")
        queries.insertNode("Embassy Area", "Location")
        queries.insertNode("Revenge", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual X", "Entity").executeAsOne(), queries.findNodeByNameAndType("Shooting", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Firearm", "Method").executeAsOne(), queries.findNodeByNameAndType("Shooting", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2021-03-04", "Date").executeAsOne(), queries.findNodeByNameAndType("Shooting", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Embassy Area", "Location").executeAsOne(), queries.findNodeByNameAndType("Shooting", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Revenge", "Motive").executeAsOne(), queries.findNodeByNameAndType("Shooting", "Article").executeAsOne(), "Why")

// Article 3
        queries.insertNode("Hostage Situation", "Article")
        queries.insertNode("Group Beta", "Entity")
        queries.insertNode("Hostage Taking", "Method")
        queries.insertNode("2020-08-19", "Date")
        queries.insertNode("Hotel Complex", "Location")
        queries.insertNode("Negotiation Leverage", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Beta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Hostage Taking", "Method").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2020-08-19", "Date").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Hotel Complex", "Location").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Negotiation Leverage", "Motive").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Why")

// Article 4
        queries.insertNode("Suicide Bombing", "Article")
        queries.insertNode("Sect Zeta", "Entity")
        queries.insertNode("Suicide Vest", "Method")
        queries.insertNode("2021-11-03", "Date")
        queries.insertNode("Train Station", "Location")
        queries.insertNode("Religious Motivation", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Sect Zeta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Suicide Vest", "Method").executeAsOne(), queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2021-11-03", "Date").executeAsOne(), queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Train Station", "Location").executeAsOne(), queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Religious Motivation", "Motive").executeAsOne(), queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(), "Why")

// Article 5
        queries.insertNode("Knife Attack", "Article")
        queries.insertNode("Individual Y", "Entity")
        queries.insertNode("Knife", "Method")
        queries.insertNode("2020-09-15", "Date")
        queries.insertNode("Shopping Center", "Location")
        queries.insertNode("Personal Grievance", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual Y", "Entity").executeAsOne(), queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Knife", "Method").executeAsOne(), queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2020-09-15", "Date").executeAsOne(), queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Shopping Center", "Location").executeAsOne(), queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Personal Grievance", "Motive").executeAsOne(), queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(), "Why")

// Article 6
        queries.insertNode("Vehicle Attack", "Article")
        queries.insertNode("Group Gamma", "Entity")
        queries.insertNode("Truck", "Method")
        queries.insertNode("2019-07-22", "Date")
        queries.insertNode("City Square", "Location")
        queries.insertNode("Maximize Casualties", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Gamma", "Entity").executeAsOne(), queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Truck", "Method").executeAsOne(), queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2019-07-22", "Date").executeAsOne(), queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("City Square", "Location").executeAsOne(), queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Maximize Casualties", "Motive").executeAsOne(), queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(), "Why")

// Article 7
        queries.insertNode("Arson", "Article")
        queries.insertNode("Individual Z", "Entity")
        queries.insertNode("Incendiary Device", "Method")
        queries.insertNode("2020-02-11", "Date")
        queries.insertNode("Warehouse District", "Location")
        queries.insertNode("Economic Disruption", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual Z", "Entity").executeAsOne(), queries.findNodeByNameAndType("Arson", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Incendiary Device", "Method").executeAsOne(), queries.findNodeByNameAndType("Arson", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2020-02-11", "Date").executeAsOne(), queries.findNodeByNameAndType("Arson", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Warehouse District", "Location").executeAsOne(), queries.findNodeByNameAndType("Arson", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Economic Disruption", "Motive").executeAsOne(), queries.findNodeByNameAndType("Arson", "Article").executeAsOne(), "Why")

// Article 8
        queries.insertNode("Cyber Attack", "Article")
        queries.insertNode("Group Delta", "Entity")
        queries.insertNode("Malware", "Method")
        queries.insertNode("2021-05-30", "Date")
        queries.insertNode("Government Servers", "Location")
        queries.insertNode("Data Theft", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Delta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Malware", "Method").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2021-05-30", "Date").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Government Servers", "Location").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Data Theft", "Motive").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "Why")

// Article 9
        queries.insertNode("Grenade Attack", "Article")
        queries.insertNode("Group Epsilon", "Entity")
        queries.insertNode("Grenades", "Method")
        queries.insertNode("2019-10-05", "Date")
        queries.insertNode("Police Station", "Location")
        queries.insertNode("Weaken Law Enforcement", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Epsilon", "Entity").executeAsOne(), queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Grenades", "Method").executeAsOne(), queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2019-10-05", "Date").executeAsOne(), queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Police Station", "Location").executeAsOne(), queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Weaken Law Enforcement", "Motive").executeAsOne(), queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(), "Why")

// Article 10
        queries.insertNode("Bomb Threat", "Article")
        queries.insertNode("Individual Q", "Entity")
        queries.insertNode("Phone Call", "Method")
        queries.insertNode("2020-12-01", "Date")
        queries.insertNode("School Building", "Location")
        queries.insertNode("Evacuation", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual Q", "Entity").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Phone Call", "Method").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2020-12-01", "Date").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("School Building", "Location").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Evacuation", "Motive").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Why")

        // Article 11
        queries.insertNode("Explosion", "Article")
//        queries.insertEdge(queries.findNodeByNameAndType("Group Alpha", "Entity").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Explosives", "Method").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "How")
        queries.insertNode("2021-08-08", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2021-08-08", "Date").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Market District", "Location").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Intimidation", "Motive").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "Why")

// Article 12
        queries.insertNode("Library Arson", "Article")
//        queries.insertNode("Individual Z", "Entity")
        queries.insertNode("Firebomb", "Method")
        queries.insertNode("2021-03-12", "Date")
        queries.insertNode("City Library", "Location")
        queries.insertNode("Destroy Records", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual Z", "Entity").executeAsOne(), queries.findNodeByNameAndType("Library Arson", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Firebomb", "Method").executeAsOne(), queries.findNodeByNameAndType("Library Arson", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2021-03-12", "Date").executeAsOne(), queries.findNodeByNameAndType("Library Arson", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("City Library", "Location").executeAsOne(), queries.findNodeByNameAndType("Library Arson", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Destroy Records", "Motive").executeAsOne(), queries.findNodeByNameAndType("Library Arson", "Article").executeAsOne(), "Why")

// Article 13
        queries.insertNode("Cyber Breach", "Article")
        queries.insertEdge(queries.findNodeByNameAndType("Group Delta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Malware", "Method").executeAsOne(), queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(), "How")
        queries.insertNode("2022-03-15", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2022-03-15", "Date").executeAsOne(), queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Government Servers", "Location").executeAsOne(), queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Data Theft", "Motive").executeAsOne(), queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(), "Why")

// Article 14
        queries.insertNode("Truck Ramming", "Article")
        queries.insertEdge(queries.findNodeByNameAndType("Group Gamma", "Entity").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Truck", "Method").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "How")
        queries.insertNode("2019-12-25", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2019-12-25", "Date").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("City Square", "Location").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Maximize Casualties", "Motive").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "Why")

// Article 15
//        queries.insertEdge(queries.findNodeByNameAndType("Group Beta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Hostage Taking", "Method").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "How")
//        queries.insertNode("2021-04-17", "Date")
//        queries.insertEdge(queries.findNodeByNameAndType("2021-04-17", "Date").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Hotel Complex", "Location").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Negotiation Leverage", "Motive").executeAsOne(), queries.findNodeByNameAndType("Hostage Situation", "Article").executeAsOne(), "Why")

// Article 16
        queries.insertNode("Stabbing", "Article")
        queries.insertNode("Individual V", "Entity")
        queries.insertNode("2022-02-20", "Date")
        queries.insertNode("Random Violence", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Individual V", "Entity").executeAsOne(), queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Knife", "Method").executeAsOne(), queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2022-02-20", "Date").executeAsOne(), queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Train Station", "Location").executeAsOne(), queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Random Violence", "Motive").executeAsOne(), queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(), "Why")

// Article 17
        queries.insertEdge(queries.findNodeByNameAndType("Individual Q", "Entity").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Phone Call", "Method").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "How")
        queries.insertNode("2022-06-06", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2022-06-06", "Date").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("School Building", "Location").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Evacuation", "Motive").executeAsOne(), queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(), "Why")

// Article 18
        queries.insertNode("Arson Attack", "Article")
//        queries.insertEdge(queries.findNodeByNameAndType("Individual Z", "Entity").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Incendiary Device", "Method").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "How")
        queries.insertNode("2021-09-09", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2021-09-09", "Date").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Warehouse District", "Location").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Economic Disruption", "Motive").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "Why")

// Article 19
        queries.insertNode("Cybersecurity Breach", "Article")
        queries.insertNode("Group Theta", "Entity")
        queries.insertNode("Ransomware", "Method")
        queries.insertNode("2022-05-05", "Date")
        queries.insertNode("Hospital Network", "Location")
        queries.insertNode("Financial Gain", "Motive")

        queries.insertEdge(queries.findNodeByNameAndType("Group Theta", "Entity").executeAsOne(), queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Ransomware", "Method").executeAsOne(), queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(), "How")
        queries.insertEdge(queries.findNodeByNameAndType("2022-05-05", "Date").executeAsOne(), queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(), "When")
        queries.insertEdge(queries.findNodeByNameAndType("Hospital Network", "Location").executeAsOne(), queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(), "Where")
        queries.insertEdge(queries.findNodeByNameAndType("Financial Gain", "Motive").executeAsOne(), queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(), "Why")

// Article 20
        queries.insertNode("Grenade Explosion", "Article")
        queries.insertEdge(queries.findNodeByNameAndType("Group Epsilon", "Entity").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "Who")
        queries.insertEdge(queries.findNodeByNameAndType("Grenades", "Method").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "How")
        queries.insertNode("2020-03-03", "Date")
        queries.insertEdge(queries.findNodeByNameAndType("2020-03-03", "Date").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Police Station", "Location").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Weaken Law Enforcement", "Motive").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "Why")

    }

//    fun initialiseDatabase() {
//        // Users
//        queries.insertNode("Emily", "User")
//        queries.insertNode("Liam", "User")
//        queries.insertNode("Olivia", "User")
//        queries.insertNode("Noah", "User")
//        queries.insertNode("Ava", "User")
//        queries.insertNode("Sophia", "User")
//        queries.insertNode("Daniel", "User")
//        queries.insertNode("Grace", "User")
//
//        // Apps
//        queries.insertNode("Spotify", "App")
//        queries.insertNode("Nike Training Club", "App")
//        queries.insertNode("Mint", "App")
//        queries.insertNode("Clash of Clans", "App")
//        queries.insertNode("Amazon", "App")
//        queries.insertNode("Notion", "App")
//        queries.insertNode("Calm", "App")
//        queries.insertNode("YouTube", "App")
//        queries.insertNode("Google Fit", "App")
//        queries.insertNode("Trello", "App")
//        queries.insertNode("Snapchat", "App")
//        queries.insertNode("Zara", "App")
//        queries.insertNode("Headspace", "App")
//
//        // Categories (Why)
//        queries.insertNode("Music", "Category")
//        queries.insertNode("Fitness", "Category")
//        queries.insertNode("Finance", "Category")
//        queries.insertNode("Games", "Category")
//        queries.insertNode("Shopping", "Category")
//        queries.insertNode("Productivity", "Category")
//        queries.insertNode("Lifestyle", "Category")
//
//        // Countries (Where)
//        queries.insertNode("United States", "Country")
//        queries.insertNode("Japan", "Country")
//        queries.insertNode("Germany", "Country")
//        queries.insertNode("India", "Country")
//        queries.insertNode("Brazil", "Country")
//
//        // Duration (When)
//        queries.insertNode("<15 mins", "Duration")
//        queries.insertNode("15–30 mins", "Duration")
//        queries.insertNode("30–60 mins", "Duration")
//        queries.insertNode(">60 mins", "Duration")
//
//        // Device Types (How)
//        queries.insertNode("Smartphone", "DeviceType")
//        queries.insertNode("Laptop", "DeviceType")
//        queries.insertNode("Tablet", "DeviceType")
//        queries.insertNode("Smartwatch", "DeviceType")
//
//        // Who → App
//        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Liam", "User").executeAsOne(), queries.findNodeByNameAndType("Mint", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Olivia", "User").executeAsOne(), queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Noah", "User").executeAsOne(), queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Ava", "User").executeAsOne(), queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Sophia", "User").executeAsOne(), queries.findNodeByNameAndType("Notion", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Calm", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Emily", "User").executeAsOne(), queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), "Who")  // add if not already
//        queries.insertEdge(queries.findNodeByNameAndType("Daniel", "User").executeAsOne(), queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Daniel", "User").executeAsOne(), queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Trello", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Grace", "User").executeAsOne(), queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), "Who")
//        queries.insertEdge(queries.findNodeByNameAndType("Ava", "User").executeAsOne(), queries.findNodeByNameAndType("Zara", "App").executeAsOne(), "Who")
//
//        // How → DeviceType
//        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Smartwatch", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Tablet", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("Smartwatch", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Laptop", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Smartphone", "DeviceType").executeAsOne(), "How")
//
//        // When → Duration
//        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("<15 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType(">60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("30–60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType(">60 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("<15 mins", "Duration").executeAsOne(), "When")
//        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("15–30 mins", "Duration").executeAsOne(), "When")
//
//        // Where → Country
//        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Brazil", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("United States", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Japan", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("India", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("United States", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Japan", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Brazil", "Country").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Germany", "Country").executeAsOne(), "Where")
//
//        // Why → Category
//        queries.insertEdge(queries.findNodeByNameAndType("Spotify", "App").executeAsOne(), queries.findNodeByNameAndType("Music", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Mint", "App").executeAsOne(), queries.findNodeByNameAndType("Finance", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Nike Training Club", "App").executeAsOne(), queries.findNodeByNameAndType("Fitness", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Clash of Clans", "App").executeAsOne(), queries.findNodeByNameAndType("Games", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Amazon", "App").executeAsOne(), queries.findNodeByNameAndType("Shopping", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Notion", "App").executeAsOne(), queries.findNodeByNameAndType("Productivity", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Calm", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("YouTube", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Google Fit", "App").executeAsOne(), queries.findNodeByNameAndType("Fitness", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Trello", "App").executeAsOne(), queries.findNodeByNameAndType("Productivity", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Snapchat", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Zara", "App").executeAsOne(), queries.findNodeByNameAndType("Shopping", "Category").executeAsOne(), "Why")
//        queries.insertEdge(queries.findNodeByNameAndType("Headspace", "App").executeAsOne(), queries.findNodeByNameAndType("Lifestyle", "Category").executeAsOne(), "Why")
//    }
}