package com.example.graphapp.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.graphdb.GraphDatabase

fun createDriver(context: Context): SqlDriver {
    val dbName = "graph_database.db"
    val dbPath = context.getDatabasePath(dbName)

    // Delete the database file on every app start
    if (dbPath.exists()) {
        dbPath.delete()
    }

    return AndroidSqliteDriver(GraphDatabase.Schema, context, dbName)
}