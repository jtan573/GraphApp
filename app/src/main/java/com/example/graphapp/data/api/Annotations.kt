package com.example.graphapp.data.api

import com.google.gson.Gson
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KProperty1

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtractContext(val action: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Extract(val action: String)

data class Task(
    @Extract("id") val identity: String,
    @Extract("how") val job: String
)

fun extractAnnotatedFields(obj: Any, method: KFunction<*>): Map<String, Any?> {
    val context = method.findAnnotation<ExtractContext>()?.action ?: return emptyMap()
    val kClass = obj::class

    return kClass.members
        .filterIsInstance<KProperty1<Any, *>>()
        .mapNotNull { prop ->
            val annotation = prop.findAnnotation<Extract>()
            annotation?.let { "${context}:${it.action}" to prop.get(obj) }
        }
        .toMap()
}

@ExtractContext("create")
fun createTask() {}

fun main() {
    val json = """{"identity": "task_001", "job": "Collect data"}"""
    val task = Gson().fromJson(json, Task::class.java)

    val extracted = extractAnnotatedFields(task, ::createTask)
    println(extracted)
}
