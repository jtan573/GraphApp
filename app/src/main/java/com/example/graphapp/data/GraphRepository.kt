package com.example.graphapp.data

import android.content.Context
import com.example.graphapp.data.local.createDriver
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphdb.Edge
import com.example.graphdb.GraphDatabase
import com.example.graphdb.Node

class GraphRepository (context: Context) {
    private val database = GraphDatabase(createDriver(context))
    private val queries = database.graphDatabaseQueries

    fun getAllNodes(): List<Node> {
        return queries.selectAllNodes().executeAsList()
    }

    fun getAllKeyNodeIds(): List<Long> {
        return getAllNodes()
            .filter { it.type in keyNodes }
            .map { it.id }
    }

    fun getAllEdges(): List<Edge> {
        return queries.selectAllEdges().executeAsList()
    }

    fun insertNode(newNodeName: String, newNodeType: String, newNodeDesc: String?) {
        val count = queries.checkDuplicateNode(newNodeName, newNodeType).executeAsOne()
        if (count > 0) {
            return
        } else {
            queries.insertNode(newNodeName, newNodeType, newNodeDesc)
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

    fun getDescOfNodeById(id: Long): Node {
        return queries.getDescOfNodeById(id).executeAsOne()
    }

    fun getEdgeBetweenNodes(id1: Long, id2: Long): List<Edge> {
        return queries.getEdgeBetweenNodes(id1, id2, id1, id2).executeAsList()
    }

    fun initialiseDatabase() {
        queries.insertNode("Bombing", "Article", null)
        queries.insertNode("Group Alpha", "Entity", "A leading organization characterized by strong internal cohesion.")
        queries.insertNode("Explosives", "Method", null)
        queries.insertNode("2022-06-12", "Date", null)
        queries.insertNode("Market District", "Location", null)
        queries.insertNode("Intimidation", "Motive", null)

        queries.insertEdge(
            queries.findNodeByNameAndType("Group Alpha", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Explosives", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2022-06-12", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Market District", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Intimidation", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Bombing", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Suicide Bombing", "Article", null)
        queries.insertNode("Sect Zeta", "Entity", "A specialized faction known for its unique ideology and practices.")
        queries.insertNode("Suicide Vest", "Method", null)
        queries.insertNode("2021-11-03", "Date", null)
        queries.insertNode("Train Station", "Location", null)
        queries.insertNode("Religious Motivation", "Motive", "Based on ideological or faith-driven objectives.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Sect Zeta", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Suicide Vest", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2021-11-03", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Train Station", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Religious Motivation", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Suicide Bombing", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Knife Attack", "Article", null)
        queries.insertNode("Individual Y", "Entity", "A key figure known for their independent actions.")
        queries.insertNode("Knife", "Method", null)
        queries.insertNode("2020-09-15", "Date", null)
        queries.insertNode("Shopping Center", "Location", null)
        queries.insertNode("Personal Grievance", "Motive", "Driven by individual resentment or perceived injustice.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Individual Y", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Knife", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2020-09-15", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Shopping Center", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Personal Grievance", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Knife Attack", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Vehicle Attack", "Article", null)
        queries.insertNode("Group Gamma", "Entity", "An organized collective recognized for coordinated initiatives.")
        queries.insertNode("Truck", "Method", null)
        queries.insertNode("2019-07-22", "Date", null)
        queries.insertNode("City Square", "Location", null)
        queries.insertNode("Maximize Casualties", "Motive", "Intending to cause the highest possible loss of life.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Group Gamma", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Truck", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2019-07-22", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("City Square", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Maximize Casualties", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Vehicle Attack", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Arson", "Article", null)
        queries.insertNode("Individual Z", "Entity", "Notable for their influential role within the community.")
        queries.insertNode("Incendiary Device", "Method", null)
        queries.insertNode("2020-02-11", "Date", null)
        queries.insertNode("Warehouse District", "Location", null)
        queries.insertNode("Economic Disruption", "Motive", "Aiming to acquire money or valuable assets.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Individual Z", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Arson", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Incendiary Device", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Arson", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2020-02-11", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Arson", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Warehouse District", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Arson", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Economic Disruption", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Arson", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Cyber Attack", "Article", null)
        queries.insertNode("Group Delta", "Entity", "A prominent group engaged in various collaborative projects.")
        queries.insertNode("Malware", "Method", null)
        queries.insertNode("2021-05-30", "Date", null)
        queries.insertNode("Government Servers", "Location", null)
        queries.insertNode("Data Theft", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Group Delta", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Malware", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2021-05-30", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(),
            "When"
        )
//        queries.insertEdge(queries.findNodeByNameAndType("Government Servers", "Location").executeAsOne(), queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(), "Where")
        queries.insertEdge(
            queries.findNodeByNameAndType("Data Theft", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Attack", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Grenade Attack", "Article", null)
        queries.insertNode("Group Epsilon", "Entity", "Recognized for its strategic influence and structured organization.")
        queries.insertNode("Grenades", "Method", null)
        queries.insertNode("2019-10-05", "Date", null)
        queries.insertNode("Police Station", "Location", null)
        queries.insertNode("Weaken Law Enforcement", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Group Epsilon", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Grenades", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2019-10-05", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Police Station", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Weaken Law Enforcement", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Attack", "Article").executeAsOne(),
            "Why"
        )

        queries.insertNode("Bomb Threat", "Article", null)
        queries.insertNode("Individual Q", "Entity", "Has a reputation for decisive leadership and personal achievements.")
        queries.insertNode("Phone Call", "Method", null)
        queries.insertNode("2020-12-01", "Date", null)
        queries.insertNode("School Building", "Location", null)
        queries.insertNode("Evacuation", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Individual Q", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Phone Call", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2020-12-01", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("School Building", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Evacuation", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Bomb Threat", "Article").executeAsOne(),
            "Why"
        )

        // Article 11
        queries.insertNode("Explosion", "Article", null)
//        queries.insertEdge(queries.findNodeByNameAndType("Group Alpha", "Entity").executeAsOne(), queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(), "Who")
        queries.insertEdge(
            queries.findNodeByNameAndType("Explosives", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(),
            "How"
        )
        queries.insertNode("2021-08-08", "Date", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("2021-08-08", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Market District", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Intimidation", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Explosion", "Article").executeAsOne(),
            "Why"
        )

// Article 13
        queries.insertNode("Cyber Breach", "Article", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("Group Delta", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Malware", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(),
            "How"
        )
        queries.insertNode("2022-03-15", "Date", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("2022-03-15", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Government Servers", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Data Theft", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Cyber Breach", "Article").executeAsOne(),
            "Why"
        )

// Article 14
        queries.insertNode("Truck Ramming", "Article", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("Group Gamma", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Truck", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(),
            "How"
        )
        queries.insertNode("2019-12-25", "Date", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("2019-12-25", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("City Square", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(),
            "Where"
        )
//        queries.insertEdge(queries.findNodeByNameAndType("Maximize Casualties", "Motive").executeAsOne(), queries.findNodeByNameAndType("Truck Ramming", "Article").executeAsOne(), "Why")

// Article 16
        queries.insertNode("Stabbing", "Article", null)
        queries.insertNode("Individual V", "Entity", "Distinguished by their contributions and distinctive perspective.")
        queries.insertNode("2022-02-20", "Date", null)
        queries.insertNode("Random Violence", "Motive", "Involving unpredictable attacks without specific targets.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Individual V", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Knife", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2022-02-20", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Train Station", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Random Violence", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Stabbing", "Article").executeAsOne(),
            "Why"
        )

// Article 18
        queries.insertNode("Arson Attack", "Article", null)
//        queries.insertEdge(queries.findNodeByNameAndType("Individual Z", "Entity").executeAsOne(), queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(), "Who")
        queries.insertEdge(
            queries.findNodeByNameAndType("Incendiary Device", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(),
            "How"
        )
        queries.insertNode("2021-09-09", "Date", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("2021-09-09", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Warehouse District", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Economic Disruption", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Arson Attack", "Article").executeAsOne(),
            "Why"
        )

// Article 19
        queries.insertNode("Cybersecurity Breach", "Article", null)
        queries.insertNode("Group Theta", "Entity", "Operates as a cohesive unit with significant collective impact.")
        queries.insertNode("Ransomware", "Method", null)
        queries.insertNode("2022-05-05", "Date", null)
        queries.insertNode("Hospital Network", "Location", null)
        queries.insertNode("Financial Gain", "Motive", "Intending to destabilize financial systems or markets.")

        queries.insertEdge(
            queries.findNodeByNameAndType("Group Theta", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Ransomware", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(),
            "How"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("2022-05-05", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(),
            "When"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Hospital Network", "Location").executeAsOne(),
            queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(),
            "Where"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Financial Gain", "Motive").executeAsOne(),
            queries.findNodeByNameAndType("Cybersecurity Breach", "Article").executeAsOne(),
            "Why"
        )

// Article 20
        queries.insertNode("Grenade Explosion", "Article", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("Group Epsilon", "Entity").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(),
            "Who"
        )
        queries.insertEdge(
            queries.findNodeByNameAndType("Grenades", "Method").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(),
            "How"
        )
        queries.insertNode("2020-03-03", "Date", null)
        queries.insertEdge(
            queries.findNodeByNameAndType("2020-03-03", "Date").executeAsOne(),
            queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(),
            "When"
        )
//        queries.insertEdge(queries.findNodeByNameAndType("Police Station", "Location").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "Where")
//        queries.insertEdge(queries.findNodeByNameAndType("Weaken Law Enforcement", "Motive").executeAsOne(), queries.findNodeByNameAndType("Grenade Explosion", "Article").executeAsOne(), "Why")

// Inserting Description Nodes for Method
        queries.insertNode("Difficult to trace origin", "Description", null)
        queries.insertEdge(queries.findNodeByNameAndType("Difficult to trace origin", "Description").executeAsOne(), queries.findNodeByNameAndType("Malware", "Method").executeAsOne(), "Description")
        queries.insertEdge(queries.findNodeByNameAndType("Difficult to trace origin", "Description").executeAsOne(), queries.findNodeByNameAndType("Ransomware", "Method").executeAsOne(), "Description")

// Inserting Description Nodes for Motive
        queries.insertNode("Instil Fear", "Description", null)
        queries.insertEdge(queries.findNodeByNameAndType("Instil Fear", "Description").executeAsOne(), queries.findNodeByNameAndType("Intimidation", "Motive").executeAsOne(), "Description")
        queries.insertEdge(queries.findNodeByNameAndType("Instil Fear", "Description").executeAsOne(), queries.findNodeByNameAndType("Evacuation", "Motive").executeAsOne(), "Description")
    }
}