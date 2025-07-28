package com.example.graphapp.data.repository

import android.content.Context
import android.util.Log
import com.example.graphapp.data.embedding.SentenceEmbedding
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.EventDatabaseQueries
import com.example.graphapp.data.schema.GraphSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class EventRepository(
    private val sentenceEmbedding: SentenceEmbedding
) {

    private val queries = EventDatabaseQueries()

    // Function to add node
    suspend fun insertEventNodeIntoDb(
        inputName: String,
        inputType: String,
        inputDescription: String? = null,
        inputFrequency: Int? = 1
    ) {
        val nodeFound = queries.findNodeByNameTypeQuery(inputName, inputType)

        if (nodeFound != null) {
            queries.incrementFreqOfNodeQuery(nodeFound)

        } else {
            queries.addNodeIntoDbQuery(
                name = inputName,
                type = inputType,
                description = inputDescription,
                frequency = inputFrequency,
                embedding = sentenceEmbedding.encode(inputName)
            )
        }
    }

    // Function to add edge
    fun insertEventEdgeIntoDb(
        fromNode: EventNodeEntity?,
        toNode: EventNodeEntity?
    ) {
        if (fromNode == null || toNode == null) {
            return
        } else {
            val edgeType = GraphSchema.SchemaEdgeLabels["${fromNode.type}-${toNode.type}"]
            queries.addEdgeIntoDbQuery(fromNode.id, toNode.id, edgeType!!)
        }
    }

    // Get all nodes
    fun getAllEventNodes() : List<EventNodeEntity> {
        return queries.findAllNodesQuery()
    }

    fun getAllEventEdges() : List<EventEdgeEntity> {
        return queries.findAllEdgesQuery()
    }

    // Get node by Id
    fun getEventNodeById(inputId: Long) : EventNodeEntity? {
        val nodeFound = queries.findNodeByIdQuery(inputId)
        return nodeFound
    }

    // Get node by name and type
    fun getEventNodeByNameAndType(inputName: String, inputType: String) : EventNodeEntity? {
        val nodeFound = queries.findNodeByNameTypeQuery(inputName, inputType)
        return nodeFound
    }

    fun getEventNodesByType(type: String): List<EventNodeEntity>? {
        return queries.findNodesByTypeQuery(type)
    }

    // Get all nodes without their embedding
    fun getAllEventNodesWithoutEmbedding() : List<EventNodeEntity> {
        return queries.findAllNodesWithoutEmbeddingQuery()
    }

    // Get all nodes frequencies
    fun getAllEventNodeFrequencies(): Map<Long, Int> {
        return queries.findAllNodeFrequenciesQuery()
    }

    // Get node frequencies
    fun getEventNodeFrequencyOfNodeId(id: Long): Int? {
        return queries.findNodeFrequencyOfNodeId(id)
    }

    fun findAllEdgesAroundNodeId(id: Long): List<EventEdgeEntity> {
        return queries.findAllEdgesAroundNodeIdQuery(id)
    }

    // Find edges
    fun getNeighborsOfEventNodeById(id: Long): List<EventNodeEntity> {
        val neighbourEdges = queries.findAllEdgesAroundNodeIdQuery(id)

        val neighbourNodes = mutableSetOf<EventNodeEntity>()

        for (edge in neighbourEdges) {
            val node = if (edge.firstNodeId == id) {
                queries.findNodeByIdQuery(edge.secondNodeId)!!
            } else {
                queries.findNodeByIdQuery(edge.firstNodeId)!!
            }
            neighbourNodes.add(node)
        }
        return neighbourNodes.toList()
    }

    fun getEdgeBetweenEventNodes(first: Long, second: Long): EventEdgeEntity {
        return queries.findEdgeBetweenNodeIdsQuery(first, second)!!
    }

    suspend fun getTemporaryEventNode(
        value: String,
        type: String,
        embeddingRepository: EmbeddingRepository,
    ): EventNodeEntity {
        return EventNodeEntity(
            id = (-1L * (1..1_000_000).random()),
            name = value,
            type = type,
            embedding = embeddingRepository.getTextEmbeddings(value)
        )
    }

    // Function to initialise repository
    suspend fun initialiseEventRepository() {

        // --- Entities ---
        insertEventNodeIntoDb("Alpha Company", "Entity")
        insertEventNodeIntoDb("Bravo Company", "Entity")
        insertEventNodeIntoDb("Charlie Squadron", "Entity")
        insertEventNodeIntoDb("Delta Platoon", "Entity")
        insertEventNodeIntoDb("Echo Unit", "Entity")
        insertEventNodeIntoDb("Enemy Forces", "Entity")
        insertEventNodeIntoDb("Enemy Group", "Entity")
        insertEventNodeIntoDb("Enemy Anonymous", "Entity")
        insertEventNodeIntoDb("Joint Task Force Command", "Entity")
        insertEventNodeIntoDb("Insurgent Cell", "Entity")
        insertEventNodeIntoDb("Hostile Militia", "Entity")
        insertEventNodeIntoDb("Opposing Battalion", "Entity")
        insertEventNodeIntoDb("Rival Faction", "Entity")
        insertEventNodeIntoDb("Support Platoon", "Entity")
        insertEventNodeIntoDb("Engineering Corps", "Entity")
        insertEventNodeIntoDb("Medical Detachment", "Entity")
        insertEventNodeIntoDb("Logistics Division", "Entity")


// --- Locations ---
        insertEventNodeIntoDb("1.3521,103.8198", "Location")
        insertEventNodeIntoDb("1.1344,104.0495", "Location")
        insertEventNodeIntoDb("1.4765,103.7636", "Location")
        insertEventNodeIntoDb("1.4250,103.8500", "Location")
        insertEventNodeIntoDb("1.3600,103.7500", "Location")
        insertEventNodeIntoDb("1.3300,103.9200", "Location")
        insertEventNodeIntoDb("1.4100,103.7600", "Location")
        insertEventNodeIntoDb("1.1155,104.0421", "Location")
        insertEventNodeIntoDb("1.3000,103.9000", "Location")
        insertEventNodeIntoDb("1.3530,103.7200", "Location")

// --- Methods ---
        insertEventNodeIntoDb("Foot patrol with UAV support", "Method")
        insertEventNodeIntoDb("Armored convoy", "Method")
        insertEventNodeIntoDb("Helicopter insertion", "Method")
        insertEventNodeIntoDb("IED detonation", "Method")
        insertEventNodeIntoDb("Small arms engagement", "Method")
        insertEventNodeIntoDb("MEDEVAC extraction", "Method")
        insertEventNodeIntoDb("Casualty extraction", "Method")
        insertEventNodeIntoDb("Sniper overwatch", "Method")
        insertEventNodeIntoDb("Drone surveillance", "Method")
        insertEventNodeIntoDb("Night vision assault", "Method")
        insertEventNodeIntoDb("Night operations", "Method")
        insertEventNodeIntoDb("Precision drone strike", "Method")

// --- Motives ---
        insertEventNodeIntoDb("Gather enemy intel", "Motive")
        insertEventNodeIntoDb("Secure supply route", "Motive")
        insertEventNodeIntoDb("Disrupt enemy logistics", "Motive")
        insertEventNodeIntoDb("Respond to threat", "Motive")
        insertEventNodeIntoDb("Establish forward base", "Motive")
        insertEventNodeIntoDb("Retaliate against attack", "Motive")
        insertEventNodeIntoDb("Secure high-value target", "Motive")
        insertEventNodeIntoDb("Protect civilian population", "Motive")
        insertEventNodeIntoDb("Enemy information acquisition", "Motive")

// --- Dates (unique) ---
        insertEventNodeIntoDb("2023-09-15T06:00Z", "Date") //1
        insertEventNodeIntoDb("2023-09-15T08:30Z", "Date") //2
//        insertNodeIntoDb("2023-09-15T09:15Z", "Date") //3
        insertEventNodeIntoDb("2023-09-16T10:45Z", "Date") //4
        insertEventNodeIntoDb("2023-09-16T14:20Z", "Date") //5
        insertEventNodeIntoDb("2023-09-17T05:10Z", "Date") //6
        insertEventNodeIntoDb("2023-09-17T18:00Z", "Date") //7
        insertEventNodeIntoDb("2023-09-18T04:00Z", "Date") //8
        insertEventNodeIntoDb("2023-09-18T12:00Z", "Date") //9
        insertEventNodeIntoDb("2023-09-19T07:30Z", "Date") //10
        insertEventNodeIntoDb("2023-09-20T05:00Z", "Date") //11
        insertEventNodeIntoDb("2023-09-21T11:45Z", "Date") //12
        insertEventNodeIntoDb("2023-09-22T02:15Z", "Date") //13
        insertEventNodeIntoDb("2023-09-23T04:30Z", "Date") //14
        insertEventNodeIntoDb("2023-09-24T03:20Z", "Date") //15
        insertEventNodeIntoDb("2023-09-25T07:00Z", "Date") //16
        insertEventNodeIntoDb("2023-09-26T08:00Z", "Date") //17
        insertEventNodeIntoDb("2023-09-28T12:30Z", "Date") //19
        insertEventNodeIntoDb("2023-09-29T15:45Z", "Date") //20

        // Event Nodes
        insertEventNodeIntoDb("Reconnaissance Patrol", "Task")
        insertEventNodeIntoDb("Convoy Escort", "Task")
        insertEventNodeIntoDb("Forward Observation", "Task")
        insertEventNodeIntoDb("Resupply Mission", "Task")
        insertEventNodeIntoDb("Quick Reaction Deployment", "Task")
        insertEventNodeIntoDb("Area Surveillance Operation", "Task")
        insertEventNodeIntoDb("Supply Convoy Security", "Task")

        insertEventNodeIntoDb("Ambush", "Incident")
        insertEventNodeIntoDb("Roadside Bombing", "Incident")
        insertEventNodeIntoDb("Sniper Attack", "Incident")
        insertEventNodeIntoDb("Vehicle Breakdown", "Incident")
        insertEventNodeIntoDb("Airstrike Misfire", "Incident")
        insertEventNodeIntoDb("Surprise Attack", "Incident")
        insertEventNodeIntoDb("Improvised Explosive Strike", "Incident")

        insertEventNodeIntoDb("Extraction Completed", "Outcome")
        insertEventNodeIntoDb("Objective Secured", "Outcome")
        insertEventNodeIntoDb("Casualty Evacuation", "Outcome")
        insertEventNodeIntoDb("Mission Delay", "Outcome")
        insertEventNodeIntoDb("Equipment Loss", "Outcome")
        insertEventNodeIntoDb("Evacuation Finalized", "Outcome")
        insertEventNodeIntoDb("Target Area Secured", "Outcome")

        insertEventNodeIntoDb("Operational Delay", "Impact")
        insertEventNodeIntoDb("Intel Gap Created", "Impact")
        insertEventNodeIntoDb("Resource Shortage", "Impact")
        insertEventNodeIntoDb("Increased Hostilities", "Impact")
        insertEventNodeIntoDb("Strategic Advantage Lost", "Impact")
        insertEventNodeIntoDb("Mission Timeline Extended", "Impact")

// --- Tasks (unique properties per Task) ---
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Alpha Company", "Entity"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Gather enemy intel", "Motive"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-15T06:00Z", "Date"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3521,103.8198", "Location"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Foot patrol with UAV support", "Method"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))

        insertEventNodeIntoDb("2023-09-15T07:00Z", "Date")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Alpha Company", "Entity"), getEventNodeByNameAndType("Area Surveillance Operation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy information acquisition", "Motive"), getEventNodeByNameAndType("Area Surveillance Operation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-15T07:00Z", "Date"), getEventNodeByNameAndType("Area Surveillance Operation", "Task"))
//        insertEdgeIntoDB(getNodeByNameAndType("1.3521,103.8198", "Location"), getNodeByNameAndType("Area Surveillance Operation", "Task"))
//        insertEdgeIntoDB(getNodeByNameAndType("Foot patrol", "Method"), getNodeByNameAndType("Area Surveillance Operation", "Task"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Bravo Company", "Entity"), getEventNodeByNameAndType("Convoy Escort", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Secure supply route", "Motive"), getEventNodeByNameAndType("Convoy Escort", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-16T10:45Z", "Date"), getEventNodeByNameAndType("Convoy Escort", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.1155,104.0421", "Location"), getEventNodeByNameAndType("Convoy Escort", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Armored convoy", "Method"), getEventNodeByNameAndType("Convoy Escort", "Task"))

        insertEventNodeIntoDb("Defend transport route", "Motive")
        insertEventNodeIntoDb("Protected convoy", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Bravo Company", "Entity"), getEventNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Defend transport route", "Motive"), getEventNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-16T10:45Z", "Date"), getEventNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.1155,104.0421", "Location"), getEventNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Protected convoy", "Method"), getEventNodeByNameAndType("Supply Convoy Security", "Task"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Charlie Squadron", "Entity"), getEventNodeByNameAndType("Forward Observation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Secure high-value target", "Motive"), getEventNodeByNameAndType("Forward Observation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-17T05:10Z", "Date"), getEventNodeByNameAndType("Forward Observation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4765,103.7636", "Location"), getEventNodeByNameAndType("Forward Observation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Helicopter insertion", "Method"), getEventNodeByNameAndType("Forward Observation", "Task"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Delta Platoon", "Entity"), getEventNodeByNameAndType("Resupply Mission", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Protect civilian population", "Motive"), getEventNodeByNameAndType("Resupply Mission", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-20T05:00Z", "Date"), getEventNodeByNameAndType("Resupply Mission", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4100,103.7600", "Location"), getEventNodeByNameAndType("Resupply Mission", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Armored convoy", "Method"), getEventNodeByNameAndType("Resupply Mission", "Task"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Echo Unit", "Entity"), getEventNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Retaliate against attack", "Motive"), getEventNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-21T11:45Z", "Date"), getEventNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3000,103.9000", "Location"), getEventNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Night vision assault", "Method"), getEventNodeByNameAndType("Quick Reaction Deployment", "Task"))


// --- Incidents (unique Dates, varied properties) ---
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Forces", "Entity"), getEventNodeByNameAndType("Ambush", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Retaliate against attack", "Motive"), getEventNodeByNameAndType("Ambush", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-15T08:30Z", "Date"), getEventNodeByNameAndType("Ambush", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.1344,104.0495", "Location"), getEventNodeByNameAndType("Ambush", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("IED detonation", "Method"), getEventNodeByNameAndType("Ambush", "Incident"))

        insertEventNodeIntoDb("Enemies", "Entity")
        insertEventNodeIntoDb("Counterattack", "Motive")
        insertEventNodeIntoDb("2023-09-15T10:50Z", "Date")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemies", "Entity"), getEventNodeByNameAndType("Surprise Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Counterattack", "Motive"), getEventNodeByNameAndType("Surprise Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-15T10:50Z", "Date"), getEventNodeByNameAndType("Surprise Attack", "Incident"))
//        insertEdgeIntoDB(getNodeByNameAndType("1.1344,104.0495", "Location"), getNodeByNameAndType("Surprise Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("IED detonation", "Method"), getEventNodeByNameAndType("Surprise Attack", "Incident"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Group", "Entity"), getEventNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Disrupt enemy logistics", "Motive"), getEventNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-16T14:20Z", "Date"), getEventNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.1155,104.0421", "Location"), getEventNodeByNameAndType("Roadside Bombing", "Incident"))
//        insertEdgeIntoDB(getNodeByNameAndType("IED explosion", "Method"), getNodeByNameAndType("Roadside Bombing", "Incident"))

        insertEventNodeIntoDb("Interfere with enemy supply lines", "Motive")
        insertEventNodeIntoDb("Explosion of IED", "Method")
        insertEventNodeIntoDb("2023-09-16T15:42Z", "Date")
//        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Group", "Entity"), getEventNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Interfere with enemy supply lines", "Motive"), getEventNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-16T15:42Z", "Date"), getEventNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3530,103.7200", "Location"), getEventNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Explosion of IED", "Method"), getEventNodeByNameAndType("Improvised Explosive Strike", "Incident"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Anonymous", "Entity"), getEventNodeByNameAndType("Sniper Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Secure high-value target", "Motive"), getEventNodeByNameAndType("Sniper Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-17T18:00Z", "Date"), getEventNodeByNameAndType("Sniper Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4765,103.7636", "Location"), getEventNodeByNameAndType("Sniper Attack", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Sniper overwatch", "Method"), getEventNodeByNameAndType("Sniper Attack", "Incident"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Hostile Militia", "Entity"), getEventNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Secure supply route", "Motive"), getEventNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-18T04:00Z", "Date"), getEventNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3600,103.7500", "Location"), getEventNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Armored convoy", "Method"), getEventNodeByNameAndType("Vehicle Breakdown", "Incident"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Rival Faction", "Entity"), getEventNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Protect civilian population", "Motive"), getEventNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-22T02:15Z", "Date"), getEventNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4100,103.7600", "Location"), getEventNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Precision drone strike", "Method"), getEventNodeByNameAndType("Airstrike Misfire", "Incident"))


        // --- Outcomes (unique Dates, varied properties) ---
//        insertEdgeIntoDB(getNodeByNameAndType("Echo Unit", "Entity"), getNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Protect civilian population", "Motive"), getEventNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-23T04:30Z", "Date"), getEventNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3300,103.9200", "Location"), getEventNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("MEDEVAC extraction", "Method"), getEventNodeByNameAndType("Extraction Completed", "Outcome"))

        insertEventNodeIntoDb("Guard general public", "Motive")
        insertEventNodeIntoDb("2023-09-23T04:30Z", "Date")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Echo Unit", "Entity"), getEventNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Guard general public", "Motive"), getEventNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-23T04:30Z", "Date"), getEventNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3300,103.9200", "Location"), getEventNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("MEDEVAC extraction", "Method"), getEventNodeByNameAndType("Evacuation Finalized", "Outcome"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Bravo Company", "Entity"), getEventNodeByNameAndType("Objective Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Establish forward base", "Motive"), getEventNodeByNameAndType("Objective Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-24T03:20Z", "Date"), getEventNodeByNameAndType("Objective Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3000,103.9000", "Location"), getEventNodeByNameAndType("Objective Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Night vision assault", "Method"), getEventNodeByNameAndType("Objective Secured", "Outcome"))

        insertEventNodeIntoDb("Create advanced base", "Motive")
        insertEventNodeIntoDb("2023-09-28T08:20Z", "Date")
        insertEventNodeIntoDb("Nighttime offensive", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Bravo Company", "Entity"), getEventNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Create advanced base", "Motive"), getEventNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-28T08:20Z", "Date"), getEventNodeByNameAndType("Target Area Secured", "Outcome"))
//        insertEdgeIntoDB(getNodeByNameAndType("1.3000,103.9000", "Location"), getNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Nighttime offensive", "Method"), getEventNodeByNameAndType("Target Area Secured", "Outcome"))


        insertEventEdgeIntoDb(getEventNodeByNameAndType("Medical Detachment", "Entity"), getEventNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Respond to threat", "Motive"), getEventNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-25T07:00Z", "Date"), getEventNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3300,103.9200", "Location"), getEventNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Casualty extraction", "Method"), getEventNodeByNameAndType("Casualty Evacuation", "Outcome"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Delta Platoon", "Entity"), getEventNodeByNameAndType("Mission Delay", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Disrupt enemy logistics", "Motive"), getEventNodeByNameAndType("Mission Delay", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-26T08:00Z", "Date"), getEventNodeByNameAndType("Mission Delay", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4765,103.7636", "Location"), getEventNodeByNameAndType("Mission Delay", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Drone surveillance", "Method"), getEventNodeByNameAndType("Mission Delay", "Outcome"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Support Platoon", "Entity"), getEventNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Retaliate against attack", "Motive"), getEventNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-27T09:15Z", "Date"), getEventNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3600,103.7500", "Location"), getEventNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Small arms engagement", "Method"), getEventNodeByNameAndType("Equipment Loss", "Outcome"))

// --- Impacts (unique Dates, varied properties) ---
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Joint Task Force Command", "Entity"), getEventNodeByNameAndType("Operational Delay", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Gather enemy intel", "Motive"), getEventNodeByNameAndType("Operational Delay", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-28T12:30Z", "Date"), getEventNodeByNameAndType("Operational Delay", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4250,103.8500", "Location"), getEventNodeByNameAndType("Operational Delay", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Drone surveillance", "Method"), getEventNodeByNameAndType("Operational Delay", "Impact"))

        insertEventNodeIntoDb("Gather enemy information", "Motive")
        insertEventNodeIntoDb("Drone monitoring", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Joint Task Force Command", "Entity"), getEventNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Gather enemy information", "Motive"), getEventNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-28T12:30Z", "Date"), getEventNodeByNameAndType("Mission Timeline Extended", "Impact"))
//        insertEdgeIntoDB(getNodeByNameAndType("1.4250,103.8500", "Location"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Drone monitoring", "Method"), getEventNodeByNameAndType("Mission Timeline Extended", "Impact"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Engineering Corps", "Entity"), getEventNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Secure high-value target", "Motive"), getEventNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-29T15:45Z", "Date"), getEventNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3000,103.9000", "Location"), getEventNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Precision drone strike", "Method"), getEventNodeByNameAndType("Intel Gap Created", "Impact"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Support Platoon", "Entity"), getEventNodeByNameAndType("Resource Shortage", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Disrupt enemy logistics", "Motive"), getEventNodeByNameAndType("Resource Shortage", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-19T07:30Z", "Date"), getEventNodeByNameAndType("Resource Shortage", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4100,103.7600", "Location"), getEventNodeByNameAndType("Resource Shortage", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Armored convoy", "Method"), getEventNodeByNameAndType("Resource Shortage", "Impact"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Anonymous", "Entity"), getEventNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Retaliate against attack", "Motive"), getEventNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-18T12:00Z", "Date"), getEventNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.1155,104.0421", "Location"), getEventNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Small arms engagement", "Method"), getEventNodeByNameAndType("Increased Hostilities", "Impact"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Joint Task Force Command", "Entity"), getEventNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Establish forward base", "Motive"), getEventNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-17T05:10Z", "Date"), getEventNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4765,103.7636", "Location"), getEventNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Night operations", "Method"), getEventNodeByNameAndType("Strategic Advantage Lost", "Impact"))

        // --- Edges between Events ---
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Ambush", "Incident"), getEventNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Roadside Bombing", "Incident"), getEventNodeByNameAndType("Convoy Escort", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Sniper Attack", "Incident"), getEventNodeByNameAndType("Forward Observation", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Vehicle Breakdown", "Incident"), getEventNodeByNameAndType("Resupply Mission", "Task"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Reconnaissance Patrol", "Outcome"), getEventNodeByNameAndType("Extraction Completed", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Convoy Escort", "Outcome"), getEventNodeByNameAndType("Objective Secured", "Task"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Forward Observation", "Outcome"), getEventNodeByNameAndType("Casualty Evacuation", "Task"))

        insertEventEdgeIntoDb(getEventNodeByNameAndType("Operational Delay", "Impact"), getEventNodeByNameAndType("Ambush", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Resource Shortage", "Impact"), getEventNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Increased Hostilities", "Impact"), getEventNodeByNameAndType("Sniper Attack", "Incident"))

        // Incidents for Suspicious Behaviour Detection
        insertEventNodeIntoDb("Individual Observes Entry Point", "Incident")
        insertEventNodeIntoDb("Unidentified Male", "Entity")
        insertEventNodeIntoDb("2023-09-10T08:45Z", "Date")
        insertEventNodeIntoDb("1.3589,103.6999", "Location")
        insertEventNodeIntoDb("Loitering with camera", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Unidentified Male", "Entity"), getEventNodeByNameAndType("Individual Observes Entry Point", "Incident"))
//        insertEventEdgeIntoDb(getEventNodeByNameAndType("Observe access patterns", "Motive"), getEventNodeByNameAndType("Individual Observes Entry Point", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-10T08:45Z", "Date"), getEventNodeByNameAndType("Individual Observes Entry Point", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3589,103.6999", "Location"), getEventNodeByNameAndType("Individual Observes Entry Point", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Loitering with camera", "Method"), getEventNodeByNameAndType("Individual Observes Entry Point", "Incident"))

        insertEventNodeIntoDb("Repeated Walk-By Near Security Fence", "Incident")
        insertEventNodeIntoDb("Middle-aged Woman", "Entity")
        insertEventNodeIntoDb("2023-09-11T10:12Z", "Date")
        insertEventNodeIntoDb("1.3453,103.5300", "Location")
        insertEventNodeIntoDb("Walking in circles", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Middle-aged Woman", "Entity"), getEventNodeByNameAndType("Repeated Walk-By Near Security Fence", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Test patrol timing", "Motive"), getEventNodeByNameAndType("Repeated Walk-By Near Security Fence", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-11T10:12Z", "Date"), getEventNodeByNameAndType("Repeated Walk-By Near Security Fence", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3453,103.5300", "Location"), getEventNodeByNameAndType("Repeated Walk-By Near Security Fence", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Walking in circles", "Method"), getEventNodeByNameAndType("Repeated Walk-By Near Security Fence", "Incident"))

        insertEventNodeIntoDb("Note-taking by Bus Stop Near Gate", "Incident")
        insertEventNodeIntoDb("Unidentified Youth", "Entity")
        insertEventNodeIntoDb("2023-09-12T07:05Z", "Date")
        insertEventNodeIntoDb("1.3425,103.6897", "Location")
        insertEventNodeIntoDb("Taking notes discreetly", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Unidentified Youth", "Entity"), getEventNodeByNameAndType("Note-taking by Bus Stop Near Gate", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Record shift changes", "Motive"), getEventNodeByNameAndType("Note-taking by Bus Stop Near Gate", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-12T07:05Z", "Date"), getEventNodeByNameAndType("Note-taking by Bus Stop Near Gate", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3425,103.6897", "Location"), getEventNodeByNameAndType("Note-taking by Bus Stop Near Gate", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Taking notes discreetly", "Method"), getEventNodeByNameAndType("Note-taking by Bus Stop Near Gate", "Incident"))

        insertEventNodeIntoDb("Object Handover at Park Bench", "Incident")
        insertEventNodeIntoDb("Two Individuals", "Entity")
        insertEventNodeIntoDb("2023-09-13T13:37Z", "Date")
        insertEventNodeIntoDb("1.3802,103.6902", "Location")
        insertEventNodeIntoDb("Briefcase handover", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Two Individuals", "Entity"), getEventNodeByNameAndType("Object Handover at Park Bench", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Pass instructions or items", "Motive"), getEventNodeByNameAndType("Object Handover at Park Bench", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-13T13:37Z", "Date"), getEventNodeByNameAndType("Object Handover at Park Bench", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3802,103.6902", "Location"), getEventNodeByNameAndType("Object Handover at Park Bench", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Briefcase handover", "Method"), getEventNodeByNameAndType("Object Handover at Park Bench", "Incident"))

        insertEventNodeIntoDb("Group Seen Marking Utility Pole", "Incident")
        insertEventNodeIntoDb("Small Group", "Entity")
        insertEventNodeIntoDb("2023-09-14T09:55Z", "Date")
        insertEventNodeIntoDb("1.4100,103.8801", "Location")
        insertEventNodeIntoDb("Use of chalk/paint marking", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Small Group", "Entity"), getEventNodeByNameAndType("Group Seen Marking Utility Pole", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Signal specific location", "Motive"), getEventNodeByNameAndType("Group Seen Marking Utility Pole", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-14T09:55Z", "Date"), getEventNodeByNameAndType("Group Seen Marking Utility Pole", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.4100,103.8801", "Location"), getEventNodeByNameAndType("Group Seen Marking Utility Pole", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Use of chalk/paint marking", "Method"), getEventNodeByNameAndType("Group Seen Marking Utility Pole", "Incident"))


        /*-----------------------------------------
        |    FOR ROUTE INTEGRITY USE CASE         |
        -----------------------------------------*/
        // Incident 1: Bombing at Urban Supply Depot (near route index 21)
        insertEventNodeIntoDb("Bombing at Urban Supply Depot", "Incident")
        insertEventNodeIntoDb("Unknown Operative", "Entity")
        insertEventNodeIntoDb("Disrupt supply lines", "Motive")
        insertEventNodeIntoDb("2023-09-13T07:20Z", "Date")
        insertEventNodeIntoDb("1.3250,103.8098", "Location")
        insertEventNodeIntoDb("Explosive Charges", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Unknown Operative", "Entity"), getEventNodeByNameAndType("Bombing at Urban Supply Depot", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Disrupt supply lines", "Motive"), getEventNodeByNameAndType("Bombing at Urban Supply Depot", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-13T07:20Z", "Date"), getEventNodeByNameAndType("Bombing at Urban Supply Depot", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3250,103.8098", "Location"), getEventNodeByNameAndType("Bombing at Urban Supply Depot", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Explosive Charges", "Method"), getEventNodeByNameAndType("Bombing at Urban Supply Depot", "Incident"))

// Incident 2: Sniper Nest Detected on Ridge (near route index 37)
        insertEventNodeIntoDb("Sniper Nest Detected on Ridge", "Incident")
        insertEventNodeIntoDb("Hostile Marksman", "Entity")
        insertEventNodeIntoDb("Target high-ranking officer", "Motive")
        insertEventNodeIntoDb("2023-09-13T07:32Z", "Date")
        insertEventNodeIntoDb("1.3010,103.7845", "Location")
        insertEventNodeIntoDb("Scoped Rifle from Elevated Cover", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Hostile Marksman", "Entity"), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Target high-ranking officer", "Motive"), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-13T07:32Z", "Date"), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3010,103.7845", "Location"), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Scoped Rifle from Elevated Cover", "Method"), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", "Incident"))

// Incident 3: Sabotage at Communications Relay (near route index 43)
        insertEventNodeIntoDb("Sabotage at Communications Relay", "Incident")
        insertEventNodeIntoDb("Insider Threat", "Entity")
        insertEventNodeIntoDb("Blind surveillance systems", "Motive")
        insertEventNodeIntoDb("2023-09-13T07:40Z", "Date")
        insertEventNodeIntoDb("1.2859,103.7298", "Location")
        insertEventNodeIntoDb("Signal Jammer Deployment", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Insider Threat", "Entity"), getEventNodeByNameAndType("Sabotage at Communications Relay", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Blind surveillance systems", "Motive"), getEventNodeByNameAndType("Sabotage at Communications Relay", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-13T07:40Z", "Date"), getEventNodeByNameAndType("Sabotage at Communications Relay", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.2859,103.7298", "Location"), getEventNodeByNameAndType("Sabotage at Communications Relay", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Signal Jammer Deployment", "Method"), getEventNodeByNameAndType("Sabotage at Communications Relay", "Incident"))

// Incident 4: Enemy Encampment Spotted in Jungle (near route index 44)
        insertEventNodeIntoDb("Enemy Encampment Spotted in Jungle", "Incident")
        insertEventNodeIntoDb("Militant Group Foxtrot", "Entity")
        insertEventNodeIntoDb("Staging ground for ambush", "Motive")
        insertEventNodeIntoDb("2023-09-13T07:53Z", "Date")
        insertEventNodeIntoDb("1.2948,103.7521", "Location")
        insertEventNodeIntoDb("Camouflaged Tent Setup", "Method")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Militant Group Foxtrot", "Entity"), getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Staging ground for ambush", "Motive"), getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-13T07:53Z", "Date"), getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.2948,103.7521", "Location"), getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Camouflaged Tent Setup", "Method"), getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", "Incident"))

        // Testing direction of airflow thing
        insertEventNodeIntoDb("Chemical Release Into Air", "Incident")
        insertEventNodeIntoDb("Malicious intent", "Motive")
        insertEventNodeIntoDb("2023-09-23T22:15Z", "Date")
        insertEventNodeIntoDb("1.3521,103.7927", "Location")
        insertEventNodeIntoDb("Release of toxic materials from factory", "Method")
//        insertEventEdgeIntoDb(getEventNodeByNameAndType("Unknown Source", "Entity"), getEventNodeByNameAndType("Chemical Release Into Air", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Malicious intent", "Motive"), getEventNodeByNameAndType("Chemical Release Into Air", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-23T22:15Z", "Date"), getEventNodeByNameAndType("Chemical Release Into Air", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("1.3521,103.7927", "Location"), getEventNodeByNameAndType("Chemical Release Into Air", "Incident"))
        insertEventEdgeIntoDb(getEventNodeByNameAndType("Release of toxic materials from factory", "Method"), getEventNodeByNameAndType("Chemical Release Into Air", "Incident"))

        insertEventNodeIntoDb("SE", "Wind")
        insertEventEdgeIntoDb(getEventNodeByNameAndType("2023-09-23T22:15Z", "Date"), getEventNodeByNameAndType("SE", "Wind"))

        Log.d("INITIALISE DATABASE", "Data initialised.")
}
}