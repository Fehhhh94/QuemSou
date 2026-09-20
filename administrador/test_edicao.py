"""Compatibilidade de estado, ids imutaveis, teto e sincronia das dicas."""
import copy
import unittest

import edicao
from apoio_de_teste import baralho, card_com_banco, card_simples


def baralho_vivo():
    return baralho(
        "demo-evolucao",
        "Demo",
        "EM_DESENVOLVIMENTO",
        [card_com_banco("de-001", "Resposta Viva"), card_simples("de-002", "Resposta Legada")],
    )


class TesteDeCompatibilidade(unittest.TestCase):
    def test_finalizado_legado_permite_edicao(self):
        final = baralho("demo-final", "Demo", "FINALIZADO", [card_simples("df-001", "X")])
        novo = edicao.aplicar_edicao_no_baralho(final, {"nome": "Outro nome"})
        self.assertEqual(novo["nome"], "Outro nome")
        self.assertEqual(novo["estado"], "FINALIZADO")

    def test_estado_desconhecido_e_recusado(self):
        desconhecido = baralho("demo", "Demo", "DESCONHECIDO", [card_simples("d-001", "X")])
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_edicao_no_baralho(desconhecido, {"nome": "Outro nome"})
        self.assertEqual(contexto.exception.codigo, "estado_incompativel")

    def test_id_do_baralho_nao_muda(self):
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_edicao_no_baralho(baralho_vivo(), {"id": "outro-id"})
        self.assertEqual(contexto.exception.codigo, "campo_imutavel")

    def test_estado_e_versao_nao_mudam_pelo_editor(self):
        for alteracao in ({"estado": "FINALIZADO"}, {"versao": 9}):
            with self.subTest(alteracao=alteracao):
                with self.assertRaises(edicao.FalhaDaEdicao):
                    edicao.aplicar_edicao_no_baralho(baralho_vivo(), alteracao)

    def test_id_do_card_nao_muda(self):
        original = baralho_vivo()
        cards = [copy.deepcopy(card) for card in original["cards"]]
        cards[0]["id"] = "outro"
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.aplicar_edicao_no_baralho(original, {"cards": cards})

    def test_resposta_id_nao_muda(self):
        original = baralho_vivo()
        cards = [copy.deepcopy(card) for card in original["cards"]]
        cards[0]["respostaId"] = "outra-resposta"
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.aplicar_edicao_no_baralho(original, {"cards": cards})

    def test_cards_nao_podem_ser_acrescentados_nem_removidos(self):
        original = baralho_vivo()
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.aplicar_edicao_no_baralho(original, {"cards": original["cards"][:1]})

    def test_id_de_fato_do_banco_nao_muda(self):
        original = baralho_vivo()
        cards = [copy.deepcopy(card) for card in original["cards"]]
        cards[0]["bancoDeDicas"][0]["id"] = "inventado"
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.aplicar_edicao_no_baralho(original, {"cards": cards})

    def test_banco_nao_pode_perder_fatos(self):
        original = baralho_vivo()
        cards = [copy.deepcopy(card) for card in original["cards"]]
        cards[0]["bancoDeDicas"] = cards[0]["bancoDeDicas"][:-1]
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.aplicar_edicao_no_baralho(original, {"cards": cards})


class TesteDeSincroniaDeDicas(unittest.TestCase):
    def setUp(self):
        self.original = baralho_vivo()
        self.card = self.original["cards"][0]

    def _editar_dica(self, posicao, texto):
        cards = [copy.deepcopy(card) for card in self.original["cards"]]
        cards[0]["clues"][posicao] = texto
        novo = edicao.aplicar_edicao_no_baralho(self.original, {"cards": cards})
        return novo["cards"][0]

    def test_correcao_factual_mantem_o_id_do_fato(self):
        id_do_fato = self.card["bancoDeDicas"][2]["id"]
        card = self._editar_dica(2, "pista corrigida com dado novo")
        fato = next(f for f in card["bancoDeDicas"] if f["id"] == id_do_fato)
        self.assertEqual(fato["texto"], "pista corrigida com dado novo")
        self.assertEqual(card["clues"][2], "pista corrigida com dado novo")

    def test_correcao_nao_descarta_os_demais_fatos(self):
        antes = len(self.card["bancoDeDicas"])
        card = self._editar_dica(0, "outra pista")
        self.assertEqual(len(card["bancoDeDicas"]), antes)
        self.assertEqual(
            [fato["id"] for fato in card["bancoDeDicas"]],
            [fato["id"] for fato in self.card["bancoDeDicas"]],
        )

    def test_fatos_alem_das_dez_continuam_intactos(self):
        extras_antes = [f for f in self.card["bancoDeDicas"] if ":x" in f["id"]]
        card = self._editar_dica(5, "pista revisada")
        extras_depois = [f for f in card["bancoDeDicas"] if ":x" in f["id"]]
        self.assertEqual(extras_antes, extras_depois)
        self.assertEqual(len(extras_depois), 3)

    def test_editar_fato_extra_nao_mexe_nas_dez_dicas(self):
        cards = [copy.deepcopy(card) for card in self.original["cards"]]
        alvo = next(f for f in cards[0]["bancoDeDicas"] if ":x" in f["id"])
        alvo["texto"] = "fato extra revisado"
        novo = edicao.aplicar_edicao_no_baralho(self.original, {"cards": cards})
        self.assertEqual(novo["cards"][0]["clues"], self.card["clues"])
        fato = next(f for f in novo["cards"][0]["bancoDeDicas"] if f["id"] == alvo["id"])
        self.assertEqual(fato["texto"], "fato extra revisado")

    def test_editar_fato_que_e_dica_propaga_para_a_dica(self):
        cards = [copy.deepcopy(card) for card in self.original["cards"]]
        alvo = cards[0]["bancoDeDicas"][3]
        alvo["texto"] = "fato usado como dica, revisado"
        novo = edicao.aplicar_edicao_no_baralho(self.original, {"cards": cards})
        self.assertEqual(novo["cards"][0]["clues"][3], "fato usado como dica, revisado")

    def test_campos_desconhecidos_sobrevivem(self):
        card = self._editar_dica(1, "pista nova")
        self.assertEqual(card["campoDesconhecido"], "precisa sobreviver")
        self.assertEqual(card["respostaId"], self.card["respostaId"])

    def test_card_legado_sem_banco_continua_editavel(self):
        cards = [copy.deepcopy(card) for card in self.original["cards"]]
        cards[1]["clues"][0] = "pista legada revisada"
        novo = edicao.aplicar_edicao_no_baralho(self.original, {"cards": cards})
        self.assertEqual(novo["cards"][1]["clues"][0], "pista legada revisada")
        self.assertNotIn("bancoDeDicas", novo["cards"][1])

    def test_exige_exatamente_dez_dicas(self):
        cards = [copy.deepcopy(card) for card in self.original["cards"]]
        cards[0]["clues"] = cards[0]["clues"][:9]
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_edicao_no_baralho(self.original, {"cards": cards})
        self.assertEqual(contexto.exception.codigo, "quantidade_de_dicas")


class TesteDeRascunhoNovo(unittest.TestCase):
    def test_cria_baralho_em_desenvolvimento_com_slug(self):
        novo = edicao.criar_baralho_de_rascunho("Bar do Zé — Piloto", "Especiais", "⭐", "ESPECIAIS")
        self.assertEqual(novo["id"], "bar-do-ze-piloto")
        self.assertEqual(novo["estado"], "EM_DESENVOLVIMENTO")
        self.assertEqual(novo["versao"], 1)
        self.assertEqual(novo["cards"], [])

    def test_categoria_desconhecida_e_recusada(self):
        with self.assertRaises(edicao.FalhaDaEdicao):
            edicao.criar_baralho_de_rascunho("Nome", "Grupo", "⭐", "INVENTADA")

    def test_ids_de_card_sao_novos_estaveis_e_nao_reaproveitados(self):
        novo = edicao.criar_baralho_de_rascunho("Tema Piloto", "Especiais", "⭐", "ESPECIAIS")
        novo, primeiro = edicao.acrescentar_card(novo)
        novo, segundo = edicao.acrescentar_card(novo)
        self.assertRegex(primeiro, r"^tema-piloto-c[0-9a-f]{12}$")
        self.assertRegex(segundo, r"^tema-piloto-c[0-9a-f]{12}$")
        self.assertNotEqual(primeiro, segundo)
        novo = edicao.remover_card_do_rascunho(novo, segundo)
        novo, terceiro = edicao.acrescentar_card(novo)
        self.assertNotIn(terceiro, (primeiro, segundo))

    def test_card_novo_nasce_com_dez_dicas_vazias(self):
        novo = edicao.criar_baralho_de_rascunho("Tema Piloto", "Especiais", "⭐", "ESPECIAIS")
        novo, _ = edicao.acrescentar_card(novo)
        self.assertEqual(len(novo["cards"][0]["clues"]), 10)

    def test_teto_de_500_cards(self):
        cards = [card_simples("tp-{:03d}".format(i), "Resposta {}".format(i)) for i in range(499)]
        quase_cheio = baralho("tema-piloto", "Tema Piloto", "EM_DESENVOLVIMENTO", cards)
        cheio, _ = edicao.acrescentar_card(quase_cheio)
        self.assertEqual(len(cheio["cards"]), 500)
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.acrescentar_card(cheio)
        self.assertEqual(contexto.exception.codigo, "teto_de_cards")


def _banco(quantidade, prefixo="fato"):
    return [
        {"id": "{}-{}".format(prefixo, i), "texto": "Texto {} do banco projetado.".format(i)}
        for i in range(quantidade)
    ]


class TesteDeProjecaoDoAcervo(unittest.TestCase):
    """A trilha de projeção (vinda do acervo remoto) PODE mudar o tamanho do
    banco — ao contrário da edição manual — mas nunca a identidade do card.
    """

    def test_projecao_substitui_banco_e_clues_preservando_identidade(self):
        original = baralho_vivo()
        card_original = original["cards"][0]
        banco = _banco(15)
        clues = [fato["texto"] for fato in banco[:10]]
        novo = edicao.aplicar_projecao_no_baralho(
            original, {card_original["id"]: {"bancoDeDicas": banco, "clues": clues}}
        )
        novo_card = novo["cards"][0]
        self.assertEqual(novo_card["id"], card_original["id"])
        self.assertEqual(novo_card["respostaId"], card_original["respostaId"])
        self.assertEqual(novo_card["answer"], card_original["answer"])
        self.assertEqual(len(novo_card["bancoDeDicas"]), 15)
        self.assertEqual(novo_card["clues"], clues)
        # O segundo card, fora da projeção, sai intacto.
        self.assertEqual(novo["cards"][1], original["cards"][1])

    def test_banco_projetado_abaixo_de_dez_e_recusado(self):
        original = baralho_vivo()
        card_original = original["cards"][0]
        banco = _banco(9)
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(
                original,
                {card_original["id"]: {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco[:9]] + ["x"]}},
            )
        self.assertEqual(contexto.exception.codigo, "banco_invalido")

    def test_banco_projetado_acima_de_quinhentos_e_recusado(self):
        original = baralho_vivo()
        card_original = original["cards"][0]
        banco = _banco(501)
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(
                original,
                {card_original["id"]: {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco[:10]]}},
            )
        self.assertEqual(contexto.exception.codigo, "banco_invalido")

    def test_clue_fora_do_banco_e_recusada(self):
        original = baralho_vivo()
        card_original = original["cards"][0]
        banco = _banco(10)
        clues = [f["texto"] for f in banco[:9]] + ["texto que não está no banco"]
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(
                original, {card_original["id"]: {"bancoDeDicas": banco, "clues": clues}}
            )
        self.assertEqual(contexto.exception.codigo, "dica_sem_fato")

    def test_id_de_card_fora_do_baralho_e_recusado(self):
        original = baralho_vivo()
        banco = _banco(10)
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(
                original, {"card-inexistente": {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco[:10]]}}
            )
        self.assertEqual(contexto.exception.codigo, "card_ausente")

    def test_projecao_vazia_e_recusada(self):
        original = baralho_vivo()
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(original, {})
        self.assertEqual(contexto.exception.codigo, "projecao_vazia")

    def test_ids_de_fato_repetidos_no_banco_projetado_sao_recusados(self):
        original = baralho_vivo()
        card_original = original["cards"][0]
        banco = _banco(10)
        banco[1]["id"] = banco[0]["id"]
        with self.assertRaises(edicao.FalhaDaEdicao) as contexto:
            edicao.aplicar_projecao_no_baralho(
                original, {card_original["id"]: {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco[:10]]}}
            )
        self.assertEqual(contexto.exception.codigo, "campo_invalido")


if __name__ == "__main__":
    unittest.main()
