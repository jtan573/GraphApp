package com.example.graphapp.backend.services

import com.example.graphapp.data.api.ApiRequest
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.DbAction
import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.api.RequestData.*
import com.example.graphapp.data.api.ResponseStatus
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.google.common.cache.CacheBuilder
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ApiRouter {

    val dbLogic = DatabaseLogic
    val backendLogic = BackendLogic

    suspend fun handlePredict(
        request: ApiRequest,
        embeddingRepository: EmbeddingRepository,
        eventRepository: EventRepository,
        userActionRepository: UserActionRepository
    ) : ApiResponse {

        val requestData = request.inputData

        val apiRes = when (requestData) {

            // Personnel Related
            is PersonnelRequestData -> {

                when (request.action) {
                    DbAction.DELETE -> {
                        dbLogic.removeUserActionFromDb(request.inputData, userActionRepository)
                    }

                    DbAction.CREATE -> {
                        dbLogic.addPersonnelToDatabase(request.inputData, userActionRepository)
                    }

                    DbAction.QUERY -> {
                        // Use Case: Find relevant personnel on demand
                        if (requestData.metadata != null) {
                            backendLogic.findRelevantPersonnelOnDemand(
                                eventLocation = requestData.metadata.incidentLocation,
                                eventDescription = requestData.metadata.incidentDescription,
                                userActionRepository = userActionRepository,
                                embeddingRepository = embeddingRepository
                            )
                        } else {
                            ApiResponse(
                                status = ResponseStatus.FAILED,
                                timestamp = System.currentTimeMillis(),
                                message = "Input does not apply to any of the use cases.",
                                data = null
                            )
                        }
                    }
                }
            }

            // Events Related
            is EventRequestData -> {
                when (request.action) {
                    DbAction.DELETE -> {
                        dbLogic.removeEventFromEventDb(
                            inputData = requestData,
                            eventRepository = eventRepository
                        )
                    }

                    DbAction.QUERY -> {
                        // Check route
                        if (requestData.metadata?.routeCoordinates != null &&
                            requestData.metadata.routeCoordinates.isNotEmpty()) {
                            backendLogic.findAffectedRouteStationsByLocation(
                                locations = requestData.metadata.routeCoordinates,
                                eventRepository = eventRepository,
                                embeddingRepository = embeddingRepository
                            )
                        }

                        // Threat detection?
//                        else if (requestData.eventType == "Incident" &&
//                            (requestData.details?.whatValue != null || requestData.details?.howValue != null)
//                        ) {
//                            backendLogic.findThreatAlertAndResponse(
//                                eventInput = requestData.details,
//                                userActionRepository = userActionRepository,
//                                embeddingRepository = embeddingRepository,
//                                eventRepository = eventRepository
//                            )
//                        }

                        // Suspicious?
                        else if (requestData.eventType == "Incident" &&
                            (requestData.details?.whatValue != null || requestData.details?.howValue != null ||
                                    requestData.details?.whereValue != null)
                        ) {
                            backendLogic.findDataIndicatingSuspiciousBehaviour(
                                eventInput = requestData.details,
                                eventRepository = eventRepository,
                                embeddingRepository = embeddingRepository
                            )
                        }

                        else {
                            ApiResponse(
                                status = ResponseStatus.FAILED,
                                timestamp = System.currentTimeMillis(),
                                message = "Input does not apply to any of the use cases.",
                                data = null
                            )
                        }
                    }

                    DbAction.CREATE -> {
                        // Update cache and database
                        val hash = dbLogic.hashRequestData(requestData)
                        val foundInCache = dbLogic.requestHashCache.getIfPresent(hash)
                        if (foundInCache == null) {
                            dbLogic.requestHashCache.put(hash, true)
                            dbLogic.addEventToDatabase(
                                inputData = requestData,
                                eventRepository = eventRepository
                            )
                        } else {
                            ApiResponse(
                                status = ResponseStatus.FAILED,
                                timestamp = System.currentTimeMillis(),
                                message = "Duplicate data found, data not added to database.",
                                data = null
                            )
                        }
                    }
                }
//                 else {
//                    ApiResponse(
//                        status = ResponseStatus.FAILED,
//                        timestamp = System.currentTimeMillis(),
//                        message = "Duplicate data found, data not added to database.",
//                        data = null
//                    )
//                }
            }
        }



        return apiRes
    }
}