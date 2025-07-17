package com.example.graphapp.data.db

import android.util.Log
import io.objectbox.kotlin.boxFor
import org.intellij.lang.annotations.Identifier

class UserActionDatabaseQueries() {

    private val usersBox = VectorDatabase.store.boxFor(UserNodeEntity::class)
    private val actionsBox = VectorDatabase.store.boxFor(ActionNodeEntity::class)
    private val actionsEdgesBox = VectorDatabase.store.boxFor(ActionEdgeEntity::class)

    fun addUserNodeIntoDbQuery(
        identifier: String,
        role: String,
        specialisation: String,
        embedding: FloatArray,
        actionsTaken: MutableList<Long> = mutableListOf()
    ) {
        usersBox.put(
            UserNodeEntity(
                identifier = identifier,
                role = role,
                specialisation = specialisation,
                embedding = embedding,
                actionsTaken = actionsTaken
            )
        )
        return
    }

    fun addActionNodeIntoDbQuery(
        userIdentifier: String,
        actionName: String
    ) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("GMT+08:00")
        val timestamp = sdf.format(java.util.Date())

        val (lastType, lastId) = findLastNodeInUserHistoryQuery(userIdentifier)

        actionsBox.put(
            ActionNodeEntity(actionName = actionName, timestamp = timestamp)
        )
        val newActionNode = findActionNodeByName(actionName)

        if (newActionNode != null) {
            updateUserNodeHistoryQuery(userIdentifier, newActionNode.id)
            addActionEdgeIntoDbQuery(lastId, lastType, newActionNode.id, "Action")
        }

        return
    }

    fun updateUserNodeHistoryQuery(userIdentifier: String, actionNodeId: Long) {
        val userNode = findUserNodeByIdentifierQuery(userIdentifier)!!
        userNode.actionsTaken.add(actionNodeId)
        usersBox.put(userNode)
    }

    fun findLastNodeInUserHistoryQuery(userIdentifier: String): Pair<String, Long> {
        val userNode = findUserNodeByIdentifierQuery(userIdentifier)!!
        if (userNode.actionsTaken.isNotEmpty()) {
            return "Action" to userNode.actionsTaken.last()
        }
        return "User" to userNode.id
    }

    fun addActionEdgeIntoDbQuery(fromId: Long, fromType: String, toId: Long, toType: String){
        actionsEdgesBox.put(
            ActionEdgeEntity(
                fromNodeId = fromId, fromNodeType = fromType, toNodeId = toId, toNodeType = toType
            )
        )
        return
    }

    fun findUserNodeByIdentifierQuery(identifier: String) : UserNodeEntity? {
        val nodeFound = usersBox
            .query(UserNodeEntity_.identifier.equal(identifier))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findUserNodeByIdQuery(inputId: Long) : UserNodeEntity? {
        val nodeFound = usersBox
            .query(UserNodeEntity_.id.equal(inputId))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findActionNodeByIdQuery(inputId: Long) : ActionNodeEntity? {
        val nodeFound = actionsBox
            .query(ActionNodeEntity_.id.equal(inputId))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findActionNodeByName(actionName: String) : ActionNodeEntity? {
        val nodeFound = actionsBox
            .query(ActionNodeEntity_.actionName.equal(actionName))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findAllUserNodesQuery() : List<UserNodeEntity> {
        return usersBox.query().build().find()
    }

    fun findAllActionNodesQuery() : List<ActionNodeEntity> {
        return actionsBox.query().build().find()
    }

    fun findAllActionEdgesQuery() : List<ActionEdgeEntity> {
        return actionsEdgesBox.query().build().find()
    }

    fun findAllUserNodesWithoutEmbeddingQuery() : List<UserNodeEntity> {
        return usersBox.query().build().find()
            .map { node ->
                UserNodeEntity(
                    id = node.id,
                    identifier = node.identifier,
                    role = node.role,
                    specialisation = node.specialisation
                )
            }
    }

    fun findAllActionNodesWithoutEmbeddingQuery() : List<ActionNodeEntity> {
        return actionsBox.query().build().find()
            .map { node ->
                ActionNodeEntity(
                    id = node.id,
                    actionName = node.actionName,
                    timestamp = node.timestamp
                )
            }
    }

}