package com.example.graphapp.backend.services.kgraph.admin

import android.content.Context
import android.net.Uri
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.model.dto.EventRequestData
import com.example.graphapp.backend.model.dto.UserDetailData
import com.example.graphapp.backend.services.kgraph.GraphAccess
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class AdminGraph @Inject constructor(
    private val graph: GraphAccess,
    private val context: Context
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

    private fun hashRequestData(requestData: EventRequestData): String {
        val json = jsonFormatter.encodeToString(requestData)
        return sha256(json)
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    override suspend fun addEventToDatabase(inputData: EventRequestData): Boolean {

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

    // TODO()
    override suspend fun addActionToDatabase(
        inputActionName: String,
        inputUserData: UserDetailData
    ): Boolean {

        if (inputActionName.isBlank()) {
            return false
        }

        if (!inputUserData.identifier.isNullOrBlank()) {
            graph.userActionRepository.insertUserNodeIntoDb(
                inputIdentifier = inputUserData.identifier,
                inputRole = inputUserData.role?: "",
                inputSpecialisation = inputUserData.specialisation?: "",
                inputLocation = inputUserData.currentLocation?: ""
            )

            graph.userActionRepository.insertActionNodeIntoDb(
                userIdentifier = inputUserData.identifier, inputName = inputActionName
            )
            return true
        }
        return false
    }

    override fun removeEventFromDatabase(inputData: EventRequestData): Boolean {
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

    override fun resetApp(): Boolean {
        graph.userActionRepository.resetPersonnelDb()
        graph.eventRepository.resetEventDb()
        graph.dictionaryRepository.resetDictionaryDb()
        return true
    }

    override fun exportDb(destUri: Uri): Boolean {
//           /data/data/<your_app_package_name>/files/objectbox/objectbox/data.mdb
        return try {
            val dbFile = File(context.filesDir, "objectbox/objectbox/data.mdb")
            context.contentResolver.openOutputStream(destUri, "w")!!.use { out ->
                FileInputStream(dbFile).use { src ->
                    src.copyTo(out)
                    out.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun importDb(srcUri: Uri): Boolean {
        return try {
            val dbFile = File(context.filesDir, "objectbox/objectbox/data.mdb")
            dbFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(srcUri)!!.use { input ->
                FileOutputStream(dbFile, false).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

}