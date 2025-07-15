package com.example.graphapp.data

import android.content.Context
import android.util.Log
import com.example.graphapp.data.embedding.SentenceEmbedding
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.VectorDBQueries
import com.example.graphapp.data.schema.GraphSchema.SchemaEdgeLabels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class VectorRepository(private val context: Context) {

    private val sentenceEmbedding = SentenceEmbedding()
    private val queries = VectorDBQueries()

    suspend fun initializeEmbedding() = withContext(Dispatchers.IO) {

        // Copy model file to filesDir
        val modelFile = File(context.filesDir, "sentence_transformer.onnx")
        if (!modelFile.exists()) {
            context.assets.open("models/sentence_transformer.onnx").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val tokenizerBytes = context.assets.open("models/tokenizer.json").readBytes()

        sentenceEmbedding.init(
            modelFilepath = modelFile.absolutePath,
            tokenizerBytes = tokenizerBytes,
            useTokenTypeIds = true,
            outputTensorName = "sentence_embedding",
            useFP16 = false,
            useXNNPack = false,
            normalizeEmbeddings = true
        )
    }

    suspend fun getTextEmbeddings(inputString: String): FloatArray {
        return sentenceEmbedding.encode(inputString)
    }

    // Function to calculate cosine similarity between nodes
    fun cosineDistance(
        x1: FloatArray,
        x2: FloatArray
    ): Float {
        var mag1 = 0.0f
        var mag2 = 0.0f
        var product = 0.0f
        for (i in x1.indices) {
            mag1 += x1[i].pow(2)
            mag2 += x2[i].pow(2)
            product += x1[i] * x2[i]
        }
        mag1 = sqrt(mag1)
        mag2 = sqrt(mag2)
        return product / (mag1 * mag2)
    }

    // Function to add node
    suspend fun insertNodeIntoDb(
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
    fun insertEdgeIntoDB(
        fromNode: NodeEntity?,
        toNode: NodeEntity?
    ) {
        if (fromNode == null || toNode == null) {
            return
        } else {
            val edgeType = SchemaEdgeLabels["${fromNode.type}-${toNode.type}"]
            queries.addEdgeIntoDbQuery(fromNode.id, toNode.id, edgeType!!)
        }
    }

    // Get all nodes
    fun getAllNodes() : List<NodeEntity> {
        return queries.findAllNodesQuery()
    }

    fun getAllEdges() : List<EdgeEntity> {
        return queries.findAllEdgesQuery()
    }

    // Get node by Id
    fun getNodeById(inputId: Long) : NodeEntity? {
        val nodeFound = queries.findNodeByIdQuery(inputId)
        return nodeFound
    }

    // Get node by name and type
    fun getNodeByNameAndType(inputName: String, inputType: String) : NodeEntity? {
        val nodeFound = queries.findNodeByNameTypeQuery(inputName, inputType)
        return nodeFound
    }

    // Get all nodes without their embedding
    fun getAllNodesWithoutEmbedding() : List<NodeEntity> {
        return queries.findAllNodesWithoutEmbeddingQuery()
    }

    // Get all nodes frequencies
    fun getAllNodeFrequencies(): Map<Long, Int> {
        return queries.findAllNodeFrequenciesQuery()
    }

    // Get node frequencies
    fun getNodeFrequencyOfNodeId(id: Long): Int? {
        return queries.findNodeFrequencyOfNodeId(id)
    }

    fun findAllEdgesAroundNodeId(id: Long): List<EdgeEntity> {
        return queries.findAllEdgesAroundNodeIdQuery(id)
    }

    // Find edges
    fun getNeighborsOfNodeById(id: Long): List<NodeEntity> {
        val neighbourEdges = queries.findAllEdgesAroundNodeIdQuery(id)

        val neighbourNodes = mutableSetOf<NodeEntity>()

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

    fun getEdgeBetweenNodes(first: Long, second: Long): EdgeEntity {
        return queries.findEdgeBetweenNodeIdsQuery(first, second)!!
    }

    // Function to initialise repository
    suspend fun initialiseVectorRepository() {

        // --- Entities ---
        insertNodeIntoDb("Alpha Company", "Entity")
        insertNodeIntoDb("Bravo Company", "Entity")
        insertNodeIntoDb("Charlie Squadron", "Entity")
        insertNodeIntoDb("Delta Platoon", "Entity")
        insertNodeIntoDb("Echo Unit", "Entity")
        insertNodeIntoDb("Enemy Forces", "Entity")
        insertNodeIntoDb("Enemy Group", "Entity")
        insertNodeIntoDb("Enemy Anonymous", "Entity")
        insertNodeIntoDb("Joint Task Force Command", "Entity")
        insertNodeIntoDb("Insurgent Cell", "Entity")
        insertNodeIntoDb("Hostile Militia", "Entity")
        insertNodeIntoDb("Opposing Battalion", "Entity")
        insertNodeIntoDb("Rival Faction", "Entity")
        insertNodeIntoDb("Support Platoon", "Entity")
        insertNodeIntoDb("Engineering Corps", "Entity")
        insertNodeIntoDb("Medical Detachment", "Entity")
        insertNodeIntoDb("Logistics Division", "Entity")


// --- Locations ---
        insertNodeIntoDb("Sector Alpha", "Location")
        insertNodeIntoDb("Sector Bravo", "Location")
        insertNodeIntoDb("Sector Charlie", "Location")
        insertNodeIntoDb("Sector Delta", "Location")
        insertNodeIntoDb("Route Red", "Location")
        insertNodeIntoDb("Landing Zone Echo", "Location")
        insertNodeIntoDb("Village Delta", "Location")
        insertNodeIntoDb("Checkpoint Zulu", "Location")
        insertNodeIntoDb("Base Camp Foxtrot", "Location")

// --- Methods ---
        insertNodeIntoDb("Foot patrol with UAV support", "Method")
        insertNodeIntoDb("Armored convoy", "Method")
        insertNodeIntoDb("Helicopter insertion", "Method")
        insertNodeIntoDb("IED detonation", "Method")
        insertNodeIntoDb("Small arms engagement", "Method")
        insertNodeIntoDb("MEDEVAC extraction", "Method")
        insertNodeIntoDb("Casualty extraction", "Method")
        insertNodeIntoDb("Sniper overwatch", "Method")
        insertNodeIntoDb("Drone surveillance", "Method")
        insertNodeIntoDb("Night vision assault", "Method")
        insertNodeIntoDb("Night operations", "Method")
        insertNodeIntoDb("Precision drone strike", "Method")

// --- Motives ---
        insertNodeIntoDb("Gather enemy intel", "Motive")
        insertNodeIntoDb("Secure supply route", "Motive")
        insertNodeIntoDb("Disrupt enemy logistics", "Motive")
        insertNodeIntoDb("Respond to threat", "Motive")
        insertNodeIntoDb("Establish forward base", "Motive")
        insertNodeIntoDb("Retaliate against attack", "Motive")
        insertNodeIntoDb("Secure high-value target", "Motive")
        insertNodeIntoDb("Protect civilian population", "Motive")
        insertNodeIntoDb("Enemy information acquisition", "Motive")

// --- Dates (unique) ---
        insertNodeIntoDb("2023-09-15T06:00Z", "Date") //1
        insertNodeIntoDb("2023-09-15T08:30Z", "Date") //2
//        insertNodeIntoDb("2023-09-15T09:15Z", "Date") //3
        insertNodeIntoDb("2023-09-16T10:45Z", "Date") //4
        insertNodeIntoDb("2023-09-16T14:20Z", "Date") //5
        insertNodeIntoDb("2023-09-17T05:10Z", "Date") //6
        insertNodeIntoDb("2023-09-17T18:00Z", "Date") //7
        insertNodeIntoDb("2023-09-18T04:00Z", "Date") //8
        insertNodeIntoDb("2023-09-18T12:00Z", "Date") //9
        insertNodeIntoDb("2023-09-19T07:30Z", "Date") //10
        insertNodeIntoDb("2023-09-20T05:00Z", "Date") //11
        insertNodeIntoDb("2023-09-21T11:45Z", "Date") //12
        insertNodeIntoDb("2023-09-22T02:15Z", "Date") //13
        insertNodeIntoDb("2023-09-23T04:30Z", "Date") //14
        insertNodeIntoDb("2023-09-24T03:20Z", "Date") //15
        insertNodeIntoDb("2023-09-25T07:00Z", "Date") //16
        insertNodeIntoDb("2023-09-26T08:00Z", "Date") //17
        insertNodeIntoDb("2023-09-28T12:30Z", "Date") //19
        insertNodeIntoDb("2023-09-29T15:45Z", "Date") //20

        // Event Nodes
        insertNodeIntoDb("Reconnaissance Patrol", "Task")
        insertNodeIntoDb("Convoy Escort", "Task")
        insertNodeIntoDb("Forward Observation", "Task")
        insertNodeIntoDb("Resupply Mission", "Task")
        insertNodeIntoDb("Quick Reaction Deployment", "Task")
        insertNodeIntoDb("Area Surveillance Operation", "Task")
        insertNodeIntoDb("Supply Convoy Security", "Task")

        insertNodeIntoDb("Ambush", "Incident")
        insertNodeIntoDb("Roadside Bombing", "Incident")
        insertNodeIntoDb("Sniper Attack", "Incident")
        insertNodeIntoDb("Vehicle Breakdown", "Incident")
        insertNodeIntoDb("Airstrike Misfire", "Incident")
        insertNodeIntoDb("Surprise Attack", "Incident")
        insertNodeIntoDb("Improvised Explosive Strike", "Incident")

        insertNodeIntoDb("Extraction Completed", "Outcome")
        insertNodeIntoDb("Objective Secured", "Outcome")
        insertNodeIntoDb("Casualty Evacuation", "Outcome")
        insertNodeIntoDb("Mission Delay", "Outcome")
        insertNodeIntoDb("Equipment Loss", "Outcome")
        insertNodeIntoDb("Evacuation Finalized", "Outcome")
        insertNodeIntoDb("Target Area Secured", "Outcome")

        insertNodeIntoDb("Operational Delay", "Impact")
        insertNodeIntoDb("Intel Gap Created", "Impact")
        insertNodeIntoDb("Resource Shortage", "Impact")
        insertNodeIntoDb("Increased Hostilities", "Impact")
        insertNodeIntoDb("Strategic Advantage Lost", "Impact")
        insertNodeIntoDb("Mission Timeline Extended", "Impact")

// --- Tasks (unique properties per Task) ---
        insertEdgeIntoDB(getNodeByNameAndType("Alpha Company", "Entity"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Gather enemy intel", "Motive"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-15T06:00Z", "Date"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Alpha", "Location"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Foot patrol with UAV support", "Method"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))

        insertNodeIntoDb("2023-09-15T07:00Z", "Date")
        insertEdgeIntoDB(getNodeByNameAndType("Alpha Company", "Entity"), getNodeByNameAndType("Area Surveillance Operation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Enemy information acquisition", "Motive"), getNodeByNameAndType("Area Surveillance Operation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-15T07:00Z", "Date"), getNodeByNameAndType("Area Surveillance Operation", "Task"))
//        insertEdgeIntoDB(getNodeByNameAndType("Sector Alpha", "Location"), getNodeByNameAndType("Area Surveillance Operation", "Task"))
//        insertEdgeIntoDB(getNodeByNameAndType("Foot patrol", "Method"), getNodeByNameAndType("Area Surveillance Operation", "Task"))


        insertEdgeIntoDB(getNodeByNameAndType("Bravo Company", "Entity"), getNodeByNameAndType("Convoy Escort", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Secure supply route", "Motive"), getNodeByNameAndType("Convoy Escort", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-16T10:45Z", "Date"), getNodeByNameAndType("Convoy Escort", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Checkpoint Zulu", "Location"), getNodeByNameAndType("Convoy Escort", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Armored convoy", "Method"), getNodeByNameAndType("Convoy Escort", "Task"))

        insertNodeIntoDb("Defend transport route", "Motive")
        insertNodeIntoDb("Protected convoy", "Method")
        insertEdgeIntoDB(getNodeByNameAndType("Bravo Company", "Entity"), getNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Defend transport route", "Motive"), getNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-16T10:45Z", "Date"), getNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Checkpoint Zulu", "Location"), getNodeByNameAndType("Supply Convoy Security", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Protected convoy", "Method"), getNodeByNameAndType("Supply Convoy Security", "Task"))


        insertEdgeIntoDB(getNodeByNameAndType("Charlie Squadron", "Entity"), getNodeByNameAndType("Forward Observation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Secure high-value target", "Motive"), getNodeByNameAndType("Forward Observation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-17T05:10Z", "Date"), getNodeByNameAndType("Forward Observation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Charlie", "Location"), getNodeByNameAndType("Forward Observation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Helicopter insertion", "Method"), getNodeByNameAndType("Forward Observation", "Task"))

        insertEdgeIntoDB(getNodeByNameAndType("Delta Platoon", "Entity"), getNodeByNameAndType("Resupply Mission", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Protect civilian population", "Motive"), getNodeByNameAndType("Resupply Mission", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-20T05:00Z", "Date"), getNodeByNameAndType("Resupply Mission", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Village Delta", "Location"), getNodeByNameAndType("Resupply Mission", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Armored convoy", "Method"), getNodeByNameAndType("Resupply Mission", "Task"))

        insertEdgeIntoDB(getNodeByNameAndType("Echo Unit", "Entity"), getNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Retaliate against attack", "Motive"), getNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-21T11:45Z", "Date"), getNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Base Camp Foxtrot", "Location"), getNodeByNameAndType("Quick Reaction Deployment", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Night vision assault", "Method"), getNodeByNameAndType("Quick Reaction Deployment", "Task"))


// --- Incidents (unique Dates, varied properties) ---
        insertEdgeIntoDB(getNodeByNameAndType("Enemy Forces", "Entity"), getNodeByNameAndType("Ambush", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Retaliate against attack", "Motive"), getNodeByNameAndType("Ambush", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-15T08:30Z", "Date"), getNodeByNameAndType("Ambush", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Bravo", "Location"), getNodeByNameAndType("Ambush", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("IED detonation", "Method"), getNodeByNameAndType("Ambush", "Incident"))

        insertNodeIntoDb("Enemies", "Entity")
        insertNodeIntoDb("Counterattack", "Motive")
        insertNodeIntoDb("2023-09-15T10:50Z", "Date")
        insertEdgeIntoDB(getNodeByNameAndType("Enemies", "Entity"), getNodeByNameAndType("Surprise Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Counterattack", "Motive"), getNodeByNameAndType("Surprise Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-15T10:50Z", "Date"), getNodeByNameAndType("Surprise Attack", "Incident"))
//        insertEdgeIntoDB(getNodeByNameAndType("Sector Bravo", "Location"), getNodeByNameAndType("Surprise Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("IED detonation", "Method"), getNodeByNameAndType("Surprise Attack", "Incident"))


        insertEdgeIntoDB(getNodeByNameAndType("Enemy Group", "Entity"), getNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Disrupt enemy logistics", "Motive"), getNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-16T14:20Z", "Date"), getNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Checkpoint Zulu", "Location"), getNodeByNameAndType("Roadside Bombing", "Incident"))
//        insertEdgeIntoDB(getNodeByNameAndType("IED explosion", "Method"), getNodeByNameAndType("Roadside Bombing", "Incident"))

        insertNodeIntoDb("Interfere with enemy supply lines", "Motive")
        insertNodeIntoDb("Explosion of IED", "Method")
        insertNodeIntoDb("2023-09-16T15:42Z", "Date")
//        insertEdgeIntoDB(getNodeByNameAndType("Enemy Group", "Entity"), getNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Interfere with enemy supply lines", "Motive"), getNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-16T15:42Z", "Date"), getNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Checkpoint Zulu", "Location"), getNodeByNameAndType("Improvised Explosive Strike", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Explosion of IED", "Method"), getNodeByNameAndType("Improvised Explosive Strike", "Incident"))


        insertEdgeIntoDB(getNodeByNameAndType("Enemy Anonymous", "Entity"), getNodeByNameAndType("Sniper Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Secure high-value target", "Motive"), getNodeByNameAndType("Sniper Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-17T18:00Z", "Date"), getNodeByNameAndType("Sniper Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Charlie", "Location"), getNodeByNameAndType("Sniper Attack", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Sniper overwatch", "Method"), getNodeByNameAndType("Sniper Attack", "Incident"))

        insertEdgeIntoDB(getNodeByNameAndType("Hostile Militia", "Entity"), getNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Secure supply route", "Motive"), getNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-18T04:00Z", "Date"), getNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Route Red", "Location"), getNodeByNameAndType("Vehicle Breakdown", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Armored convoy", "Method"), getNodeByNameAndType("Vehicle Breakdown", "Incident"))

        insertEdgeIntoDB(getNodeByNameAndType("Rival Faction", "Entity"), getNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Protect civilian population", "Motive"), getNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-22T02:15Z", "Date"), getNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Village Delta", "Location"), getNodeByNameAndType("Airstrike Misfire", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Precision drone strike", "Method"), getNodeByNameAndType("Airstrike Misfire", "Incident"))


        // --- Outcomes (unique Dates, varied properties) ---
//        insertEdgeIntoDB(getNodeByNameAndType("Echo Unit", "Entity"), getNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Protect civilian population", "Motive"), getNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-23T04:30Z", "Date"), getNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Landing Zone Echo", "Location"), getNodeByNameAndType("Extraction Completed", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("MEDEVAC extraction", "Method"), getNodeByNameAndType("Extraction Completed", "Outcome"))

        insertNodeIntoDb("Guard general public", "Motive")
        insertNodeIntoDb("2023-09-23T04:30Z", "Date")
        insertEdgeIntoDB(getNodeByNameAndType("Echo Unit", "Entity"), getNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Guard general public", "Motive"), getNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-23T04:30Z", "Date"), getNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Landing Zone Echo", "Location"), getNodeByNameAndType("Evacuation Finalized", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("MEDEVAC extraction", "Method"), getNodeByNameAndType("Evacuation Finalized", "Outcome"))


        insertEdgeIntoDB(getNodeByNameAndType("Bravo Company", "Entity"), getNodeByNameAndType("Objective Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Establish forward base", "Motive"), getNodeByNameAndType("Objective Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-24T03:20Z", "Date"), getNodeByNameAndType("Objective Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Base Camp Foxtrot", "Location"), getNodeByNameAndType("Objective Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Night vision assault", "Method"), getNodeByNameAndType("Objective Secured", "Outcome"))

        insertNodeIntoDb("Create advanced base", "Motive")
        insertNodeIntoDb("2023-09-28T08:20Z", "Date")
        insertNodeIntoDb("Nighttime offensive", "Method")
        insertEdgeIntoDB(getNodeByNameAndType("Bravo Company", "Entity"), getNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Create advanced base", "Motive"), getNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-28T08:20Z", "Date"), getNodeByNameAndType("Target Area Secured", "Outcome"))
//        insertEdgeIntoDB(getNodeByNameAndType("Base Camp Foxtrot", "Location"), getNodeByNameAndType("Target Area Secured", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Nighttime offensive", "Method"), getNodeByNameAndType("Target Area Secured", "Outcome"))


        insertEdgeIntoDB(getNodeByNameAndType("Medical Detachment", "Entity"), getNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Respond to threat", "Motive"), getNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-25T07:00Z", "Date"), getNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Landing Zone Echo", "Location"), getNodeByNameAndType("Casualty Evacuation", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Casualty extraction", "Method"), getNodeByNameAndType("Casualty Evacuation", "Outcome"))

        insertEdgeIntoDB(getNodeByNameAndType("Delta Platoon", "Entity"), getNodeByNameAndType("Mission Delay", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Disrupt enemy logistics", "Motive"), getNodeByNameAndType("Mission Delay", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-26T08:00Z", "Date"), getNodeByNameAndType("Mission Delay", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Charlie", "Location"), getNodeByNameAndType("Mission Delay", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Drone surveillance", "Method"), getNodeByNameAndType("Mission Delay", "Outcome"))

        insertEdgeIntoDB(getNodeByNameAndType("Support Platoon", "Entity"), getNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Retaliate against attack", "Motive"), getNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-27T09:15Z", "Date"), getNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Route Red", "Location"), getNodeByNameAndType("Equipment Loss", "Outcome"))
        insertEdgeIntoDB(getNodeByNameAndType("Small arms engagement", "Method"), getNodeByNameAndType("Equipment Loss", "Outcome"))

// --- Impacts (unique Dates, varied properties) ---
        insertEdgeIntoDB(getNodeByNameAndType("Joint Task Force Command", "Entity"), getNodeByNameAndType("Operational Delay", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Gather enemy intel", "Motive"), getNodeByNameAndType("Operational Delay", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-28T12:30Z", "Date"), getNodeByNameAndType("Operational Delay", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Delta", "Location"), getNodeByNameAndType("Operational Delay", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Drone surveillance", "Method"), getNodeByNameAndType("Operational Delay", "Impact"))

        insertNodeIntoDb("Gather enemy information", "Motive")
        insertNodeIntoDb("Drone monitoring", "Method")
        insertEdgeIntoDB(getNodeByNameAndType("Joint Task Force Command", "Entity"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Gather enemy information", "Motive"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-28T12:30Z", "Date"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))
//        insertEdgeIntoDB(getNodeByNameAndType("Sector Delta", "Location"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Drone monitoring", "Method"), getNodeByNameAndType("Mission Timeline Extended", "Impact"))

        insertEdgeIntoDB(getNodeByNameAndType("Engineering Corps", "Entity"), getNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Secure high-value target", "Motive"), getNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-29T15:45Z", "Date"), getNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Base Camp Foxtrot", "Location"), getNodeByNameAndType("Intel Gap Created", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Precision drone strike", "Method"), getNodeByNameAndType("Intel Gap Created", "Impact"))

        insertEdgeIntoDB(getNodeByNameAndType("Support Platoon", "Entity"), getNodeByNameAndType("Resource Shortage", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Disrupt enemy logistics", "Motive"), getNodeByNameAndType("Resource Shortage", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-19T07:30Z", "Date"), getNodeByNameAndType("Resource Shortage", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Village Delta", "Location"), getNodeByNameAndType("Resource Shortage", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Armored convoy", "Method"), getNodeByNameAndType("Resource Shortage", "Impact"))

        insertEdgeIntoDB(getNodeByNameAndType("Enemy Anonymous", "Entity"), getNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Retaliate against attack", "Motive"), getNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-18T12:00Z", "Date"), getNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Checkpoint Zulu", "Location"), getNodeByNameAndType("Increased Hostilities", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Small arms engagement", "Method"), getNodeByNameAndType("Increased Hostilities", "Impact"))

        insertEdgeIntoDB(getNodeByNameAndType("Joint Task Force Command", "Entity"), getNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Establish forward base", "Motive"), getNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("2023-09-17T05:10Z", "Date"), getNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Sector Charlie", "Location"), getNodeByNameAndType("Strategic Advantage Lost", "Impact"))
        insertEdgeIntoDB(getNodeByNameAndType("Night operations", "Method"), getNodeByNameAndType("Strategic Advantage Lost", "Impact"))

        // --- Edges between Events ---
        insertEdgeIntoDB(getNodeByNameAndType("Ambush", "Incident"), getNodeByNameAndType("Reconnaissance Patrol", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Roadside Bombing", "Incident"), getNodeByNameAndType("Convoy Escort", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Sniper Attack", "Incident"), getNodeByNameAndType("Forward Observation", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Vehicle Breakdown", "Incident"), getNodeByNameAndType("Resupply Mission", "Task"))

        insertEdgeIntoDB(getNodeByNameAndType("Reconnaissance Patrol", "Outcome"), getNodeByNameAndType("Extraction Completed", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Convoy Escort", "Outcome"), getNodeByNameAndType("Objective Secured", "Task"))
        insertEdgeIntoDB(getNodeByNameAndType("Forward Observation", "Outcome"), getNodeByNameAndType("Casualty Evacuation", "Task"))

        insertEdgeIntoDB(getNodeByNameAndType("Operational Delay", "Impact"), getNodeByNameAndType("Ambush", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Resource Shortage", "Impact"), getNodeByNameAndType("Roadside Bombing", "Incident"))
        insertEdgeIntoDB(getNodeByNameAndType("Increased Hostilities", "Impact"), getNodeByNameAndType("Sniper Attack", "Incident"))

    Log.d("INITIALISE DATABASE", "Data initialised.")
}


}