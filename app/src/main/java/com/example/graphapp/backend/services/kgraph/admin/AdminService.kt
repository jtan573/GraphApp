package com.example.graphapp.backend.services.kgraph.admin

import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity

interface AdminService {

    suspend fun ensureReady()

    suspend fun addEventToDatabase(inputData: RequestData.EventRequestData): Boolean
    suspend fun addUserToDatabase(inputData: RequestData.UserRequestData): Boolean
    suspend fun addActionToDatabase(inputData: RequestData.ActionRequestData): Boolean
    fun removeEventFromDatabase(inputData: RequestData.EventRequestData,): Boolean
    fun removeUserFromDatabase(userIdentifier: String): Boolean
    fun removeActionFromDatabase(actionName: String): Boolean

    /* ------------------------------------------
        FOR INTERACTION WITH NODES AND EDGES
    ------------------------------------------ */
    fun retrieveEventNodesAndEdges(): Pair<List<EventNodeEntity>, List<EventEdgeEntity>>
    fun retrievePersonnelNodesAndEdges(): Triple<List<UserNodeEntity>, List<ActionNodeEntity>, List<ActionEdgeEntity>>
    fun retrieveAllActiveUsers(): List<UserNodeEntity>

    /* ------------------------------------------
        FOR USE CASES
    ------------------------------------------ */
    fun getDataForSuspiciousEventDetectionUseCase(events: List<String>): List<Map<String, String>>
    fun getDataForThreatAlertUseCase(identifiers: List<String>): List<UserNodeEntity>
}