package com.example.graphapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "edges",
    foreignKeys = [
        ForeignKey(
            entity = Node::class,
            parentColumns = ["id"],
            childColumns = ["fromNode"]
        ),
        ForeignKey(
            entity = Node::class,
            parentColumns = ["id"],
            childColumns = ["toNode"]
        )
    ]
)
data class Edge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromNode: Int,
    val toNode: Int,
    val relationType: String
)
