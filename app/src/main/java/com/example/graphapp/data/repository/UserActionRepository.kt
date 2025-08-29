package com.example.graphapp.data.repository

import android.util.Log
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.queries.UserActionDatabaseQueries
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
        inputLocation: String,
        inputActionsTaken: MutableList<Long> = mutableListOf()
    ) {
        val nodeFound = queries.findUserNodeByIdentifierQuery(inputIdentifier)

        if (nodeFound == null) {
            queries.addUserNodeIntoDbQuery(
                identifier = inputIdentifier,
                role = inputRole,
                specialisation = inputSpecialisation,
                currentLocation = inputLocation,
                embedding = sentenceEmbedding.encode(inputSpecialisation),
                actionsTaken = inputActionsTaken
            )
        }
        return
    }

    fun insertActionNodeIntoDb(
        inputModule: SchemaKeyEventTypeNames,
        inputAction: String,
        inputContent: String,
        inputUserIdentifier: String
    ) {
        queries.addActionNodeIntoDbQuery(
            inputModule = inputModule,
            inputAction = inputAction,
            inputContent = inputContent,
            userIdentifier = inputUserIdentifier)
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

    // Get users by their identifiers (unique)
    fun getUserNodeByIdentifier(identifier: String): UserNodeEntity? {
        return queries.findUserNodeByIdentifierQuery(identifier)
    }

    // Get action node by Id
    fun getActionNodeById(id: Long): ActionNodeEntity? {
        return queries.findActionNodeByIdQuery(id)
    }

    // Delete user
    fun removeUserFromDb(identifier: String) {
        val userNode = getUserNodeByIdentifier(identifier)
        if (userNode != null) {
            val userActions = userNode.actionsTaken.toList()
            queries.deleteNodesQuery(
                actionIdsToDelete = userActions,
                userIdsToDelete = listOf(userNode.id)
            )
        }
    }

    // Delete action
    fun removeActionFromDb(deletedActionName: String) {

        val deletedActionId = queries.findActionNodeByNameQuery(deletedActionName)?.id

        if (deletedActionId == null) {
            Log.e("MissingDbEntryError", "Action not found in database ($deletedActionName).")
        } else {
            // Delete the node
            queries.deleteNodesQuery(
                actionIdsToDelete = listOf(deletedActionId)
            )

            // Update user actions taken list
            val allUsers = queries.findAllUserNodesWithoutEmbeddingQuery()
            allUsers.forEach { user ->
                queries.updateActionsListQuery(deletedActionId, user)
            }

            // Update action edges box
            updateActionEdgeUponActionDeletion(deletedActionId)
        }
    }

    // Find neighbour nodes
    fun updateActionEdgeUponActionDeletion(deletedActionId: Long) {
        val neighbourEdges = queries.findAllEdgesAroundNodeIdQuery(deletedActionId)

        var newFromId: Long? = null
        var newFromType: String? = null
        var newToId: Long? = null
        var newToType: String? = null

        for (edge in neighbourEdges) {
            if (edge.fromNodeId == deletedActionId) {
                newToId = edge.toNodeId
                newToType = edge.toNodeType
            }
            if (edge.toNodeId == deletedActionId) {
                newFromId = edge.fromNodeId
                newFromType = edge.fromNodeType
            }
        }
        if (newFromId != null && newToId != null && newFromType != null && newToType != null) {
            queries.addActionEdgeIntoDbQuery(newFromId, newFromType, newToId, newToType)
        } else {
            Log.e("EdgeError", "Missing edge information for actionId: $deletedActionId")
        }
    }

    fun resetPersonnelDb() {
        queries.resetPersonnelDbQuery()
    }

    suspend fun initialiseUserActionRepository() {
        // Users for Tasks
        insertUserNodeIntoDb(
            inputIdentifier = "SGT-001",
            inputRole = "Squad Leader",
            inputSpecialisation = "Leads reconnaissance and patrol missions in hostile environments.",
            inputLocation = "1.3521,103.8198"
        )
        insertActionNodeIntoDb(
            inputModule = SchemaKeyEventTypeNames.INCIDENT,
            inputAction = "acknowledged",
            inputContent = "Viewed incident details",
            inputUserIdentifier = "SGT-001"
        )
        insertActionNodeIntoDb(
            inputModule = SchemaKeyEventTypeNames.TASK,
            inputAction = "assigned",
            inputContent = "Patrol perimeter near checkpoint",
            inputUserIdentifier = "SGT-001"
        )
        insertActionNodeIntoDb(
            inputModule = SchemaKeyEventTypeNames.TASK,
            inputAction = "reviewed",
            inputContent = "Nearby troop availability",
            inputUserIdentifier = "SGT-001"
        )
        insertActionNodeIntoDb(
            inputModule = SchemaKeyEventTypeNames.TASK,
            inputAction = "completed",
            inputContent = "Mission completion report confirmed",
            inputUserIdentifier = "SGT-001"
        )

        insertUserNodeIntoDb(
            inputIdentifier = "CPL-002",
            inputRole = "Convoy Commander",
            inputSpecialisation = "Responsible for planning and executing secure convoy escorts through contested routes.",
            inputLocation = "1.3554,103.8677"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "planned", inputContent = "Convoy route using map interface", inputUserIdentifier = "CPL-002")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "checked", inputContent = "Roadblock reports in area", inputUserIdentifier = "CPL-002")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "assigned", inputContent = "Task to logistics specialist", inputUserIdentifier = "CPL-002")


        insertUserNodeIntoDb(
            inputIdentifier = "LT-003",
            inputRole = "Forward Observer",
            inputSpecialisation = "Coordinates artillery support and provides real-time intelligence from observation posts.",
            inputLocation = "1.3600,103.7500"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.INCIDENT, inputAction = "reported", inputContent = "Suspicious drone activity marked on map", inputUserIdentifier = "LT-003")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.INCIDENT, inputAction = "uploaded", inputContent = "Photo evidence from observation post", inputUserIdentifier = "LT-003")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "requested", inputContent = "Artillery support through app", inputUserIdentifier = "LT-003")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "flagged", inputContent = "Enemy movement pattern to operations officer", inputUserIdentifier = "LT-003")

        insertUserNodeIntoDb(
            inputIdentifier = "SPC-004",
            inputRole = "Logistics Specialist",
            inputSpecialisation = "Oversees resupply missions and ensures timely delivery of critical supplies to forward units.",
            inputLocation = "1.3300,103.9200"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "updated", inputContent = "Convoy load manifest", inputUserIdentifier = "SPC-004")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "acknowledged", inputContent = "Supply delivery task", inputUserIdentifier = "SPC-004")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "verified", inputContent = "Drop-off completion with timestamp", inputUserIdentifier = "SPC-004")

        insertUserNodeIntoDb(
            inputIdentifier = "SGT-005",
            inputRole = "Rapid Response Leader",
            inputSpecialisation = "Leads quick reaction forces for immediate deployment during emerging threats.",
            inputLocation = "1.4100,103.7600"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "accepted", inputContent = "Rapid response task", inputUserIdentifier = "SGT-005")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "checked", inputContent = "Readiness status of nearby units", inputUserIdentifier = "SGT-005")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.INCIDENT, inputAction = "reported", inputContent = "Immediate deployment to incident site", inputUserIdentifier = "SGT-005")

        insertUserNodeIntoDb(
            inputIdentifier = "CPT-006",
            inputRole = "Operations Officer",
            inputSpecialisation = "Manages area surveillance operations using integrated aerial and ground assets.",
            inputLocation = "1.3400,103.6900"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "monitored", inputContent = "Drone surveillance feed", inputUserIdentifier = "CPT-006")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "reviewed", inputContent = "Summary of active incidents", inputUserIdentifier = "CPT-006")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "received", inputContent = "Quick Reaction Force to hotspot", inputUserIdentifier = "CPT-006")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "reviewed", inputContent = "Logistics status from SPC-004", inputUserIdentifier = "CPT-006")


        insertUserNodeIntoDb(
            inputIdentifier = "SSG-007",
            inputRole = "Security Team Leader",
            inputSpecialisation = "Provides protection for supply convoys and high-value logistical operations.",
            inputLocation = "1.3705,103.7100"
        )
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "acknowledged", inputContent = "Convoy escort mission", inputUserIdentifier = "SSG-007")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.TASK, inputAction = "reported", inputContent = "Cleared route section", inputUserIdentifier = "SSG-007")
        insertActionNodeIntoDb(inputModule = SchemaKeyEventTypeNames.INCIDENT, inputAction = "reported", inputContent = "Security breach near depot", inputUserIdentifier = "SSG-007")

        /*-----------------------------------------
        |    FOR THREAT DETECTION USE CASE        |
        -----------------------------------------*/
        // isolate crash zone
        insertUserNodeIntoDb(
            inputIdentifier = "SEC-118",
            inputRole = "Field Perimeter Controller",
            inputSpecialisation = "Manages exclusion zones and issues loudspeaker evacuation instructions during ordinance incidents.",
            inputLocation = "1.3928,103.7962"
        )
        insertUserNodeIntoDb(
            inputIdentifier = "FRS-021",
            inputRole = "Foam Suppression Technician",
            inputSpecialisation = "Deploys aerial foam blankets and maintains ember perimeter safety protocols around crash zones.",
            inputLocation = "1.3875,103.8029"
        )
        // retrieve drone wreck
        insertUserNodeIntoDb(
            inputIdentifier = "DRN-047",
            inputRole = "Drone Recovery Specialist",
            inputSpecialisation = "Locates and retrieves downed UAVs and handles data uplink reinitialisation post-crash.",
            inputLocation = "1.3937,103.8125"
        )
        insertUserNodeIntoDb(
            inputIdentifier = "SIG-033",
            inputRole = "Signal Systems Engineer",
            inputSpecialisation = "Restores drone communication by configuring repeater systems and analyzing interference logs.",
            inputLocation = "1.3889,103.8142"
        )
        // initiate fuel truck quarantine
        insertUserNodeIntoDb(
            inputIdentifier = "OPS-062",
            inputRole = "UAV Maintenance Crew Lead",
            inputSpecialisation = "Oversees launch pad readiness and executes pre-flight mechanical integrity checks on UAV fleets.",
            inputLocation = "1.3863,103.8004"
        )
        insertUserNodeIntoDb(
            inputIdentifier = "ENG-074",
            inputRole = "Rotor Diagnostics Technician",
            inputSpecialisation = "Performs debris removal and conducts fine-grain diagnostics on rotor assemblies before lift-off.",
            inputLocation = "1.3950,103.8041"
        )

        // random close troopers
        insertUserNodeIntoDb(
            inputIdentifier = "INF-564",
            inputRole = "Infantry Trooper",
            inputSpecialisation = "Standard ground unit trained in patrol, search, and perimeter defense.",
            inputLocation = "1.3915,103.8048"
        )
        insertUserNodeIntoDb(
            inputIdentifier = "SUP-276",
            inputRole = "Combat Support Specialist",
            inputSpecialisation = "Assists with equipment handling and short-range tactical mobility during field deployments.",
            inputLocation = "1.3882,103.8087"
        )
        insertUserNodeIntoDb(
            inputIdentifier = "MED-143",
            inputRole = "Field Medic",
            inputSpecialisation = "Provides on-site trauma care and casualty evacuation for forward units.",
            inputLocation = "1.3894,103.8105"
        )

        Log.d("INITIALISE USER DATABASE", "User data initialised.")
    }
}