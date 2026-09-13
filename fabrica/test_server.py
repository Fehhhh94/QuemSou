import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import uuid

from server import Fila, processar, validar_baralho


def pedido():
    return dict(id=str(uuid.uuid4()), tema="Cinema", quantidade=10, orientacoes="", feedback="")


def baralho():
    return dict(id="teste", nome="Teste", categoria="PERSONAGEM_FILME", estado="EM_DESENVOLVIMENTO",
                versao=1, colecao=dict(id="cinema", nome="Cinema", icone="C"), cards=[
                    dict(id=f"c{i}", type="PESSOA", answer=f"Pessoa {i}", respostaId=f"pessoa-{i}",
                         clues=[f"Fato {n}" for n in range(10)],
                         bancoDeDicas=[dict(id=f"d{n}", texto=f"Fato {n}") for n in range(60)])
                    for i in range(10)])


class FilaTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.fila = Fila(Path(self.temp.name) / "fila.db")

    def test_idempotencia_e_isolamento(self):
        p = pedido()
        self.fila.receber("ana", p)
        self.fila.receber("ana", p)
        self.assertEqual(len(self.fila.listar("ana")), 1)
        self.assertEqual(self.fila.listar("bia"), [])
        self.assertIsNotNone(self.fila.proximo())
        self.assertIsNone(self.fila.proximo())

    def test_limite_e_recuperacao_nao_reexecutam(self):
        for _ in range(3):
            self.fila.receber("ana", pedido())
        with self.assertRaises(ValueError):
            self.fila.receber("ana", pedido())
        self.fila.proximo()
        self.fila.recuperar_interrompidos()
        self.assertEqual(sum(p["estado"] == "FALHOU" for p in self.fila.listar("ana")), 1)

    def test_pipeline_so_libera_apos_validacao_e_revisao(self):
        p = pedido()
        self.fila.receber("ana", p)
        resultados = iter([baralho(), dict(aprovado=True, problemas=[])])
        processar(self.fila, self.fila.proximo(), lambda *_: next(resultados))
        pronto = self.fila.listar("ana")[0]
        self.assertEqual(pronto["estado"], "PRONTO")
        self.assertEqual(pronto["baralhoId"], "pedido-" + p["id"])
        self.assertEqual(self.fila.resultado("ana", p["id"])["id"], pronto["baralhoId"])
        self.assertIsNone(self.fila.resultado("bia", p["id"]))

    def test_revisao_reprovada_nao_entrega_baralho(self):
        self.fila.receber("ana", pedido())
        resultados = iter([baralho(), dict(aprovado=False, problemas=["Fato duvidoso"])])
        processar(self.fila, self.fila.proximo(), lambda *_: next(resultados))
        self.assertEqual(self.fila.listar("ana")[0]["estado"], "FALHOU")
        self.assertIsNone(self.fila.listar("ana")[0]["baralhoId"])

    def test_dica_repetida_com_pontuacao_e_rejeitada(self):
        b = baralho()
        b["cards"][0]["bancoDeDicas"][1]["texto"] = "FATO 0!!!"
        with self.assertRaises(ValueError):
            validar_baralho(b, 10)

    def test_ampliacao_de_outro_dono_e_recusada(self):
        p = pedido()
        p["baralhoBase"] = "privado-de-outra-pessoa"
        with self.assertRaises(ValueError):
            self.fila.receber("ana", p)

    def test_ampliacao_preserva_identidades_e_acumula_acervo(self):
        p = pedido()
        self.fila.receber("ana", p)
        resultados = iter([baralho(), dict(aprovado=True, problemas=[])])
        processar(self.fila, self.fila.proximo(), lambda *_: next(resultados))
        anterior = self.fila.resultado("ana", p["id"])
        ampliado = json.loads(json.dumps(anterior))
        for card in ampliado["cards"]:
            card["bancoDeDicas"] += [dict(id=f"d{n}", texto=f"Fato {n}") for n in range(60, 120)]
        novo = pedido()
        novo["baralhoBase"] = anterior["id"]
        self.fila.receber("ana", novo)
        resultados = iter([ampliado, dict(aprovado=True, problemas=[])])
        processar(self.fila, self.fila.proximo(), lambda *_: next(resultados))
        recebido = self.fila.resultado("ana", novo["id"])
        self.assertEqual(recebido["versao"], 2)
        self.assertEqual(recebido["id"], anterior["id"])
        for antigo, atual in zip(anterior["cards"], recebido["cards"]):
            self.assertEqual(atual["respostaId"], antigo["respostaId"])
            self.assertEqual(len(atual["bancoDeDicas"]), 120)
            self.assertTrue({d["id"] for d in antigo["bancoDeDicas"]} <= {d["id"] for d in atual["bancoDeDicas"]})
        self.assertEqual(len(self.fila.acervo("ana")), 10)
        self.assertEqual(self.fila.acervo("bia"), [])


if __name__ == "__main__":
    unittest.main()
