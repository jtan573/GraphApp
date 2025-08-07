package com.example.graphapp.backend.services

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.api.RequestData.EventRequestData
import com.example.graphapp.data.api.RequestData.PersonnelRequestData
import com.example.graphapp.data.api.ResponseStatus
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.google.common.cache.CacheBuilder
import com.google.common.cache.Cache
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object DatabaseLogic {
    
    val requestHashCache: Cache<String?, Boolean?> = CacheBuilder.newBuilder()
        .maximumSize(1000) // max entries
        .expireAfterWrite(10, TimeUnit.MINUTES) // auto-expire
        .build<String, Boolean>()

    val jsonFormatter = Json {
        encodeDefaults = true
        classDiscriminator = "type"
        prettyPrint = false
        isLenient = false
        ignoreUnknownKeys = false
    }

    fun hashRequestData(requestData: RequestData): String {
        val json = jsonFormatter.encodeToString(requestData)
        return sha256(json)
    }

    fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /* -------------------------------------------------------
        Helper to add data to DBs
    -------------------------------------------------------*/
    suspend fun addEventToDatabase(
       inputData: EventRequestData,
       eventRepository: EventRepository
    ): ApiResponse {

        var keyNode: EventNodeEntity? = null
        if (inputData.eventType != null && inputData.details?.whatValue != null) {
            val toNodeId = eventRepository.insertEventNodeIntoDb(
                inputName = inputData.details.whatValue,
                inputType = inputData.eventType
            )
            keyNode = eventRepository.getEventNodeById(toNodeId)
        }

        val fromNodeList = mutableListOf<EventNodeEntity>()
        listOf(
            PropertyNames.WHO.key to inputData.details?.whoValue,
            PropertyNames.WHEN.key to inputData.details?.whenValue,
            PropertyNames.WHERE.key to inputData.details?.whereValue,
            PropertyNames.WHY.key to inputData.details?.whyValue,
            PropertyNames.HOW.key to inputData.details?.howValue,
        ).forEach { (key, value) ->
            if (value != null) {
                val nodeId = eventRepository.insertEventNodeIntoDb(
                    inputName = value,
                    inputType = key
                )
                fromNodeList.add(eventRepository.getEventNodeById(nodeId)!!)
            }
        }

        if (keyNode != null) {
            fromNodeList.forEach { fromNode ->
                eventRepository.insertEventEdgeIntoDb(
                    fromNode = fromNode,
                    toNode = keyNode
                )

            }
        }
        return ApiResponse(
            status = ResponseStatus.SUCCESS,
            timestamp = System.currentTimeMillis(),
            message = "Event data added to database successfully.",
            data = null
        )
    }

    suspend fun addPersonnelToDatabase(
        inputData: PersonnelRequestData,
        userActionRepository: UserActionRepository
    ): ApiResponse {

        val user = inputData.userData
        // Check if userData or any of the fields are missing
        if (user != null) {

            if (user.identifier.isNullOrBlank() || user.role.isNullOrBlank() ||
            user.specialisation.isNullOrBlank() || user.currentLocation.isNullOrBlank()) {

                return ApiResponse(
                    status = ResponseStatus.FAILED,
                    timestamp = System.currentTimeMillis(),
                    message = "Missing required user data fields.",
                    data = null
                )
            }

            userActionRepository.insertUserNodeIntoDb(
                inputIdentifier = inputData.userData.identifier,
                inputRole = inputData.userData.role,
                inputSpecialisation = inputData.userData.specialisation,
                inputLocation = inputData.userData.currentLocation
            )
        }

        // Check if actionData or any of the fields are missing
        val action = inputData.actionData
        if (action != null) {

            if (action.actionName.isNullOrBlank() || action.userIdentifier.isNullOrBlank() ||
                userActionRepository.getUserNodeByIdentifier(action.userIdentifier) == null) {
                return ApiResponse(
                    status = ResponseStatus.FAILED,
                    timestamp = System.currentTimeMillis(),
                    message = "Missing/Incorrect required action data fields.",
                    data = null
                )
            }

            userActionRepository.insertActionNodeIntoDb(
                userIdentifier = action.userIdentifier,
                inputName = action.actionName
            )
        }

        return ApiResponse(
            status = ResponseStatus.SUCCESS,
            timestamp = System.currentTimeMillis(),
            message = "Personnel data added to database successfully.",
            data = null
        )
    }

    /* -------------------------------------------------------
        Helper to delete data from DBs
    -------------------------------------------------------*/
    fun removeEventFromEventDb(
        inputData: EventRequestData,
        eventRepository: EventRepository
    ) : ApiResponse {
        if (inputData.eventType != null && inputData.details?.whatValue != null) {
            val nodeId = eventRepository.getEventNodeByNameAndType(
                inputName = inputData.details.whatValue,
                inputType = inputData.eventType
            )?.id
            if (nodeId != null) {
                eventRepository.removeNodeById(nodeId)
            }
            return ApiResponse(
                status = ResponseStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
                message = "Deleted ${inputData.eventType} (${inputData.details.whatValue}) from database.",
                data = null
            )
        } else {
            return ApiResponse(
                status = ResponseStatus.FAILED,
                timestamp = System.currentTimeMillis(),
                message = "DataDeletionError: Incomplete input, cannot delete anything based on supplied information.",
                data = null
            )
        }
    }

    // TODO: FIX LOGIC ERROR
    fun removeUserActionFromDb(
        inputData: PersonnelRequestData,
        userActionRepository: UserActionRepository
    ): ApiResponse {

        var deleted = false
        var message: String = ""
        val action = inputData.actionData
        if (action != null) {
            if (!action.actionName.isNullOrBlank()) {
                userActionRepository.removeActionFromDb(action.actionName)
                deleted = true
                message = message + "SUCCESS: Deleted action (${inputData.actionData.actionName}) from database."
            }
            else {
                message = message + "ERROR: Action (${inputData.actionData.actionName}) cannot be deleted from database."
            }
        }

        val user = inputData.userData
        if (user != null) {
            if (!user.identifier.isNullOrBlank()) {
                userActionRepository.removeUserFromDb(user.identifier)
                deleted = true
                message = message + "SUCCESS: Deleted user (${user.identifier}) from database."
            }
            else {
                deleted = false
                message = message + "ERROR: uSER (${user.identifier}) cannot be deleted from database."
            }
        }

        if (deleted) {
            return ApiResponse(
                status = ResponseStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
                message = message,
                data = null
            )
        } else {
            return ApiResponse(
                status = ResponseStatus.FAILED,
                timestamp = System.currentTimeMillis(),
                message = "DataDeletionError: Incomplete input, cannot delete anything based on supplied information.",
                data = null
            )
        }
    }


}