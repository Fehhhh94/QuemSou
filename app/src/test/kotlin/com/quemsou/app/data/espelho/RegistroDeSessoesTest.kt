package com.quemsou.app.data.espelho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroDeSessoesTest {

    /** Tokens previsíveis: "t1", "t2", … — nada aqui depende de acaso. */
    private var emitidos = 0
    private val registro = RegistroDeSessoes(gerarToken = { "t${++emitidos}" })

    private val elenco = listOf(
        JogadorDoEspelho(id = "j1", nome = "Ana"),
        JogadorDoEspelho(id = "j2", nome = "Bruno"),
        JogadorDoEspelho(id = "j3", nome = "Carla"),
    )

    private fun comElenco() = registro.also { it.definirJogadores(elenco) }

    @Test
    fun `elenco novo nasce inteiro desconectado`() {
        comElenco()

        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to false), registro.conectados.value)
        assertEquals(elenco, registro.jogadores.value)
    }

    @Test
    fun `reivindicar marca conectado e devolve token da sessao`() {
        comElenco()

        val resultado = registro.reivindicar("j2", token = null)

        assertEquals(
            ResultadoDaReivindicacao.Sucesso("t1", JogadorDoEspelho("j2", "Bruno")),
            resultado,
        )
        assertEquals(mapOf("j1" to false, "j2" to true, "j3" to false), registro.conectados.value)
        assertEquals("j2", registro.jogadorDe("t1"))
    }

    @Test
    fun `id ja tomado por outra sessao falha e nao muda nada`() {
        comElenco()
        registro.reivindicar("j1", token = null)

        val invasor = registro.reivindicar("j1", token = "token-de-outro")

        assertEquals(ResultadoDaReivindicacao.JaTomado, invasor)
        assertEquals("j1", registro.jogadorDe("t1"))
        assertEquals(1, emitidos)
    }

    @Test
    fun `id ja tomado tambem falha para navegador sem token nenhum`() {
        comElenco()
        registro.reivindicar("j1", token = null)

        assertEquals(ResultadoDaReivindicacao.JaTomado, registro.reivindicar("j1", token = null))
    }

    @Test
    fun `reconexao com o mesmo token retoma o proprio lugar sem token novo`() {
        comElenco()
        val primeira = registro.reivindicar("j1", token = null) as ResultadoDaReivindicacao.Sucesso

        val segunda = registro.reivindicar("j1", token = primeira.token)

        assertEquals(primeira, segunda)
        assertEquals(1, emitidos)
        assertTrue(registro.conectados.value.getValue("j1"))
    }

    @Test
    fun `sessao conhecida que troca de nome libera o lugar anterior`() {
        comElenco()
        val entrada = registro.reivindicar("j1", token = null) as ResultadoDaReivindicacao.Sucesso

        val troca = registro.reivindicar("j3", token = entrada.token)

        assertEquals(
            ResultadoDaReivindicacao.Sucesso(entrada.token, JogadorDoEspelho("j3", "Carla")),
            troca,
        )
        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to true), registro.conectados.value)
        assertNull(registro.jogadorDe("t2"))
    }

    @Test
    fun `token desconhecido vale como sessao nova`() {
        comElenco()

        // Token guardado no navegador de uma execução anterior do servidor.
        val resultado = registro.reivindicar("j1", token = "token-velho")

        assertEquals(
            ResultadoDaReivindicacao.Sucesso("t1", JogadorDoEspelho("j1", "Ana")),
            resultado,
        )
        assertNull(registro.jogadorDe("token-velho"))
    }

    @Test
    fun `id fora do elenco e recusado`() {
        comElenco()

        assertEquals(
            ResultadoDaReivindicacao.JogadorDesconhecido,
            registro.reivindicar("j9", token = null),
        )
        assertEquals(0, emitidos)
    }

    @Test
    fun `sem elenco definido nada pode ser reivindicado`() {
        assertEquals(
            ResultadoDaReivindicacao.JogadorDesconhecido,
            registro.reivindicar("j1", token = null),
        )
        assertEquals(emptyMap<String, Boolean>(), registro.conectados.value)
    }

    @Test
    fun `redefinir o elenco preserva quem continua e descarta quem saiu`() {
        comElenco()
        val deAna = registro.reivindicar("j1", token = null) as ResultadoDaReivindicacao.Sucesso
        registro.reivindicar("j3", token = null)

        // O anfitrião removeu a Carla e renomeou a Ana.
        registro.definirJogadores(
            listOf(JogadorDoEspelho("j1", "Ana Paula"), JogadorDoEspelho("j2", "Bruno")),
        )

        assertEquals(mapOf("j1" to true, "j2" to false), registro.conectados.value)
        assertEquals("j1", registro.jogadorDe(deAna.token))
        // O lugar da Carla saiu junto com ela: aquela sessão não é mais dona de nada.
        assertNull(registro.jogadorDe("t2"))
    }

    @Test
    fun `limpar esquece elenco e reivindicacoes`() {
        comElenco()
        val entrada = registro.reivindicar("j1", token = null) as ResultadoDaReivindicacao.Sucesso

        registro.limpar()

        assertEquals(emptyList<JogadorDoEspelho>(), registro.jogadores.value)
        assertEquals(emptyMap<String, Boolean>(), registro.conectados.value)
        assertNull(registro.jogadorDe(entrada.token))
    }

    // region "Este aparelho" — quem segura o celular anfitrião

    @Test
    fun `ninguem nasce marcado como este aparelho`() {
        comElenco()

        assertNull(registro.esteAparelho.value)
        assertEquals(elenco, registro.jogadoresOferecidos())
    }

    @Test
    fun `marcar ocupa o id e o tira da lista oferecida na web`() {
        comElenco()

        registro.marcarEsteAparelho("j2")

        assertEquals("j2", registro.esteAparelho.value)
        assertEquals(listOf("j1", "j3"), registro.jogadoresOferecidos().map { it.id })
        assertEquals(ResultadoDaReivindicacao.JaTomado, registro.reivindicar("j2", token = null))
        // Ocupado não é o mesmo que conectado pela rede: o ✓ verde não acende.
        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to false), registro.conectados.value)
    }

    @Test
    fun `desmarcar devolve o id para a mesa`() {
        comElenco()
        registro.marcarEsteAparelho("j2")

        registro.marcarEsteAparelho(null)

        assertNull(registro.esteAparelho.value)
        assertEquals(elenco, registro.jogadoresOferecidos())
        assertTrue(registro.reivindicar("j2", token = null) is ResultadoDaReivindicacao.Sucesso)
    }

    @Test
    fun `marcar outro move a marca e nunca deixa dois marcados`() {
        comElenco()
        registro.marcarEsteAparelho("j1")

        registro.marcarEsteAparelho("j3")

        assertEquals("j3", registro.esteAparelho.value)
        assertEquals(listOf("j1", "j2"), registro.jogadoresOferecidos().map { it.id })
        assertTrue(registro.reivindicar("j1", token = null) is ResultadoDaReivindicacao.Sucesso)
    }

    @Test
    fun `marcar id fora do elenco e ignorado`() {
        comElenco()

        registro.marcarEsteAparelho("j9")

        assertNull(registro.esteAparelho.value)
    }

    @Test
    fun `marcar quem ja tinha entrado pela rede toma o lugar dele`() {
        comElenco()
        val entrada = registro.reivindicar("j1", token = null) as ResultadoDaReivindicacao.Sucesso

        registro.marcarEsteAparelho("j1")

        assertEquals("j1", registro.esteAparelho.value)
        assertNull(registro.jogadorDe(entrada.token))
        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to false), registro.conectados.value)
    }

    @Test
    fun `remover do elenco o jogador marcado limpa a marca`() {
        comElenco()
        registro.marcarEsteAparelho("j3")

        registro.definirJogadores(elenco.take(2))

        assertNull(registro.esteAparelho.value)
    }

    @Test
    fun `limpar tambem esquece a marca`() {
        comElenco()
        registro.marcarEsteAparelho("j1")

        registro.limpar()

        assertNull(registro.esteAparelho.value)
        assertEquals(emptyList<JogadorDoEspelho>(), registro.jogadoresOferecidos())
    }

    // endregion

    // region Liberar lugar de sessão morta

    @Test
    fun `liberar devolve o id e permite entrar de novo`() {
        comElenco()
        val morta = registro.reivindicar("j2", token = null) as ResultadoDaReivindicacao.Sucesso

        registro.liberarLugar("j2")

        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to false), registro.conectados.value)
        assertNull(registro.jogadorDe(morta.token))
        // O nome volta a ser oferecido e outro navegador pega o lugar.
        assertEquals(
            ResultadoDaReivindicacao.Sucesso("t2", JogadorDoEspelho("j2", "Bruno")),
            registro.reivindicar("j2", token = null),
        )
    }

    @Test
    fun `o token liberado nao retoma o lugar de quem entrou depois`() {
        comElenco()
        val morta = registro.reivindicar("j2", token = null) as ResultadoDaReivindicacao.Sucesso
        registro.liberarLugar("j2")
        registro.reivindicar("j2", token = null)

        assertEquals(ResultadoDaReivindicacao.JaTomado, registro.reivindicar("j2", token = morta.token))
    }

    @Test
    fun `liberar lugar livre nao muda nada`() {
        comElenco()

        registro.liberarLugar("j1")

        assertEquals(mapOf("j1" to false, "j2" to false, "j3" to false), registro.conectados.value)
    }

    @Test
    fun `liberar nao mexe na marca de este aparelho`() {
        comElenco()
        registro.marcarEsteAparelho("j1")

        registro.liberarLugar("j1")

        assertEquals("j1", registro.esteAparelho.value)
        assertEquals(ResultadoDaReivindicacao.JaTomado, registro.reivindicar("j1", token = null))
    }

    // endregion
}
