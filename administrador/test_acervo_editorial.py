"""Acervo editorial: identidade legado, migração idempotente e edição de dicas.

Testes isolados com dados fictícios; não dependem de conteúdo editorial real.
"""
import json
import tempfile
import unittest
from pathlib import Path

import acervo_editorial as acervo
from apoio_de_teste import baralho, montar_ambiente
from config import Configuracao
from fontes import normalizar



class TesteDeIdentidadeLegado(unittest.TestCase):
    """A identidade de alias tem que bater com `SelecionadorDeDicas` do app."""

    def test_alias_de_resposta_e_o_nome_normalizado(self):
        self.assertEqual(acervo.alias_de_resposta("Pessoa Inventada"), normalizar("Pessoa Inventada"))

    def test_alias_de_dica_usa_prefixo_legado(self):
        texto = "Uma pista fictícia com acentuação e pontuação."
        self.assertEqual(acervo.alias_de_dica(texto), "legado:{}".format(normalizar(texto)))

    def test_resposta_sem_texto_falha(self):
        with self.assertRaises(acervo.FalhaDoAcervo) as contexto:
            acervo.alias_de_resposta("   ")
        self.assertEqual(contexto.exception.codigo, "resposta_sem_texto")

    def test_card_ficticio_legado_preserva_identidade_de_feedback(self):
        card = {"id": "demo-028", "answer": "Pessoa Inventada",
                "clues": ["Fato sintético número {}.".format(i) for i in range(10)]}
        resposta_id, origem = acervo._identidade_do_card(card)
        self.assertEqual(resposta_id, "pessoa inventada")
        self.assertEqual(origem, acervo.ORIGEM_ALIAS_LEGADO)
        fatos = acervo._fatos_do_card(card)
        self.assertEqual(len(fatos), 10)
        self.assertEqual(fatos[9][0], "legado:" + normalizar(card["clues"][9]))
        self.assertEqual(fatos[9][1], card["clues"][9])


class TesteDeEscopo(unittest.TestCase):
    def test_especiais_e_privado_por_padrao(self):
        alvo = baralho("especial-1", "Especial fictício", "EM_DESENVOLVIMENTO", [], categoria="ESPECIAIS")
        self.assertEqual(acervo.escopo_do_baralho(alvo), acervo.ESCOPO_PRIVADO)

    def test_demais_categorias_sao_publicas(self):
        alvo = baralho("cc-1", "Cinema", "EM_DESENVOLVIMENTO", [], categoria="PERSONAGEM_FILME")
        self.assertEqual(acervo.escopo_do_baralho(alvo), acervo.ESCOPO_PUBLICO)


class TesteDeMigracao(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))

    def test_migracao_agrega_respostas_editoriais_e_aliases(self):
        previa = acervo.gerar_previa_da_migracao(self.config)
        self.assertIn("resposta-de-001", previa["respostas"])
        self.assertEqual(previa["respostas"]["resposta-de-001"]["origem"], acervo.ORIGEM_EDITORIAL)
        self.assertEqual(len(previa["dicas"]["resposta-de-001"]), 13)

        alias_final = acervo.alias_de_resposta("Resposta Final")
        self.assertIn(alias_final, previa["respostas"])
        self.assertEqual(previa["respostas"][alias_final]["origem"], acervo.ORIGEM_ALIAS_LEGADO)
        self.assertEqual(len(previa["dicas"][alias_final]), 10)

    def test_migracao_e_idempotente(self):
        primeira = acervo.gerar_previa_da_migracao(self.config)
        segunda = acervo.gerar_previa_da_migracao(self.config)
        self.assertTrue(acervo.execucoes_equivalentes(primeira, segunda))
        self.assertEqual(primeira["conflitos"], segunda["conflitos"])

    def test_copia_legada_do_mesmo_texto_vira_conflito_de_duplicata(self):
        """demo-evolucao existe no asset (com banco, respostaId explícito) e,
        só o card de-001 sem banco, no catálogo — mesmo texto de resposta
        ("Resposta Viva"), duas identidades desconectadas (`resposta-de-001`
        explícito vs. alias `resposta viva`). Isso NUNCA deve virar dois
        bancos paralelos silenciosos: precisa aparecer como conflito para
        decisão humana (dar respostaId explícito ao card legado, por ex.).
        """
        previa = acervo.gerar_previa_da_migracao(self.config)
        alias_de_resposta_viva = acervo.alias_de_resposta("Resposta Viva")
        duplicatas = [
            item for item in previa["conflitos"] if item.get("tipo") == "alias_e_editorial_duplicados"
        ]
        self.assertTrue(duplicatas, "Esperava um conflito de alias/editorial duplicados.")
        ids_relatados = duplicatas[0]["respostaIds"]
        self.assertIn("resposta-de-001", ids_relatados)
        self.assertIn(alias_de_resposta_viva, ids_relatados)
        self.assertFalse(previa["segura"])

    def test_resposta_divergente_gera_conflito_sem_sobrescrever(self):
        raiz = Path(self.temporario.name) / "conflito"
        app = raiz / "app"
        catalogo = raiz / "catalogo"
        privado = raiz / "privado"
        (privado / "origens").mkdir(parents=True)
        (privado / "origens" / "embarcados-legado.json").write_text(
            json.dumps(
                {
                    "version": 1,
                    "baralhos": [
                        baralho(
                            "b1",
                            "B1",
                            "EM_DESENVOLVIMENTO",
                            [
                                {
                                    "id": "c1",
                                    "type": "PESSOA",
                                    "answer": "Nome Um",
                                    "respostaId": "resposta-compartilhada",
                                    "clues": ["x"] * 10,
                                    "bancoDeDicas": [
                                        {"id": "fato-1", "texto": "Primeiro texto do fato."}
                                    ],
                                }
                            ],
                        )
                    ],
                }
            ),
            encoding="utf-8",
        )
        (catalogo / "baralhos").mkdir(parents=True)
        (catalogo / "baralhos" / "b2.json").write_text(
            json.dumps(
                baralho(
                    "b2",
                    "B2",
                    "EM_DESENVOLVIMENTO",
                    [
                        {
                            "id": "c2",
                            "type": "PESSOA",
                            "answer": "Nome Dois",
                            "respostaId": "resposta-compartilhada",
                            "clues": ["y"] * 10,
                            "bancoDeDicas": [
                                {"id": "fato-1", "texto": "Segundo texto divergente do fato."}
                            ],
                        }
                    ],
                )
            ),
            encoding="utf-8",
        )
        (catalogo / "indice.json").write_text(json.dumps({"baralhos": []}), encoding="utf-8")
        app.mkdir(parents=True, exist_ok=True)
        (app / "gradlew.bat").write_text("@echo off\n", encoding="utf-8")

        config = Configuracao(
            raiz_do_app=app, raiz_do_catalogo=catalogo, diretorio_privado=privado, porta=0
        )
        config.preparar_diretorio_privado()

        previa = acervo.gerar_previa_da_migracao(config)
        tipos = {c["tipo"] for c in previa["conflitos"]}
        self.assertIn("resposta_divergente", tipos)
        self.assertIn("dica_divergente", tipos)
        # O primeiro texto encontrado prevalece; nada é sobrescrito sem revisão humana.
        self.assertEqual(previa["respostas"]["resposta-compartilhada"]["texto"], "Nome Um")
        self.assertEqual(
            previa["dicas"]["resposta-compartilhada"]["fato-1"]["texto"],
            "Primeiro texto do fato.",
        )

    def test_baralho_tecnico_fica_de_fora_por_padrao(self):
        previa = acervo.gerar_previa_da_migracao(self.config)
        alias_tecnico = acervo.alias_de_resposta("Resposta Técnica")
        self.assertNotIn(alias_tecnico, previa["respostas"])
        self.assertFalse(previa["incluiTecnicos"])

    def test_baralho_tecnico_entra_com_opt_in(self):
        previa = acervo.gerar_previa_da_migracao(self.config, incluir_tecnicos=True)
        alias_tecnico = acervo.alias_de_resposta("Resposta Técnica")
        self.assertIn(alias_tecnico, previa["respostas"])
        self.assertTrue(previa["incluiTecnicos"])

    def test_avisos_de_origem_ausente_aparecem_sem_derrubar_a_previa(self):
        config = montar_ambiente(Path(self.temporario.name) / "sem-catalogo", com_catalogo=False)
        previa = acervo.gerar_previa_da_migracao(config)
        self.assertGreater(previa["quantidadeDeRespostas"], 0)
        # A pasta do catálogo não existe nesta configuração: isso vira aviso.
        self.assertTrue(previa["avisos"], "Esperava um aviso sobre a origem ausente.")


class TesteDeIdDoDocumento(unittest.TestCase):
    def test_codec_uniforme_sem_colisoes_e_reversivel(self):
        ids = ("resposta-de-001", "de-001:f1", "legado:shakira uma indireta", ".", "_.", "..", "_..", "ação / 100%", "__reservado__")
        self.assertEqual(len({acervo.id_do_documento(i) for i in ids}), len(ids))
        for id_logico in ids:
            self.assertEqual(acervo.id_logico_do_documento(acervo.id_do_documento(id_logico)), id_logico)
        self.assertEqual(acervo.id_do_documento("ação / 100%"), "id_YcOnw6NvIC8gMTAwJQ")

    def test_barra_e_reversivel(self):
        original = "algo/com/barra"
        codificado = acervo.id_do_documento(original)
        self.assertNotIn("/", codificado)
        self.assertEqual(acervo.id_logico_do_documento(codificado), original)

    def test_percentual_e_reversivel(self):
        original = "100% garantido"
        codificado = acervo.id_do_documento(original)
        self.assertEqual(acervo.id_logico_do_documento(codificado), original)

    def test_ids_reservados_sao_reversiveis(self):
        for original in (".", "..", "__reservado__"):
            codificado = acervo.id_do_documento(original)
            self.assertNotEqual(codificado, original)
            self.assertEqual(acervo.id_logico_do_documento(codificado), original)


class TesteDeEscopoPadraoParaNovaDica(unittest.TestCase):
    def _dica(self, escopo):
        return {"escopo": escopo, "status": acervo.STATUS_ATIVA}

    def test_sem_dicas_usa_publico(self):
        self.assertEqual(acervo.escopo_padrao_para_nova_dica({}), acervo.ESCOPO_PUBLICO)

    def test_maioria_privada_usa_privado(self):
        dicas = {"a": self._dica(acervo.ESCOPO_PRIVADO), "b": self._dica(acervo.ESCOPO_PUBLICO),
                  "c": self._dica(acervo.ESCOPO_PRIVADO)}
        self.assertEqual(acervo.escopo_padrao_para_nova_dica(dicas), acervo.ESCOPO_PRIVADO)

    def test_empate_usa_privado_por_seguranca(self):
        dicas = {"a": self._dica(acervo.ESCOPO_PRIVADO), "b": self._dica(acervo.ESCOPO_PUBLICO)}
        self.assertEqual(acervo.escopo_padrao_para_nova_dica(dicas), acervo.ESCOPO_PRIVADO)

    def test_so_publicas_usa_publico(self):
        dicas = {"a": self._dica(acervo.ESCOPO_PUBLICO)}
        self.assertEqual(acervo.escopo_padrao_para_nova_dica(dicas), acervo.ESCOPO_PUBLICO)


class TesteDeRespostasSegurasParaMigrar(unittest.TestCase):
    def test_exclui_respostas_com_conflito_e_banco_acima_do_limite(self):
        previa = {
            "respostas": {"a": {}, "b": {}, "c": {}},
            "conflitos": [{"respostaId": "a", "tipo": "x"}],
            "bancosAcimaDoLimite": [{"respostaId": "b", "quantidade": 501}],
        }
        self.assertEqual(acervo.respostas_seguras_para_migrar(previa), ["c"])


class TesteDePublicacaoPorEscopo(unittest.TestCase):
    def _dica(self, dica_id, texto, escopo, status=acervo.STATUS_ATIVA):
        return {
            "dicaId": dica_id,
            "texto": texto,
            "escopo": escopo,
            "status": status,
            "revisaoTecnica": 1,
        }

    def test_publico_nunca_recebe_dica_privada(self):
        dicas = {
            "d1": self._dica("d1", "Pública 1", acervo.ESCOPO_PUBLICO),
            "d2": self._dica("d2", "Privada 1", acervo.ESCOPO_PRIVADO),
        }
        selecionadas = acervo.dicas_para_publicacao(dicas, acervo.ESCOPO_PUBLICO)
        self.assertEqual([d["dicaId"] for d in selecionadas], ["d1"])

    def test_privado_recebe_publicas_e_privadas(self):
        dicas = {
            "d1": self._dica("d1", "Pública 1", acervo.ESCOPO_PUBLICO),
            "d2": self._dica("d2", "Privada 1", acervo.ESCOPO_PRIVADO),
        }
        selecionadas = acervo.dicas_para_publicacao(dicas, acervo.ESCOPO_PRIVADO)
        self.assertEqual({d["dicaId"] for d in selecionadas}, {"d1", "d2"})

    def test_dica_removida_nunca_e_publicada(self):
        dicas = {"d1": self._dica("d1", "Removida", acervo.ESCOPO_PUBLICO, acervo.STATUS_REMOVIDA)}
        self.assertEqual(acervo.dicas_para_publicacao(dicas, acervo.ESCOPO_PUBLICO), [])

    def test_texto_duplicado_normalizado_conta_uma_vez(self):
        dicas = {
            "d1": self._dica("d1", "É Verde!", acervo.ESCOPO_PUBLICO),
            "d2": self._dica("d2", "e verde", acervo.ESCOPO_PUBLICO),
        }
        selecionadas = acervo.dicas_para_publicacao(dicas, acervo.ESCOPO_PUBLICO)
        self.assertEqual(len(selecionadas), 1)

    def test_pronta_para_publicar_exige_minimo_de_dez(self):
        dicas = {
            "d{}".format(i): self._dica("d{}".format(i), "Texto {}".format(i), acervo.ESCOPO_PUBLICO)
            for i in range(9)
        }
        self.assertFalse(acervo.pronta_para_publicar(dicas, acervo.ESCOPO_PUBLICO))
        dicas["d9"] = self._dica("d9", "Texto 9", acervo.ESCOPO_PUBLICO)
        self.assertTrue(acervo.pronta_para_publicar(dicas, acervo.ESCOPO_PUBLICO))

    def test_pronta_para_publicar_recusa_acima_de_quinhentas(self):
        dicas = {
            "d{}".format(i): self._dica("d{}".format(i), "Texto único {}".format(i), acervo.ESCOPO_PUBLICO)
            for i in range(500)
        }
        self.assertTrue(acervo.pronta_para_publicar(dicas, acervo.ESCOPO_PUBLICO))
        dicas["d500"] = self._dica("d500", "Texto único 500", acervo.ESCOPO_PUBLICO)
        self.assertFalse(acervo.pronta_para_publicar(dicas, acervo.ESCOPO_PUBLICO))


class TesteDeEdicaoDeDica(unittest.TestCase):
    def test_acrescentar_dica_gera_id_estavel_e_sem_colisao(self):
        existentes = {"d1": {"dicaId": "resposta-x-dABC123"}}
        nova = acervo.acrescentar_dica("resposta-x", existentes, "Uma dica nova.", acervo.ESCOPO_PUBLICO)
        self.assertTrue(nova["dicaId"].startswith("resposta-x-d"))
        self.assertNotEqual(nova["dicaId"], "resposta-x-dABC123")
        self.assertEqual(nova["status"], acervo.STATUS_ATIVA)
        self.assertEqual(nova["revisaoTecnica"], 1)

    def test_acrescentar_dica_respeita_teto_de_quinhentas(self):
        existentes = {"d{}".format(i): {"dicaId": "d{}".format(i)} for i in range(500)}
        with self.assertRaises(acervo.FalhaDoAcervo) as contexto:
            acervo.acrescentar_dica("resposta-x", existentes, "Mais uma.", acervo.ESCOPO_PUBLICO)
        self.assertEqual(contexto.exception.codigo, "teto_de_dicas")

    def test_texto_vazio_e_recusado(self):
        with self.assertRaises(acervo.FalhaDoAcervo) as contexto:
            acervo.acrescentar_dica("resposta-x", {}, "   ", acervo.ESCOPO_PUBLICO)
        self.assertEqual(contexto.exception.codigo, "campo_invalido")

    def test_texto_acima_do_limite_e_recusado(self):
        with self.assertRaises(acervo.FalhaDoAcervo):
            acervo.acrescentar_dica("resposta-x", {}, "a" * 501, acervo.ESCOPO_PUBLICO)

    def test_editar_texto_mantem_id_e_incrementa_revisao(self):
        original = acervo.acrescentar_dica("resposta-x", {}, "Texto original.", acervo.ESCOPO_PUBLICO)
        editada = acervo.editar_texto_da_dica(original, "Texto corrigido.")
        self.assertEqual(editada["dicaId"], original["dicaId"])
        self.assertEqual(editada["texto"], "Texto corrigido.")
        self.assertEqual(editada["revisaoTecnica"], original["revisaoTecnica"] + 1)

    def test_desativar_e_reativar_preserva_id_e_e_recuperavel(self):
        original = acervo.acrescentar_dica("resposta-x", {}, "Texto.", acervo.ESCOPO_PUBLICO)
        desativada = acervo.desativar_dica(original)
        self.assertEqual(desativada["status"], acervo.STATUS_REMOVIDA)
        self.assertEqual(desativada["dicaId"], original["dicaId"])
        reativada = acervo.reativar_dica(desativada)
        self.assertEqual(reativada["status"], acervo.STATUS_ATIVA)
        self.assertEqual(reativada["dicaId"], original["dicaId"])
        self.assertEqual(reativada["revisaoTecnica"], original["revisaoTecnica"] + 2)

    def test_nao_pode_editar_texto_de_dica_removida_sem_reativar(self):
        original = acervo.acrescentar_dica("resposta-x", {}, "Texto.", acervo.ESCOPO_PUBLICO)
        desativada = acervo.desativar_dica(original)
        with self.assertRaises(acervo.FalhaDoAcervo) as contexto:
            acervo.editar_texto_da_dica(desativada, "Outro texto.")
        self.assertEqual(contexto.exception.codigo, "dica_removida")


if __name__ == "__main__":
    unittest.main()
