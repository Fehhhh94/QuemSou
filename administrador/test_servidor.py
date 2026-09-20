"""Contrato HTTP real em loopback: Host, Origin, CSRF, corpo e estaticos."""
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


def exportacao_de_feedback():
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


class TesteDoServidor(unittest.TestCase):
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

    def pedir(self, caminho, cabecalhos=None):
        conexao = self._conexao()
        conexao.request("GET", caminho, headers=cabecalhos or {})
        resposta = conexao.getresponse()
        corpo = resposta.read().decode("utf-8")
        resposta.close()
        conexao.close()
        return resposta.status, corpo, resposta

    def postar(self, caminho, corpo, cabecalhos=None):
        padrao = {"Content-Type": "application/json", "X-Token-Central": self.token}
        padrao.update(cabecalhos or {})
        conexao = self._conexao()
        conexao.request("POST", caminho, body=json.dumps(corpo).encode("utf-8"), headers=padrao)
        resposta = conexao.getresponse()
        texto = resposta.read().decode("utf-8")
        resposta.close()
        conexao.close()
        return resposta.status, texto

    def postar_com_resposta(self, caminho, corpo):
        cabecalhos = {"Content-Type": "application/json", "X-Token-Central": self.token}
        conexao = self._conexao()
        conexao.request(
            "POST", caminho, body=json.dumps(corpo).encode("utf-8"), headers=cabecalhos
        )
        resposta = conexao.getresponse()
        texto = resposta.read().decode("utf-8")
        resposta.close()
        conexao.close()
        return resposta.status, texto, resposta

    def abrir(self, chave):
        status, corpo, _ = self.pedir("/api/baralho?chave=" + chave)
        self.assertEqual(status, 200)
        return json.loads(corpo)

    # ---- estaticos ------------------------------------------------------

    def test_pagina_inicial_entrega_o_token_e_a_politica(self):
        status, corpo, resposta = self.pedir("/")
        self.assertEqual(status, 200)
        self.assertIn(self.token, corpo)
        self.assertNotIn("__TOKEN_CSRF__", corpo)
        self.assertIn("default-src 'none'", resposta.getheader("Content-Security-Policy"))
        self.assertIn("connect-src 'self'", resposta.getheader("Content-Security-Policy"))

    def test_sem_cabecalho_de_cors(self):
        _, _, resposta = self.pedir("/")
        self.assertIsNone(resposta.getheader("Access-Control-Allow-Origin"))

    def test_so_serve_os_estaticos_conhecidos(self):
        for caminho in (
            "/config.py",
            "/administrador/config.py",
            "/estaticos/index.html",
            "/../config.py",
            "/index.html",
            "/app.js.map",
        ):
            with self.subTest(caminho=caminho):
                status, _, _ = self.pedir(caminho)
                self.assertEqual(status, 404)

    def test_estaticos_permitidos_respondem(self):
        for caminho in ("/estilo.css", "/app.js"):
            with self.subTest(caminho=caminho):
                status, _, _ = self.pedir(caminho)
                self.assertEqual(status, 200)

    # ---- barreiras ------------------------------------------------------

    def test_host_estranho_e_recusado(self):
        status, corpo, _ = self.pedir("/", {"Host": "painel.exemplo.invalido"})
        self.assertEqual(status, 403)
        self.assertEqual(json.loads(corpo)["erro"], "host_recusado")

    def test_origin_externo_e_recusado(self):
        status, corpo, _ = self.pedir(
            "/api/inventario", {"Origin": "https://exemplo.invalido"}
        )
        self.assertEqual(status, 403)
        self.assertEqual(json.loads(corpo)["erro"], "origem_recusada")

    def test_requisicao_de_outro_site_e_recusada(self):
        status, _, _ = self.pedir("/api/inventario", {"Sec-Fetch-Site": "cross-site"})
        self.assertEqual(status, 403)

    def test_origin_local_e_aceito(self):
        status, _, _ = self.pedir(
            "/api/inventario", {"Origin": "http://127.0.0.1:{}".format(self.porta)}
        )
        self.assertEqual(status, 200)

    def test_escrita_sem_token_e_recusada(self):
        status, corpo = self.postar(
            "/api/rascunho",
            {"chave": "asset:demo-evolucao", "alteracoes": {"nome": "X"}},
            {"X-Token-Central": ""},
        )
        self.assertEqual(status, 403)
        self.assertEqual(json.loads(corpo)["erro"], "token_invalido")

    def test_escrita_com_token_errado_e_recusada(self):
        status, _ = self.postar(
            "/api/rascunho",
            {"chave": "asset:demo-evolucao", "alteracoes": {}},
            {"X-Token-Central": "token-de-outro-lugar"},
        )
        self.assertEqual(status, 403)

    def test_escrita_exige_json(self):
        status, corpo = self.postar(
            "/api/rascunho", {"chave": "asset:demo-evolucao"}, {"Content-Type": "text/plain"}
        )
        self.assertEqual(status, 415)
        self.assertEqual(json.loads(corpo)["erro"], "tipo_invalido")

    def test_corpo_grande_demais_e_recusado(self):
        conexao = self._conexao()
        conexao.putrequest("POST", "/api/rascunho")
        conexao.putheader("Content-Type", "application/json")
        conexao.putheader("X-Token-Central", self.token)
        conexao.putheader("Content-Length", str(64 * 1024 * 1024))
        conexao.endheaders()
        resposta = conexao.getresponse()
        self.assertEqual(resposta.status, 413)
        resposta.close()
        conexao.close()

    def test_chave_com_travessia_e_recusada(self):
        status, corpo, _ = self.pedir("/api/baralho?chave=catalogo:../../indice")
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "chave_invalida")

    def test_rota_desconhecida(self):
        status, _, _ = self.pedir("/api/inventado")
        self.assertEqual(status, 404)

    # ---- fluxo ----------------------------------------------------------

    def test_inventario_lista_as_origens(self):
        status, corpo, _ = self.pedir("/api/inventario")
        self.assertEqual(status, 200)
        dados = json.loads(corpo)
        chaves = [item["chave"] for item in dados["baralhos"]]
        self.assertIn("asset:demo-evolucao", chaves)
        self.assertIn("catalogo:demo-catalogo", chaves)

    def test_configuracao_firebase_ausente_e_explicita(self):
        status, corpo, _ = self.pedir("/api/firebase")
        self.assertEqual(status, 200)
        self.assertFalse(json.loads(corpo)["configurado"])

    def test_importa_feedback_e_o_exibe_na_dica_do_baralho(self):
        status, corpo = self.postar(
            "/api/feedbacks/importar",
            {"nome": "lista-teste.json", "conteudo": exportacao_de_feedback()},
        )
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(corpo)["quantidadeDeDicas"], 1)
        aberto = self.abrir("asset:demo-evolucao")
        feedback = aberto["feedbacks"]["porCard"]["de-001"][0][0]
        self.assertEqual(feedback["listaNome"], "lista-teste.json")
        self.assertNotIn("sessaoId", json.dumps(aberto))

    def test_remove_baralho_com_confirmacao_textual(self):
        aberto = self.abrir("asset:demo-evolucao")
        status, corpo = self.postar(
            "/api/baralho/remover",
            {
                "chave": "asset:demo-evolucao",
                "revisao": aberto["revisao"],
                "confirmacao": "demo-evolucao",
            },
        )
        self.assertEqual(status, 200)
        self.assertTrue(Path(json.loads(corpo)["backup"]).is_dir())
        ids = [item["id"] for item in json.loads(
            self.config.arquivo_do_asset.read_text(encoding="utf-8")
        )["baralhos"]]
        self.assertNotIn("demo-evolucao", ids)

    def test_salvar_rascunho_pelo_http_nao_toca_a_origem(self):
        antes = self.config.arquivo_do_asset.read_text(encoding="utf-8")
        revisao = self.abrir("asset:demo-evolucao")["revisao"]
        status, corpo = self.postar(
            "/api/rascunho",
            {
                "chave": "asset:demo-evolucao",
                "revisao": revisao,
                "alteracoes": {"nome": "Pelo HTTP"},
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(corpo)["baralho"]["nome"], "Pelo HTTP")
        self.assertEqual(self.config.arquivo_do_asset.read_text(encoding="utf-8"), antes)

    def test_servidor_edita_finalizado_legado(self):
        revisao = self.abrir("asset:demo-final")["revisao"]
        status, corpo = self.postar(
            "/api/rascunho",
            {
                "chave": "asset:demo-final",
                "revisao": revisao,
                "alteracoes": {"nome": "Pode atualizar"},
            },
        )
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(corpo)["baralho"]["nome"], "Pode atualizar")

    def test_servidor_recusa_renomear_id_de_card(self):
        revisao = self.abrir("asset:demo-evolucao")["revisao"]
        status, corpo = self.postar(
            "/api/rascunho",
            {
                "chave": "asset:demo-evolucao",
                "revisao": revisao,
                "alteracoes": {
                    "cards": [
                        {"id": "inventado", "clues": ["a"] * 10},
                        {"id": "de-002", "clues": ["b"] * 10},
                    ]
                },
            },
        )
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "campo_imutavel")

    def test_aplicar_sem_validar_e_recusado_pelo_servidor(self):
        revisao = self.abrir("asset:demo-evolucao")["revisao"]
        status, corpo = self.postar(
            "/api/rascunho",
            {
                "chave": "asset:demo-evolucao",
                "revisao": revisao,
                "alteracoes": {"nome": "Sem validar"},
            },
        )
        self.assertEqual(status, 200)
        revisao = json.loads(corpo)["revisao"]
        status, corpo = self.postar(
            "/api/aplicar",
            {"chave": "asset:demo-evolucao", "revisao": revisao, "confirmacao": True},
        )
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "sem_validacao")

    def test_exportar_rascunho_novo_devolve_json_para_download(self):
        status, corpo = self.postar(
            "/api/rascunho/novo",
            {"nome": "Tema Piloto", "grupo": "Especiais", "icone": "⭐", "categoria": "ESPECIAIS"},
        )
        self.assertEqual(status, 200)
        criado = json.loads(corpo)
        chave = criado["chave"]
        status, corpo = self.postar(
            "/api/validar", {"chave": chave, "revisao": criado["revisao"]}
        )
        self.assertEqual(status, 200)
        trabalho = json.loads(corpo)
        self.servidor.central.validacoes.aguardar(trabalho["id"])
        status, _, _ = self.pedir("/api/validacao?id=" + trabalho["id"])
        self.assertEqual(status, 200)
        status, corpo, resposta = self.postar_com_resposta(
            "/api/exportar", {"chave": chave, "revisao": criado["revisao"]}
        )
        self.assertEqual(status, 200)
        self.assertIn("attachment", resposta.getheader("Content-Disposition"))
        self.assertEqual(json.loads(corpo)["estado"], "EM_DESENVOLVIMENTO")

    def test_exportar_sem_validar_o_conteudo_exato_e_recusado(self):
        status, corpo = self.postar(
            "/api/rascunho/novo",
            {
                "nome": "Tema Sem Validação",
                "grupo": "Especiais",
                "icone": "⭐",
                "categoria": "ESPECIAIS",
            },
        )
        self.assertEqual(status, 200)
        criado = json.loads(corpo)
        status, corpo = self.postar(
            "/api/exportar",
            {"chave": criado["chave"], "revisao": criado["revisao"]},
        )
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "sem_validacao")

    def test_revisao_ausente_e_recusada(self):
        status, corpo = self.postar(
            "/api/rascunho",
            {"chave": "asset:demo-evolucao", "alteracoes": {"nome": "Sem revisão"}},
        )
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "revisao_ausente")

    def test_segunda_aba_com_revisao_antiga_e_recusada(self):
        revisao = self.abrir("asset:demo-evolucao")["revisao"]
        primeiro = {
            "chave": "asset:demo-evolucao",
            "revisao": revisao,
            "alteracoes": {"nome": "Primeira aba"},
        }
        self.assertEqual(self.postar("/api/rascunho", primeiro)[0], 200)
        segundo = dict(primeiro)
        segundo["alteracoes"] = {"nome": "Segunda aba"}
        status, corpo = self.postar("/api/rascunho", segundo)
        self.assertEqual(status, 400)
        self.assertEqual(json.loads(corpo)["erro"], "revisao_obsoleta")


if __name__ == "__main__":
    unittest.main()
