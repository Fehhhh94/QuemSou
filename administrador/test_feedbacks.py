"""Importacao privada, sanitizacao e associacao dos feedbacks por dica."""
import json
import tempfile
import unittest
from pathlib import Path

from apoio_de_teste import montar_ambiente
from feedbacks import ArmazemDeFeedbacks, FalhaDosFeedbacks


def exportacao(dica_id="de-001:f1", texto="pista 1 do card de-001"):
    contexto = {
        "sessaoId": "segredo-da-sessao",
        "rodada": 2,
        "posicao": 4,
        "respostaId": "resposta-de-001",
        "resposta": "Resposta Viva",
        "dicaId": dica_id,
        "texto": texto,
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
                "comentario": "Ficou ambígua",
                "rodada": 2,
                "resultadoDoTurno": "DICA_REVELADA",
                "numeroDaDicaDoAcerto": None,
                "criadoEm": "2026-09-19T11:59:00Z",
                "contextoJson": json.dumps(contexto, ensure_ascii=False),
            },
            {
                "baralhoId": "demo-evolucao",
                "cardId": "de-001",
                "resposta": "Resposta Viva",
                "voto": "BOM",
                "comentario": None,
                "rodada": 2,
                "resultadoDoTurno": "ACERTO",
                "numeroDaDicaDoAcerto": 3,
                "criadoEm": "2026-09-19T12:00:00Z",
                "contextoJson": "{}",
            },
        ],
    }


class TesteDeFeedbacks(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))
        self.armazem = ArmazemDeFeedbacks(self.config.pasta_de_feedbacks)

    def test_importa_sanitiza_e_nao_duplica_a_mesma_lista(self):
        primeira = self.armazem.importar("pasta\\lista-1.json", exportacao())
        segunda = self.armazem.importar("outro-nome.json", exportacao())
        self.assertEqual(primeira["nome"], "lista-1.json")
        self.assertEqual(primeira["quantidadeDeDicas"], 1)
        self.assertEqual(primeira["quantidadeIgnorada"], 1)
        self.assertFalse(primeira["repetida"])
        self.assertTrue(segunda["repetida"])
        arquivos = list(self.config.pasta_de_feedbacks.glob("*.json"))
        self.assertEqual(len(arquivos), 1)
        self.assertNotIn("segredo-da-sessao", arquivos[0].read_text(encoding="utf-8"))

    def test_associa_pelo_id_estavel_mesmo_com_texto_da_dica_revisado(self):
        self.armazem.importar("rodada-financas.json", exportacao())
        envelope = json.loads(self.config.arquivo_do_asset.read_text(encoding="utf-8"))
        baralho = next(item for item in envelope["baralhos"] if item["id"] == "demo-evolucao")
        baralho["cards"][0]["clues"][0] = "texto revisado"
        baralho["cards"][0]["bancoDeDicas"][0]["texto"] = "texto revisado"
        resultado = self.armazem.para_baralho(baralho)
        feedback = resultado["porCard"]["de-001"][0][0]
        self.assertEqual(feedback["listaNome"], "rodada-financas.json")
        self.assertEqual(feedback["comentario"], "Ficou ambígua")
        self.assertEqual(resultado["quantidadeNasDicas"], 1)

    def test_associa_card_legado_pelo_texto(self):
        dados = exportacao("legado:pista 1 do card de-002", "pista 1 do card de-002")
        dados["itens"][0]["cardId"] = "de-002"
        self.armazem.importar("legado.json", dados)
        envelope = json.loads(self.config.arquivo_do_asset.read_text(encoding="utf-8"))
        baralho = next(item for item in envelope["baralhos"] if item["id"] == "demo-evolucao")
        resultado = self.armazem.para_baralho(baralho)
        self.assertEqual(resultado["porCard"]["de-002"][0][0]["voto"], "FRACO")

    def test_recusa_versao_sem_contexto_de_dica(self):
        dados = exportacao()
        dados["versao"] = 2
        with self.assertRaises(FalhaDosFeedbacks) as contexto:
            self.armazem.importar("antigo.json", dados)
        self.assertEqual(contexto.exception.codigo, "formato_de_feedback_incompativel")

    def _exportacao_para(self, resposta_id, dica_id, texto, comentario):
        contexto = {
            "sessaoId": "segredo",
            "rodada": 1,
            "posicao": 0,
            "respostaId": resposta_id,
            "resposta": resposta_id,
            "dicaId": dica_id,
            "texto": texto,
            "versaoDoBaralho": 1,
        }
        return {
            "formato": "quemsou-feedback",
            "versao": 3,
            "exportadoEm": "2026-09-19T12:00:00Z",
            "itens": [
                {
                    "baralhoId": "b",
                    "cardId": "c",
                    "resposta": resposta_id,
                    "voto": "FRACO",
                    "comentario": comentario,
                    "rodada": 1,
                    "resultadoDoTurno": "DICA_REVELADA",
                    "numeroDaDicaDoAcerto": None,
                    "criadoEm": "2026-09-19T12:00:00Z",
                    "contextoJson": json.dumps(contexto, ensure_ascii=False),
                }
            ],
        }

    def test_para_dicas_isola_por_resposta_mesmo_com_dicaid_repetido(self):
        """Um dicaId explícito ("fato-1") só é único DENTRO de uma resposta."""
        self.armazem.importar(
            "a.json", self._exportacao_para("resposta-a", "fato-1", "Texto A", "Sobre A")
        )
        self.armazem.importar(
            "b.json", self._exportacao_para("resposta-b", "fato-1", "Texto B", "Sobre B")
        )
        de_a = self.armazem.para_dicas("resposta-a", ["fato-1"])
        de_b = self.armazem.para_dicas("resposta-b", ["fato-1"])
        self.assertEqual(len(de_a["porDica"]["fato-1"]), 1)
        self.assertEqual(de_a["porDica"]["fato-1"][0]["comentario"], "Sobre A")
        self.assertEqual(len(de_b["porDica"]["fato-1"]), 1)
        self.assertEqual(de_b["porDica"]["fato-1"][0]["comentario"], "Sobre B")

    def test_para_dicas_sem_resposta_id_usa_referencia_exata(self):
        dados = exportacao()
        contexto = json.loads(dados["itens"][0]["contextoJson"])
        contexto.pop("respostaId")
        dados["itens"][0]["contextoJson"] = json.dumps(contexto)
        self.armazem.importar("legado-sem-resposta.json", dados)
        referencias = [{"baralhoId": "demo-evolucao", "cardId": "de-001"}]
        certo = self.armazem.para_dicas("resposta-de-001", ["de-001:f1"], referencias)
        self.assertEqual(len(certo["porDica"]["de-001:f1"]), 1)
        errado = self.armazem.para_dicas("outra", ["de-001:f1"], [{"baralhoId": "b", "cardId": "c"}])
        self.assertEqual(errado["porDica"]["de-001:f1"], [])

    def test_para_dicas_isola_alias_legado_repetido_entre_respostas(self):
        """Duas respostas diferentes podem ter uma dica com o MESMO texto
        normalizado, virando o mesmo `legado:<texto>` em identidades
        (`respostaId`) diferentes — o filtro por resposta evita a mistura.
        """
        dica_id = "legado:mesmo texto"
        self.armazem.importar(
            "x.json", self._exportacao_para("resposta-x", dica_id, "Mesmo texto", "Sobre X")
        )
        self.armazem.importar(
            "y.json", self._exportacao_para("resposta-y", dica_id, "Mesmo texto", "Sobre Y")
        )
        de_x = self.armazem.para_dicas("resposta-x", [dica_id])
        de_y = self.armazem.para_dicas("resposta-y", [dica_id])
        self.assertEqual([item["comentario"] for item in de_x["porDica"][dica_id]], ["Sobre X"])
        self.assertEqual([item["comentario"] for item in de_y["porDica"][dica_id]], ["Sobre Y"])


if __name__ == "__main__":
    unittest.main()
