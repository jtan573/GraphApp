package com.example.graphapp.data

import android.content.Context
import android.util.Log
import com.example.graphapp.data.embedding.SentenceEmbedding
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.NodeWithoutEmbedding
import com.example.graphapp.data.schema.GraphSchema.edgeLabels
import com.example.graphapp.data.local.VectorDBQueries
import com.example.graphdb.Edge
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
            val edgeType = edgeLabels["${fromNode.type}-${toNode.type}"]
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

    // Find edges
    fun getNeighborsOfNodeById(id: Long): List<NodeEntity> {
        val neighbourEdges = queries.findAllEdgesAroundNodeIdQuery(id)
        val neighbourNodes = mutableSetOf<NodeEntity>()

        for (edge in neighbourEdges) {
            val node = if (edge.fromId == id) {
                queries.findNodeByIdQuery(edge.toId)!!
            } else {
                queries.findNodeByIdQuery(edge.fromId)!!
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
        insertNodeIntoDb("Bombing", "Article")
        insertNodeIntoDb("Group Alpha", "Entity", "A leading organization characterized by strong internal cohesion.")
        insertNodeIntoDb("Explosives", "Method")
        insertNodeIntoDb("2022-06-12T04:23:11Z", "Date")
        insertNodeIntoDb("Market District", "Location")
        insertNodeIntoDb("Intimidation", "Motive")

        insertEdgeIntoDB(getNodeByNameAndType("Group Alpha", "Entity"), getNodeByNameAndType("Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Explosives", "Method"), getNodeByNameAndType("Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2022-06-12T04:23:11Z", "Date"), getNodeByNameAndType("Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Market District", "Location"), getNodeByNameAndType("Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Intimidation", "Motive"), getNodeByNameAndType("Bombing", "Article"))

        insertNodeIntoDb("Suicide Bombing", "Article")
        insertNodeIntoDb("Sect Zeta", "Entity", "A specialized faction known for its unique ideology and practices.")
        insertNodeIntoDb("Suicide Vest", "Method")
        insertNodeIntoDb("2021-11-03T14:10:45Z", "Date")
        insertNodeIntoDb("Train Station", "Location")
        insertNodeIntoDb("Religious Motivation", "Motive", "Based on ideological or faith-driven objectives.")

        insertEdgeIntoDB(getNodeByNameAndType("Sect Zeta", "Entity"), getNodeByNameAndType("Suicide Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Suicide Vest", "Method"), getNodeByNameAndType("Suicide Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2021-11-03T14:10:45Z", "Date"), getNodeByNameAndType("Suicide Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Train Station", "Location"), getNodeByNameAndType("Suicide Bombing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Religious Motivation", "Motive"), getNodeByNameAndType("Suicide Bombing", "Article"))

        insertNodeIntoDb("Knife Attack", "Article")
        insertNodeIntoDb("Individual Y", "Entity", "A key figure known for their independent actions.")
        insertNodeIntoDb("Knife", "Method")
        insertNodeIntoDb("2020-09-15T21:55:30Z", "Date")
        insertNodeIntoDb("Shopping Center", "Location")
        insertNodeIntoDb("Personal Grievance", "Motive", "Driven by individual resentment or perceived injustice.")

        insertEdgeIntoDB(getNodeByNameAndType("Individual Y", "Entity"), getNodeByNameAndType("Knife Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Knife", "Method"), getNodeByNameAndType("Knife Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2020-09-15T21:55:30Z", "Date"), getNodeByNameAndType("Knife Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Shopping Center", "Location"), getNodeByNameAndType("Knife Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Personal Grievance", "Motive"), getNodeByNameAndType("Knife Attack", "Article"))

        insertNodeIntoDb("Vehicle Attack", "Article")
        insertNodeIntoDb("Group Gamma", "Entity", "An organized collective recognized for coordinated initiatives.")
        insertNodeIntoDb("Truck", "Method")
        insertNodeIntoDb("2019-07-22T08:02:19Z", "Date")
        insertNodeIntoDb("City Square", "Location")
        insertNodeIntoDb("Maximize Casualties", "Motive", "Intending to cause the highest possible loss of life.")

        insertEdgeIntoDB(getNodeByNameAndType("Group Gamma", "Entity"), getNodeByNameAndType("Vehicle Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Truck", "Method"), getNodeByNameAndType("Vehicle Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2019-07-22T08:02:19Z", "Date"), getNodeByNameAndType("Vehicle Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("City Square", "Location"), getNodeByNameAndType("Vehicle Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Maximize Casualties", "Motive"), getNodeByNameAndType("Vehicle Attack", "Article"))

        insertNodeIntoDb("Arson", "Article")
        insertNodeIntoDb("Individual Z", "Entity", "Notable for their influential role within the community.")
        insertNodeIntoDb("Incendiary Device", "Method")
        insertNodeIntoDb("2020-02-11T19:45:00Z", "Date")
        insertNodeIntoDb("Warehouse District", "Location")
        insertNodeIntoDb("Economic Disruption", "Motive", "Aiming to acquire money or valuable assets.")

        insertEdgeIntoDB(getNodeByNameAndType("Individual Z", "Entity"), getNodeByNameAndType("Arson", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Incendiary Device", "Method"), getNodeByNameAndType("Arson", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2020-02-11T19:45:00Z", "Date"), getNodeByNameAndType("Arson", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Warehouse District", "Location"), getNodeByNameAndType("Arson", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Economic Disruption", "Motive"), getNodeByNameAndType("Arson", "Article"))

        insertNodeIntoDb("Cyber Attack", "Article")
        insertNodeIntoDb("Group Delta", "Entity", "A prominent group engaged in various collaborative projects.")
        insertNodeIntoDb("Malware", "Method")
        insertNodeIntoDb("2021-05-30T02:15:42Z", "Date")
        insertNodeIntoDb("Government Servers", "Location")
        insertNodeIntoDb("Data Theft", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        insertEdgeIntoDB(getNodeByNameAndType("Group Delta", "Entity"), getNodeByNameAndType("Cyber Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Malware", "Method"), getNodeByNameAndType("Cyber Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2021-05-30T02:15:42Z", "Date"), getNodeByNameAndType("Cyber Attack", "Article"))
//        insertEdgeIntoDB(getNodeByNameAndType("Government Servers", "Location"), getNodeByNameAndType("Cyber Attack", "Article"), "Where")
        insertEdgeIntoDB(getNodeByNameAndType("Data Theft", "Motive"), getNodeByNameAndType("Cyber Attack", "Article"))

        insertNodeIntoDb("Grenade Attack", "Article")
        insertNodeIntoDb("Group Epsilon", "Entity", "Recognized for its strategic influence and structured organization.")
        insertNodeIntoDb("Projectiles", "Method")
        insertNodeIntoDb("2019-10-05T13:38:27Z", "Date")
        insertNodeIntoDb("Police Station", "Location")
        insertNodeIntoDb("Weaken Law Enforcement", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        insertEdgeIntoDB(getNodeByNameAndType("Group Epsilon", "Entity"), getNodeByNameAndType("Grenade Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Projectiles", "Method"), getNodeByNameAndType("Grenade Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType( "2019-10-05T13:38:27Z", "Date"), getNodeByNameAndType("Grenade Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Police Station", "Location"), getNodeByNameAndType("Grenade Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Weaken Law Enforcement", "Motive"), getNodeByNameAndType("Grenade Attack", "Article"))

        insertNodeIntoDb("Bomb Threat", "Article")
        insertNodeIntoDb("Individual Q", "Entity", "Has a reputation for decisive leadership and personal achievements.")
        insertNodeIntoDb("Phone Call", "Method")
        insertNodeIntoDb("2020-12-01T23:59:59Z", "Date")
        insertNodeIntoDb("School Building", "Location")
        insertNodeIntoDb("Evacuation", "Motive", "Focusing on the unauthorized acquisition of sensitive information.")

        insertEdgeIntoDB(getNodeByNameAndType("Individual Q", "Entity"), getNodeByNameAndType("Bomb Threat", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Phone Call", "Method"), getNodeByNameAndType("Bomb Threat", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2020-12-01T23:59:59Z", "Date"), getNodeByNameAndType("Bomb Threat", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("School Building", "Location"), getNodeByNameAndType("Bomb Threat", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Evacuation", "Motive"), getNodeByNameAndType("Bomb Threat", "Article"))

        insertNodeIntoDb("Explosion", "Article")
        insertNodeIntoDb("2021-08-08T06:06:06Z", "Date")

        insertEdgeIntoDB(getNodeByNameAndType("Explosives", "Method"), getNodeByNameAndType("Explosion", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2021-08-08T06:06:06Z", "Date"), getNodeByNameAndType("Explosion", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Market District", "Location"), getNodeByNameAndType("Explosion", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Intimidation", "Motive"), getNodeByNameAndType("Explosion", "Article"))

        insertNodeIntoDb("Cyber Breach", "Article")
        insertNodeIntoDb("2022-03-15T15:45:15Z", "Date")

        insertEdgeIntoDB(getNodeByNameAndType("Group Delta", "Entity"), getNodeByNameAndType("Cyber Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Malware", "Method"), getNodeByNameAndType("Cyber Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2022-03-15T15:45:15Z", "Date"), getNodeByNameAndType("Cyber Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Government Servers", "Location"), getNodeByNameAndType("Cyber Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Data Theft", "Motive"), getNodeByNameAndType("Cyber Breach", "Article"))

        insertNodeIntoDb("Truck Ramming", "Article")
        insertNodeIntoDb("2019-12-25T11:22:33Z", "Date")

        insertEdgeIntoDB(getNodeByNameAndType("Group Gamma", "Entity"), getNodeByNameAndType("Truck Ramming", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Truck", "Method"), getNodeByNameAndType("Truck Ramming", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2019-12-25T11:22:33Z", "Date"), getNodeByNameAndType("Truck Ramming", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("City Square", "Location"), getNodeByNameAndType("Truck Ramming", "Article"))

        insertNodeIntoDb("Stabbing", "Article")
        insertNodeIntoDb("Individual V", "Entity", "Distinguished by their contributions and distinctive perspective.")
        insertNodeIntoDb("2022-02-20T05:05:05Z", "Date")
        insertNodeIntoDb("Random Violence", "Motive", "Involving unpredictable attacks without specific targets.")

        insertEdgeIntoDB(getNodeByNameAndType("Individual V", "Entity"), getNodeByNameAndType("Stabbing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Knife", "Method"), getNodeByNameAndType("Stabbing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2022-02-20T05:05:05Z", "Date"), getNodeByNameAndType("Stabbing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Train Station", "Location"), getNodeByNameAndType("Stabbing", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Random Violence", "Motive"), getNodeByNameAndType("Stabbing", "Article"))

        insertNodeIntoDb("Arson Attack", "Article")
        insertNodeIntoDb("2021-09-09T17:30:00Z", "Date")

        insertEdgeIntoDB(getNodeByNameAndType("Incendiary Device", "Method"), getNodeByNameAndType("Arson Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2021-09-09T17:30:00Z", "Date"), getNodeByNameAndType("Arson Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Warehouse District", "Location"), getNodeByNameAndType("Arson Attack", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Economic Disruption", "Motive"), getNodeByNameAndType("Arson Attack", "Article"))

        insertNodeIntoDb("Cybersecurity Breach", "Article")
        insertNodeIntoDb("Group Theta", "Entity", "Operates as a cohesive unit with significant collective impact.")
        insertNodeIntoDb("Ransomware", "Method")
        insertNodeIntoDb("2022-05-05T03:14:15Z", "Date")
        insertNodeIntoDb("Hospital Network", "Location")
        insertNodeIntoDb("Financial Gain", "Motive", "Intending to destabilize financial systems or markets.")

        insertEdgeIntoDB(getNodeByNameAndType("Group Theta", "Entity"), getNodeByNameAndType("Cybersecurity Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Ransomware", "Method"), getNodeByNameAndType("Cybersecurity Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2022-05-05T03:14:15Z", "Date"), getNodeByNameAndType("Cybersecurity Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Hospital Network", "Location"), getNodeByNameAndType("Cybersecurity Breach", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Financial Gain", "Motive"), getNodeByNameAndType("Cybersecurity Breach", "Article"))

        insertNodeIntoDb("Grenade Explosion", "Article")
        insertNodeIntoDb("2020-03-03T22:00:00Z", "Date")

        insertEdgeIntoDB(getNodeByNameAndType("Group Epsilon", "Entity"), getNodeByNameAndType("Grenade Explosion", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("Grenades", "Method"), getNodeByNameAndType("Grenade Explosion", "Article"))
        insertEdgeIntoDB(getNodeByNameAndType("2020-03-03T22:00:00Z", "Date"), getNodeByNameAndType("Grenade Explosion", "Article"))

        insertNodeIntoDb("Difficult to trace origin", "Description")
        insertEdgeIntoDB(getNodeByNameAndType("Difficult to trace origin", "Description"), getNodeByNameAndType("Malware", "Method"))
        insertEdgeIntoDB(getNodeByNameAndType("Difficult to trace origin", "Description"), getNodeByNameAndType("Ransomware", "Method"))

        insertNodeIntoDb("Instil Fear", "Description")
        insertEdgeIntoDB(getNodeByNameAndType("Instil Fear", "Description"), getNodeByNameAndType("Intimidation", "Motive"))
        insertEdgeIntoDB(getNodeByNameAndType("Instil Fear", "Description"), getNodeByNameAndType("Evacuation", "Motive"))

    }


}