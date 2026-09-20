"""Importacao privada e associacao de feedbacks exportados pelo aplicativo.

O painel aceita somente o envelope ``quemsou-feedback`` v3, extrai apenas os
campos necessarios para revisar dicas e descarta o ``sessaoId``. Os arquivos
sanitizados ficam no diretorio privado; nenhum feedback e enviado pela rede.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import tempfile
import unicodedata
from datetime import datetime, timezone


FORMATO = "quemsou-feedback"
VERSAO = 3
FORMATO_INTERNO = "quemsou-feedback-importado"
MAXIMO_DE_ITENS = 50_000


class FalhaDosFeedbacks(ValueError):
    """Erro esperado de importacao, com codigo estavel para a interface."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


def _agora():
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def _normalizar(texto):
    decomposto = unicodedata.normalize("NFD", str(texto or "")).lower()
    return " ".join(
        "".join(
            caractere if caractere.isalnum() else " "
            for caractere in decomposto
            if unicodedata.category(caractere) != "Mn"
        ).split()
    )


def _nome_seguro(nome):
    nome = re.split(r"[\\/]", str(nome or ""))[-1]
    nome = "".join(c for c in nome if c.isprintable()).strip()
    return (nome or "feedback.json")[:120]


def _texto(valor, campo, limite, obrigatorio=True):
    if valor is None and not obrigatorio:
        return None
    if not isinstance(valor, str):
        raise FalhaDosFeedbacks(
            "feedback_invalido", "O campo {} de um feedback é inválido.".format(campo)
        )
    valor = valor.strip()
    if obrigatorio and not valor:
        raise FalhaDosFeedbacks(
            "feedback_invalido", "O campo {} de um feedback está vazio.".format(campo)
        )
    if len(valor) > limite:
        raise FalhaDosFeedbacks(
            "feedback_invalido", "O campo {} de um feedback é grande demais.".format(campo)
        )
    return valor or None


def _gravar_atomico(caminho, dados):
    texto = json.dumps(dados, ensure_ascii=False, indent=2) + "\n"
    descritor, temporario = tempfile.mkstemp(
        prefix=".feedback-", suffix=".parcial", dir=str(caminho.parent)
    )
    try:
        with os.fdopen(descritor, "w", encoding="utf-8", newline="\n") as saida:
            saida.write(texto)
        os.replace(temporario, caminho)
    except Exception:
        try:
            os.unlink(temporario)
        except FileNotFoundError:
            pass
        raise


class ArmazemDeFeedbacks:
    """Colecao local de listas importadas e ja reduzidas ao contexto editorial."""

    def __init__(self, pasta):
        self.pasta = pasta

    def importar(self, nome, conteudo):
        if not isinstance(conteudo, dict):
            raise FalhaDosFeedbacks(
                "feedback_invalido", "A lista de feedback precisa ser um objeto JSON."
            )
        if conteudo.get("formato") != FORMATO or conteudo.get("versao") != VERSAO:
            raise FalhaDosFeedbacks(
                "formato_de_feedback_incompativel",
                "Use um arquivo quemsou-feedback versão 3 exportado pelo app.",
            )
        itens = conteudo.get("itens")
        if not isinstance(itens, list) or len(itens) > MAXIMO_DE_ITENS:
            raise FalhaDosFeedbacks(
                "feedback_invalido", "A lista de feedbacks é inválida ou grande demais."
            )

        canonico = json.dumps(conteudo, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        identificador = hashlib.sha256(canonico.encode("utf-8")).hexdigest()[:32]
        self.pasta.mkdir(parents=True, exist_ok=True)
        caminho = self.pasta / "{}.json".format(identificador)
        if caminho.is_file():
            registro = self._ler(caminho)
            return self._resposta_da_importacao(registro, repetida=True)

        extraidos = []
        ignorados = 0
        for indice, item in enumerate(itens, start=1):
            if not isinstance(item, dict):
                raise FalhaDosFeedbacks(
                    "feedback_invalido", "O feedback {} não é um objeto.".format(indice)
                )
            if item.get("resultadoDoTurno") != "DICA_REVELADA":
                ignorados += 1
                continue
            contexto_bruto = item.get("contextoJson")
            if not isinstance(contexto_bruto, str):
                raise FalhaDosFeedbacks(
                    "feedback_invalido",
                    "O feedback {} não contém o contexto da dica.".format(indice),
                )
            try:
                contexto = json.loads(contexto_bruto)
            except (TypeError, ValueError):
                raise FalhaDosFeedbacks(
                    "feedback_invalido",
                    "O contexto da dica no feedback {} não é válido.".format(indice),
                )
            if not isinstance(contexto, dict):
                raise FalhaDosFeedbacks(
                    "feedback_invalido",
                    "O contexto da dica no feedback {} não é um objeto.".format(indice),
                )
            voto = _texto(item.get("voto"), "voto", 20)
            if voto not in ("BOM", "FRACO"):
                raise FalhaDosFeedbacks(
                    "feedback_invalido", "O voto do feedback {} é desconhecido.".format(indice)
                )
            extraidos.append(
                {
                    "baralhoId": _texto(item.get("baralhoId"), "baralhoId", 120),
                    "cardId": _texto(item.get("cardId"), "cardId", 120),
                    "resposta": _texto(item.get("resposta"), "resposta", 500, obrigatorio=False),
                    "voto": voto,
                    "comentario": _texto(
                        item.get("comentario"), "comentario", 1000, obrigatorio=False
                    ),
                    "rodada": item.get("rodada") if isinstance(item.get("rodada"), int) else None,
                    "criadoEm": _texto(item.get("criadoEm"), "criadoEm", 80),
                    "dicaId": _texto(contexto.get("dicaId"), "dicaId", 300),
                    "textoAvaliado": _texto(contexto.get("texto"), "texto", 500),
                    "posicao": contexto.get("posicao")
                    if isinstance(contexto.get("posicao"), int)
                    else None,
                    "respostaId": _texto(
                        contexto.get("respostaId"), "respostaId", 300, obrigatorio=False
                    ),
                    "versaoDoBaralho": contexto.get("versaoDoBaralho")
                    if isinstance(contexto.get("versaoDoBaralho"), int)
                    else None,
                }
            )

        registro = {
            "formato": FORMATO_INTERNO,
            "versao": 1,
            "id": identificador,
            "nome": _nome_seguro(nome),
            "importadoEm": _agora(),
            "exportadoEm": _texto(
                conteudo.get("exportadoEm"), "exportadoEm", 80, obrigatorio=False
            ),
            "quantidadeDeItens": len(itens),
            "quantidadeDeDicas": len(extraidos),
            "quantidadeIgnorada": ignorados,
            "itens": extraidos,
        }
        try:
            _gravar_atomico(caminho, registro)
        except OSError:
            raise FalhaDosFeedbacks(
                "feedback_nao_salvo", "Não foi possível guardar a lista de feedbacks."
            )
        return self._resposta_da_importacao(registro, repetida=False)

    @staticmethod
    def _resposta_da_importacao(registro, repetida):
        return {
            "id": registro["id"],
            "nome": registro["nome"],
            "quantidadeDeItens": registro["quantidadeDeItens"],
            "quantidadeDeDicas": registro["quantidadeDeDicas"],
            "quantidadeIgnorada": registro["quantidadeIgnorada"],
            "repetida": repetida,
        }

    def _ler(self, caminho):
        try:
            dados = json.loads(caminho.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            raise FalhaDosFeedbacks(
                "feedback_armazenado_invalido", "Uma lista privada de feedback está corrompida."
            )
        if not isinstance(dados, dict) or dados.get("formato") != FORMATO_INTERNO:
            raise FalhaDosFeedbacks(
                "feedback_armazenado_invalido", "Uma lista privada de feedback é incompatível."
            )
        return dados

    def _registros(self):
        if not self.pasta.is_dir():
            return []
        return [self._ler(caminho) for caminho in sorted(self.pasta.glob("*.json"))]

    def resumo(self):
        listas = [self._resumo_de(registro) for registro in self._registros()]
        return {
            "quantidadeDeListas": len(listas),
            "quantidadeDeDicas": sum(item["quantidadeDeDicas"] for item in listas),
            "listas": listas,
        }

    @staticmethod
    def _resumo_de(registro):
        return {
            "id": registro["id"],
            "nome": registro["nome"],
            "importadoEm": registro.get("importadoEm"),
            "exportadoEm": registro.get("exportadoEm"),
            "quantidadeDeItens": registro.get("quantidadeDeItens", 0),
            "quantidadeDeDicas": registro.get("quantidadeDeDicas", 0),
            "quantidadeIgnorada": registro.get("quantidadeIgnorada", 0),
        }

    def para_baralho(self, baralho):
        cards = baralho.get("cards") or []
        por_card = {card.get("id"): [[] for _ in (card.get("clues") or [])] for card in cards}
        indices = {card.get("id"): self._indice_de_dicas(card) for card in cards}
        relacionados = {}
        associados = 0
        nao_associados = 0
        baralho_id = baralho.get("id")

        for registro in self._registros():
            encontrados_na_lista = 0
            for item in registro.get("itens") or []:
                if item.get("baralhoId") != baralho_id:
                    continue
                card_id = item.get("cardId")
                indice = indices.get(card_id)
                posicao = self._localizar_dica(indice, item) if indice else None
                if posicao is None:
                    nao_associados += 1
                    continue
                exibido = {
                    "listaId": registro["id"],
                    "listaNome": registro["nome"],
                    "voto": item.get("voto"),
                    "comentario": item.get("comentario"),
                    "criadoEm": item.get("criadoEm"),
                    "rodada": item.get("rodada"),
                    "dicaId": item.get("dicaId"),
                    "textoAvaliado": item.get("textoAvaliado"),
                    "versaoDoBaralho": item.get("versaoDoBaralho"),
                }
                por_card[card_id][posicao].append(exibido)
                associados += 1
                encontrados_na_lista += 1
            if encontrados_na_lista:
                resumo = self._resumo_de(registro)
                resumo["quantidadeNesteBaralho"] = encontrados_na_lista
                relacionados[registro["id"]] = resumo

        for dicas in por_card.values():
            for itens in dicas:
                itens.sort(key=lambda item: item.get("criadoEm") or "", reverse=True)
        return {
            "porCard": por_card,
            "quantidadeNasDicas": associados,
            "quantidadeNaoAssociada": nao_associados,
            "listas": list(relacionados.values()),
        }

    def para_dicas(self, resposta_id, ids_de_dicas, referencias=()):
        """Feedbacks agrupados por dicaId, para o editor do acervo editorial.

        Ao contrário de `para_baralho`, não depende de baralhoId/cardId: os
        ids de dica do acervo (explícitos ou `legado:<texto normalizado>`) já
        são a mesma identidade que o app grava no feedback. MAS um dicaId só
        é único DENTRO de uma resposta — dois autores podem escolher "fato-1"
        para respostas diferentes, e dois cards legados podem ter uma dica
        com texto idêntico virando o mesmo `legado:<texto>` para respostas
        diferentes. Por isso o filtro por `respostaId` vem primeiro; o app
        sempre grava esse campo (`RegistroDeFeedback.respostaId`).
        """
        agrupado = {dica_id: [] for dica_id in (ids_de_dicas or [])}
        relacionados = {}
        for registro in self._registros():
            encontrados = 0
            for item in registro.get("itens") or []:
                identificada = item.get("respostaId")
                par_referenciado = any(
                    ref.get("baralhoId") == item.get("baralhoId") and ref.get("cardId") == item.get("cardId")
                    for ref in referencias
                )
                if (identificada and identificada != resposta_id) or (not identificada and not par_referenciado):
                    continue
                dica_id = item.get("dicaId")
                if not dica_id or (ids_de_dicas is not None and dica_id not in agrupado):
                    continue
                agrupado.setdefault(dica_id, []).append(
                    {
                        "listaId": registro["id"],
                        "listaNome": registro["nome"],
                        "voto": item.get("voto"),
                        "comentario": item.get("comentario"),
                        "criadoEm": item.get("criadoEm"),
                        "rodada": item.get("rodada"),
                        "textoAvaliado": item.get("textoAvaliado"),
                    }
                )
                encontrados += 1
            if encontrados:
                resumo = self._resumo_de(registro)
                resumo["quantidadeNestaResposta"] = encontrados
                relacionados[registro["id"]] = resumo
        for itens in agrupado.values():
            itens.sort(key=lambda item: item.get("criadoEm") or "", reverse=True)
        return {"porDica": agrupado, "listas": list(relacionados.values())}

    @staticmethod
    def _indice_de_dicas(card):
        clues = card.get("clues") or []
        por_id = {}
        por_texto = {}
        for posicao, texto in enumerate(clues):
            normalizado = _normalizar(texto)
            por_texto.setdefault(normalizado, posicao)
            por_id.setdefault("legado:{}".format(normalizado), posicao)
        for fato in card.get("bancoDeDicas") or []:
            if not isinstance(fato, dict):
                continue
            posicao = por_texto.get(_normalizar(fato.get("texto")))
            if posicao is not None and isinstance(fato.get("id"), str):
                por_id[fato["id"]] = posicao
        return {"porId": por_id, "porTexto": por_texto}

    @staticmethod
    def _localizar_dica(indice, item):
        posicao = indice["porId"].get(item.get("dicaId"))
        if posicao is not None:
            return posicao
        return indice["porTexto"].get(_normalizar(item.get("textoAvaliado")))
