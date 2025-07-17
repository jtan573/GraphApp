package com.example.graphapp.domain.usecases

import android.location.Location
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.GraphSchema

fun findAffectedEventsByLocation(
   eventRepository: EventRepository,
   threatLocation: Location,
   radiusInMeters: Float
): List<Triple<Long, String, String>> {
    val allEvents = eventRepository.getAllEventNodesWithoutEmbedding()
        .filter  { it.type in GraphSchema.SchemaKeyNodes }

    val affectedEvents = mutableListOf<Triple<Long, String, String>>()
    for (event in allEvents) {

        val locationOfEventString = eventRepository.getNeighborsOfEventNodeById(event.id)
            .firstOrNull() { it.type == "Location" }?.name

        if (locationOfEventString != null) {
            val parts = locationOfEventString.split(",")
            val lat = parts[0].toDouble()
            val lon = parts[1].toDouble()
            val restoredLocation = Location("").apply {
                latitude = lat
                longitude = lon
            }

            val distance = threatLocation.distanceTo(restoredLocation)
            if (distance <= radiusInMeters) {
                affectedEvents.add(Triple(event.id, event.type, event.name))
            }
        }
    }

    return affectedEvents.toList()
}