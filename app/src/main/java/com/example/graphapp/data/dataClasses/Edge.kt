package com.example.graphapp.data.dataClasses

import androidx.room.Relation

data class Edge(
    val id: Int,
    val fromNode: Int,
    val toNode: Int,
    val relation: String
)
