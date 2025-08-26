package com.example.graphapp.backend.services.kgraph

import com.example.graphapp.backend.services.kgraph.admin.AdminService
import com.example.graphapp.backend.services.kgraph.query.QueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewModelManager @Inject constructor(
    val queryService: QueryService,
    val adminService: AdminService
)