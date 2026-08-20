package com.quemsou.app.data.espelho

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Um jogador oferecido ao espelho de leitura: o [id] é estável dentro da
 * partida (o Setup o atribui e nunca o reaproveita) e o [nome] é o texto
 * exibido no navegador.
 */
data class JogadorDoEspelho(
    val id: String,
    val nome: String,
)

/** Desfecho de uma tentativa de reivindicar um jogador no espelho. */
sealed interface ResultadoDaReivindicacao {

    /** Reivindicação aceita; [token] identifica a sessão daquele navegador. */
    data class Sucesso(val token: String, val jogador: JogadorDoEspelho) : ResultadoDaReivindicacao

    /** O id pedido não pertence ao elenco atual da partida. */
    data object JogadorDesconhecido : ResultadoDaReivindicacao

    /** O id já foi reivindicado por outra sessão. */
    data object JaTomado : ResultadoDaReivindicacao
}

/**
 * Quem já pegou qual jogador no espelho de leitura.
 *
 * Kotlin puro, sem Android — é a peça com regra de verdade do espelho e a
 * única coberta por testes JVM. Invariantes:
 *
 * - **Um jogador, uma sessão**: reivindicar um id já tomado por outra sessão
 *   falha com [ResultadoDaReivindicacao.JaTomado].
 * - **Reconexão não rouba lugar**: a sessão que já é dona do id pode
 *   reivindicá-lo quantas vezes quiser (recarregar a página, perder o
 *   Wi-Fi) e recebe de volta o **mesmo** token.
 * - **Trocar de nome no mesmo navegador é permitido**: uma sessão conhecida
 *   que reivindica outro id livre **libera o anterior** e mantém o token —
 *   quem tocou no nome errado se conserta sem ficar com dois lugares.
 * - **Token desconhecido é token novo**: um token de uma execução anterior
 *   do servidor (guardado no `localStorage` do navegador) não vale nada
 *   aqui; a reivindicação segue como se fosse a primeira e devolve um token
 *   novo.
 * - **Um lugar, um dono**: quem está em [esteAparelho] ocupa o lugar sem
 *   token nenhum, e ninguém o reivindica pela rede.
 *
 * [conectados] é a fonte do ✓ ao vivo na lista do Setup: mapeia todo id do
 * elenco para `true`/`false`. "Conectado" quer dizer **reivindicado pela
 * rede** — não depende do canal de eventos estar aberto, e **não** inclui
 * quem segura o aparelho anfitrião (esse fica em [esteAparelho], que a UI lê
 * à parte porque o estado dele é outro).
 *
 * @param gerarToken injetável para os testes ficarem determinísticos; em
 *   produção é um UUID aleatório. Nada aqui alimenta seed ou embaralhamento
 *   — o determinismo do jogo não passa por este registro.
 */
class RegistroDeSessoes(
    private val gerarToken: () -> String = { UUID.randomUUID().toString() },
) {

    private val _jogadores = MutableStateFlow<List<JogadorDoEspelho>>(emptyList())

    /** Elenco atual da partida, na ordem em que o Setup o entregou. */
    val jogadores: StateFlow<List<JogadorDoEspelho>> = _jogadores.asStateFlow()

    private val _conectados = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /** Mapa jogadorId → já reivindicado pela rede, uma entrada por jogador. */
    val conectados: StateFlow<Map<String, Boolean>> = _conectados.asStateFlow()

    private val _esteAparelho = MutableStateFlow<String?>(null)

    /**
     * Quem segura o aparelho anfitrião, ou `null` se ninguém foi marcado
     * (o padrão). Essa pessoa nunca vai escanear o QR: o lugar dela precisa
     * nascer ocupado, senão ela fica eternamente "aguardando" e o nome dela
     * segue disponível para outro tocar por engano.
     */
    val esteAparelho: StateFlow<String?> = _esteAparelho.asStateFlow()

    /** jogadorId → token da sessão dona. Só contém ids reivindicados pela rede. */
    private val tokenPorJogador = mutableMapOf<String, String>()

    /**
     * Redefine o elenco (o Setup chama a cada mudança na lista de jogadores).
     * Reivindicações de ids que continuam no elenco são preservadas; as dos
     * ids que sumiram são descartadas — inclusive a marca de [esteAparelho],
     * se o jogador marcado foi removido da partida.
     */
    @Synchronized
    fun definirJogadores(jogadores: List<JogadorDoEspelho>) {
        val ids = jogadores.map { it.id }.toSet()
        tokenPorJogador.keys.retainAll(ids)
        val marcado = _esteAparelho.value
        if (marcado != null && marcado !in ids) _esteAparelho.value = null
        _jogadores.value = jogadores
        publicarConectados()
    }

    /**
     * Marca [jogadorId] como quem segura o aparelho anfitrião; `null`
     * desmarca. **No máximo um por vez** — marcar outro move a marca.
     *
     * Marcar toma o lugar: se aquele id tinha sessão de rede, ela cai junto
     * (quem está com o celular na mão manda). Ids fora do elenco são
     * ignorados.
     */
    @Synchronized
    fun marcarEsteAparelho(jogadorId: String?) {
        if (jogadorId != null && _jogadores.value.none { it.id == jogadorId }) return
        _esteAparelho.value = jogadorId
        if (jogadorId != null) tokenPorJogador.remove(jogadorId)
        publicarConectados()
    }

    /**
     * Devolve o lugar de [jogadorId] à mesa, esquecendo a sessão de rede que
     * o ocupava.
     *
     * Existe porque a parte 1 não tem como perceber sessão morta: se alguém
     * fecha a aba anônima, limpa o navegador ou troca de celular, o token
     * some do lado de lá e o lugar fica preso do lado de cá. A parte 2, com
     * o SSE acompanhando presença, resolve isso sozinha — até lá, o
     * anfitrião libera na mão.
     */
    @Synchronized
    fun liberarLugar(jogadorId: String) {
        if (tokenPorJogador.remove(jogadorId) == null) return
        publicarConectados()
    }

    /**
     * Tenta dar o jogador [jogadorId] à sessão de [token] (nulo quando o
     * navegador ainda não tem sessão nenhuma).
     */
    @Synchronized
    fun reivindicar(jogadorId: String, token: String?): ResultadoDaReivindicacao {
        val jogador = _jogadores.value.firstOrNull { it.id == jogadorId }
            ?: return ResultadoDaReivindicacao.JogadorDesconhecido
        if (jogadorId == _esteAparelho.value) return ResultadoDaReivindicacao.JaTomado
        val donoAtual = tokenPorJogador[jogadorId]
        if (donoAtual != null && donoAtual != token) return ResultadoDaReivindicacao.JaTomado

        val tokenDaSessao = token?.takeIf { it in tokenPorJogador.values } ?: gerarToken()
        // A sessão só pode ser dona de um jogador por vez: se ela já tinha
        // outro, o lugar antigo volta a ficar livre para a mesa.
        tokenPorJogador.entries.removeAll { (id, dono) -> dono == tokenDaSessao && id != jogadorId }
        tokenPorJogador[jogadorId] = tokenDaSessao
        publicarConectados()
        return ResultadoDaReivindicacao.Sucesso(tokenDaSessao, jogador)
    }

    /** Qual jogador pertence a [token], ou `null` se o token é desconhecido. */
    @Synchronized
    fun jogadorDe(token: String): String? =
        tokenPorJogador.entries.firstOrNull { it.value == token }?.key

    /**
     * Elenco como o cliente web deve vê-lo: quem está em [esteAparelho]
     * **some da lista** em vez de aparecer esmaecido — aquela pessoa está do
     * outro lado do QR, e oferecer o nome dela só convidaria a um toque por
     * engano.
     */
    @Synchronized
    fun jogadoresOferecidos(): List<JogadorDoEspelho> =
        _jogadores.value.filter { it.id != _esteAparelho.value }

    /** Esquece elenco, reivindicações e a marca — o servidor parou. */
    @Synchronized
    fun limpar() {
        tokenPorJogador.clear()
        _jogadores.value = emptyList()
        _conectados.value = emptyMap()
        _esteAparelho.value = null
    }

    private fun publicarConectados() {
        _conectados.value = _jogadores.value.associate { it.id to (it.id in tokenPorJogador) }
    }
}
