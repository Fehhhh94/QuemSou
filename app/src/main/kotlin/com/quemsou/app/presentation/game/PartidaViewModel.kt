package com.quemsou.app.presentation.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.EstadoDoTurno
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.ModoDeJogo
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.Placar
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.model.Turno
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.domain.usecase.CriarPartida
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.navigation.PartidaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Um único ViewModel para a partida inteira (decisão da 3.2): traduz
 * [EstadoDoTurno]/[Placar] do domínio em [PartidaUiState] e repassa eventos —
 * **nunca duplica regra do domínio**.
 *
 * ## Eventos fora de fase
 * Evento que não corresponde à fase atual é **ignorado** (toques duplicados e
 * corridas de UI não podem derrubar o app). As exceções do domínio continuam
 * como rede de proteção contra bugs de programação: os guards daqui garantem
 * que só chamadas válidas cheguem ao domínio.
 *
 * ## Morte de processo (SavedStateHandle)
 * Persiste apenas o **mínimo não derivável**: rodada atual, placar acumulado,
 * fase da UI, posições reveladas na ordem e o acertador do anúncio. Todo o
 * resto é reconstruído por determinismo: o baralho e o grid de cada turno
 * derivam da seed do código ([ConfiguracaoDaPartida], que chega pelo argumento
 * da rota e já sobrevive no `SavedStateHandle`), e o rodízio de leitor e de
 * escolhedor deriva da rodada e das posições reveladas — reexecutar as
 * revelações na ordem salva devolve o turno exatamente ao mesmo estado.
 */
@HiltViewModel
class PartidaViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repositorioDeCards: RepositorioDeCards,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartidaUiState>(PartidaUiState.Carregando)

    /** Estado único que dirige toda a UI da partida. */
    val uiState: StateFlow<PartidaUiState> = _uiState.asStateFlow()

    private val _abandonoSolicitado = MutableStateFlow(false)

    /**
     * `true` quando o jogador pediu para abandonar a partida (botão voltar
     * interceptado). A UI de confirmação vem na 3.3; [continuarPartida]
     * cancela o pedido.
     */
    val abandonoSolicitado: StateFlow<Boolean> = _abandonoSolicitado.asStateFlow()

    private lateinit var partida: Partida
    private var turno: Turno? = null

    init {
        viewModelScope.launch { inicializar() }
    }

    // region Eventos

    /** Começa o turno da rodada atual (fase VezDeJogar → Grid). */
    fun iniciarTurno() {
        if (_uiState.value !is PartidaUiState.VezDeJogar) return
        turno = partida.iniciarTurno()
        savedStateHandle[CHAVE_POSICOES] = intArrayOf()
        mudarFase(FASE_GRID, estadoGrid())
    }

    /** O escolhedor da vez toca a [posicao] do grid (fase Grid → DicaRevelada). */
    fun revelarDica(posicao: Int) {
        val turnoAtual = turno ?: return
        if (_uiState.value !is PartidaUiState.Grid) return
        if (posicao !in 1..Card.QUANTIDADE_DE_DICAS) return
        if (posicao in turnoAtual.posicoesReveladas) return
        val avancado = turnoAtual.revelarDica(posicao)
        turno = avancado
        savedStateHandle[CHAVE_POSICOES] = avancado.posicoesReveladas.toIntArray()
        mudarFase(FASE_DICA_REVELADA, estadoDicaRevelada())
    }

    /**
     * Ninguém arriscou: volta ao grid com o próximo escolhedor — ou, se as 10
     * dicas já foram usadas, o card queima (fase DicaRevelada → Grid|Anuncio).
     */
    fun outraDica() {
        val turnoAtual = turno ?: return
        if (_uiState.value !is PartidaUiState.DicaRevelada) return
        val avancado = turnoAtual.outraDica()
        turno = avancado
        if (avancado.estado is EstadoDoTurno.TurnoEncerrado) {
            savedStateHandle[CHAVE_ACERTADOR] = null
            mudarFase(FASE_ANUNCIO, estadoAnuncio())
        } else {
            mudarFase(FASE_GRID, estadoGrid())
        }
    }

    /**
     * Alguém gritou a resposta: abre a escolha de quem acertou — ou, com um
     * único adivinhador, registra o acerto direto (fase DicaRevelada →
     * QuemAcertou|Anuncio).
     */
    fun abrirQuemAcertou() {
        val turnoAtual = turno ?: return
        if (_uiState.value !is PartidaUiState.DicaRevelada) return
        val unico = turnoAtual.adivinhadores.singleOrNull()
        if (unico != null) {
            registrarAcertoInterno(unico.id)
        } else {
            mudarFase(FASE_QUEM_ACERTOU, estadoQuemAcertou())
        }
    }

    /** Registra o acerto do adivinhador [jogadorId] (fase QuemAcertou → Anuncio). */
    fun registrarAcerto(jogadorId: String) {
        if (_uiState.value !is PartidaUiState.QuemAcertou) return
        registrarAcertoInterno(jogadorId)
    }

    /** Os adivinhadores desistem: card queimado (fases ativas do turno → Anuncio). */
    fun queimarCard() {
        val turnoAtual = turno ?: return
        val fase = _uiState.value
        val faseComTurnoAtivo = fase is PartidaUiState.Grid ||
            fase is PartidaUiState.DicaRevelada ||
            fase is PartidaUiState.QuemAcertou
        if (!faseComTurnoAtivo) return
        turno = turnoAtual.queimarCard()
        savedStateHandle[CHAVE_ACERTADOR] = null
        mudarFase(FASE_ANUNCIO, estadoAnuncio())
    }

    /**
     * Fecha o anúncio: aplica o placar e avança para a próxima rodada — ou
     * para o placar final se esta era a última (fase Anuncio →
     * VezDeJogar|PlacarFinal).
     */
    fun proximoTurno() {
        val turnoAtual = turno ?: return
        if (_uiState.value !is PartidaUiState.Anuncio) return
        partida = partida.encerrarTurno(turnoAtual)
        turno = null
        savedStateHandle[CHAVE_RODADA] = partida.rodadaAtual
        savedStateHandle[CHAVE_PLACAR] =
            partida.jogadores.map { partida.placar.pontosDe(it.id) }.toIntArray()
        savedStateHandle[CHAVE_POSICOES] = intArrayOf()
        savedStateHandle[CHAVE_ACERTADOR] = null
        if (partida.encerrada) {
            mudarFase(FASE_PLACAR_FINAL, estadoPlacarFinal())
        } else {
            mudarFase(FASE_VEZ_DE_JOGAR, estadoVezDeJogar())
        }
    }

    /** Botão voltar interceptado: pede para abandonar a partida. */
    fun abandonarPartida() {
        _abandonoSolicitado.value = true
    }

    /** Cancela o pedido de abandono e segue o jogo. */
    fun continuarPartida() {
        _abandonoSolicitado.value = false
    }

    // endregion

    // region Montagem e restauração

    private suspend fun inicializar() {
        val configuracao = ConfiguracaoDaPartida.deJson(
            requireNotNull(savedStateHandle.get<String>(ARGUMENTO_CONFIGURACAO)) {
                "Rota Partida sem o argumento de configuração."
            },
        )
        // Ids determinísticos por índice: a restauração pós-morte de processo
        // reconstrói os mesmos ids a partir da mesma configuração.
        val jogadores = configuracao.jogadores.mapIndexed { indice, jogador ->
            Jogador(id = "j${indice + 1}", nome = jogador.nome, timeId = jogador.timeId)
        }
        val cards = repositorioDeCards.buscarPorCategoria(configuracao.categoria)
        val base = CriarPartida.executar(
            codigo = configuracao.codigo,
            jogadores = jogadores,
            modoDeJogo = configuracao.modoDeJogo,
            regras = RegrasPartida(
                leitorPontua = configuracao.leitorPontua,
                numeroDeRodadas = configuracao.numeroDeRodadas,
            ),
            cardsDisponiveis = cards,
        )
        restaurar(base)
    }

    /**
     * Recoloca a partida no ponto salvo: aplica rodada/placar à partida base
     * e, se havia turno em andamento, o reconstrói pela seed reexecutando as
     * revelações na ordem salva (mesmo grid, mesmo escolhedor, mesma fase).
     */
    private fun restaurar(base: Partida) {
        val fase = savedStateHandle.get<String>(CHAVE_FASE) ?: FASE_VEZ_DE_JOGAR
        val rodada = savedStateHandle.get<Int>(CHAVE_RODADA) ?: 1
        val pontos = savedStateHandle.get<IntArray>(CHAVE_PLACAR)
        val posicoes = savedStateHandle.get<IntArray>(CHAVE_POSICOES) ?: intArrayOf()
        val acertadorId = savedStateHandle.get<String>(CHAVE_ACERTADOR)

        partida = base.copy(
            rodadaAtual = rodada,
            indiceDoLeitor = (rodada - 1) % base.jogadores.size,
            placar = pontos
                ?.let { salvo ->
                    Placar(base.jogadores.mapIndexed { i, j -> j.id to salvo[i] }.toMap())
                }
                ?: base.placar,
            encerrada = fase == FASE_PLACAR_FINAL,
        )

        when (fase) {
            FASE_VEZ_DE_JOGAR -> _uiState.value = estadoVezDeJogar()
            FASE_PLACAR_FINAL -> _uiState.value = estadoPlacarFinal()
            else -> {
                var restaurado = partida.iniciarTurno()
                posicoes.forEachIndexed { indice, posicao ->
                    restaurado = restaurado.revelarDica(posicao)
                    if (indice != posicoes.lastIndex) restaurado = restaurado.outraDica()
                }
                turno = when (fase) {
                    FASE_GRID -> if (posicoes.isEmpty()) restaurado else restaurado.outraDica()
                    FASE_ANUNCIO ->
                        if (acertadorId != null) restaurado.registrarAcerto(acertadorId)
                        else restaurado.queimarCard()

                    else -> restaurado // DICA_REVELADA e QUEM_ACERTOU param na dica revelada
                }
                _uiState.value = when (fase) {
                    FASE_GRID -> estadoGrid()
                    FASE_DICA_REVELADA -> estadoDicaRevelada()
                    FASE_QUEM_ACERTOU -> estadoQuemAcertou()
                    else -> estadoAnuncio()
                }
            }
        }
    }

    // endregion

    // region Tradução domínio → PartidaUiState

    private fun registrarAcertoInterno(jogadorId: String) {
        val turnoAtual = turno ?: return
        if (turnoAtual.adivinhadores.none { it.id == jogadorId }) return
        turno = turnoAtual.registrarAcerto(jogadorId)
        savedStateHandle[CHAVE_ACERTADOR] = jogadorId
        mudarFase(FASE_ANUNCIO, estadoAnuncio())
    }

    private fun mudarFase(fase: String, estado: PartidaUiState) {
        savedStateHandle[CHAVE_FASE] = fase
        _uiState.value = estado
    }

    private fun estadoVezDeJogar() = PartidaUiState.VezDeJogar(
        rodada = partida.rodadaAtual,
        totalDeRodadas = partida.totalDeRodadas,
        nomeDoLeitor = partida.leitorDaVez.nome,
        nomesDosAdivinhadores = partida.jogadores
            .filter { it.id != partida.leitorDaVez.id }
            .map { it.nome },
    )

    private fun estadoGrid(): PartidaUiState.Grid {
        val turnoAtual = checkNotNull(turno)
        return PartidaUiState.Grid(
            posicoesReveladas = turnoAtual.posicoesReveladas,
            nomeDoEscolhedor = turnoAtual.escolhedorDaVez.nome,
            respostaParaOLeitor = turnoAtual.card.answer,
            pontosEmJogo = Card.QUANTIDADE_DE_DICAS - turnoAtual.dicasUsadas,
        )
    }

    private fun estadoDicaRevelada(): PartidaUiState.DicaRevelada {
        val turnoAtual = checkNotNull(turno)
        val dicaAtual = turnoAtual.estado as EstadoDoTurno.DicaRevelada
        return PartidaUiState.DicaRevelada(
            posicao = dicaAtual.posicao,
            texto = turnoAtual.dicaNaPosicao(dicaAtual.posicao),
            valor = Card.QUANTIDADE_DE_DICAS + 1 - turnoAtual.dicasUsadas,
        )
    }

    private fun estadoQuemAcertou() = PartidaUiState.QuemAcertou(
        adivinhadores = checkNotNull(turno).adivinhadores.map { AdivinhadorUi(it.id, it.nome) },
    )

    private fun estadoAnuncio(): PartidaUiState.Anuncio =
        when (val fim = checkNotNull(turno).estado) {
            is EstadoDoTurno.TurnoEncerrado.Acerto -> PartidaUiState.Anuncio.Acerto(
                resposta = fim.resposta,
                dicasUsadas = fim.dicasUsadas,
                nomeDoAcertador = nomeDe(fim.acertadorId),
                pontosDoAcertador = fim.pontosAcertador,
                pontosDoLeitor = fim.pontosLeitor,
            )

            is EstadoDoTurno.TurnoEncerrado.Queimado -> PartidaUiState.Anuncio.Queimado(
                resposta = fim.resposta,
                dicasUsadas = fim.dicasUsadas,
            )

            else -> error("Anúncio sem turno encerrado (estado: $fim).")
        }

    private fun estadoPlacarFinal(): PartidaUiState.PlacarFinal {
        val vencedores = partida.vencedores()
        return PartidaUiState.PlacarFinal(
            ranking = partida.placar.ranking().map { (id, pontos) ->
                LinhaDoPlacar(nome = nomeDe(id), pontos = pontos)
            },
            vencedores = when (partida.modoDeJogo) {
                ModoDeJogo.INDIVIDUAL -> vencedores.map(::nomeDe)
                ModoDeJogo.TIMES -> vencedores
            },
            empate = vencedores.size > 1,
        )
    }

    private fun nomeDe(jogadorId: String): String =
        partida.jogadores.first { it.id == jogadorId }.nome

    // endregion

    private companion object {
        /** Nome do campo da [PartidaRoute] — a navegação tipada o grava no SavedStateHandle. */
        const val ARGUMENTO_CONFIGURACAO = "configuracao"

        const val CHAVE_FASE = "estado_fase"
        const val CHAVE_RODADA = "estado_rodada"
        const val CHAVE_PLACAR = "estado_placar"
        const val CHAVE_POSICOES = "estado_posicoes"
        const val CHAVE_ACERTADOR = "estado_acertador"

        const val FASE_VEZ_DE_JOGAR = "VEZ_DE_JOGAR"
        const val FASE_GRID = "GRID"
        const val FASE_DICA_REVELADA = "DICA_REVELADA"
        const val FASE_QUEM_ACERTOU = "QUEM_ACERTOU"
        const val FASE_ANUNCIO = "ANUNCIO"
        const val FASE_PLACAR_FINAL = "PLACAR_FINAL"
    }
}
