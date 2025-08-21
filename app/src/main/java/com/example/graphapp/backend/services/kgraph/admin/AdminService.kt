package com.example.graphapp.backend.services.kgraph.admin

import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.api.UserDetailData
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity

interface AdminService {

    suspend fun addEventToDatabase(inputData: RequestData.EventRequestData): Boolean
    suspend fun addActionToDatabase(inputActionName: String, inputUserData: UserDetailData): Boolean
    fun removeEventFromDatabase(inputData: RequestData.EventRequestData,): Boolean
    fun removeUserFromDatabase(userIdentifier: String): Boolean
    fun removeActionFromDatabase(actionName: String): Boolean

    fun resetApp(): Boolean
    fun exportEventDb(): Boolean
    fun exportPersonnelDb(): Boolean
    fun exportAllDb(): Boolean
    fun importEventDb(): Boolean
    fun importPersonnelDb(): Boolean

    /* ------------------------------------------
        FOR UI
    ------------------------------------------ */
    suspend fun ensureReady()
    fun retrieveEventNodesAndEdges(): Pair<List<EventNodeEntity>, List<EventEdgeEntity>>
    fun retrievePersonnelNodesAndEdges(): Triple<List<UserNodeEntity>, List<ActionNodeEntity>, List<ActionEdgeEntity>>
    fun retrieveAllActiveUsers(): List<UserNodeEntity>

    /* ------------------------------------------
        FOR USE CASES
    ------------------------------------------ */
    fun getDataForSuspiciousEventDetectionUseCase(events: List<String>): List<Map<String, String>>
    fun getDataForThreatAlertUseCase(identifiers: List<String>): List<UserNodeEntity>
}