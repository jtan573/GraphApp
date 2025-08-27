package com.example.graphapp.data.db.queries

import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionEdgeEntity_
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.ActionNodeEntity_
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.db.UserNodeEntity_
import com.example.graphapp.data.db.VectorDatabase
import io.objectbox.kotlin.boxFor

class UserActionDatabaseQueries() {

    private val usersBox = VectorDatabase.store.boxFor(UserNodeEntity::class)
    private val actionsBox = VectorDatabase.store.boxFor(ActionNodeEntity::class)
    private val actionsEdgesBox = VectorDatabase.store.boxFor(ActionEdgeEntity::class)

    fun addUserNodeIntoDbQuery(
        identifier: String,
        role: String,
        specialisation: String,
        currentLocation: String,
        embedding: FloatArray,
        actionsTaken: MutableList<Long> = mutableListOf()
    ) {
        usersBox.put(
            UserNodeEntity(
                identifier = identifier,
                role = role,
                specialisation = specialisation,
                currentLocation = currentLocation,
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
        val (lastType, lastId) = findLastNodeInUserHistoryQuery(userIdentifier)

        actionsBox.put(
            ActionNodeEntity(actionName = actionName, timestamp = System.currentTimeMillis())
        )
        val newActionNode = findActionNodeByNameQuery(actionName)

        if (newActionNode != null) {
            updateUserNodeHistoryQuery(userIdentifier, newActionNode)
            addActionEdgeIntoDbQuery(lastId, lastType, newActionNode.id, "Action")
        }

        return
    }

    fun updateUserNodeHistoryQuery(userIdentifier: String, actionNode: ActionNodeEntity) {
        val userNode = findUserNodeByIdentifierQuery(userIdentifier)!!
        userNode.actionsTaken.add(actionNode.id)
        userNode.actions.add(actionNode)
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

    fun findActionEdgeQuery(fromId: Long, fromType:String, toId: Long, toType: String) : ActionEdgeEntity? {
        return actionsEdgesBox.query(
            (ActionEdgeEntity_.fromNodeId.equal(fromId).and(ActionEdgeEntity_.fromNodeType.equal(fromType)))
                .and(ActionEdgeEntity_.toNodeId.equal(toId).and(ActionEdgeEntity_.toNodeType.equal(toType)))
        ).build().findFirst()
    }

    fun findUserNodeByIdentifierQuery(identifier: String) : UserNodeEntity? {
        val nodeFound = usersBox
            .query(UserNodeEntity_.identifier.equal(identifier))
            .build()
            .findFirst()

        return nodeFound
    }

    fun findAllEdgesAroundNodeIdQuery(id: Long): List<ActionEdgeEntity> {
        return actionsEdgesBox.query(
            ActionEdgeEntity_.fromNodeId.equal(id).or(ActionEdgeEntity_.toNodeId.equal(id))
        ).build().find()
    }

    fun deleteNodesQuery(
        actionIdsToDelete: List<Long>? = null,
        userIdsToDelete: List<Long>? = null,
        actionEdgeIdsToDelete: List<Long>? = null
    ) {
        usersBox.removeByIds(userIdsToDelete)
        actionsBox.removeByIds(actionIdsToDelete)
        actionsEdgesBox.removeByIds(actionEdgeIdsToDelete)
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

    fun findActionNodeByNameQuery(actionName: String) : ActionNodeEntity? {
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
                    specialisation = node.specialisation,
                    currentLocation = node.currentLocation
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

    fun updateActionsListQuery(deletedActionId: Long, userNode: UserNodeEntity) {
        if (deletedActionId in userNode.actionsTaken) {
            userNode.actionsTaken.remove(deletedActionId)
            usersBox.put(userNode)
        }
    }

    fun resetPersonnelDbQuery() {
        usersBox.removeAll()
        actionsBox.removeAll()
        actionsEdgesBox.removeAll()
    }

}