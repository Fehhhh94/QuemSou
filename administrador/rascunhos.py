"""Rascunhos privados: trabalho em andamento que nunca toca o acervo real.

Cada rascunho e um arquivo JSON no diretorio privado (fora do repositorio),
nomeado por uma derivacao segura da chave do baralho. Descartar um rascunho
apaga somente esse arquivo; a origem continua intacta.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import tempfile
from datetime import datetime, timezone

from fontes import forma_canonica

_CARACTERES_SEGUROS = re.compile(r"[^a-z0-9._-]+")


def agora():
    """Instante em ISO-8601 UTC, para mostrar quando o rascunho foi salvo."""
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


class Rascunhos:
    """Guarda e recupera rascunhos. Uma instancia por diretorio privado."""

    def __init__(self, pasta):
        self.pasta = pasta

    def _arquivo(self, chave):
        # A chave vem do servidor, nunca crua do navegador; ainda assim o nome
        # do arquivo e sanitizado e recebe um sufixo de hash para nao colidir.
        base = _CARACTERES_SEGUROS.sub("-", (chave or "").lower())[:64]
        digest = hashlib.sha256((chave or "").encode("utf-8")).hexdigest()[:12]
        return self.pasta / "{}-{}.json".format(base or "rascunho", digest)

    def ler(self, chave):
        """Devolve o registro do rascunho ou None."""
        arquivo = self._arquivo(chave)
        try:
            registro = json.loads(arquivo.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return None
        return registro if isinstance(registro, dict) else None

    def salvar(self, registro):
        """Grava o rascunho de forma atomica e devolve o registro salvo."""
        registro = dict(registro)
        registro["atualizadoEm"] = agora()
        registro.setdefault("criadoEm", registro["atualizadoEm"])
        self.pasta.mkdir(parents=True, exist_ok=True)
        arquivo = self._arquivo(registro["chave"])
        descritor, nome_temporario = tempfile.mkstemp(
            prefix=".{}-".format(arquivo.stem), suffix=".parcial", dir=str(self.pasta)
        )
        try:
            with os.fdopen(descritor, "w", encoding="utf-8", newline="\n") as saida:
                json.dump(registro, saida, ensure_ascii=False, indent=2)
                saida.write("\n")
            os.replace(nome_temporario, arquivo)
        except Exception:
            try:
                os.unlink(nome_temporario)
            except FileNotFoundError:
                pass
            raise
        return registro

    def descartar(self, chave):
        """Apaga apenas o rascunho. Nenhum arquivo de origem e tocado."""
        arquivo = self._arquivo(chave)
        try:
            arquivo.unlink()
            return True
        except FileNotFoundError:
            return False

    def listar(self):
        """Todos os rascunhos guardados, do mais recente para o mais antigo."""
        if not self.pasta.is_dir():
            return []
        registros = []
        for arquivo in self.pasta.glob("*.json"):
            try:
                registro = json.loads(arquivo.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            if isinstance(registro, dict) and registro.get("chave"):
                registros.append(registro)
        registros.sort(key=lambda r: r.get("atualizadoEm") or "", reverse=True)
        return registros


def impressao_do_candidato(candidato):
    """Hash do conteudo exato que seria gravado, em forma canonica."""
    return hashlib.sha256(forma_canonica(candidato).encode("utf-8")).hexdigest()


def marcar_aprovacao(registro, alvo, hash_do_candidato, hash_da_validacao, resumo=""):
    """Prende a aprovacao ao conteudo exato validado.

    Qualquer edicao posterior muda a forma canonica do candidato e, com ela, o
    hash: a aprovacao deixa de valer sozinha, sem depender de o painel lembrar
    de limpa-la.
    """
    registro["aprovacao"] = {
        "alvo": alvo,
        "hashDoCandidato": hash_do_candidato,
        "hashDaValidacao": hash_da_validacao,
        "hashDaOrigem": registro.get("hashDaOrigem"),
        "hashDoIndice": registro.get("hashDoIndice"),
        "quando": agora(),
        "resumo": resumo,
    }
    return registro


def aprovacao_valida(registro, alvo, hash_do_candidato, hash_da_validacao):
    """True se ha aprovacao para o alvo e ela corresponde ao candidato atual."""
    aprovacao = registro.get("aprovacao")
    if not isinstance(aprovacao, dict) or aprovacao.get("alvo") != alvo:
        return False
    return (
        aprovacao.get("hashDoCandidato") == hash_do_candidato
        and aprovacao.get("hashDaValidacao") == hash_da_validacao
    )
