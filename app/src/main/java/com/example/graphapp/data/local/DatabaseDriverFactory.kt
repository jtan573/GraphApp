package com.example.graphapp.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.graphdb.GraphDatabase

fun createDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(GraphDatabase.Schema, context, "graph_database.db")
}