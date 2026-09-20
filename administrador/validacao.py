"""Validacao pela regua REAL do app, executada por Gradle.

O painel nao reimplementa o ValidadorEditorial nem o ParserDoCatalogo. Ele
escreve o candidato exato num arquivo temporario privado e chama as tarefas
que ja existem no repositorio:

    gradlew.bat validarBaralho  -Parquivo=<candidato.json>
    gradlew.bat validarCatalogo -Ppasta=<copia temporaria do catalogo>

A execucao acontece numa thread, com uma trava que garante um Gradle por vez;
o servidor continua respondendo enquanto isso. Os argumentos sao fixos e os
caminhos sao gerados aqui, nunca recebidos do navegador.
"""
from __future__ import annotations

import os
import re
import signal
import shutil
import subprocess
import threading
import uuid
from datetime import datetime, timezone

from aplicacao import ALVO_BARALHO, ALVO_CATALOGO
from config import gradle_user_home_sugerido, java_home_sugerido
from fontes import PADRAO_DE_ID, serializar

#: Nenhum destes caracteres aparece em caminho gerado por este modulo. Se
#: aparecer, algo saiu do previsto e a execucao e recusada antes de comecar.
CARACTERES_PROIBIDOS = re.compile(r"[&|<>^%\"\r\n]")
CODIGOS_ANSI = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
LIMITE_DA_SAIDA = 20000

ESTADO_NA_FILA = "na_fila"
ESTADO_EXECUTANDO = "executando"
ESTADO_APROVADO = "aprovado"
ESTADO_REPROVADO = "reprovado"
ESTADO_FALHA = "falha"
ESTADO_TEMPO_ESGOTADO = "tempo_esgotado"


class FalhaDaValidacao(ValueError):
    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


def _agora():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def argumento_seguro(valor):
    """Recusa qualquer caminho com caractere que teria sentido para o cmd."""
    texto = str(valor)
    if CARACTERES_PROIBIDOS.search(texto):
        raise FalhaDaValidacao(
            "caminho_inseguro", "Caminho temporário inesperado; validação cancelada."
        )
    return texto


def comando_de_baralho(config, arquivo):
    return [
        argumento_seguro(config.gradlew),
        "validarBaralho",
        "-Parquivo={}".format(argumento_seguro(arquivo)),
        "--offline",
        "--no-daemon",
        "--console=plain",
    ]


def comando_de_catalogo(config, pasta):
    return [
        argumento_seguro(config.gradlew),
        "validarCatalogo",
        "-Ppasta={}".format(argumento_seguro(pasta)),
        "--offline",
        "--no-daemon",
        "--console=plain",
    ]


def ambiente_do_gradle(ambiente_base=None):
    """JBR e cache local do Gradle quando existirem; caso contrario, o padrao."""
    ambiente = dict(os.environ if ambiente_base is None else ambiente_base)
    java = java_home_sugerido()
    if java is not None:
        ambiente["JAVA_HOME"] = str(java)
    cache = gradle_user_home_sugerido()
    if cache is not None:
        ambiente["GRADLE_USER_HOME"] = str(cache)
    return ambiente


def _mensagens_da_saida(saida):
    """Extrai as linhas de violacao impressas pelas ferramentas do app."""
    mensagens = []
    for linha in (saida or "").splitlines():
        texto = CODIGOS_ANSI.sub("", linha).strip()
        item = re.match(r"^-\s+(.+)$", texto)
        if item:
            mensagens.append(item.group(1))
        elif texto.startswith("✗"):
            mensagens.append(texto.lstrip("✗ ").strip())
    return mensagens[:200]


def executar_com_gradle(config, comando):
    """Executa o Gradle sem shell, janela visível ou processo filho órfão."""
    kwargs = {}
    if os.name == "nt":
        kwargs["creationflags"] = (
            getattr(subprocess, "CREATE_NO_WINDOW", 0)
            | getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0)
        )
    else:
        kwargs["start_new_session"] = True
    try:
        processo = subprocess.Popen(
            comando,
            cwd=str(config.raiz_do_app),
            env=ambiente_do_gradle(),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            shell=False,
            **kwargs
        )
    except FileNotFoundError:
        return None, "O gradlew.bat não foi encontrado em {}.".format(config.raiz_do_app)
    try:
        saida, _ = processo.communicate(timeout=config.tempo_limite_da_validacao)
    except subprocess.TimeoutExpired as erro:
        _encerrar_arvore(processo)
        try:
            restante, _ = processo.communicate(timeout=5)
        except subprocess.TimeoutExpired:
            processo.kill()
            restante, _ = processo.communicate()
        acumulada = (erro.output or b"") + (restante or b"")
        raise subprocess.TimeoutExpired(
            comando, config.tempo_limite_da_validacao, output=acumulada
        )
    texto = (saida or b"").decode("utf-8", errors="replace")
    return processo.returncode, texto[-LIMITE_DA_SAIDA:]


def _encerrar_arvore(processo):
    """Encerra somente a árvore criada para esta validação."""
    if processo.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(
            ["taskkill", "/PID", str(processo.pid), "/T", "/F"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            shell=False,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
            check=False,
        )
        return
    try:
        os.killpg(processo.pid, signal.SIGTERM)
    except ProcessLookupError:
        return


def preparar_arquivos(config, plano, pasta):
    """Escreve o candidato e, quando for o caso, a copia temporaria do catalogo."""
    identificador = plano.candidato.get("id") or ""
    if not PADRAO_DE_ID.match(identificador):
        raise FalhaDaValidacao("id_invalido", "O id do baralho não é um slug válido.")
    pasta.mkdir(parents=True, exist_ok=True)
    arquivo = pasta / "candidato.json"
    arquivo.write_text(serializar(plano.candidato), encoding="utf-8", newline="\n")

    staging = None
    if ALVO_CATALOGO in plano.alvos:
        staging = pasta / "catalogo"
        (staging / "baralhos").mkdir(parents=True, exist_ok=True)
        pasta_de_origem = config.pasta_de_baralhos_do_catalogo
        pasta_real = pasta_de_origem.resolve()
        if pasta_de_origem.is_dir():
            for origem in pasta_de_origem.glob("*.json"):
                try:
                    origem_real = origem.resolve(strict=True)
                except OSError:
                    raise FalhaDaValidacao(
                        "caminho_inseguro",
                        "Um arquivo do catálogo não pôde ser resolvido com segurança.",
                    )
                if origem_real.parent != pasta_real or not origem_real.is_file():
                    raise FalhaDaValidacao(
                        "caminho_inseguro",
                        "Um arquivo do catálogo aponta para fora de baralhos/.",
                    )
                shutil.copy2(origem_real, staging / "baralhos" / origem.name)
        (staging / "baralhos" / "{}.json".format(identificador)).write_text(
            serializar(plano.candidato), encoding="utf-8", newline="\n"
        )
        (staging / "indice.json").write_text(
            serializar(plano.indice), encoding="utf-8", newline="\n"
        )
    return arquivo, staging


class Validacoes:
    """Fila de validacoes. Uma execucao de Gradle por vez, HTTP nunca bloqueado."""

    def __init__(self, config, executar=None):
        self.config = config
        self._executar = executar or executar_com_gradle
        self._trabalhos = {}
        self._trava = threading.Lock()
        self._trava_do_gradle = threading.Lock()

    def iniciar(self, plano, chave):
        identificador = uuid.uuid4().hex
        trabalho = {
            "id": identificador,
            "chave": chave,
            "alvos": list(plano.alvos),
            "estado": ESTADO_NA_FILA,
            "hashDoCandidato": plano.hash_do_candidato,
            "hashDaValidacao": plano.hash_da_validacao,
            "etapas": [],
            "mensagens": [],
            "resumo": "Aguardando o Gradle.",
            "iniciadoEm": _agora(),
            "terminadoEm": None,
            "simulado": self._executar is not executar_com_gradle,
        }
        with self._trava:
            self._trabalhos[identificador] = trabalho
        thread = threading.Thread(
            target=self._processar, args=(identificador, plano), daemon=True
        )
        trabalho["_thread"] = thread
        thread.start()
        return self.estado(identificador)

    def estado(self, identificador):
        with self._trava:
            trabalho = self._trabalhos.get(identificador)
            if trabalho is None:
                return None
            return {k: v for k, v in trabalho.items() if not k.startswith("_")}

    def aguardar(self, identificador, tempo=60):
        """Usado pelos testes; o servidor nunca espera uma validacao terminar."""
        with self._trava:
            trabalho = self._trabalhos.get(identificador)
        if trabalho is not None and trabalho.get("_thread") is not None:
            trabalho["_thread"].join(tempo)
        return self.estado(identificador)

    def _atualizar(self, identificador, **campos):
        with self._trava:
            trabalho = self._trabalhos.get(identificador)
            if trabalho is not None:
                trabalho.update(campos)

    def _acrescentar_etapa(self, identificador, etapa):
        with self._trava:
            trabalho = self._trabalhos.get(identificador)
            if trabalho is not None:
                trabalho["etapas"].append(etapa)

    def _processar(self, identificador, plano):
        pasta = self.config.pasta_temporaria / "validacao-{}".format(identificador)
        try:
            arquivo, staging = preparar_arquivos(self.config, plano, pasta)
            etapas = [(ALVO_BARALHO, comando_de_baralho(self.config, arquivo))]
            if staging is not None:
                etapas.append((ALVO_CATALOGO, comando_de_catalogo(self.config, staging)))
        except FalhaDaValidacao as falha:
            self._encerrar(identificador, ESTADO_FALHA, falha.mensagem, [falha.mensagem])
            shutil.rmtree(pasta, ignore_errors=True)
            return
        except OSError:
            self._encerrar(
                identificador,
                ESTADO_FALHA,
                "Não foi possível preparar os arquivos temporários da validação.",
                [],
            )
            return

        try:
            with self._trava_do_gradle:
                self._atualizar(identificador, estado=ESTADO_EXECUTANDO, resumo="Gradle em execução.")
                for alvo, comando in etapas:
                    if not self._rodar_etapa(identificador, alvo, comando):
                        return
            self._encerrar(
                identificador,
                ESTADO_APROVADO,
                "Aprovado pela régua do app.",
                [],
            )
        finally:
            shutil.rmtree(pasta, ignore_errors=True)

    def _rodar_etapa(self, identificador, alvo, comando):
        """Devolve True quando a etapa passou e a proxima pode rodar."""
        try:
            codigo, saida = self._executar(self.config, comando)
        except subprocess.TimeoutExpired:
            self._encerrar(
                identificador,
                ESTADO_TEMPO_ESGOTADO,
                "A validação passou de {} s e foi interrompida.".format(
                    self.config.tempo_limite_da_validacao
                ),
                [],
            )
            return False
        except OSError as erro:
            self._encerrar(
                identificador, ESTADO_FALHA, "Não foi possível executar o Gradle.", [str(erro)]
            )
            return False

        mensagens = _mensagens_da_saida(saida)
        self._acrescentar_etapa(
            identificador,
            {
                "alvo": alvo,
                "tarefa": comando[1],
                "codigo": codigo,
                "mensagens": mensagens,
                "saida": (saida or "")[-LIMITE_DA_SAIDA:],
            },
        )
        if codigo == 0:
            return True
        reprovacao_editorial = (
            codigo == 1
            and (
                bool(mensagens)
                or "Baralho reprovado." in (saida or "")
                or "RESUMO:" in (saida or "")
            )
        )
        if reprovacao_editorial:
            self._encerrar(
                identificador,
                ESTADO_REPROVADO,
                "Reprovado pela régua do app ({}).".format(comando[1]),
                mensagens,
            )
            return False
        self._encerrar(
            identificador,
            ESTADO_FALHA,
            "O Gradle não concluiu a validação ({}).".format(comando[1]),
            mensagens or ["Saída do Gradle sem violações reconhecíveis."],
        )
        return False

    def _encerrar(self, identificador, estado, resumo, mensagens):
        self._atualizar(
            identificador,
            estado=estado,
            resumo=resumo,
            mensagens=mensagens,
            terminadoEm=_agora(),
        )
