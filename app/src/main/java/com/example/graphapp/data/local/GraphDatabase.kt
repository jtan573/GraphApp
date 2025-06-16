package com.example.graphapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import kotlin.jvm.java

@Database(entities = [Node::class, Edge::class], version = 1)
abstract class GraphDatabase : RoomDatabase(){
    abstract fun graphDao(): GraphDAO

    companion object {
        @Volatile
        private var Instance: GraphDatabase? = null

        fun getDatabase(context: Context): GraphDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, GraphDatabase::class.java, "graph_database")
                    .createFromAsset("testdb.db")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
