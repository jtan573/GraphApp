package com.example.graphapp.backend.services.di

import android.content.Context
import com.example.graphapp.backend.services.kgraph.GraphAccess
import com.example.graphapp.backend.services.kgraph.admin.AdminGraph
import com.example.graphapp.backend.services.kgraph.admin.AdminService
import com.example.graphapp.backend.services.kgraph.nlp.NlpService
import com.example.graphapp.backend.services.kgraph.query.QueryGraph
import com.example.graphapp.backend.services.kgraph.query.QueryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This module's dependencies live as long as the application
object AppModule {

    @Provides
    @Singleton
    fun provideAdminService(graphAccess: GraphAccess): AdminService {
        return AdminGraph(graphAccess)
    }

    @Provides
    @Singleton
    fun provideQueryService(graphAccess: GraphAccess): QueryService {
        return QueryGraph(graphAccess)
    }

//    @Provides
//    @Singleton
//    fun provideNlpService(@ApplicationContext context: Context): NlpService {
//        return NlpManager(context)
//    }
}