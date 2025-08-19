package com.example.graphapp.backend.services.kgraph.nlp

interface NlpService {
    fun runQuery(query: String): Map<String, Float>
    fun close()
}