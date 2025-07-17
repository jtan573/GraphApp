package com.example.graphapp.data.repository

import android.content.Context
import android.util.Log
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserActionDatabaseQueries
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.embedding.SentenceEmbedding

class UserActionRepository (
    private val sentenceEmbedding: SentenceEmbedding
) {

    private val queries = UserActionDatabaseQueries()

    // Function to add node
    suspend fun insertUserNodeIntoDb(
        inputIdentifier: String,
        inputRole: String,
        inputSpecialisation: String,
        inputActionsTaken: MutableList<Long> = mutableListOf()
    ) {
        val nodeFound = queries.findUserNodeByIdentifierQuery(inputIdentifier)

        if (nodeFound == null) {
            queries.addUserNodeIntoDbQuery(
                identifier = inputIdentifier,
                role = inputRole,
                specialisation = inputSpecialisation,
                embedding = sentenceEmbedding.encode(inputSpecialisation),
                actionsTaken = inputActionsTaken
            )
        }
        return
    }

    fun insertActionNodeIntoDb(userIdentifier: String, inputName: String) {
        queries.addActionNodeIntoDbQuery(userIdentifier, inputName)
    }

    // Get all nodes
    fun getAllUserNodes() : List<UserNodeEntity> {
        return queries.findAllUserNodesQuery()
    }

    fun getAllActionNodes() : List<ActionNodeEntity> {
        return queries.findAllActionNodesQuery()
    }

    fun getAllActionEdges() : List<ActionEdgeEntity> {
        return queries.findAllActionEdgesQuery()
    }

    // Get all USER nodes without their embedding
    fun getAllUserNodesWithoutEmbedding() : List<UserNodeEntity> {
        return queries.findAllUserNodesWithoutEmbeddingQuery()
    }

    // Get all ACTION nodes without their embedding
    fun getAllActionNodesWithoutEmbedding() : List<ActionNodeEntity> {
        return queries.findAllActionNodesWithoutEmbeddingQuery()
    }

    suspend fun initialiseUserActionRepository() {

        // Users for Tasks
        insertUserNodeIntoDb(
            inputIdentifier = "SGT-001",
            inputRole = "Squad Leader",
            inputSpecialisation = "Leads reconnaissance and patrol missions in hostile environments."
        )

        insertActionNodeIntoDb("SGT-001", "Planned Recon Route in Sector Bravo")
        insertActionNodeIntoDb("SGT-001", "Deployed Surveillance Drone for Area Scan")
        insertActionNodeIntoDb("SGT-001", "Identified Hostile Activity Near Checkpoint Delta")
        insertActionNodeIntoDb("SGT-001", "Radioed HQ with Updated Intel")
        insertActionNodeIntoDb("SGT-001", "Led Extraction of Team Under Fire")

        insertUserNodeIntoDb(
            inputIdentifier = "CPL-002",
            inputRole = "Convoy Commander",
            inputSpecialisation = "Responsible for planning and executing secure convoy escorts through contested routes."
        )

        insertActionNodeIntoDb("CPL-002", "Planned Convoy Route Through Sector Echo")
        insertActionNodeIntoDb("CPL-002", "Inspected Vehicles for Deployment Readiness")
        insertActionNodeIntoDb("CPL-002", "Briefed Troopers on Ambush Protocols")
        insertActionNodeIntoDb("CPL-002", "Led Convoy Movement at 0600 Hours")

        insertUserNodeIntoDb(
            inputIdentifier = "LT-003",
            inputRole = "Forward Observer",
            inputSpecialisation = "Coordinates artillery support and provides real-time intelligence from observation posts."
        )

        insertActionNodeIntoDb("LT-003", "Deployed to Observation Post Sierra")
        insertActionNodeIntoDb("LT-003", "Confirmed Visuals of Enemy Movement")
        insertActionNodeIntoDb("LT-003", "Relayed Coordinates to Artillery Battery")

        insertUserNodeIntoDb(
            inputIdentifier = "SPC-004",
            inputRole = "Logistics Specialist",
            inputSpecialisation = "Oversees resupply missions and ensures timely delivery of critical supplies to forward units."
        )

        insertUserNodeIntoDb(
            inputIdentifier = "SGT-005",
            inputRole = "Rapid Response Leader",
            inputSpecialisation = "Leads quick reaction forces for immediate deployment during emerging threats."
        )

        insertUserNodeIntoDb(
            inputIdentifier = "CPT-006",
            inputRole = "Operations Officer",
            inputSpecialisation = "Manages area surveillance operations using integrated aerial and ground assets."
        )

        insertUserNodeIntoDb(
            inputIdentifier = "SSG-007",
            inputRole = "Security Team Leader",
            inputSpecialisation = "Provides protection for supply convoys and high-value logistical operations."
        )

        Log.d("INITIALISE USER DATABASE", "User data initialised.")
    }
}