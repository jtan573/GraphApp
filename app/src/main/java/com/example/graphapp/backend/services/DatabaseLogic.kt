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
        Helper to add data to DB
    -------------------------------------------------------*/
    suspend fun addEventToDatabase(
       inputData: RequestData,
       eventRepository: EventRepository
    ) {
        if (inputData is EventRequestData) {
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
                Log.d("DATABASE UPDATED:", "Event added: ${keyNode.name}")
            }
        }
    }

    /* -------------------------------------------------------
        Helper to delete data from DB
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

    fun removeUserActionFromDb(
        inputData: PersonnelRequestData,
        userActionRepository: UserActionRepository
    ): ApiResponse {
        if (inputData.actionData != null) {
            userActionRepository.removeActionFromDb(inputData.actionData.actionName)
            return ApiResponse(
                status = ResponseStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
                message = "Deleted action (${inputData.actionData.actionName}) from database.",
                data = null
            )
        }
        else if (inputData.userData != null) {
            userActionRepository.removeUserFromDb(inputData.userData.identifier)
            return ApiResponse(
                status = ResponseStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
                message = "Deleted user (${inputData.userData.identifier}) from database.",
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