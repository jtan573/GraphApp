package com.example.graphapp.backend.services

import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.ContactRelevantPersonnelResponse
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.data.api.buildApiResponseFromResult
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.backend.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.backend.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.backend.usecases.findRelevantPersonnelByLocationUseCase
import com.example.graphapp.backend.usecases.findThreatResponses
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.api.ResponseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BackendLogic {
    /* -------------------------------------------------------
            Functions for Use Case 1: Find relevant personnel
        -------------------------------------------------------*/
    suspend fun findRelevantPersonnelOnDemand (
        eventLocation: String?,
        eventDescription: String?,
        userActionRepository: UserActionRepository,
        embeddingRepository: EmbeddingRepository
    ) : ApiResponse {

        val contactsFound = findRelevantPersonnelByLocationUseCase(
            userActionRepository = userActionRepository,
            embeddingRepository = embeddingRepository,
            threatLocation = eventLocation,
            threatDescription = eventDescription,
            radiusInMeters = 3000f
        )

        val apiRes = buildApiResponseFromResult(ContactRelevantPersonnelResponse(contactsFound))
        return apiRes
    }

    /* -------------------------------------------------------
        Function for Use Case 2: Threat Alert and Response
    -------------------------------------------------------*/
    suspend fun findThreatAlertAndResponse(
        eventInput: EventDetailData,
        userActionRepository: UserActionRepository,
        embeddingRepository: EmbeddingRepository,
        eventRepository: EventRepository,
    ) : ApiResponse {

        val incidentResponse = findThreatResponses(
            eventInput, userActionRepository, embeddingRepository, eventRepository
        )

        val apiRes = buildApiResponseFromResult(incidentResponse)
        return apiRes
    }

    /* -------------------------------------------------------
        Function for Use Case 3: Suspicious Behaviour Detection
    -------------------------------------------------------*/
    suspend fun findDataIndicatingSuspiciousBehaviour(
        eventInput: EventDetailData,
        eventRepository: EventRepository,
        embeddingRepository: EmbeddingRepository
    ): ApiResponse {

        val incidentResponse = findRelatedSuspiciousEventsUseCase(
            eventInput, eventRepository, embeddingRepository
        )

        val apiRes = buildApiResponseFromResult(incidentResponse)
        return apiRes
    }

    /* -------------------------------------------------------
        Function for Use Case 4: Route Integrity Check
    -------------------------------------------------------*/
    suspend fun findAffectedRouteStationsByLocation(
        locations: List<String>,
        eventRepository: EventRepository,
        embeddingRepository: EmbeddingRepository
    ) : ApiResponse {
        val results = findAffectedRouteStationsByLocUseCase(
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            routeStations = locations,
        )

        val apiRes = buildApiResponseFromResult(
            ThreatAlertResponse(incidentsAffectingStations = results)
        )
        return apiRes
    }


}