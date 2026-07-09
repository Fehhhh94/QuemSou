package com.quemsou.app.presentation.setup

import androidx.lifecycle.ViewModel
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.ModoDeJogo
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.navigation.JogadorConfigurado
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado da tela de configuração da partida, com validação viva:
 * [podeComecar] e [motivoDoBloqueio] são recalculados a cada mudança.
 */
data class SetupUiState(
    val categoria: CardCategory = CardCategory.LIVRE,
    val modoDeJogo: ModoDeJogo = ModoDeJogo.INDIVIDUAL,
    val jogadores: List<JogadorEmEdicao> = List(Partida.MINIMO_DE_JOGADORES) { JogadorEmEdicao() },
    val numeroDeRodadas: Int = 5,
    val leitorPontua: Boolean = true,
    val jogadoresTocados: Set<Int> = emptySet(),
    val tentouComecar: Boolean = false,
) {
    /** Primeiro motivo que impede a partida de começar; `null` se está tudo certo. */
    val motivoDoBloqueio: MotivoDoBloqueio?
        get() = when {
            jogadores.size < Partida.MINIMO_DE_JOGADORES -> MotivoDoBloqueio.POUCOS_JOGADORES
            jogadores.any { it.nome.isBlank() } -> MotivoDoBloqueio.NOMES_VAZIOS
            modoDeJogo == ModoDeJogo.TIMES && jogadores.any { it.timeId.isNullOrBlank() } ->
                MotivoDoBloqueio.TIMES_INCOMPLETOS

            modoDeJogo == ModoDeJogo.TIMES &&
                jogadores.mapNotNull { it.timeId }.toSet().size < 2 ->
                MotivoDoBloqueio.TIMES_INSUFICIENTES

            else -> null
        }

    /** `true` quando a configuração é válida e a partida pode começar. */
    val podeComecar: Boolean
        get() = motivoDoBloqueio == null

    /**
     * [motivoDoBloqueio] pronto para exibição. [MotivoDoBloqueio.NOMES_VAZIOS]
     * fica escondido até que o usuário interaja — os 2 jogadores em branco do
     * estado inicial já disparariam essa mensagem assim que a tela abre, sem
     * nenhuma ação do usuário. Ela só aparece quando um campo de nome vazio
     * já foi tocado ([jogadoresTocados]) ou quando o usuário tentou começar a
     * partida ([tentouComecar]) com a configuração inválida. Os demais
     * motivos (times incompletos/insuficientes, poucos jogadores) só surgem
     * como consequência direta de uma interação real (trocar para o modo
     * Times, remover jogador) e continuam aparecendo imediatamente.
     */
    val motivoDoBloqueioVisivel: MotivoDoBloqueio?
        get() {
            val motivo = motivoDoBloqueio ?: return null
            if (motivo != MotivoDoBloqueio.NOMES_VAZIOS) return motivo
            if (tentouComecar) return motivo
            val campoTocadoEVazio = jogadores.withIndex().any { (indice, jogador) ->
                indice in jogadoresTocados && jogador.nome.isBlank()
            }
            return motivo.takeIf { campoTocadoEVazio }
        }
}

/** Um jogador em edição no Setup. */
data class JogadorEmEdicao(
    val nome: String = "",
    val timeId: String? = null,
)

/** Por que o botão de começar está bloqueado. */
enum class MotivoDoBloqueio {
    POUCOS_JOGADORES,
    NOMES_VAZIOS,
    TIMES_INCOMPLETOS,
    TIMES_INSUFICIENTES,
}

/**
 * ViewModel da tela de configuração. Edita a lista de jogadores (2–4, com
 * clamp nos eventos de adicionar/remover), o modo, a categoria e as regras;
 * ao [confirmar], monta a [ConfiguracaoDaPartida] e a expõe em
 * [configuracaoPronta] para a UI navegar até a rota Partida.
 */
@HiltViewModel
class SetupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _configuracaoPronta = MutableStateFlow<ConfiguracaoDaPartida?>(null)

    /** Configuração montada ao confirmar; a UI navega e chama [consumirConfiguracaoPronta]. */
    val configuracaoPronta: StateFlow<ConfiguracaoDaPartida?> = _configuracaoPronta.asStateFlow()

    fun selecionarCategoria(categoria: CardCategory) {
        _uiState.update { it.copy(categoria = categoria) }
    }

    fun selecionarModo(modoDeJogo: ModoDeJogo) {
        _uiState.update { it.copy(modoDeJogo = modoDeJogo) }
    }

    /** Adiciona um jogador em branco; ignorado se a partida já tem 4. */
    fun adicionarJogador() {
        _uiState.update { estado ->
            if (estado.jogadores.size >= Partida.MAXIMO_DE_JOGADORES) return@update estado
            estado.copy(jogadores = estado.jogadores + JogadorEmEdicao())
        }
    }

    /** Remove o jogador do [indice]; ignorado se a partida já está no mínimo de 2. */
    fun removerJogador(indice: Int) {
        _uiState.update { estado ->
            if (estado.jogadores.size <= Partida.MINIMO_DE_JOGADORES) return@update estado
            if (indice !in estado.jogadores.indices) return@update estado
            estado.copy(
                jogadores = estado.jogadores.filterIndexed { i, _ -> i != indice },
                // Os índices tocados após o removido deslizam uma posição para trás,
                // acompanhando o mesmo deslocamento da lista de jogadores.
                jogadoresTocados = estado.jogadoresTocados
                    .filter { it != indice }
                    .map { if (it > indice) it - 1 else it }
                    .toSet(),
            )
        }
    }

    fun renomearJogador(indice: Int, nome: String) {
        atualizarJogador(indice) { it.copy(nome = nome) }
    }

    /** Marca o campo de nome do [indice] como já tocado (perdeu o foco ao menos uma vez). */
    fun marcarJogadorTocado(indice: Int) {
        _uiState.update { it.copy(jogadoresTocados = it.jogadoresTocados + indice) }
    }

    /** Atribui o [timeId] ao jogador do [indice] (modo TIMES). */
    fun atribuirTime(indice: Int, timeId: String) {
        atualizarJogador(indice) { it.copy(timeId = timeId) }
    }

    /** Define o total de rodadas; ignorado se menor que 1. */
    fun definirRodadas(rodadas: Int) {
        if (rodadas < 1) return
        _uiState.update { it.copy(numeroDeRodadas = rodadas) }
    }

    fun alternarLeitorPontua() {
        _uiState.update { it.copy(leitorPontua = !it.leitorPontua) }
    }

    /**
     * Monta a configuração e a publica em [configuracaoPronta]; ignorado
     * enquanto [SetupUiState.podeComecar] for `false` — nesse caso, marca
     * [SetupUiState.tentouComecar] para revelar o motivo do bloqueio.
     *
     * O código da partida é sorteado aqui (4 letras): aleatoriedade de verdade
     * é desejada na *escolha* do código — o determinismo sagrado do projeto
     * começa na seed derivada dele.
     */
    fun confirmar() {
        val estado = _uiState.value
        if (!estado.podeComecar) {
            _uiState.update { it.copy(tentouComecar = true) }
            return
        }
        _configuracaoPronta.value = ConfiguracaoDaPartida(
            codigo = gerarCodigo(),
            categoria = estado.categoria,
            modoDeJogo = estado.modoDeJogo,
            numeroDeRodadas = estado.numeroDeRodadas,
            leitorPontua = estado.leitorPontua,
            jogadores = estado.jogadores.map { jogador ->
                JogadorConfigurado(
                    nome = jogador.nome.trim(),
                    timeId = jogador.timeId.takeIf { estado.modoDeJogo == ModoDeJogo.TIMES },
                )
            },
        )
    }

    /** A UI chama após navegar, para não repetir a navegação em recomposição. */
    fun consumirConfiguracaoPronta() {
        _configuracaoPronta.value = null
    }

    private fun atualizarJogador(indice: Int, transformacao: (JogadorEmEdicao) -> JogadorEmEdicao) {
        _uiState.update { estado ->
            if (indice !in estado.jogadores.indices) return@update estado
            estado.copy(
                jogadores = estado.jogadores.mapIndexed { i, jogador ->
                    if (i == indice) transformacao(jogador) else jogador
                },
            )
        }
    }

    private fun gerarCodigo(): String = buildString {
        repeat(TAMANHO_DO_CODIGO) { append(('A'..'Z').random()) }
    }

    private companion object {
        const val TAMANHO_DO_CODIGO = 4
    }
}
