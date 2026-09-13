"""Contrato HTTP real em loopback; geração simulada, sem Codex ou rede externa."""
import concurrent.futures
import hashlib
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
import tempfile
import threading
import unittest

from server import Fila, FalhaDaFabrica, handler, processar
from test_server import pedido, baralho


class HttpTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.fila = Fila(Path(self.temp.name) / "fila.db")
        self.tokens = {"a": "a" * 43, "b": "b" * 43}
        clientes = {hashlib.sha256(t.encode()).hexdigest(): d for d, t in self.tokens.items()}
        self.http = ThreadingHTTPServer(("127.0.0.1", 0), handler(self.fila, clientes))
        self.thread = threading.Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self.fechar)

    def fechar(self):
        self.http.shutdown()
        self.http.server_close()
        self.thread.join()

    def request(self, path="/pedidos", body=None, dono="a", bearer=True):
        conn = HTTPConnection(*self.http.server_address, timeout=5)
        token = self.tokens.get(dono, "invalido")
        headers = {"Authorization": ("Bearer " if bearer else "") + token}
        try:
            conn.request("POST" if body is not None else "GET", path,
                         body=json.dumps(body).encode() if body is not None else None, headers=headers)
            resp = conn.getresponse()
            return resp.status, json.loads(resp.read())
        finally:
            conn.close()

    def test_autenticacao_e_resultado_privado(self):
        self.assertEqual(self.request(dono="invalido")[0], 401)
        self.assertEqual(self.request(bearer=False)[0], 401)
        p = pedido()
        self.assertEqual(self.request(body=p)[0], 202)
        resultados = iter([baralho(), {"aprovado": True, "problemas": []}])
        processar(self.fila, self.fila.proximo(), lambda *_: next(resultados))
        self.assertEqual(self.request(f"/pedidos/{p['id']}/baralho")[0], 200)
        self.assertEqual(self.request(f"/pedidos/{p['id']}/baralho", dono="b")[0], 404)
        self.assertEqual(self.request(dono="b")[1], [])

    def test_reenvio_igual_idempotente_mas_payload_diferente_recusado(self):
        p = pedido()
        self.assertEqual(self.request(body=p)[0], 202)
        self.assertEqual(self.request(body=p)[0], 202)
        p["tema"] = "Outro tema"
        self.assertEqual(self.request(body=p), (409, {"erro": "ID_EM_CONFLITO"}))
        self.assertEqual(len(self.request()[1]), 1)

    def test_pedido_invalido_nao_derruba_servidor(self):
        for invalido in [[], {}, {**pedido(), "quantidade": True}, {**pedido(), "quantidade": 999}]:
            self.assertEqual(self.request(body=invalido)[0], 400)
        self.assertEqual(self.request()[0], 200)

    def test_pedido_antigo_nao_some_apos_vinte_novos(self):
        ids = []
        for _ in range(25):
            p = pedido()
            ids.append(p["id"])
            self.fila.receber("a", p)
            self.fila.estado("a", p["id"], "FALHOU", erro="INTERROMPIDO")
        self.assertEqual({p["id"] for p in self.request()[1]}, set(ids))

    def test_limite_de_fila_e_atomico_em_concorrencia(self):
        def enviar(_):
            try:
                self.fila.receber("a", pedido())
                return True
            except FalhaDaFabrica:
                return False
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            self.assertEqual(sum(executor.map(enviar, range(8))), 3)

    def test_ampliacao_480_ate_500_e_limite_explicito(self):
        p = pedido()
        self.fila.receber("a", p)
        antigo = baralho()
        antigo["id"] = "pedido-" + p["id"]
        for c in antigo["cards"]:
            c["bancoDeDicas"] = [{"id": f"d{n}", "texto": f"Fato {n}"} for n in range(480)]
        self.fila.estado("a", p["id"], "PRONTO", antigo)
        novo = {**pedido(), "baralhoBase": antigo["id"]}
        self.fila.receber("a", novo)
        ampliado = json.loads(json.dumps(antigo))
        for c in ampliado["cards"]:
            c["bancoDeDicas"] += [{"id": f"d{n}", "texto": f"Fato {n}"} for n in range(480, 500)]
        respostas = iter([ampliado, {"aprovado": True, "problemas": []}])
        processar(self.fila, self.fila.proximo(), lambda *_: next(respostas))
        resultado = self.fila.resultado("a", novo["id"])
        self.assertEqual(len(resultado["cards"][0]["bancoDeDicas"]), 500)
        self.assertEqual(self.request(body={**pedido(), "baralhoBase": antigo["id"]}),
                         (409, {"erro": "LIMITE_DE_DICAS"}))
        self.assertFalse(next(p for p in self.request()[1] if p["id"] == novo["id"])["podeAmpliar"])

    def test_falha_do_executor_e_interrupcao_nao_entregam_conteudo(self):
        p = pedido()
        self.fila.receber("a", p)
        def indisponivel(*_):
            raise FileNotFoundError("não deve aparecer na resposta HTTP")
        processar(self.fila, self.fila.proximo(), indisponivel)
        item = self.request()[1][0]
        self.assertEqual(item["erro"], "GERADOR_INDISPONIVEL")
        self.assertEqual(self.request(f"/pedidos/{p['id']}/baralho")[0], 404)


if __name__ == "__main__":
    unittest.main()
