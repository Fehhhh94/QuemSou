package com.quemsou.app.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quemsou.app.data.espelho.JogadorDoEspelho
import com.quemsou.app.data.espelho.ResultadoDoInicio
import com.quemsou.app.data.espelho.ServidorDoEspelho
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.navigation.JogadorConfigurado
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de configuração da partida, com validação viva:
 * [podeComecar] e [motivoDoBloqueio] são recalculados a cada mudança.
 *
 * A partida é montada por **baralhos** (5A parte 2): a seleção começa com
 * todos os baralhos do aparelho marcados (equivalente ao antigo "Livre") e o
 * contador de união mostra o monte vivo. Não existe mais "modo de jogo"
 * (especificação v4): [jogarEmTimes] só liga a UI de agrupamento.
 */
data class SetupUiState(
    val baralhosDisponiveis: List<BaralhoParaSelecao> = emptyList(),
    val baralhosSelecionados: Set<String> = emptySet(),
    val baralhosCarregados: Boolean = false,
    val jogarEmTimes: Boolean = false,
    val jogadores: List<JogadorEmEdicao> = List(Partida.MINIMO_DE_JOGADORES) {
        JogadorEmEdicao(id = "j${it + 1}")
    },
    val numeroDeRodadas: Int = 5,
    val leitorPontua: Boolean = true,
    val modoShot: Boolean = false,
    val quantidadeDeShots: Int = RegrasPartida.QUANTIDADE_PADRAO_DE_SHOTS,
    val jogadoresTocados: Set<Int> = emptySet(),
    val tentouComecar: Boolean = false,
    val espelhoLigado: Boolean = false,
    val espelhoIniciando: Boolean = false,
    val espelhoEndereco: String? = null,
    val espelhoFalha: FalhaDoEspelho? = null,
    val espelhoConectados: Set<String> = emptySet(),
    val espelhoEsteAparelho: String? = null,
    val espelhoLugarALiberar: String? = null,
) {

    /** Em que estado de pareamento está a linha do jogador [id] no espelho. */
    fun estadoNoEspelho(id: String): EstadoNoEspelho = when {
        id == espelhoEsteAparelho -> EstadoNoEspelho.ESTE_APARELHO
        id in espelhoConectados -> EstadoNoEspelho.ENTROU
        else -> EstadoNoEspelho.AGUARDANDO
    }
    /** Total de cards do monte da união dos baralhos selecionados. */
    val cardsNoMonte: Int
        get() = baralhosDisponiveis
            .filter { it.id in baralhosSelecionados }
            .sumOf { it.quantidadeDeCards }

    /** Primeiro motivo que impede a partida de começar; `null` se está tudo certo. */
    val motivoDoBloqueio: MotivoDoBloqueio?
        get() = when {
            jogadores.size < Partida.MINIMO_DE_JOGADORES -> MotivoDoBloqueio.POUCOS_JOGADORES
            jogadores.any { it.nome.isBlank() } -> MotivoDoBloqueio.NOMES_VAZIOS
            baralhosCarregados && baralhosSelecionados.isEmpty() -> MotivoDoBloqueio.NENHUM_BARALHO
            baralhosCarregados && cardsNoMonte < numeroDeRodadas -> MotivoDoBloqueio.CARDS_INSUFICIENTES
            else -> null
        }

    /** `true` quando a configuração é válida e a partida pode começar. */
    val podeComecar: Boolean
        get() = motivoDoBloqueio == null && baralhosCarregados

    /**
     * [motivoDoBloqueio] pronto para exibição. [MotivoDoBloqueio.NOMES_VAZIOS]
     * fica escondido até que o usuário interaja — os 2 jogadores em branco do
     * estado inicial já disparariam essa mensagem assim que a tela abre, sem
     * nenhuma ação do usuário. Ela só aparece quando um campo de nome vazio
     * já foi tocado ([jogadoresTocados]) ou quando o usuário tentou começar a
     * partida ([tentouComecar]) com a configuração inválida. Os demais
     * motivos só surgem como consequência direta de uma interação real
     * (remover jogador, desmarcar baralhos, subir rodadas) e continuam
     * aparecendo imediatamente.
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

/** Um baralho do aparelho como oferecido na seleção do Setup. */
data class BaralhoParaSelecao(
    val id: String,
    val nome: String,
    val estado: EstadoDoBaralho,
    val colecaoId: String,
    val colecaoNome: String,
    val colecaoIcone: String,
    val quantidadeDeCards: Int,
)

/**
 * Um jogador em edição no Setup.
 *
 * @property id identidade estável da linha, atribuída pelo ViewModel e nunca
 *   reaproveitada. Não vai para o domínio: serve ao espelho de leitura, para
 *   que remover ou renomear um jogador não faça o ✓ de quem já entrou pular
 *   de linha.
 * @property grupo número do grupo escolhido no ciclo do chip (1, 2, 3…);
 *   `null` = "Sem grupo", ou seja, grupo próprio de tamanho 1.
 */
data class JogadorEmEdicao(
    val id: String,
    val nome: String = "",
    val grupo: Int? = null,
)

/** Por que o botão de começar está bloqueado. */
enum class MotivoDoBloqueio {
    POUCOS_JOGADORES,
    NOMES_VAZIOS,
    NENHUM_BARALHO,
    CARDS_INSUFICIENTES,
}

/**
 * Estado de pareamento de um jogador na lista do espelho. Define o que a
 * linha exibe **e** o que um toque nela faz — os três casos são exaustivos e
 * mutuamente exclusivos de propósito.
 */
enum class EstadoNoEspelho {
    /** Ninguém pegou este lugar; tocar marca "este aparelho". */
    AGUARDANDO,

    /** Entrou pela rede; tocar abre a confirmação de liberar o lugar. */
    ENTROU,

    /** É quem segura o celular anfitrião; tocar desmarca. */
    ESTE_APARELHO,
}

/** Por que o espelho de leitura não conseguiu subir. */
enum class FalhaDoEspelho {
    /** Nenhuma interface de rede local — sem Wi-Fi não há espelho. */
    SEM_REDE,

    /** Todas as portas da faixa estão ocupadas neste aparelho. */
    SEM_PORTA,
}

/**
 * ViewModel da tela de configuração. Edita a seleção de baralhos (carregada
 * do aparelho e recarregável ao voltar do catálogo), a lista de jogadores
 * (2–4, com clamp nos eventos de adicionar/remover), o agrupamento em times
 * e as regras; ao [confirmar], monta a [ConfiguracaoDaPartida] e a expõe em
 * [configuracaoPronta] para a UI navegar até a rota Partida.
 *
 * Também é o dono do **espelho de leitura** (4A parte 1): o servidor sobe e
 * cai com esta tela ([onCleared] o derruba). Na parte 2 ele passa a
 * sobreviver até o fim da partida. O espelho **nunca** bloqueia o jogo —
 * [podeComecar] não olha para ele.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repositorioDeCards: RepositorioDeCards,
    private val servidorDoEspelho: ServidorDoEspelho,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _configuracaoPronta = MutableStateFlow<ConfiguracaoDaPartida?>(null)

    /** Configuração montada ao confirmar; a UI navega e chama [consumirConfiguracaoPronta]. */
    val configuracaoPronta: StateFlow<ConfiguracaoDaPartida?> = _configuracaoPronta.asStateFlow()

    /** Sequência dos ids de linha; nunca reaproveitada dentro desta tela. */
    private var ultimoIdDeJogador = _uiState.value.jogadores.size

    init {
        recarregarBaralhos()
        observarEspelho()
    }

    /**
     * Duas assinaturas do espelho: o ✓ ao vivo vindo do servidor e o elenco
     * indo para ele. Mantê-las aqui, e não espalhadas por cada evento de
     * edição de jogador, garante que renomear, adicionar e remover cheguem
     * ao navegador sem ninguém lembrar de avisar.
     */
    private fun observarEspelho() {
        viewModelScope.launch {
            servidorDoEspelho.conectados.collect { conectados ->
                _uiState.update { estado ->
                    estado.copy(espelhoConectados = conectados.filterValues { it }.keys)
                }
            }
        }
        viewModelScope.launch {
            servidorDoEspelho.esteAparelho.collect { marcado ->
                _uiState.update { it.copy(espelhoEsteAparelho = marcado) }
            }
        }
        viewModelScope.launch {
            _uiState
                .map { estado -> estado.jogadores.map { JogadorDoEspelho(it.id, it.nome.trim()) } }
                .distinctUntilChanged()
                .collect { servidorDoEspelho.atualizarJogadores(it) }
        }
    }

    /**
     * (Re)carrega os baralhos do aparelho — chamada no início e ao voltar da
     * tela de catálogo (pode ter baralho novo). Na primeira carga, todos
     * nascem selecionados (equivalente ao antigo "Livre"); nas seguintes, a
     * seleção do usuário é preservada e baralhos novos entram desmarcados.
     */
    fun recarregarBaralhos() {
        viewModelScope.launch {
            val baralhos = repositorioDeCards.buscarTodos()
                .map { baralho ->
                    BaralhoParaSelecao(
                        id = baralho.id,
                        nome = baralho.nome,
                        estado = baralho.estado,
                        colecaoId = baralho.colecao.id,
                        colecaoNome = baralho.colecao.nome,
                        colecaoIcone = baralho.colecao.icone,
                        quantidadeDeCards = baralho.quantidadeDeCards,
                    )
                }
                .sortedWith(compareBy({ it.colecaoNome }, { it.nome }))
            _uiState.update { estado ->
                val idsDisponiveis = baralhos.map { it.id }.toSet()
                estado.copy(
                    baralhosDisponiveis = baralhos,
                    baralhosSelecionados = if (estado.baralhosCarregados) {
                        estado.baralhosSelecionados intersect idsDisponiveis
                    } else {
                        idsDisponiveis
                    },
                    baralhosCarregados = true,
                )
            }
        }
    }

    /** Marca/desmarca o baralho [id] na seleção da partida. */
    fun alternarBaralho(id: String) {
        _uiState.update { estado ->
            if (estado.baralhosDisponiveis.none { it.id == id }) return@update estado
            estado.copy(
                baralhosSelecionados = if (id in estado.baralhosSelecionados) {
                    estado.baralhosSelecionados - id
                } else {
                    estado.baralhosSelecionados + id
                },
            )
        }
    }

    /** Atalho "Selecionar todos" da seção de baralhos. */
    fun selecionarTodosBaralhos() {
        _uiState.update { estado ->
            estado.copy(baralhosSelecionados = estado.baralhosDisponiveis.map { it.id }.toSet())
        }
    }

    /**
     * Liga/desliga a UI de agrupamento em times. Desligado (padrão), cada
     * jogador joga em grupo próprio de 1 — o agrupamento escolhido é mantido
     * no estado, mas descartado ao [confirmar].
     */
    fun alternarJogarEmTimes() {
        _uiState.update { it.copy(jogarEmTimes = !it.jogarEmTimes) }
    }

    /** Adiciona um jogador em branco; ignorado se a partida já tem 4. */
    fun adicionarJogador() {
        _uiState.update { estado ->
            if (estado.jogadores.size >= Partida.MAXIMO_DE_JOGADORES) return@update estado
            estado.copy(jogadores = estado.jogadores + JogadorEmEdicao(id = "j${++ultimoIdDeJogador}"))
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

    /**
     * Cicla o grupo do jogador do [indice]: Sem grupo → Grupo 1 → Grupo 2 →
     * Grupo 3 → Sem grupo. O ciclo é só de exibição (com o teto de 4
     * jogadores da partida, 3 grupos nomeados cobrem qualquer agrupamento) —
     * o domínio não valida quantidade de grupos.
     */
    fun ciclarGrupo(indice: Int) {
        atualizarJogador(indice) { jogador ->
            val proximo = (jogador.grupo ?: 0) + 1
            jogador.copy(grupo = proximo.takeIf { it <= ULTIMO_GRUPO_DO_CICLO })
        }
    }

    /** Define o total de rodadas; ignorado se menor que 1. */
    fun definirRodadas(rodadas: Int) {
        if (rodadas < 1) return
        _uiState.update { it.copy(numeroDeRodadas = rodadas) }
    }

    fun alternarLeitorPontua() {
        _uiState.update { it.copy(leitorPontua = !it.leitorPontua) }
    }

    fun alternarModoShot() {
        _uiState.update { it.copy(modoShot = !it.modoShot) }
    }

    /** Define quantas posições do grid têm shot; ignorado fora da faixa 1–3 da regra. */
    fun definirQuantidadeDeShots(quantidade: Int) {
        if (quantidade !in RegrasPartida.MINIMO_DE_SHOTS..RegrasPartida.MAXIMO_DE_SHOTS) return
        _uiState.update { it.copy(quantidadeDeShots = quantidade) }
    }

    /**
     * Monta a configuração e a publica em [configuracaoPronta]; ignorado
     * enquanto [SetupUiState.podeComecar] for `false` — nesse caso, marca
     * [SetupUiState.tentouComecar] para revelar o motivo do bloqueio.
     *
     * Os baralhos selecionados saem em ordem estável (por id) — a ordem da
     * seleção não muda o monte (união determinística). O agrupamento vira
     * [JogadorConfigurado.grupoId] ("g1", "g2"…) apenas com
     * [SetupUiState.jogarEmTimes] ligado; caso contrário todo jogador sai
     * sem grupo — grupo próprio de 1, o estado padrão do modelo v4.
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
            baralhos = estado.baralhosSelecionados.sorted(),
            numeroDeRodadas = estado.numeroDeRodadas,
            leitorPontua = estado.leitorPontua,
            modoShot = estado.modoShot,
            quantidadeDeShots = estado.quantidadeDeShots,
            jogadores = estado.jogadores.map { jogador ->
                JogadorConfigurado(
                    nome = jogador.nome.trim(),
                    grupoId = jogador.grupo?.let { "g$it" }.takeIf { estado.jogarEmTimes },
                )
            },
        )
    }

    /** A UI chama após navegar, para não repetir a navegação em recomposição. */
    fun consumirConfiguracaoPronta() {
        _configuracaoPronta.value = null
    }

    /**
     * Liga/desliga o espelho de leitura. **Nada sobe sem este toque** — o
     * padrão é desligado. Falha ao subir volta o switch para desligado com o
     * motivo em [SetupUiState.espelhoFalha]; o botão de começar a partida
     * segue intocado nos dois casos.
     */
    fun alternarEspelho() {
        if (_uiState.value.espelhoIniciando) return
        if (_uiState.value.espelhoLigado) {
            // `parar()` limpa o registro, e o marcador "este aparelho" cai
            // junto pelo próprio fluxo observado — aqui só sobra o que é
            // puramente de tela.
            servidorDoEspelho.parar()
            _uiState.update {
                it.copy(
                    espelhoLigado = false,
                    espelhoEndereco = null,
                    espelhoFalha = null,
                    espelhoLugarALiberar = null,
                )
            }
            return
        }
        var deveIniciar = false
        _uiState.update { estado ->
            if (estado.espelhoLigado || estado.espelhoIniciando) {
                estado
            } else {
                deveIniciar = true
                estado.copy(espelhoIniciando = true, espelhoFalha = null)
            }
        }
        if (!deveIniciar) return
        viewModelScope.launch {
            try {
                val elenco = _uiState.value.jogadores.map { JogadorDoEspelho(it.id, it.nome.trim()) }
                when (val resultado = servidorDoEspelho.iniciar(elenco)) {
                    is ResultadoDoInicio.NoAr -> _uiState.update {
                        it.copy(
                            espelhoLigado = true,
                            espelhoEndereco = resultado.endereco,
                            espelhoFalha = null,
                        )
                    }

                    ResultadoDoInicio.SemRede -> _uiState.update {
                        it.copy(
                            espelhoLigado = false,
                            espelhoEndereco = null,
                            espelhoFalha = FalhaDoEspelho.SEM_REDE,
                        )
                    }

                    ResultadoDoInicio.SemPorta -> _uiState.update {
                        it.copy(
                            espelhoLigado = false,
                            espelhoEndereco = null,
                            espelhoFalha = FalhaDoEspelho.SEM_PORTA,
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(espelhoIniciando = false) }
            }
        }
    }

    /**
     * Toque numa linha da lista do espelho. O desfecho depende do estado da
     * linha ([EstadoNoEspelho]) — um gesto só, três significados, porque em
     * cada estado existe exatamente uma ação sensata:
     *
     * - **aguardando** → marca como "este aparelho" (quem segura o celular
     *   anfitrião nunca vai escanear o próprio QR);
     * - **este aparelho** → desmarca;
     * - **entrou** → abre a confirmação de liberar o lugar.
     */
    fun tocarNaLinhaDoEspelho(jogadorId: String) {
        when (_uiState.value.estadoNoEspelho(jogadorId)) {
            EstadoNoEspelho.AGUARDANDO -> servidorDoEspelho.marcarEsteAparelho(jogadorId)
            EstadoNoEspelho.ESTE_APARELHO -> servidorDoEspelho.marcarEsteAparelho(null)
            EstadoNoEspelho.ENTROU -> _uiState.update { it.copy(espelhoLugarALiberar = jogadorId) }
        }
    }

    /** Confirma a liberação: o lugar volta a "aguardando" e reaparece na web. */
    fun confirmarLiberarLugar() {
        val jogadorId = _uiState.value.espelhoLugarALiberar ?: return
        servidorDoEspelho.liberarLugar(jogadorId)
        _uiState.update { it.copy(espelhoLugarALiberar = null) }
    }

    /** Fecha a confirmação sem mexer em nada. */
    fun cancelarLiberarLugar() {
        _uiState.update { it.copy(espelhoLugarALiberar = null) }
    }

    /** Nesta parte 1 o espelho é escopado à tela: sair do Setup o derruba. */
    override fun onCleared() {
        servidorDoEspelho.parar()
        super.onCleared()
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

        /** Último grupo do ciclo do chip — limite de exibição, não do domínio. */
        const val ULTIMO_GRUPO_DO_CICLO = 3
    }
}
