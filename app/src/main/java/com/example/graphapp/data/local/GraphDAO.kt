package com.example.graphapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphDAO {
    @Query("SELECT * FROM nodes")
    suspend fun getAllNodes(): List<Node>

    @Query("SELECT * FROM edges")
    suspend fun getAllEdges(): List<Edge>

    @Query("""
        SELECT n1.name || ' --[' || e.relationType || ']--> ' || n2.name AS relation
        FROM edges e
        JOIN nodes n1 ON e.fromNode = n1.id
        JOIN nodes n2 ON e.toNode = n2.id
    """)
    fun getRelations(): Flow<List<String>>
}