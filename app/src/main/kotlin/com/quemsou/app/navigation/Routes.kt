package com.quemsou.app.navigation

import kotlinx.serialization.Serializable

/** Tela inicial: criar partida ou entrar com código. */
@Serializable
object HomeRoute

/** Tela de configuração da partida (regras, jogadores, categorias). */
@Serializable
object SetupRoute

/** Tela principal do jogo, onde cada jogador vê seu card de dicas. */
@Serializable
object GameRoute

/** Tela de placar ao final da partida. */
@Serializable
object ScoreRoute
