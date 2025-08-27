package com.example.graphapp.backend.services.kgraph.admin

import android.net.Uri
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.model.dto.EventRequestData
import com.example.graphapp.backend.model.dto.UserDetailData
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity

interface AdminService {

    /**
     * Adds a new event to the database.
     * @param inputData Event details to insert, including key type and 5W1H.
     * @return true if the operation was successful, false otherwise.
     */
    suspend fun addEventToDatabase(inputData: EventRequestData): Boolean

    /**
     * Adds an action to the database for the specified user.
     * @param inputActionName Name of the action to record.
     * @param inputUserData Details of user who performed the action.
     * @return true if the operation succeeded, false otherwise.
     */
    suspend fun addActionToDatabase(
        inputModule: SchemaKeyEventTypeNames,
        inputAction: String,
        inputContent: String,
        inputUserData: UserDetailData
    ): Boolean

    /**
     * Removes an event from the database.
     * @param inputData Event details identifying the record to delete.
     * @return true if the event was removed, false otherwise.
     */
    fun removeEventFromDatabase(inputData: EventRequestData,): Boolean

    /**
     * Removes a user from the database.
     * @param userIdentifier Identifier of the user to delete.
     * @return true if the user was removed, false otherwise.
     */
    fun removeUserFromDatabase(userIdentifier: String): Boolean

    /**
     * Removes an action from the database.
     * @param actionName Name of the action to delete.
     * @return true if the action was removed, false otherwise.
     */
    fun removeActionFromDatabase(actionName: String): Boolean

    /**
     * Resets the application to a clean state, clearing local data and caches.
     * @return true if the reset succeeded, false otherwise.
     */
    fun resetApp(): Boolean

    /**
     * Exports the database to the specified destination.
     * @param destUri Destination URI to write the database file to.
     * @return true if the export succeeded, false otherwise.
     */
    fun exportDb(destUri: Uri): Boolean

    /**
     * Imports a database from the specified source, replacing the local database.
     * @param srcUri Source URI to read the database file from.
     * @return true if the import succeeded, false otherwise.
     */
    fun importDb(srcUri: Uri): Boolean


    /* ------------------------------------------
        FOR UI
    ------------------------------------------ */
    /**
     * Suspends until all underlying services and repositories are initialized.
     */
    suspend fun ensureReady()

    /**
     * Retrieves all event nodes and their connecting edges.
     * @return Pair of (event nodes, event edges).
     */
    fun retrieveEventNodesAndEdges(): Pair<List<EventNodeEntity>, List<EventEdgeEntity>>

    /**
     * Retrieves personnel graph components.
     * @return Triple of (user nodes, action nodes, action edges).
     */
    fun retrievePersonnelNodesAndEdges(): Triple<List<UserNodeEntity>, List<ActionNodeEntity>, List<ActionEdgeEntity>>

    /**
     * Retrieves all active users.
     * @return List of active user nodes.
     */
    fun retrieveAllActiveUsers(): List<UserNodeEntity>

}