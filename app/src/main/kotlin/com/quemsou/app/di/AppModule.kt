package com.quemsou.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo Hilt raiz do app, com escopo de aplicação.
 * Vazio por enquanto — providers virão junto com as camadas de dados e domínio.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
