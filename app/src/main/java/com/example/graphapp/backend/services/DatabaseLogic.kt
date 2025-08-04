package com.example.graphapp.backend.services

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.api.RequestData.EventRequestData
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EventRepository
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
    
}