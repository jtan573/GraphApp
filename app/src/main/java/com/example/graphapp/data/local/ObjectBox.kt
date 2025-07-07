package com.example.graphapp.data.local

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context)
            .build()
    }
}

@Entity
data class NodeEntity(
    @Id
    var id: Long = 0,
    var name: String? = null,
    var type: String? = null,
    var description: String? = null,
    var frequency: Int? = 0
)

@Entity
data class EdgeEntity(
    @Id
    var id: Long = 0,
    var fromId: Long? = null,
    var toId: Long? = null,
    var edgeType: String? = null
)