package com.example.graphapp.backend.services.kgraph.admin

import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.services.kgraph.GraphAccess
import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class AdminGraph @Inject constructor(
    private val graph: GraphAccess
) : AdminService {

    override suspend fun ensureReady() = graph.awaitReady()

    /* ------------------------------------------
        CUD OPERATIONS
    ------------------------------------------ */
    private val requestHashCache: Cache<String?, Boolean?> = CacheBuilder.newBuilder()
        .maximumSize(1000) // max entries
        .expireAfterWrite(10, TimeUnit.MINUTES) // auto-expire
        .build<String, Boolean>()

    private val jsonFormatter = Json {
        encodeDefaults = true
        classDiscriminator = "type"
        prettyPrint = false
        isLenient = false
        ignoreUnknownKeys = false
    }

    private fun hashRequestData(requestData: RequestData): String {
        val json = jsonFormatter.encodeToString(requestData)
        return sha256(json)
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    override suspend fun addEventToDatabase(inputData: RequestData.EventRequestData): Boolean {

        val hash = hashRequestData(inputData)
        val foundInCache = requestHashCache.getIfPresent(hash)
        if (foundInCache != null) {
            return false
        }

        var keyNode: EventNodeEntity? = null
        if (inputData.eventType != null && inputData.details?.whatValue != null) {
            val toNodeId = graph.eventRepository.insertEventNodeIntoDb(
                inputName = inputData.details.whatValue,
                inputType = SchemaKeyEventTypeNames.Companion.toKey(inputData.eventType)
            )
            keyNode = graph.eventRepository.getEventNodeById(toNodeId)
        }

        val fromNodeList = mutableListOf<EventNodeEntity>()
        listOf(
            GraphSchema.SchemaEventTypeNames.WHO.key to inputData.details?.whoValue,
            GraphSchema.SchemaEventTypeNames.WHEN.key to inputData.details?.whenValue,
            GraphSchema.SchemaEventTypeNames.WHERE.key to inputData.details?.whereValue,
            GraphSchema.SchemaEventTypeNames.WHY.key to inputData.details?.whyValue,
            GraphSchema.SchemaEventTypeNames.HOW.key to inputData.details?.howValue,
        ).forEach { (key, value) ->
            if (value != null) {
                val nodeId = graph.eventRepository.insertEventNodeIntoDb(
                    inputName = value,
                    inputType = key
                )
                fromNodeList.add(graph.eventRepository.getEventNodeById(nodeId)!!)
            }
        }

        if (keyNode != null) {
            fromNodeList.forEach { fromNode ->
                graph.eventRepository.insertEventEdgeIntoDb(
                    fromNode = fromNode,
                    toNode = keyNode
                )

            }
        }
        return true
    }

    override suspend fun addUserToDatabase(inputData: RequestData.UserRequestData): Boolean {
        val user = inputData.userData
        if (user != null) {

            if (user.identifier.isNullOrBlank() || user.role.isNullOrBlank() ||
                user.specialisation.isNullOrBlank() || user.currentLocation.isNullOrBlank()) {
                return false
            }

            graph.userActionRepository.insertUserNodeIntoDb(
                inputIdentifier = inputData.userData.identifier,
                inputRole = inputData.userData.role,
                inputSpecialisation = inputData.userData.specialisation,
                inputLocation = inputData.userData.currentLocation
            )
            return true
        }
        return false
    }

    override suspend fun addActionToDatabase(inputData: RequestData.ActionRequestData): Boolean {
        val action = inputData.actionData
        if (action != null) {

            if (action.actionName.isNullOrBlank() || action.userIdentifier.isNullOrBlank()) {
                return false
            }

            graph.userActionRepository.insertActionNodeIntoDb(
                userIdentifier = action.userIdentifier, inputName = action.actionName
            )
            return true
        }
        return false
    }

    override fun removeEventFromDatabase(inputData: RequestData.EventRequestData): Boolean {
        if (inputData.eventType != null && inputData.details?.whatValue != null) {
            val nodeId = graph.eventRepository.getEventNodeByNameAndType(
                inputName = inputData.details.whatValue,
                inputType = SchemaKeyEventTypeNames.toKey(inputData.eventType)
            )?.id
            if (nodeId != null) {
                graph.eventRepository.removeNodeById(nodeId)
            }
            return true
        } else {
            return false
        }
    }

    override fun removeUserFromDatabase(userIdentifier: String): Boolean {
        if (userIdentifier.isNotBlank()) {
            graph.userActionRepository.removeUserFromDb(userIdentifier)
            return true
        } else {
            return false
        }
    }

    override fun removeActionFromDatabase(actionName: String): Boolean {
        if (actionName.isNotBlank()) {
            graph.userActionRepository.removeActionFromDb(actionName)
            return true
        } else {
            return false
        }
    }

    /* ------------------------------------------
        FOR INTERACTION WITH NODES AND EDGES
    ------------------------------------------ */
    override fun retrieveEventNodesAndEdges(): Pair<List<EventNodeEntity>, List<EventEdgeEntity>> {
        val eventNodes = graph.eventRepository.getAllEventNodesWithoutEmbedding()
        val eventEdges = graph.eventRepository.getAllEventEdges()
        return eventNodes to eventEdges
    }

    override fun retrievePersonnelNodesAndEdges(): Triple<List<UserNodeEntity>, List<ActionNodeEntity>, List<ActionEdgeEntity>> {
        val userNodes = graph.userActionRepository.getAllUserNodesWithoutEmbedding()
        val actionNodes = graph.userActionRepository.getAllActionNodesWithoutEmbedding()
        val actionEdges = graph.userActionRepository.getAllActionEdges()
        return Triple(userNodes, actionNodes, actionEdges)
    }

    override fun retrieveAllActiveUsers(): List<UserNodeEntity> {
        return graph.userActionRepository.getAllUserNodesWithoutEmbedding()
    }

    /* ------------------------------------------
        FOR USE CASES
    ------------------------------------------ */
    override fun getDataForSuspiciousEventDetectionUseCase(events: List<String>): List<Map<String, String>> {
        val listOfEvents = mutableListOf<Map<String, String>>()
        for (eventName in events) {
            val eventMap = mutableMapOf<String, String>()
            val eventNode = graph.eventRepository.getEventNodeByNameAndType(eventName, GraphSchema.SchemaEventTypeNames.INCIDENT.key)
            if (eventNode != null) {
                eventMap.put(eventNode.type, eventNode.name)
                val neighbours = graph.eventRepository.getNeighborsOfEventNodeById(eventNode.id)
                neighbours.forEach { neighbour ->
                    eventMap.put(neighbour.type, neighbour.name)
                }
            }
            listOfEvents.add(eventMap)
        }
        return listOfEvents
    }

    override fun getDataForThreatAlertUseCase(identifiers: List<String>): List<UserNodeEntity> {
        val listOfUsers = mutableListOf<UserNodeEntity>()
        for (id in identifiers) {
            listOfUsers.add(graph.userActionRepository.getUserNodeByIdentifier(id)!!)
        }
        return listOfUsers
    }
}