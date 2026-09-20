"""Servidor HTTP local da Central de Baralhos.

Escuta somente em 127.0.0.1. Cada requisicao passa por tres barreiras antes de
chegar ao servico:

1. cabecalho Host precisa ser o proprio loopback na porta em uso (barra DNS
   rebinding, em que um dominio externo resolve para 127.0.0.1);
2. cabecalho Origin, quando presente, precisa ser esta mesma origem;
3. escrita (POST) exige o token gerado na subida, entregue dentro do HTML e
   devolvido em um cabecalho proprio (protecao contra CSRF).

Nao existe endpoint de shell, nao ha CORS, nao ha CDN e o cliente nunca envia
caminho de arquivo: ele envia chaves opacas que o servidor traduz.
"""
from __future__ import annotations

import json
import secrets
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from aplicacao import FalhaDaAplicacao
from acervo_editorial import FalhaDoAcervo
from config import MAXIMO_DO_CORPO
from edicao import FalhaDaEdicao
from feedbacks import FalhaDosFeedbacks
from firebase_config import configuracao_publica
from fontes import FalhaDaFonte
from servico import Central, FalhaDoServico
from validacao import FalhaDaValidacao

PASTA_DE_ESTATICOS = Path(__file__).resolve().parent / "estaticos"

#: Lista fechada. Nada mais do disco e servido, em nenhuma circunstancia.
ESTATICOS = {
    "/": ("index.html", "text/html; charset=utf-8"),
    "/estilo.css": ("estilo.css", "text/css; charset=utf-8"),
    "/app.js": ("app.js", "text/javascript; charset=utf-8"),
    "/acervo.js": ("acervo.js", "text/javascript; charset=utf-8"),
    "/acervo-firestore.js": ("acervo-firestore.js", "text/javascript; charset=utf-8"),
    "/acervo-ui.js": ("acervo-ui.js", "text/javascript; charset=utf-8"),
}

POLITICA_DE_CONTEUDO = (
    "default-src 'none'; script-src 'self'; style-src 'self'; "
    "connect-src 'self' https://identitytoolkit.googleapis.com "
    "https://securetoken.googleapis.com https://firestore.googleapis.com; "
    "img-src 'self' data:; form-action 'none'; "
    "base-uri 'none'; frame-ancestors 'none'"
)

FALHAS_PREVISTAS = (
    FalhaDoServico,
    FalhaDaEdicao,
    FalhaDaFonte,
    FalhaDaAplicacao,
    FalhaDaValidacao,
    FalhaDosFeedbacks,
    FalhaDoAcervo,
)


class ServidorDaCentral(ThreadingHTTPServer):
    """ThreadingHTTPServer para que uma validacao longa nao trave o painel."""

    daemon_threads = True

    def __init__(self, endereco, handler, central, token):
        super().__init__(endereco, handler)
        self.central = central
        self.token = token
        porta = self.server_address[1]
        self.hosts_aceitos = {
            "127.0.0.1:{}".format(porta),
            "localhost:{}".format(porta),
        }
        self.origens_aceitas = {
            "http://127.0.0.1:{}".format(porta),
            "http://localhost:{}".format(porta),
        }


def criar_servidor(config, central=None, porta=None):
    """Sobe o servidor em 127.0.0.1 e devolve (servidor, token)."""
    config.preparar_diretorio_privado()
    central = central or Central(config)
    token = secrets.token_urlsafe(32)
    servidor = ServidorDaCentral(
        ("127.0.0.1", config.porta if porta is None else porta),
        ManipuladorDaCentral,
        central,
        token,
    )
    return servidor, token


class ManipuladorDaCentral(BaseHTTPRequestHandler):
    server_version = "CentralDeBaralhos"
    sys_version = ""
    protocol_version = "HTTP/1.1"

    # ---- infraestrutura -------------------------------------------------

    def log_message(self, formato, *args):
        """Log minimo: metodo e caminho sem query. Nunca cabecalho, corpo ou token."""
        caminho = urlparse(self.path).path if self.path else "-"
        print("[central] {} {}".format(self.command or "-", caminho))

    def _cabecalhos_comuns(self):
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")

    def _responder(self, codigo, corpo, tipo, extras=None):
        dados = corpo if isinstance(corpo, bytes) else corpo.encode("utf-8")
        self.send_response(codigo)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(dados)))
        self._cabecalhos_comuns()
        for nome, valor in (extras or {}).items():
            self.send_header(nome, valor)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(dados)

    def _json(self, codigo, dados):
        self._responder(
            codigo,
            json.dumps(dados, ensure_ascii=False),
            "application/json; charset=utf-8",
        )

    def _erro(self, codigo, chave, mensagem):
        self._json(codigo, {"erro": chave, "mensagem": mensagem})

    # ---- barreiras ------------------------------------------------------

    def _requisicao_aceita(self, escrita):
        if self.headers.get("Host") not in self.server.hosts_aceitos:
            self._erro(403, "host_recusado", "Endereço não aceito por este painel local.")
            return False
        origem = self.headers.get("Origin")
        if origem is not None and origem not in self.server.origens_aceitas:
            self._erro(403, "origem_recusada", "Origem externa não é aceita.")
            return False
        destino = self.headers.get("Sec-Fetch-Site")
        if destino is not None and destino not in ("same-origin", "none"):
            self._erro(403, "origem_recusada", "Requisição de outro site não é aceita.")
            return False
        if not escrita:
            return True
        if not secrets.compare_digest(
            self.headers.get("X-Token-Central") or "", self.server.token
        ):
            self._erro(403, "token_invalido", "Recarregue o painel: a sessão mudou.")
            return False
        tipo = (self.headers.get("Content-Type") or "").split(";")[0].strip()
        if tipo != "application/json":
            self._erro(415, "tipo_invalido", "O corpo precisa ser JSON.")
            return False
        return True

    def _corpo(self):
        try:
            tamanho = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            self._erro(400, "corpo_invalido", "Tamanho do corpo inválido.")
            return None
        if tamanho <= 0:
            self._erro(400, "corpo_invalido", "Corpo ausente.")
            return None
        if tamanho > MAXIMO_DO_CORPO:
            self.close_connection = True
            self._erro(413, "corpo_grande", "O conteúdo enviado é grande demais.")
            return None
        try:
            dados = json.loads(self.rfile.read(tamanho).decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            self._erro(400, "corpo_invalido", "O corpo não é um JSON válido.")
            return None
        if not isinstance(dados, dict):
            self._erro(400, "corpo_invalido", "O corpo precisa ser um objeto JSON.")
            return None
        return dados

    # ---- rotas ----------------------------------------------------------

    def do_GET(self):
        if not self._requisicao_aceita(escrita=False):
            return
        partes = urlparse(self.path)
        caminho = partes.path
        consulta = parse_qs(partes.query)
        if caminho in ESTATICOS:
            return self._servir_estatico(caminho)
        try:
            if caminho == "/api/inventario":
                return self._json(200, self.server.central.inventario())
            if caminho == "/api/firebase":
                return self._json(200, configuracao_publica(self.server.central.config))
            if caminho == "/api/baralho":
                chave = (consulta.get("chave") or [""])[0]
                return self._json(200, self.server.central.abrir(chave))
            if caminho == "/api/validacao":
                identificador = (consulta.get("id") or [""])[0]
                return self._json(200, self.server.central.estado_da_validacao(identificador))
            if caminho == "/api/acervo/previa":
                incluir_tecnicos = (consulta.get("tecnicos") or [""])[0] == "1"
                return self._json(
                    200, self.server.central.previa_do_acervo(incluir_tecnicos=incluir_tecnicos)
                )
            if caminho == "/api/acervo/resposta":
                resposta_id = (consulta.get("respostaId") or [""])[0]
                return self._json(200, self.server.central.abrir_resposta(resposta_id))
        except FALHAS_PREVISTAS as falha:
            return self._erro(400, falha.codigo, falha.mensagem)
        except Exception:
            return self._erro(500, "erro_interno", "Falha inesperada no painel.")
        self._erro(404, "rota_desconhecida", "Endereço não encontrado.")

    def do_POST(self):
        if not self._requisicao_aceita(escrita=True):
            return
        caminho = urlparse(self.path).path
        corpo = self._corpo()
        if corpo is None:
            return
        central = self.server.central
        try:
            if caminho == "/api/rascunho":
                return self._json(
                    200,
                    central.salvar_rascunho(
                        corpo.get("chave"),
                        corpo.get("alteracoes") or {},
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/rascunho/descartar":
                return self._json(
                    200,
                    central.descartar_rascunho(corpo.get("chave"), corpo.get("revisao")),
                )
            if caminho == "/api/rascunho/novo":
                return self._json(
                    200,
                    central.criar_rascunho(
                        corpo.get("nome"),
                        corpo.get("grupo"),
                        corpo.get("icone"),
                        corpo.get("categoria"),
                    ),
                )
            if caminho == "/api/rascunho/card":
                return self._json(
                    200, central.acrescentar_card(corpo.get("chave"), corpo.get("revisao"))
                )
            if caminho == "/api/rascunho/card/remover":
                return self._json(
                    200,
                    central.remover_card(
                        corpo.get("chave"), corpo.get("cardId"), corpo.get("revisao")
                    ),
                )
            if caminho == "/api/validar":
                return self._json(
                    200, central.validar(corpo.get("chave"), corpo.get("revisao"))
                )
            if caminho == "/api/aplicar":
                return self._json(
                    200,
                    central.aplicar(
                        corpo.get("chave"), corpo.get("confirmacao"), corpo.get("revisao")
                    ),
                )
            if caminho == "/api/baralho/remover":
                return self._json(
                    200,
                    central.remover_baralho(
                        corpo.get("chave"),
                        corpo.get("confirmacao"),
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/feedbacks/importar":
                return self._json(
                    200,
                    central.importar_feedbacks(corpo.get("nome"), corpo.get("conteudo")),
                )
            if caminho == "/api/acervo/dica":
                return self._json(
                    200,
                    central.acrescentar_dica(
                        corpo.get("respostaId"),
                        corpo.get("texto"),
                        corpo.get("escopo"),
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/acervo/dica/texto":
                return self._json(
                    200,
                    central.editar_texto_da_dica(
                        corpo.get("respostaId"),
                        corpo.get("dicaId"),
                        corpo.get("texto"),
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/acervo/dica/editar":
                return self._json(
                    200,
                    central.editar_dica(
                        corpo.get("respostaId"),
                        corpo.get("dicaId"),
                        corpo.get("texto"),
                        corpo.get("escopo"),
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/acervo/dica/escopo":
                return self._json(
                    200,
                    central.alterar_escopo_da_dica(
                        corpo.get("respostaId"),
                        corpo.get("dicaId"),
                        corpo.get("escopo"),
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/acervo/dica/desativar":
                return self._json(
                    200,
                    central.desativar_dica(
                        corpo.get("respostaId"), corpo.get("dicaId"), corpo.get("revisao")
                    ),
                )
            if caminho == "/api/acervo/dica/reativar":
                return self._json(
                    200,
                    central.reativar_dica(
                        corpo.get("respostaId"), corpo.get("dicaId"), corpo.get("revisao")
                    ),
                )
            if caminho == "/api/acervo/descartar":
                return self._json(
                    200,
                    central.descartar_rascunho_da_resposta(
                        corpo.get("respostaId"), corpo.get("revisao")
                    ),
                )
            if caminho == "/api/acervo/projetar":
                return self._json(
                    200,
                    central.projetar_do_acervo(
                        corpo.get("chave"),
                        corpo.get("projecoes") or {},
                        corpo.get("revisao"),
                    ),
                )
            if caminho == "/api/exportar":
                texto, nome = central.exportar(corpo.get("chave"), corpo.get("revisao"))
                return self._responder(
                    200,
                    texto,
                    "application/json; charset=utf-8",
                    {"Content-Disposition": 'attachment; filename="{}"'.format(nome)},
                )
        except FALHAS_PREVISTAS as falha:
            return self._erro(400, falha.codigo, falha.mensagem)
        except Exception:
            return self._erro(500, "erro_interno", "Falha inesperada no painel.")
        self._erro(404, "rota_desconhecida", "Endereço não encontrado.")

    def _servir_estatico(self, caminho):
        nome, tipo = ESTATICOS[caminho]
        arquivo = (PASTA_DE_ESTATICOS / nome).resolve()
        if arquivo.parent != PASTA_DE_ESTATICOS.resolve() or not arquivo.is_file():
            return self._erro(404, "estatico_ausente", "Arquivo do painel não encontrado.")
        texto = arquivo.read_text(encoding="utf-8")
        extras = None
        if nome == "index.html":
            texto = texto.replace("__TOKEN_CSRF__", self.server.token)
            extras = {"Content-Security-Policy": POLITICA_DE_CONTEUDO}
        self._responder(200, texto, tipo, extras)
