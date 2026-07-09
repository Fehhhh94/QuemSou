package com.quemsou.app.navigation

import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.RegrasPartida
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * As 3 rotas do app. As fases internas do jogo (grid, dica revelada, anúncio…)
 * **não são rotas** — são estados da rota [PartidaRoute], dirigidos pelo
 * `PartidaViewModel`.
 */

/** Tela inicial: criar partida ou entrar com código. */
@Serializable
object HomeRoute

/** Configuração da partida (categoria, jogadores, grupos, regras). */
@Serializable
object SetupRoute

/**
 * A partida inteira, do primeiro turno ao placar final.
 *
 * Carrega apenas a [ConfiguracaoDaPartida] serializada em JSON — o mínimo
 * reproduzível para recriar a partida (nunca objetos de domínio inteiros).
 * Um único argumento string evita `NavType` customizado para listas/objetos
 * aninhados e mantém o `SavedStateHandle` reconstruível em testes JVM puros.
 */
@Serializable
data class PartidaRoute(val configuracao: String)

/**
 * Configuração mínima para recriar uma partida em qualquer aparelho: o baralho
 * e os grids de dicas derivam da seed do [codigo] (determinismo da Fase 1),
 * então nada além disto precisa viajar pela navegação.
 *
 * @property codigo código da partida; a seed deriva dele.
 * @property categoria filtro do baralho ([CardCategory.LIVRE] = todas).
 * @property numeroDeRodadas total de rodadas.
 * @property leitorPontua se o leitor ganha 1 ponto por dica revelada sem acerto.
 * @property modoShot se algumas posições do grid pedem um shot antes de
 *   revelar a dica (Modo Shot). Default `false` mantém JSONs antigos decodificáveis.
 * @property quantidadeDeShots posições com shot por turno (1–3), usado só com
 *   [modoShot] ligado.
 * @property jogadores nomes (e agrupamento) na ordem de assento.
 */
@Serializable
data class ConfiguracaoDaPartida(
    val codigo: String,
    val categoria: CardCategory,
    val numeroDeRodadas: Int,
    val leitorPontua: Boolean,
    val jogadores: List<JogadorConfigurado>,
    val modoShot: Boolean = false,
    val quantidadeDeShots: Int = RegrasPartida.QUANTIDADE_PADRAO_DE_SHOTS,
) {
    /** Serializa para o argumento JSON da [PartidaRoute]. */
    fun paraJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Reconstrói a configuração a partir do argumento JSON da rota. */
        fun deJson(conteudo: String): ConfiguracaoDaPartida =
            json.decodeFromString(serializer(), conteudo)
    }
}

/**
 * Um jogador como configurado no Setup.
 *
 * @property grupoId chave de agrupamento (especificação v4): jogadores com o
 *   mesmo `grupoId` jogam no mesmo grupo; `null` = grupo próprio de tamanho 1
 *   (o estado padrão — o antigo "individual").
 */
@Serializable
data class JogadorConfigurado(
    val nome: String,
    val grupoId: String? = null,
)
