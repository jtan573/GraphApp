package com.example.graphapp.data.repository

import android.util.Log
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.EventDatabaseQueries
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaSemanticPropertyNodes
import com.example.graphapp.backend.usecases.restoreLocationFromString
import kotlin.math.abs

class EventRepository(
    private val embeddingRepository: EmbeddingRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val posTaggerRepository: PosTaggerRepository
) {

    private val queries = EventDatabaseQueries()

    // Function to add node
    suspend fun insertEventNodeIntoDb(
        inputName: String,
        inputType: String,
        inputDescription: String? = null,
        inputFrequency: Int? = 1
    ): Long {
        val nodeFound = queries.findNodeByNameTypeQuery(inputName, inputType)

        if (nodeFound != null) {
            val nodeId = queries.incrementFreqOfNodeQuery(nodeFound)
            return nodeId
        } else {

            var allTags = listOf<String>()
            var posTags = listOf<String>()
            var filteredPosTags = listOf<String>()
            var isSuspicious = false

            if (inputType in SchemaSemanticPropertyNodes) {
                isSuspicious = dictionaryRepository.checkIfSuspicious(inputName.lowercase()).isNotEmpty()

                val (matchedPhrases, cleanedSentence) = dictionaryRepository.extractAndRemovePhrases(inputName)
                val taggedSentence = posTaggerRepository.tagText(cleanedSentence.lowercase())
                posTags = posTaggerRepository.extractTaggedWords(taggedSentence)
                filteredPosTags = dictionaryRepository.replaceSimilarTags(posTags)
                allTags = filteredPosTags + matchedPhrases
            }

            val relevantTags = if (isSuspicious) {
                allTags + "suspicious"
            } else {
                allTags
            }

            val nodeId = queries.addNodeIntoDbQuery(
                name = inputName,
                type = inputType.uppercase(),
                description = inputDescription,
                frequency = inputFrequency,
                embedding = embeddingRepository.getTextEmbeddings(inputName.lowercase()),
                tags = relevantTags
            )

            // Update dictionary repository after node is initialised
            val eventNode = getEventNodeById(nodeId)
            if (eventNode != null) {
                filteredPosTags.forEach {
                    dictionaryRepository.insertPosTagIntoDb(it, eventNode)
                }
            }

            return nodeId
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

    fun getAllEdgesAroundNodeId(id: Long): List<EventEdgeEntity> {
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

        val tags = mutableListOf<String>()
        if (type in SchemaSemanticPropertyNodes) {
            tags.addAll(posTaggerRepository.extractTaggedWords(posTaggerRepository.tagText(value)))
        }

        return EventNodeEntity(
            id = (-1L * (1..1_000_000).random()),
            name = value,
            type = type,
            embedding = embeddingRepository.getTextEmbeddings(value),
            tags = tags
        )
    }

    fun removeNodeById(inputId: Long) {
        val neighboursIds = getNeighborsOfEventNodeById(inputId).map { it.id }

        val nodeIdsToRemove = mutableListOf<Long>()
        val edgeIdsToRemove = mutableListOf<Long>()

        neighboursIds.forEach { neighbourId ->
            val neighboursOfNeighbour = getNeighborsOfEventNodeById(neighbourId)
            if (neighboursOfNeighbour.size == 1) {
                nodeIdsToRemove.add(neighbourId)
                edgeIdsToRemove.addAll(getAllEdgesAroundNodeId(neighbourId).map { it.id })
            }
        }
        queries.deleteNodesAndEdges(nodeIdsToRemove, edgeIdsToRemove)
    }

    // Retrieve nodes with relevant tags
    suspend fun getRelevantNodes(
        eventTags: List<String>,
        eventType: String   // Type of nodes to retrieve
    ): List<EventNodeEntity> {
        return dictionaryRepository.getEventsWithSimilarTags(eventTags, eventType)
    }

    // Retrieve nodes by distance
    fun getCloseNodesByLocation(
        location: String
    ): List<EventNodeEntity> {
        val relevantNodes = mutableListOf<EventNodeEntity>()
        val allNodes = getAllEventNodes().filter{ it.type == SchemaEventTypeNames.WHERE.key }
        allNodes.forEach { locationNode ->
            val distance = restoreLocationFromString(location).distanceTo(restoreLocationFromString(locationNode.name))
            if (distance < 5000f) {
                relevantNodes.add(locationNode)
            }
        }
        return relevantNodes
    }

    // Retrieve nodes by datetime
    fun getCloseNodesByDatetime(
        inputDateTime: String
    ): List<EventNodeEntity> {
        val relevantNodes = mutableListOf<EventNodeEntity>()
        val allNodes = getAllEventNodes().filter{ it.type == SchemaEventTypeNames.WHEN.key }
        allNodes.forEach { dateNode ->
            val oneDayMs = 86_400_000L
            val timeDiff = abs(inputDateTime.toLong() - dateNode.name.toLong())
            if (timeDiff <= 3 * oneDayMs) {
                relevantNodes.add(dateNode)
            }
        }
        return relevantNodes
    }

    fun resetEventDb() {
        queries.resetEventDbQuery()
    }

    // Function to initialise repository
    suspend fun initialiseEventRepository() {

        // --- Entities ---
        insertEventNodeIntoDb("Alpha Company", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Bravo Company", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Charlie Squadron", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Delta Platoon", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Echo Unit", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Enemy Forces", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Enemy Group", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Enemy Anonymous", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Joint Task Force Command", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Insurgent Cell", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Hostile Militia", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Opposing Battalion", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Rival Faction", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Support Platoon", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Engineering Corps", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Medical Detachment", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Logistics Division", SchemaEventTypeNames.WHO.key)


// --- Locations ---
        insertEventNodeIntoDb("1.3521,103.8198", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.1344,104.0495", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.4765,103.7636", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.4250,103.8500", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.3600,103.7500", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.3300,103.9200", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.4100,103.7600", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.1155,104.0421", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.3000,103.9000", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("1.3530,103.7200", SchemaEventTypeNames.WHERE.key)

// --- Methods ---
        insertEventNodeIntoDb("Foot patrol with UAV support", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Armored convoy", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Helicopter insertion", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("IED detonation", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Small arms engagement", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("MEDEVAC extraction", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Casualty extraction", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Sniper overwatch", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Drone surveillance", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Night vision assault", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Night operations", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("Precision drone strike", SchemaEventTypeNames.HOW.key)

// --- Motives ---
        insertEventNodeIntoDb("Gather enemy intel", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Secure supply route", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Disrupt enemy logistics", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Respond to threat", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Establish forward base", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Retaliate against attack", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Secure high-value target", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Protect civilian population", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Enemy information acquisition", SchemaEventTypeNames.WHY.key)

// --- DateTimes (unique) ---
        insertEventNodeIntoDb("1694757600000", SchemaEventTypeNames.WHEN.key) //1
        insertEventNodeIntoDb("1694766600000", SchemaEventTypeNames.WHEN.key) //2
//        insertNodeIntoDb("2023-09-15T09:15Z", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHEN.key) //3
        insertEventNodeIntoDb("1694861100000", SchemaEventTypeNames.WHEN.key) //4
        insertEventNodeIntoDb("1694876400000", SchemaEventTypeNames.WHEN.key) //5
        insertEventNodeIntoDb("1694937000000", SchemaEventTypeNames.WHEN.key) //6
        insertEventNodeIntoDb("1694983200000", SchemaEventTypeNames.WHEN.key) //7
        insertEventNodeIntoDb("1695019200000", SchemaEventTypeNames.WHEN.key) //8
        insertEventNodeIntoDb("1695048000000", SchemaEventTypeNames.WHEN.key) //9
        insertEventNodeIntoDb("1695118200000", SchemaEventTypeNames.WHEN.key) //10
        insertEventNodeIntoDb("1695186000000", SchemaEventTypeNames.WHEN.key) //11
        insertEventNodeIntoDb("1695296700000", SchemaEventTypeNames.WHEN.key) //12
        insertEventNodeIntoDb("1695348900000", SchemaEventTypeNames.WHEN.key) //13
        insertEventNodeIntoDb("1695443400000", SchemaEventTypeNames.WHEN.key) //14
        insertEventNodeIntoDb("1695529200000", SchemaEventTypeNames.WHEN.key) //15
        insertEventNodeIntoDb("1695634800000", SchemaEventTypeNames.WHEN.key) //16
        insertEventNodeIntoDb("1695705600000", SchemaEventTypeNames.WHEN.key) //17
        insertEventNodeIntoDb("1695894600000", SchemaEventTypeNames.WHEN.key) //19
        insertEventNodeIntoDb("1696002300000", SchemaEventTypeNames.WHEN.key) //20

        // Event Nodes
        insertEventNodeIntoDb("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Convoy Escort", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Forward Observation", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Resupply Mission", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Area Surveillance Operation", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Supply Convoy Security", SchemaEventTypeNames.TASK.key)

        insertEventNodeIntoDb("Ambush", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Surprise Attack", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Improvised Explosive Strike", SchemaEventTypeNames.INCIDENT.key)

        insertEventNodeIntoDb("Extraction Completed", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        insertEventNodeIntoDb("Target Area Secured", SchemaEventTypeNames.OUTCOME.key)

        insertEventNodeIntoDb("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key)

// --- Tasks (unique properties per Task) ---
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Alpha Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Gather enemy intel", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694757600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3521,103.8198", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Foot patrol with UAV support", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key)
        )

        insertEventNodeIntoDb("1694761200000", SchemaEventTypeNames.WHEN.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Alpha Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Area Surveillance Operation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemy information acquisition", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Area Surveillance Operation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694761200000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Area Surveillance Operation", SchemaEventTypeNames.TASK.key)
        )
//        insertEdgeIntoDB(getNodeByNameAndType("1.3521,103.8198", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHERE.key), getNodeByNameAndType("Area Surveillance Operation", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.TASK.key))
//        insertEdgeIntoDB(getNodeByNameAndType("Foot patrol", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.HOW.key), getNodeByNameAndType("Area Surveillance Operation", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.TASK.key))


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Bravo Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Secure supply route", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694861100000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.1155,104.0421", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Armored convoy", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key)
        )

        insertEventNodeIntoDb("Defend transport route", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Protected convoy", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Bravo Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Supply Convoy Security", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Defend transport route", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Supply Convoy Security", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694861100000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Supply Convoy Security", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.1155,104.0421", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Supply Convoy Security", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Protected convoy", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Supply Convoy Security", SchemaEventTypeNames.TASK.key)
        )


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Charlie Squadron", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Secure high-value target", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694937000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4765,103.7636", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Helicopter insertion", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Delta Platoon", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Protect civilian population", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695186000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4100,103.7600", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Armored convoy", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Echo Unit", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Retaliate against attack", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695296700000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3000,103.9000", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Night vision assault", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Quick Reaction Deployment", SchemaEventTypeNames.TASK.key)
        )


// --- Incidents (unique DateTimes, varied properties) ---
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemy Forces", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Retaliate against attack", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694766600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.1344,104.0495", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("IED detonation", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventNodeIntoDb("Enemies", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Counterattack", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1694775000000", SchemaEventTypeNames.WHEN.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemies", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Surprise Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Counterattack", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Surprise Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694775000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Surprise Attack", SchemaEventTypeNames.INCIDENT.key)
        )
//        insertEdgeIntoDB(getNodeByNameAndType("1.1344,104.0495", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHERE.key), getNodeByNameAndType("Surprise Attack", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.INCIDENT.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("IED detonation", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Surprise Attack", SchemaEventTypeNames.INCIDENT.key)
        )


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemy Group", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Disrupt enemy logistics", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694876400000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.1155,104.0421", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
//        insertEdgeIntoDB(getNodeByNameAndType("IED explosion", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.HOW.key), getNodeByNameAndType("Roadside Bombing", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.INCIDENT.key))

        insertEventNodeIntoDb("Interfere with enemy supply lines", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Explosion of IED", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("1694878920000", SchemaEventTypeNames.WHEN.key)
//        insertEventEdgeIntoDb(getEventNodeByNameAndType("Enemy Group", SchemaEventTypeNames.WHO.key), getEventNodeByNameAndType("Improvised Explosive Strike", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.INCIDENT.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Interfere with enemy supply lines",
                SchemaEventTypeNames.WHY.key
            ), getEventNodeByNameAndType("Improvised Explosive Strike", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694878920000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Improvised Explosive Strike", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3530,103.7200", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Improvised Explosive Strike", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Explosion of IED", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Improvised Explosive Strike", SchemaEventTypeNames.INCIDENT.key)
        )


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemy Anonymous", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Secure high-value target", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694983200000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4765,103.7636", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Sniper overwatch", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Hostile Militia", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Secure supply route", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695019200000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3600,103.7500", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Armored convoy", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Rival Faction", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Protect civilian population", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695348900000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4100,103.7600", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Precision drone strike", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Airstrike Misfire", SchemaEventTypeNames.INCIDENT.key)
        )


        // --- Outcomes (unique DateTimes, varied properties) ---
//        insertEdgeIntoDB(getNodeByNameAndType("Echo Unit", SchemaEventTypeNames.WHO.key), getNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.OUTCOME.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Protect civilian population", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695443400000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3300,103.9200", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("MEDEVAC extraction", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.OUTCOME.key)
        )

        insertEventNodeIntoDb("Guard general public", SchemaEventTypeNames.WHY.key)
//        insertEventNodeIntoDb("1695443400000", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHEN.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Echo Unit", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Guard general public", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695443400000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3300,103.9200", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("MEDEVAC extraction", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Evacuation Finalized", SchemaEventTypeNames.OUTCOME.key)
        )


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Bravo Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Establish forward base", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695529200000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3000,103.9000", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Night vision assault", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.OUTCOME.key)
        )

        insertEventNodeIntoDb("Create advanced base", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1695882000000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("Nighttime offensive", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Bravo Company", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Target Area Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Create advanced base", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Target Area Secured", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695882000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Target Area Secured", SchemaEventTypeNames.OUTCOME.key)
        )
//        insertEdgeIntoDB(getNodeByNameAndType("1.3000,103.9000", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHERE.key), getNodeByNameAndType("Target Area Secured", SchemaEventTypeNames.OUTCOME.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Nighttime offensive", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Target Area Secured", SchemaEventTypeNames.OUTCOME.key)
        )


        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Medical Detachment", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Respond to threat", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695634800000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3300,103.9200", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Casualty extraction", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.OUTCOME.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Delta Platoon", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Disrupt enemy logistics", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695705600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4765,103.7636", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Drone surveillance", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Mission Delay", SchemaEventTypeNames.OUTCOME.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Support Platoon", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Retaliate against attack", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695806100000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3600,103.7500", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Small arms engagement", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Equipment Loss", SchemaEventTypeNames.OUTCOME.key)
        )

// --- Impacts (unique DateTimes, varied properties) ---
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Joint Task Force Command", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Gather enemy intel", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695894600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4250,103.8500", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Drone surveillance", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventNodeIntoDb("Gather enemy information", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Drone monitoring", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Joint Task Force Command", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Gather enemy information", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695894600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key)
        )
//        insertEdgeIntoDB(getNodeByNameAndType("1.4250,103.8500", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.WHERE.key), getNodeByNameAndType("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Drone monitoring", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Mission Timeline Extended", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Engineering Corps", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Secure high-value target", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1696002300000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3000,103.9000", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Precision drone strike", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Intel Gap Created", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Support Platoon", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Disrupt enemy logistics", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695118200000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4100,103.7600", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Armored convoy", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Enemy Anonymous", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Retaliate against attack", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695048000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.1155,104.0421", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Small arms engagement", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Joint Task Force Command", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Establish forward base", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694937000000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.4765,103.7636", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Night operations", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Strategic Advantage Lost", SchemaEventTypeNames.IMPACT.key)
        )

        // --- Edges between Events ---
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Resupply Mission", SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Vehicle Breakdown", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Reconnaissance Patrol", SchemaEventTypeNames.OUTCOME.key),
            getEventNodeByNameAndType("Extraction Completed", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Convoy Escort", SchemaEventTypeNames.OUTCOME.key),
            getEventNodeByNameAndType("Objective Secured", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Forward Observation", SchemaEventTypeNames.OUTCOME.key),
            getEventNodeByNameAndType("Casualty Evacuation", SchemaEventTypeNames.TASK.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Operational Delay", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Ambush", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Resource Shortage", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Roadside Bombing", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Increased Hostilities", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Sniper Attack", SchemaEventTypeNames.INCIDENT.key)
        )

        /*-----------------------------------------
        |    FOR THREAT DETECTION USE CASE        |
        -----------------------------------------*/
        insertEventNodeIntoDb("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Aerial Perimeter Recon", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb(
            "Battery thermal runaway mid-air caused fireball and crash in vegetation zone",
            SchemaEventTypeNames.HOW.key
        )
        insertEventNodeIntoDb("1737177600000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3331,103.8198", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Aerial Perimeter Recon", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Battery thermal runaway mid-air caused fireball and crash in vegetation zone",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Drone Battery Ignition During Patrol",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737177600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3331,103.8198", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventNodeIntoDb("Drone Communication Loss Over Fire Zone", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Fire Perimeter Scouting", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb(
            "Thermal interference disrupted uplink, causing drone to hover erratically before crash-landing near treeline",
            SchemaEventTypeNames.HOW.key
        )
        insertEventNodeIntoDb("1737187500000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3683,103.8454", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Fire Perimeter Scouting", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Thermal interference disrupted uplink, causing drone to hover erratically before crash-landing near treeline",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737187500000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3683,103.8454", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventNodeIntoDb("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Pre-Deployment Check", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Dust ingress in rotor hub stalled motor mid-ascent, causing drone to crash near fire truck", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("1737196800000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3012,103.7880", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Pre-Deployment Check", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Dust ingress in rotor hub stalled motor mid-ascent, causing drone to crash near fire truck", SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737196800000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3012,103.7880", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )

        // task
        insertEventNodeIntoDb("Isolate Crash Zone and Suppress Ember Spread", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Prevent vegetation ignition from battery combustion debris", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Deploy foam suppressant around crash area and establish ember watch perimeter", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("1737177900000", SchemaEventTypeNames.WHEN.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Prevent vegetation ignition from battery combustion debris", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Deploy foam suppressant around crash area and establish ember watch perimeter", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread",SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737177900000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread",SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3331,103.8198", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread",SchemaEventTypeNames.TASK.key)
        )
        // crash fire chief, explosive ordinance disposal technician, military police squad leader
        insertEventNodeIntoDb(
            inputName = "Crash Fire Chief", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Controls exclusion zones, coordinates perimeter checkpoints, and delivers PA evacuation instructions during ordnance/ordinance incidents."
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Crash Fire Chief", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Explosive Ordnance Disposal Technician", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Specialist trained to identify and neutralize explosive hazards present in the crash zone.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Explosive Ordnance Disposal Technician", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Foam Suppression Technician", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Deploys aerial foam blankets, maintains ember perimeter suppression and hot-spot knockdown, and enforces crash-zone foam safety protocols with CFR guidance.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Foam Suppression Technician", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread", SchemaEventTypeNames.TASK.key)
        )

        // task
        insertEventNodeIntoDb("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Resume safe flight control over surveillance drones", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Track last known GPS path, recover wreckage, and reconfigure thermal-resistant signal repeater", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("1737187800000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3684,103.8454", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Resume safe flight control over surveillance drones", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Track last known GPS path, recover wreckage, and reconfigure thermal-resistant signal repeater", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737187800000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3684,103.8454", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        // uav recovery specialist, signals officer, electronic warfare operator
        insertEventNodeIntoDb(inputName = "Drone Recovery Specialist", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Locates and retrieves downed UAVs, secures components, and supports data uplink reinitialisation post-crash.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Drone Recovery Specialist", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Signal Systems Engineer", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Restores drone communication/uplink by configuring repeater systems, analysing interference logs, and validating protocol resets.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Signal Systems Engineer", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Electronic Warfare Operator", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Oversees launch pad readiness and pre-flight mechanical integrity checks for UAV fleets; assists post-incident inspection and safe recovery handling.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Electronic Warfare Operator", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols", SchemaEventTypeNames.TASK.key)
        )

        // task
        insertEventNodeIntoDb("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        insertEventNodeIntoDb("Prevent launch delays and rule out drone batch-wide mechanical faults", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("Clear debris from lift pad and conduct rotor health scan across nearby UAVs", SchemaEventTypeNames.HOW.key)
        insertEventNodeIntoDb("1737196860000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3013,103.7880", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Prevent launch delays and rule out drone batch-wide mechanical faults", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Clear debris from lift pad and conduct rotor health scan across nearby UAVs", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737196860000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3013,103.7880", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        // crew chief, ordinance technician, maintenance quality inspector
        insertEventNodeIntoDb(inputName = "Crew Chief", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Aircraft maintainer in charge of coordinating launch pad operations and overall readiness of airframes.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Crew Chief", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Ordnance Technician", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Specialist responsible for safe handling, installation, and inspection of aircraft munitions and ordnance systems.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Ordnance Technician", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )
        insertEventNodeIntoDb(inputName = "Maintenance Quality Inspector", inputType = SchemaEventTypeNames.WHO.key,
            inputDescription = "Performs debris removal, conducts fine-grain diagnostics on rotor assemblies, and validates lift-off safety protocols.")
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Maintenance Quality Inspector", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key)
        )

        // impact
        insertEventNodeIntoDb("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Fire Line Command Unit", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb(
            "Uplink loss triggered blackout over 400m stretch of fire line, delaying spread estimation",
            SchemaEventTypeNames.HOW.key
        )
        insertEventNodeIntoDb("1737197100000", SchemaEventTypeNames.WHEN.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Fire Line Command Unit", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Uplink loss triggered blackout over 400m stretch of fire line, delaying spread estimation",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737197100000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3012,103.7880", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventNodeIntoDb("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("Drone Patrol Zone Crew", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb(
            "Battery fragments landed in brush, igniting localized ground fire before suppression team arrived",
            SchemaEventTypeNames.HOW.key
        )
        insertEventNodeIntoDb("1737177660000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3531,103.8178", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Drone Patrol Zone Crew", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Battery fragments landed in brush, igniting localized ground fire before suppression team arrived",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737177660000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3531,103.8178", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventNodeIntoDb("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key)
        insertEventNodeIntoDb("UAV Ground Control Team", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb(
            "Pad debris and rotor jam caused a 30-minute hold on drone launch queue",
            SchemaEventTypeNames.HOW.key
        )
        insertEventNodeIntoDb("1737187560000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3685,103.8454", SchemaEventTypeNames.WHERE.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("UAV Ground Control Team", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Pad debris and rotor jam caused a 30-minute hold on drone launch queue",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1737187560000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3685,103.8454", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key)
        )

        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Isolate Crash Zone and Suppress Ember Spread",
                SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Retrieve Drone Wreck and Reset Uplink Protocols",
                SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Clear Launch Pad and Inspect Fleet", SchemaEventTypeNames.TASK.key),
            getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Small-Scale Fire Ignited in Crash Radius", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Drone Battery Ignition During Patrol", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Unmanned Zone Lost Visual Coverage", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Drone Communication Loss Over Fire Zone",
                SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Launch Operations Temporarily Suspended", SchemaEventTypeNames.IMPACT.key),
            getEventNodeByNameAndType("Drone Rotor Jammed During Lift-Off", SchemaEventTypeNames.INCIDENT.key)
        )

        /*-----------------------------------------
        |    FOR SUSPICIOUS BEHAVIOUR USE CASE    |
        -----------------------------------------*/
        insertEventNodeIntoDb(
            "Subject loiters near restricted zone appearing to scan the area",
            SchemaEventTypeNames.INCIDENT.key
        )
        insertEventNodeIntoDb("Unidentified Male", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("1694335500000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3500,103.6999", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Loitering with camera", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Unidentified Male", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType(
                "Subject loiters near restricted zone appearing to scan the area",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694335500000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType(
                "Subject loiters near restricted zone appearing to scan the area",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3500,103.6999", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType(
                "Subject loiters near restricted zone appearing to scan the area",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Loitering with camera", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType(
                "Subject loiters near restricted zone appearing to scan the area",
                SchemaEventTypeNames.INCIDENT.key
            )
        )

        insertEventNodeIntoDb(
            "Subject spotted wandering aimlessly along perimeter fencing",
            SchemaEventTypeNames.INCIDENT.key
        )
        insertEventNodeIntoDb("Middle-aged Woman", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("1694427120000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3453,103.6000", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Walking in circles", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Middle-aged Woman", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType(
                "Subject spotted wandering aimlessly along perimeter fencing",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694427120000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType(
                "Subject spotted wandering aimlessly along perimeter fencing",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3453,103.6000", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType(
                "Subject spotted wandering aimlessly along perimeter fencing",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Walking in circles", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType(
                "Subject spotted wandering aimlessly along perimeter fencing",
                SchemaEventTypeNames.INCIDENT.key
            )
        )

        insertEventNodeIntoDb(
            "Person discreetly writing or sketching near checkpoint structure",
            SchemaEventTypeNames.INCIDENT.key
        )
        insertEventNodeIntoDb("Unidentified Youth", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("1694502300000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3425,103.6897", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Taking notes discreetly", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Unidentified Youth", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType(
                "Person discreetly writing or sketching near checkpoint structure",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694502300000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType(
                "Person discreetly writing or sketching near checkpoint structure",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3425,103.6897", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType(
                "Person discreetly writing or sketching near checkpoint structure",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Taking notes discreetly", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType(
                "Person discreetly writing or sketching near checkpoint structure",
                SchemaEventTypeNames.INCIDENT.key
            )
        )

        insertEventNodeIntoDb(
            "Unusual handoff of item occurs at public bench with minimal interaction",
            SchemaEventTypeNames.INCIDENT.key
        )
        insertEventNodeIntoDb("Two Individuals", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("1694612220000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3456,103.6902", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Briefcase handover", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Two Individuals", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType(
                "Unusual handoff of item occurs at public bench with minimal interaction",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694612220000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType(
                "Unusual handoff of item occurs at public bench with minimal interaction",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3456,103.6902", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType(
                "Unusual handoff of item occurs at public bench with minimal interaction",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Briefcase handover", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType(
                "Unusual handoff of item occurs at public bench with minimal interaction",
                SchemaEventTypeNames.INCIDENT.key
            )
        )

        insertEventNodeIntoDb(
            "Small group appears to annotate or inspect public utility fixture",
            SchemaEventTypeNames.INCIDENT.key
        )
        insertEventNodeIntoDb("Small Group", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("1694685300000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3409,103.6885", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Use of chalk/paint marking", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Small Group", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType(
                "Small group appears to annotate or inspect public utility fixture",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694685300000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType(
                "Small group appears to annotate or inspect public utility fixture",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3409,103.6885", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType(
                "Small group appears to annotate or inspect public utility fixture",
                SchemaEventTypeNames.INCIDENT.key
            )
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Use of chalk/paint marking", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType(
                "Small group appears to annotate or inspect public utility fixture",
                SchemaEventTypeNames.INCIDENT.key
            )
        )


        /*-----------------------------------------
        |    FOR ROUTE INTEGRITY USE CASE         |
        -----------------------------------------*/
        // Incident 1: Bombing at Urban Supply Depot (near route index 21)
        insertEventNodeIntoDb("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Unknown Operative", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Disrupt supply lines", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1694589600000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3250,103.8098", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Explosive Charges", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Unknown Operative", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Disrupt supply lines", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694589600000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3250,103.8098", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Explosive Charges", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Bombing at Urban Supply Depot", SchemaEventTypeNames.INCIDENT.key)
        )

// Incident 2: Sniper Nest Detected on Ridge (near route index 37)
        insertEventNodeIntoDb("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Hostile Marksman", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Target high-ranking officer", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1694590320000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3010,103.7845", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Scoped Rifle from Elevated Cover", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Hostile Marksman", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Target high-ranking officer", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694590320000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3010,103.7845", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Scoped Rifle from Elevated Cover",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Sniper Nest Detected on Ridge", SchemaEventTypeNames.INCIDENT.key)
        )

// Incident 3: Sabotage at Communications Relay (near route index 43)
        insertEventNodeIntoDb("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Insider Threat", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Blind surveillance systems", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1694590800000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.2859,103.7298", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Signal Jammer Deployment", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Insider Threat", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Blind surveillance systems", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694590800000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.2859,103.7298", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Signal Jammer Deployment", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Sabotage at Communications Relay", SchemaEventTypeNames.INCIDENT.key)
        )

// Incident 4: Enemy Encampment Spotted in Jungle (near route index 44)
        insertEventNodeIntoDb("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Militant Group Foxtrot", SchemaEventTypeNames.WHO.key)
        insertEventNodeIntoDb("Staging ground for ambush", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1694591580000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.2948,103.0221", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Camouflaged Tent Setup", SchemaEventTypeNames.HOW.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Militant Group Foxtrot", SchemaEventTypeNames.WHO.key),
            getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Staging ground for ambush", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1694591580000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.2948,103.0221", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Camouflaged tent setup", SchemaEventTypeNames.HOW.key),
            getEventNodeByNameAndType("Enemy Encampment Spotted in Jungle", SchemaEventTypeNames.INCIDENT.key)
        )

        // Testing direction of airflow thing
        insertEventNodeIntoDb("Chemical release into air", SchemaEventTypeNames.INCIDENT.key)
        insertEventNodeIntoDb("Malicious intent", SchemaEventTypeNames.WHY.key)
        insertEventNodeIntoDb("1695507300000", SchemaEventTypeNames.WHEN.key)
        insertEventNodeIntoDb("1.3521,103.7927", SchemaEventTypeNames.WHERE.key)
        insertEventNodeIntoDb("Release of toxic materials from factory", SchemaEventTypeNames.HOW.key)
//        insertEventEdgeIntoDb(getEventNodeByNameAndType("Unknown Source", SchemaEventTypeNames.WHO.key), getEventNodeByNameAndType("Chemical release into air", com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames.INCIDENT.key))
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("Malicious intent", SchemaEventTypeNames.WHY.key),
            getEventNodeByNameAndType("Chemical release into air", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695507300000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("Chemical release into air", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1.3521,103.7927", SchemaEventTypeNames.WHERE.key),
            getEventNodeByNameAndType("Chemical release into air", SchemaEventTypeNames.INCIDENT.key)
        )
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType(
                "Release of toxic materials from factory",
                SchemaEventTypeNames.HOW.key
            ), getEventNodeByNameAndType("Chemical release into air", SchemaEventTypeNames.INCIDENT.key)
        )

        insertEventNodeIntoDb("SE", SchemaEventTypeNames.WIND.key)
        insertEventEdgeIntoDb(
            getEventNodeByNameAndType("1695507300000", SchemaEventTypeNames.WHEN.key),
            getEventNodeByNameAndType("SE", SchemaEventTypeNames.WIND.key)
        )

        Log.d("INITIALISE DATABASE", "Data initialised.")
    }
}