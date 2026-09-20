"""Contrato HTTP do editor de acervo: prévia, abrir resposta e editar dicas.

Execução bloqueada nesta sessão (permissão do ambiente recusa
`python -m unittest`); ver `docs/HANDOFF_ACTIVE.md`.
"""
import json
import tempfile
import threading
import unittest
from http.client import HTTPConnection
from pathlib import Path

from apoio_de_teste import montar_ambiente, validador_simulado
from servico import Central
from servidor import criar_servidor
from validacao import Validacoes


def exportacao_de_feedback_do_fato_1():
    contexto = {
        "sessaoId": "nao-expor",
        "rodada": 1,
        "posicao": 2,
        "respostaId": "resposta-de-001",
        "resposta": "Resposta Viva",
        "dicaId": "de-001:f1",
        "texto": "pista 1 do card de-001",
        "versaoDoBaralho": 1,
    }
    return {
        "formato": "quemsou-feedback",
        "versao": 3,
        "exportadoEm": "2026-09-19T12:00:00Z",
        "itens": [
            {
                "baralhoId": "demo-evolucao",
                "cardId": "de-001",
                "resposta": "Resposta Viva",
                "voto": "FRACO",
                "comentario": "Rever a precisão",
                "rodada": 1,
                "resultadoDoTurno": "DICA_REVELADA",
                "numeroDaDicaDoAcerto": None,
                "criadoEm": "2026-09-19T12:00:00Z",
                "contextoJson": json.dumps(contexto),
            }
        ],
    }


class TesteDoEditorDeAcervo(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))
        central = Central(self.config, Validacoes(self.config, executar=validador_simulado()))
        self.servidor, self.token = criar_servidor(self.config, central, porta=0)
        self.porta = self.servidor.server_address[1]
        self.thread = threading.Thread(target=self.servidor.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self._encerrar_servidor)

    def _encerrar_servidor(self):
        self.servidor.shutdown()
        self.thread.join(5)
        self.servidor.server_close()

    def _conexao(self):
        return HTTPConnection("127.0.0.1", self.porta, timeout=10)

    def pedir(self, caminho):
        conexao = self._conexao()
        conexao.request("GET", caminho, headers={})
        resposta = conexao.getresponse()
        corpo = resposta.read().decode("utf-8")
        resposta.close()
        conexao.close()
        return resposta.status, json.loads(corpo) if corpo else None

    def postar(self, caminho, corpo):
        cabecalhos = {"Content-Type": "application/json", "X-Token-Central": self.token}
        conexao = self._conexao()
        conexao.request("POST", caminho, body=json.dumps(corpo).encode("utf-8"), headers=cabecalhos)
        resposta = conexao.getresponse()
        texto = resposta.read().decode("utf-8")
        resposta.close()
        conexao.close()
        return resposta.status, json.loads(texto) if texto else None

    def test_previa_conta_respostas_e_dicas(self):
        status, dados = self.pedir("/api/acervo/previa")
        self.assertEqual(status, 200)
        self.assertGreater(dados["quantidadeDeRespostas"], 0)
        self.assertIn("resposta-de-001", dados["respostas"])

    def test_abrir_resposta_devolve_banco_e_revisao(self):
        status, dados = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        self.assertEqual(status, 200)
        self.assertEqual(len(dados["dicas"]), 13)
        self.assertTrue(dados["revisao"])
        self.assertTrue(dados["prontaParaPublicarPublico"] or dados["prontaParaPublicarPrivado"])

    def test_resposta_inexistente_falha(self):
        status, dados = self.pedir("/api/acervo/resposta?respostaId=nao-existe")
        self.assertEqual(status, 400)
        self.assertEqual(dados["erro"], "resposta_ausente")

    def test_acrescentar_editar_desativar_e_reativar_dica(self):
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        revisao = aberto["revisao"]

        status, apos_acrescentar = self.postar(
            "/api/acervo/dica",
            {
                "respostaId": "resposta-de-001",
                "texto": "Uma dica nova de teste.",
                "escopo": "PRIVADO",
                "revisao": revisao,
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(len(apos_acrescentar["dicas"]), 14)
        novo_id = next(
            dica_id
            for dica_id, dica in apos_acrescentar["dicas"].items()
            if dica["texto"] == "Uma dica nova de teste."
        )
        revisao = apos_acrescentar["revisao"]

        status, apos_editar = self.postar(
            "/api/acervo/dica/texto",
            {
                "respostaId": "resposta-de-001",
                "dicaId": novo_id,
                "texto": "Texto corrigido.",
                "revisao": revisao,
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(apos_editar["dicas"][novo_id]["texto"], "Texto corrigido.")
        self.assertEqual(apos_editar["dicas"][novo_id]["dicaId"], novo_id)
        revisao = apos_editar["revisao"]

        status, apos_desativar = self.postar(
            "/api/acervo/dica/desativar",
            {"respostaId": "resposta-de-001", "dicaId": novo_id, "revisao": revisao},
        )
        self.assertEqual(status, 200)
        self.assertEqual(apos_desativar["dicas"][novo_id]["status"], "REMOVIDA")
        self.assertEqual(apos_desativar["quantidadeDeDicasAtivas"], 13)
        revisao = apos_desativar["revisao"]

        status, apos_reativar = self.postar(
            "/api/acervo/dica/reativar",
            {"respostaId": "resposta-de-001", "dicaId": novo_id, "revisao": revisao},
        )
        self.assertEqual(status, 200)
        self.assertEqual(apos_reativar["dicas"][novo_id]["status"], "ATIVA")
        self.assertEqual(apos_reativar["quantidadeDeDicasAtivas"], 14)

    def test_revisao_obsoleta_e_recusada(self):
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        status, _ = self.postar(
            "/api/acervo/dica",
            {
                "respostaId": "resposta-de-001",
                "texto": "Primeira.",
                "escopo": "PUBLICO",
                "revisao": aberto["revisao"],
            },
        )
        self.assertEqual(status, 200)
        # A mesma revisão (agora desatualizada) é recusada na segunda tentativa.
        status, dados = self.postar(
            "/api/acervo/dica",
            {
                "respostaId": "resposta-de-001",
                "texto": "Segunda.",
                "escopo": "PUBLICO",
                "revisao": aberto["revisao"],
            },
        )
        self.assertEqual(status, 400)
        self.assertEqual(dados["erro"], "revisao_obsoleta")

    def test_feedback_aparece_sob_a_dica_correta(self):
        status, _ = self.postar(
            "/api/feedbacks/importar",
            {"nome": "export.json", "conteudo": exportacao_de_feedback_do_fato_1()},
        )
        self.assertEqual(status, 200)
        status, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        self.assertEqual(status, 200)
        feedback_do_fato = aberto["feedbacks"]["porDica"]["de-001:f1"]
        self.assertEqual(len(feedback_do_fato), 1)
        self.assertEqual(feedback_do_fato[0]["voto"], "FRACO")

    def test_dica_removida_nao_conta_para_publicacao(self):
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        self.assertTrue(aberto["prontaParaPublicarPrivado"])
        revisao = aberto["revisao"]
        for dica_id in list(aberto["dicas"].keys())[:4]:
            _, aberto = self.postar(
                "/api/acervo/dica/desativar",
                {"respostaId": "resposta-de-001", "dicaId": dica_id, "revisao": revisao},
            )
            revisao = aberto["revisao"]
        self.assertEqual(aberto["quantidadeDeDicasAtivas"], 9)
        self.assertFalse(aberto["prontaParaPublicarPrivado"])

    def test_editar_dica_atualiza_texto_e_escopo_numa_unica_chamada(self):
        """Regressão do bug: duas chamadas sequenciais com a MESMA revisão
        sempre deixavam a segunda falhar (revisao_obsoleta) depois que a
        primeira já tivesse avançado a revisão, resultando em salvamento
        parcial. Uma única chamada atômica nunca tem esse problema.
        """
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        dica_id = next(iter(aberto["dicas"]))
        status, resultado = self.postar(
            "/api/acervo/dica/editar",
            {
                "respostaId": "resposta-de-001",
                "dicaId": dica_id,
                "texto": "Texto e escopo mudando juntos.",
                "escopo": "PUBLICO",
                "revisao": aberto["revisao"],
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(resultado["dicas"][dica_id]["texto"], "Texto e escopo mudando juntos.")
        self.assertEqual(resultado["dicas"][dica_id]["escopo"], "PUBLICO")

    def test_editar_dica_sem_mudar_nada_nao_falha(self):
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        dica_id = next(iter(aberto["dicas"]))
        texto_atual = aberto["dicas"][dica_id]["texto"]
        escopo_atual = aberto["dicas"][dica_id]["escopo"]
        status, resultado = self.postar(
            "/api/acervo/dica/editar",
            {
                "respostaId": "resposta-de-001",
                "dicaId": dica_id,
                "texto": texto_atual,
                "escopo": escopo_atual,
                "revisao": aberto["revisao"],
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(resultado["dicas"][dica_id]["revisaoTecnica"], 1)

    def test_previa_exclui_tecnico_por_padrao_e_inclui_com_query(self):
        status, padrao = self.pedir("/api/acervo/previa")
        self.assertEqual(status, 200)
        self.assertFalse(padrao["incluiTecnicos"])
        status, com_tecnicos = self.pedir("/api/acervo/previa?tecnicos=1")
        self.assertEqual(status, 200)
        self.assertTrue(com_tecnicos["incluiTecnicos"])
        self.assertGreaterEqual(
            com_tecnicos["quantidadeDeRespostas"], padrao["quantidadeDeRespostas"]
        )

    def test_escopo_padrao_para_nova_dica_vem_na_resposta_aberta(self):
        _, aberto = self.pedir("/api/acervo/resposta?respostaId=resposta-de-001")
        self.assertIn("escopoPadraoParaNovaDica", aberto)
        self.assertIn(aberto["escopoPadraoParaNovaDica"], ("PUBLICO", "PRIVADO"))

    def test_projetar_do_acervo_substitui_banco_e_clues_do_card(self):
        status, aberto_do_baralho = self.pedir("/api/baralho?chave=asset:demo-evolucao")
        self.assertEqual(status, 200)
        banco = [
            {"id": "novo-fato-{}".format(i), "texto": "Novo texto projetado {}.".format(i), "escopo": "PUBLICO"}
            for i in range(12)
        ]
        clues = [fato["texto"] for fato in banco[:10]]
        status, resultado = self.postar(
            "/api/acervo/projetar",
            {
                "chave": "asset:demo-evolucao",
                "projecoes": {"de-001": {"bancoDeDicas": banco, "clues": clues}},
                "revisao": aberto_do_baralho["revisao"],
            },
        )
        self.assertEqual(status, 200)
        card_projetado = next(c for c in resultado["baralho"]["cards"] if c["id"] == "de-001")
        self.assertEqual(len(card_projetado["bancoDeDicas"]), 12)
        self.assertEqual(card_projetado["clues"], clues)
        self.assertEqual(card_projetado["respostaId"], "resposta-de-001")

    def test_projetar_do_acervo_recusa_card_fora_do_baralho(self):
        status, aberto_do_baralho = self.pedir("/api/baralho?chave=asset:demo-evolucao")
        self.assertEqual(status, 200)
        banco = [{"id": "f{}".format(i), "texto": "Texto {}".format(i), "escopo": "PUBLICO"} for i in range(10)]
        status, dados = self.postar(
            "/api/acervo/projetar",
            {
                "chave": "asset:demo-evolucao",
                "projecoes": {"card-inexistente": {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco]}},
                "revisao": aberto_do_baralho["revisao"],
            },
        )
        self.assertEqual(status, 400)
        self.assertEqual(dados["erro"], "card_ausente")


if __name__ == "__main__":
    unittest.main()
