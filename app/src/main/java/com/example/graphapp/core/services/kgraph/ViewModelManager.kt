package com.example.graphapp.core.services.kgraph

import com.example.graphapp.core.services.kgraph.admin.AdminService
import com.example.graphapp.core.services.kgraph.query.QueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewModelManager @Inject constructor(
    val queryService: QueryService,
    val adminService: AdminService
)