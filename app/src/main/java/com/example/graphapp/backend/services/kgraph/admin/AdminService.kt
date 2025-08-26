package com.example.graphapp.backend.services.kgraph.admin

import android.content.Context
import android.net.Uri
import com.example.graphapp.backend.model.dto.EventRequestData
import com.example.graphapp.backend.model.dto.UserDetailData
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity

interface AdminService {

    suspend fun addEventToDatabase(inputData: EventRequestData): Boolean
    suspend fun addActionToDatabase(inputActionName: String, inputUserData: UserDetailData): Boolean
    fun removeEventFromDatabase(inputData: EventRequestData,): Boolean
    fun removeUserFromDatabase(userIdentifier: String): Boolean
    fun removeActionFromDatabase(actionName: String): Boolean

    fun resetApp(): Boolean
    fun exportDb(destUri: Uri): Boolean
    fun importDb(srcUri: Uri): Boolean

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