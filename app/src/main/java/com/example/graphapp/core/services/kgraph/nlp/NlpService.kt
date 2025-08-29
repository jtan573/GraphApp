package com.example.graphapp.core.services.kgraph.nlp

interface NlpService {
    fun runQuery(query: String): Map<String, Float>
    fun close()
}