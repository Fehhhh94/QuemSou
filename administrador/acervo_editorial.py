"""Acervo editorial: modelo administrativo de respostas e bancos de dicas.

Este modulo e puro (sem HTTP, sem Firestore de verdade) e cobre duas coisas:

1. A migracao dos cards/bancos JA existentes nas origens locais (asset e
   copia local do catalogo) para o modelo editorial descrito em
   `docs/ADMINISTRADOR_LOCAL.md`: uma resposta com id estavel e um banco de
   dicas com id estavel por dica. E so uma PREVIA local (dry-run) - nao ha
   escrita de rede aqui. A escrita de verdade no Firestore, quando
   autorizada, acontece do navegador com o mesmo padrao ja usado para o
   catalogo (`estaticos/app.js`), reaproveitando `estaticos/acervo.js`.

2. As regras puras de edicao de uma resposta e do seu banco: acrescentar,
   desativar/reativar (nunca apagar de verdade) e corrigir o texto de uma
   dica, sempre preservando o id.

O ALIAS de card legado (sem `respostaId`/`bancoDeDicas`) usa EXATAMENTE o
mesmo esquema que o app ja usa em tempo de jogo
(`domain/rules/SelecionadorDeDicas`): a identidade da resposta e o nome
normalizado e o id de cada dica legada e `legado:<texto normalizado>`. Isso
importa de verdade: um feedback real ja foi sincronizado para a dica 10 do
card `mm_028` (Shakira) sem banco editorial, e a migracao precisa apontar
para a MESMA identidade que o app usaria, nao inventar outra.
"""
from __future__ import annotations

import copy
import base64
import re
import secrets

from fontes import (
    COLECOES_TECNICAS,
    IDS_TECNICOS,
    FalhaDaFonte,
    forma_canonica,
    ler_envelope_do_asset,
    ler_todos_os_baralhos_do_catalogo,
    normalizar,
)

SCHEMA_VERSION = 1

ESCOPO_PUBLICO = "PUBLICO"
ESCOPO_PRIVADO = "PRIVADO"
ESCOPOS = (ESCOPO_PUBLICO, ESCOPO_PRIVADO)

STATUS_ATIVA = "ATIVA"
STATUS_REMOVIDA = "REMOVIDA"
STATUS_DE_DICA = (STATUS_ATIVA, STATUS_REMOVIDA)

ORIGEM_EDITORIAL = "EDITORIAL"
ORIGEM_ALIAS_LEGADO = "ALIAS_LEGADO"

# Mesma categoria tecnica que hoje decide PUBLICO/PRIVADO na publicacao do
# catalogo (docs/CATALOG_FORMAT.md e firestore.rules).
CATEGORIA_PRIVADA_POR_PADRAO = "ESPECIAIS"

MINIMO_DE_DICAS_PARA_PUBLICAR = 10
MAXIMO_DE_DICAS = 500
MAXIMO_DE_TEXTO_DA_DICA = 500


class FalhaDoAcervo(ValueError):
    """Erro previsto de edicao ou migracao do acervo editorial."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


# ---- identidade -----------------------------------------------------------

def escopo_do_baralho(baralho):
    """PRIVADO para ESPECIAIS, PUBLICO para os demais - mesma regra da publicação."""
    categoria = (baralho or {}).get("categoria")
    return ESCOPO_PRIVADO if categoria == CATEGORIA_PRIVADA_POR_PADRAO else ESCOPO_PUBLICO


def alias_de_resposta(answer):
    """Mesma identidade que `SelecionadorDeDicas.resposta()` usa em jogo."""
    identificador = normalizar(answer or "")
    if not identificador:
        raise FalhaDoAcervo(
            "resposta_sem_texto", "Um card sem resposta não gera identidade editorial."
        )
    return identificador


def alias_de_dica(texto):
    """Mesma identidade que `SelecionadorDeDicas.banco()` usa em jogo."""
    return "legado:{}".format(normalizar(texto or ""))


def id_do_documento(id_logico):
    """UTF-8/base64url, igual ao navegador; ids lógicos nunca são renomeados."""
    if not isinstance(id_logico, str) or not id_logico:
        raise ValueError("Id vazio.")
    codificado = "id_" + base64.urlsafe_b64encode(id_logico.encode("utf-8")).decode("ascii").rstrip("=")
    if len(codificado) > 1500:
        raise ValueError("Id excede o tamanho permitido.")
    return codificado


def id_logico_do_documento(id_documento):
    """Inverso exato de `id_do_documento`."""
    if not re.fullmatch(r"id_[A-Za-z0-9_-]+", id_documento or ""):
        raise ValueError("Id remoto inválido.")
    base = id_documento[3:]
    texto = base64.urlsafe_b64decode(base + "=" * (-len(base) % 4)).decode("utf-8")
    if id_do_documento(texto) != id_documento:
        raise ValueError("Id remoto não canônico.")
    return texto


def escopo_padrao_para_nova_dica(dicas):
    """PRIVADO quando a resposta já é majoritária/empatadamente privada.

    Nunca defaultar para PUBLICO quando há dúvida: uma resposta sem nenhuma
    dica pública ainda não tem por que ganhar uma dica pública "de graça".
    """
    lista = list(dicas.values()) if isinstance(dicas, dict) else list(dicas or [])
    if not lista:
        return ESCOPO_PUBLICO
    quantidade_privada = sum(1 for d in lista if d.get("escopo") == ESCOPO_PRIVADO)
    quantidade_publica = sum(1 for d in lista if d.get("escopo") == ESCOPO_PUBLICO)
    return ESCOPO_PRIVADO if quantidade_privada >= quantidade_publica else ESCOPO_PUBLICO


def _identidade_do_card(card):
    resposta_id = card.get("respostaId") or ""
    if resposta_id:
        return resposta_id, ORIGEM_EDITORIAL
    return alias_de_resposta(card.get("answer")), ORIGEM_ALIAS_LEGADO


def _fatos_do_card(card):
    banco = card.get("bancoDeDicas")
    if isinstance(banco, list) and banco:
        return [
            (fato.get("id"), fato.get("texto"), ORIGEM_EDITORIAL)
            for fato in banco
            if isinstance(fato, dict) and fato.get("id")
        ]
    return [
        (alias_de_dica(texto), texto, ORIGEM_ALIAS_LEGADO)
        for texto in (card.get("clues") or [])
        if isinstance(texto, str) and texto.strip()
    ]


# ---- previa de migracao ----------------------------------------------------

class _EstadoDoAcervo:
    def __init__(self):
        self.respostas = {}
        self.dicas = {}
        self.conflitos = []


def _mesclar_resposta(estado, resposta_id, texto, tipo, origem, baralho_id, card_id):
    referencia = {"baralhoId": baralho_id, "cardId": card_id}
    existente = estado.respostas.get(resposta_id)
    if existente is None:
        estado.respostas[resposta_id] = {
            "schemaVersion": SCHEMA_VERSION,
            "respostaId": resposta_id,
            "texto": texto,
            "tipo": tipo,
            "origem": origem,
            "referencias": [referencia],
        }
        return
    if referencia not in existente["referencias"]:
        existente["referencias"].append(referencia)
    if existente["texto"] != texto or existente["tipo"] != tipo:
        estado.conflitos.append(
            {
                "tipo": "resposta_divergente",
                "respostaId": resposta_id,
                "textoExistente": existente["texto"],
                "textoEncontrado": texto,
                "baralhoId": baralho_id,
                "cardId": card_id,
            }
        )


def _mesclar_dica(estado, resposta_id, dica_id, texto, origem, escopo, baralho_id, card_id):
    banco = estado.dicas.setdefault(resposta_id, {})
    existente = banco.get(dica_id)
    if existente is None:
        banco[dica_id] = {
            "schemaVersion": SCHEMA_VERSION,
            "dicaId": dica_id,
            "texto": texto,
            "escopo": escopo,
            "status": STATUS_ATIVA,
            "origem": origem,
            "revisaoTecnica": 1,
        }
        return
    if existente["texto"] != texto:
        estado.conflitos.append(
            {
                "tipo": "dica_divergente",
                "respostaId": resposta_id,
                "dicaId": dica_id,
                "textoExistente": existente["texto"],
                "textoEncontrado": texto,
                "baralhoId": baralho_id,
                "cardId": card_id,
            }
        )
        return
    if existente["escopo"] != escopo:
        # Nunca relaxar: um encontro PRIVADO some com o vazamento em publicação futura.
        if ESCOPO_PRIVADO in (existente["escopo"], escopo):
            existente["escopo"] = ESCOPO_PRIVADO
        estado.conflitos.append(
            {
                "tipo": "escopo_divergente",
                "respostaId": resposta_id,
                "dicaId": dica_id,
                "escopoResolvido": existente["escopo"],
                "baralhoId": baralho_id,
                "cardId": card_id,
            }
        )


def _e_baralho_tecnico(baralho):
    colecao = baralho.get("colecao") if isinstance(baralho.get("colecao"), dict) else {}
    return (baralho.get("id") or "") in IDS_TECNICOS or colecao.get("id") in COLECOES_TECNICAS


def _baralhos_para_migrar(config, incluir_tecnicos=False):
    """Todos os baralhos do asset MAIS todos os arquivos locais do catálogo.

    Não escolhe uma origem "vencedora" por id: cópias idênticas mesclam sem
    conflito (mesmo texto/tipo/escopo) e cópias realmente divergentes viram
    conflito explícito, nunca uma perda silenciosa de conteúdo. Baralhos
    técnicos de teste (`baralho-de-teste-1` e afins) ficam de fora por
    padrão, como no resto do painel; falha ao ler uma origem vira aviso, não
    silêncio.
    """
    baralhos = []
    avisos = []
    try:
        envelope, _ = ler_envelope_do_asset(config)
        baralhos.extend(b for b in envelope.get("baralhos", []) if isinstance(b, dict))
    except FalhaDaFonte as falha:
        avisos.append("Biblioteca local legada: {}".format(falha.mensagem))
    arquivos, avisos_do_catalogo = ler_todos_os_baralhos_do_catalogo(config)
    avisos.extend("Cópia local do catálogo: {}".format(aviso) for aviso in avisos_do_catalogo)
    baralhos.extend(dados for dados in arquivos.values() if isinstance(dados, dict))
    if not incluir_tecnicos:
        baralhos = [b for b in baralhos if not _e_baralho_tecnico(b)]
    return baralhos, avisos


def _detectar_duplicatas_de_alias(estado):
    """Um alias legado e uma resposta editorial com o MESMO texto normalizado
    quase certamente descrevem a mesma resposta em duas identidades
    desconectadas — nunca fundir automaticamente, só relatar com clareza para
    decisão humana (dar `respostaId` explícito ao card legado, por exemplo).
    """
    por_texto = {}
    for resposta_id, resposta in estado.respostas.items():
        por_texto.setdefault(normalizar(resposta.get("texto") or ""), []).append(resposta_id)
    for texto_normalizado, ids in por_texto.items():
        if len(ids) < 2 or not texto_normalizado:
            continue
        origens = {estado.respostas[rid]["origem"] for rid in ids}
        if len(origens) < 2:
            continue
        estado.conflitos.append(
            {
                "tipo": "alias_e_editorial_duplicados",
                "textoNormalizado": texto_normalizado,
                "respostaIds": sorted(ids),
            }
        )


def gerar_previa_da_migracao(config, incluir_tecnicos=False):
    """Dry-run puro: conta respostas/dicas e relata conflitos, sem gravar nada."""
    estado = _EstadoDoAcervo()
    baralhos_processados = 0
    cards_processados = 0
    baralhos, avisos = _baralhos_para_migrar(config, incluir_tecnicos=incluir_tecnicos)
    for baralho in sorted(baralhos, key=lambda b: b.get("id") or ""):
        baralho_id = baralho.get("id") or ""
        escopo_padrao = escopo_do_baralho(baralho)
        baralhos_processados += 1
        for card in baralho.get("cards") or []:
            if not isinstance(card, dict):
                continue
            cards_processados += 1
            try:
                resposta_id, origem_resposta = _identidade_do_card(card)
            except FalhaDoAcervo:
                continue
            _mesclar_resposta(
                estado,
                resposta_id,
                card.get("answer", ""),
                card.get("type", ""),
                origem_resposta,
                baralho_id,
                card.get("id"),
            )
            for dica_id, texto, origem_dica in _fatos_do_card(card):
                _mesclar_dica(
                    estado, resposta_id, dica_id, texto, origem_dica, escopo_padrao,
                    baralho_id, card.get("id"),
                )
    _detectar_duplicatas_de_alias(estado)
    quantidade_de_dicas = sum(len(banco) for banco in estado.dicas.values())
    bancos_acima_do_limite = sorted(
        [
            {"respostaId": resposta_id, "quantidade": len(banco)}
            for resposta_id, banco in estado.dicas.items()
            if len(banco) > MAXIMO_DE_DICAS
        ],
        key=lambda item: item["respostaId"],
    )
    return {
        "schemaVersion": SCHEMA_VERSION,
        "incluiTecnicos": incluir_tecnicos,
        "avisos": avisos,
        "baralhosProcessados": baralhos_processados,
        "cardsProcessados": cards_processados,
        "quantidadeDeRespostas": len(estado.respostas),
        "quantidadeDeDicas": quantidade_de_dicas,
        "respostasComAliasLegado": sum(
            1 for r in estado.respostas.values() if r["origem"] == ORIGEM_ALIAS_LEGADO
        ),
        "conflitos": estado.conflitos,
        "bancosAcimaDoLimite": bancos_acima_do_limite,
        "segura": not estado.conflitos and not bancos_acima_do_limite,
        "respostas": estado.respostas,
        "dicas": estado.dicas,
    }


def respostas_seguras_para_migrar(previa):
    """Ids de resposta sem conflito e dentro do teto — só estas podem ser
    gravadas no Firestore por uma migração automática; as demais exigem
    decisão humana antes.
    """
    respostas_com_problema = set()
    for conflito in previa.get("conflitos") or []:
        if conflito.get("respostaId"):
            respostas_com_problema.add(conflito["respostaId"])
        for resposta_id in conflito.get("respostaIds") or []:
            respostas_com_problema.add(resposta_id)
    for item in previa.get("bancosAcimaDoLimite") or []:
        respostas_com_problema.add(item["respostaId"])
    return [
        resposta_id
        for resposta_id in previa.get("respostas", {})
        if resposta_id not in respostas_com_problema
    ]


def execucoes_equivalentes(previa_a, previa_b):
    """True quando duas prévias descrevem o mesmo acervo (prova de idempotência)."""

    def chave(previa):
        return forma_canonica({"respostas": previa.get("respostas"), "dicas": previa.get("dicas")})

    return chave(previa_a) == chave(previa_b)


# ---- publicação: nunca deixar PRIVADO vazar para PUBLICO -------------------

def dicas_para_publicacao(dicas, visibilidade_do_baralho):
    """Filtra dicas ATIVAS pelo escopo compatível com a visibilidade do baralho.

    PUBLICO só recebe dicas de escopo PUBLICO. PRIVADO recebe as duas, porque
    quem já lê PRIVADO tem acesso ao conteúdo público de qualquer forma.
    """
    permitidos = (ESCOPO_PUBLICO,) if visibilidade_do_baralho == ESCOPO_PUBLICO else ESCOPOS
    vistos = set()
    selecionadas = []
    origem = dicas.values() if isinstance(dicas, dict) else (dicas or [])
    for dica in origem:
        if not isinstance(dica, dict):
            continue
        if dica.get("status") != STATUS_ATIVA:
            continue
        if dica.get("escopo") not in permitidos:
            continue
        chave_texto = normalizar(dica.get("texto") or "")
        if not chave_texto or chave_texto in vistos:
            continue
        vistos.add(chave_texto)
        selecionadas.append(dica)
    return selecionadas


def pronta_para_publicar(dicas, visibilidade_do_baralho):
    quantidade = len(dicas_para_publicacao(dicas, visibilidade_do_baralho))
    return MINIMO_DE_DICAS_PARA_PUBLICAR <= quantidade <= MAXIMO_DE_DICAS


# ---- edição de uma dica -----------------------------------------------------

def _texto_da_dica(texto):
    if not isinstance(texto, str):
        raise FalhaDoAcervo("campo_invalido", "O texto da dica precisa ser texto.")
    texto = texto.strip()
    if not texto:
        raise FalhaDoAcervo("campo_invalido", "A dica não pode ficar em branco.")
    if len(texto) > MAXIMO_DE_TEXTO_DA_DICA:
        raise FalhaDoAcervo(
            "campo_invalido",
            "A dica passa de {} caracteres.".format(MAXIMO_DE_TEXTO_DA_DICA),
        )
    return texto


def _ids_existentes(dicas_existentes):
    if isinstance(dicas_existentes, dict):
        valores = dicas_existentes.values()
    else:
        valores = dicas_existentes or []
    return {
        (d.get("dicaId") if isinstance(d, dict) else None) for d in valores
    }


def gerar_id_de_dica(resposta_id, dicas_existentes):
    """Sufixo aleatório: nunca reaproveita o id de uma dica já removida."""
    prefixo = "{}-d".format((resposta_id or "resposta")[:48])
    existentes = _ids_existentes(dicas_existentes)
    while True:
        candidato = "{}{}".format(prefixo, secrets.token_hex(6))
        if candidato not in existentes:
            return candidato


def acrescentar_dica(resposta_id, dicas_existentes, texto, escopo):
    if escopo not in ESCOPOS:
        raise FalhaDoAcervo("escopo_invalido", "Escopo de dica desconhecido.")
    quantidade_atual = len(dicas_existentes) if dicas_existentes is not None else 0
    if quantidade_atual >= MAXIMO_DE_DICAS:
        raise FalhaDoAcervo(
            "teto_de_dicas", "O teto é de {} dicas por resposta.".format(MAXIMO_DE_DICAS)
        )
    identificador = gerar_id_de_dica(resposta_id, dicas_existentes)
    return {
        "schemaVersion": SCHEMA_VERSION,
        "dicaId": identificador,
        "texto": _texto_da_dica(texto),
        "escopo": escopo,
        "status": STATUS_ATIVA,
        "origem": ORIGEM_EDITORIAL,
        "revisaoTecnica": 1,
    }


def editar_texto_da_dica(dica, novo_texto):
    """Correção factual: o id nunca muda, só o texto e a revisão técnica."""
    if dica.get("status") == STATUS_REMOVIDA:
        raise FalhaDoAcervo(
            "dica_removida", "Reative a dica antes de corrigir o texto."
        )
    novo = copy.deepcopy(dica)
    novo["texto"] = _texto_da_dica(novo_texto)
    novo["revisaoTecnica"] = int(dica.get("revisaoTecnica") or 0) + 1
    return novo


def alterar_escopo_da_dica(dica, escopo):
    if escopo not in ESCOPOS:
        raise FalhaDoAcervo("escopo_invalido", "Escopo de dica desconhecido.")
    novo = copy.deepcopy(dica)
    novo["escopo"] = escopo
    novo["revisaoTecnica"] = int(dica.get("revisaoTecnica") or 0) + 1
    return novo


def desativar_dica(dica):
    """Recuperável: só muda o status, nunca apaga o documento nem o id."""
    if dica.get("status") == STATUS_REMOVIDA:
        return dica
    novo = copy.deepcopy(dica)
    novo["status"] = STATUS_REMOVIDA
    novo["revisaoTecnica"] = int(dica.get("revisaoTecnica") or 0) + 1
    return novo


def reativar_dica(dica):
    if dica.get("status") == STATUS_ATIVA:
        return dica
    novo = copy.deepcopy(dica)
    novo["status"] = STATUS_ATIVA
    novo["revisaoTecnica"] = int(dica.get("revisaoTecnica") or 0) + 1
    return novo
