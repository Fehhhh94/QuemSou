"""Regras de edicao de baralho, puras e testaveis sem HTTP.

O que este modulo garante, independentemente do que o navegador enviar:

* valores antigos de estado continuam aceitos, sem criar bloqueio editorial;
* id de baralho, id de card, respostaId e id de fato do banco nunca mudam;
* campos desconhecidos do JSON sao preservados (o painel nao e dono do
  contrato: quem manda e docs/CATALOG_FORMAT.md e o app);
* corrigir uma das dez dicas mantem o id do fato correspondente e atualiza o
  texto dele no banco, sem descartar os demais fatos.

A regua editorial de verdade NAO esta aqui. Estas checagens sao de interface,
para dar erro cedo e legivel; a autoridade e o ValidadorEditorial do app,
chamado por Gradle em validacao.py.
"""
from __future__ import annotations

import copy
import re
import secrets

from fontes import (
    ESTADO_EM_DESENVOLVIMENTO,
    ESTADOS_COMPATIVEIS,
    PADRAO_DE_ID,
    normalizar,
)

TIPOS_DE_CARD = ("PESSOA", "LUGAR", "COISA")
CATEGORIAS = ("PERSONAGEM_FILME", "MUNDO_DA_MUSICA", "ESPECIAIS")
QUANTIDADE_DE_DICAS = 10
MAXIMO_DE_CARDS = 500
MAXIMO_DE_TEXTO = 500


class FalhaDaEdicao(ValueError):
    """Erro previsto de edicao, com codigo estavel e mensagem para a tela."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


def assegurar_editavel(baralho):
    """Aceita os estados legados e recusa apenas formato desconhecido."""
    if (baralho or {}).get("estado") not in ESTADOS_COMPATIVEIS:
        raise FalhaDaEdicao(
            "estado_incompativel",
            "O estado técnico deste baralho não é reconhecido por esta versão do painel.",
        )


def _texto(valor, rotulo, permitir_vazio=False):
    if not isinstance(valor, str):
        raise FalhaDaEdicao("campo_invalido", "{} precisa ser texto.".format(rotulo))
    if len(valor) > MAXIMO_DE_TEXTO:
        raise FalhaDaEdicao(
            "campo_invalido",
            "{} passa de {} caracteres.".format(rotulo, MAXIMO_DE_TEXTO),
        )
    if not permitir_vazio and not valor.strip():
        raise FalhaDaEdicao("campo_invalido", "{} não pode ficar em branco.".format(rotulo))
    return valor


def _mapa_de_dica_para_fato(clues, banco):
    """Liga cada uma das dez dicas ao fato do banco que tem o mesmo texto."""
    if not isinstance(banco, list):
        return {}
    por_texto = {}
    for fato in banco:
        if isinstance(fato, dict) and isinstance(fato.get("texto"), str):
            por_texto.setdefault(normalizar(fato["texto"]), fato.get("id"))
    mapa = {}
    for posicao, dica in enumerate(clues):
        if isinstance(dica, str):
            fato_id = por_texto.get(normalizar(dica))
            if fato_id is not None:
                mapa[posicao] = fato_id
    return mapa


def aplicar_edicao_no_card(original, proposta):
    """Devolve uma copia de "original" com as alteracoes aceitas de "proposta".

    Preserva tudo que nao foi editado, inclusive campos que este painel nem
    conhece. Ids (do card, da resposta e dos fatos) sao lidos so para conferir
    que continuam iguais; nenhum deles e regravado a partir do navegador.
    """
    if not isinstance(proposta, dict):
        raise FalhaDaEdicao("campo_invalido", "Card inválido.")
    novo = copy.deepcopy(original)

    if "answer" in proposta:
        novo["answer"] = _texto(proposta["answer"], "A resposta", permitir_vazio=True)
    if "type" in proposta:
        if proposta["type"] not in TIPOS_DE_CARD:
            raise FalhaDaEdicao("campo_invalido", "Tipo de card desconhecido.")
        novo["type"] = proposta["type"]

    clues_originais = list(original.get("clues") or [])
    banco_original = original.get("bancoDeDicas")
    tem_banco = isinstance(banco_original, list)
    mapa = _mapa_de_dica_para_fato(clues_originais, banco_original)

    textos_originais_do_fato = {}
    if tem_banco:
        for fato in banco_original:
            if isinstance(fato, dict):
                textos_originais_do_fato[fato.get("id")] = fato.get("texto")
    textos_do_fato = dict(textos_originais_do_fato)

    if "bancoDeDicas" in proposta:
        if not tem_banco:
            raise FalhaDaEdicao(
                "banco_inexistente",
                "Este card não tem banco de dicas; só as dez dicas podem ser editadas.",
            )
        propostos = proposta["bancoDeDicas"]
        if not isinstance(propostos, list) or len(propostos) != len(banco_original):
            raise FalhaDaEdicao(
                "banco_alterado",
                "O banco de dicas não pode ganhar nem perder fatos por aqui.",
            )
        for fato_original, fato_proposto in zip(banco_original, propostos):
            if not isinstance(fato_proposto, dict):
                raise FalhaDaEdicao("campo_invalido", "Fato do banco inválido.")
            if fato_proposto.get("id") != fato_original.get("id"):
                raise FalhaDaEdicao(
                    "banco_alterado", "O id de um fato do banco não pode mudar."
                )
            textos_do_fato[fato_original.get("id")] = _texto(
                fato_proposto.get("texto"), "A dica do banco"
            )

    clues_finais = list(clues_originais)
    if "clues" in proposta:
        propostas = proposta["clues"]
        if not isinstance(propostas, list) or len(propostas) != QUANTIDADE_DE_DICAS:
            raise FalhaDaEdicao(
                "quantidade_de_dicas",
                "São exatamente {} dicas de compatibilidade.".format(QUANTIDADE_DE_DICAS),
            )
        if len(clues_originais) != QUANTIDADE_DE_DICAS:
            clues_finais = [""] * QUANTIDADE_DE_DICAS
        for posicao, dica in enumerate(propostas):
            dica = _texto(dica, "A dica {}".format(posicao + 1), permitir_vazio=True)
            anterior = clues_originais[posicao] if posicao < len(clues_originais) else None
            if dica == anterior:
                continue
            clues_finais[posicao] = dica
            if tem_banco:
                fato_id = mapa.get(posicao)
                if fato_id is None:
                    raise FalhaDaEdicao(
                        "dica_sem_fato",
                        "A dica {} não corresponde a nenhum fato do banco; "
                        "corrija o fato no banco.".format(posicao + 1),
                    )
                # Correcao factual: o id do fato continua o mesmo.
                textos_do_fato[fato_id] = dica

    # Fato editado direto no banco reflete na dica de compatibilidade que o usa.
    for posicao, fato_id in mapa.items():
        if posicao >= len(clues_finais):
            continue
        if clues_finais[posicao] != clues_originais[posicao]:
            continue
        if textos_do_fato.get(fato_id) != textos_originais_do_fato.get(fato_id):
            clues_finais[posicao] = textos_do_fato[fato_id]

    if clues_originais or "clues" in proposta:
        novo["clues"] = clues_finais
    if tem_banco:
        for fato in novo["bancoDeDicas"]:
            if isinstance(fato, dict) and fato.get("id") in textos_do_fato:
                fato["texto"] = textos_do_fato[fato["id"]]
    return novo


def aplicar_edicao_no_baralho(original, proposta):
    """Aplica nome, agrupamento e cards; recusa tudo o que nao pode mudar."""
    assegurar_editavel(original)
    if not isinstance(proposta, dict):
        raise FalhaDaEdicao("campo_invalido", "Alteração inválida.")
    for campo in ("id", "estado", "versao"):
        if campo in proposta and proposta[campo] != original.get(campo):
            raise FalhaDaEdicao(
                "campo_imutavel",
                "Id, estado e versão não são editáveis no painel.",
            )

    novo = copy.deepcopy(original)
    if "nome" in proposta:
        novo["nome"] = _texto(proposta["nome"], "O nome do baralho")
    if "categoria" in proposta:
        if proposta["categoria"] not in CATEGORIAS:
            raise FalhaDaEdicao("campo_invalido", "Categoria desconhecida.")
        novo["categoria"] = proposta["categoria"]
    if "colecao" in proposta:
        colecao = proposta["colecao"]
        if not isinstance(colecao, dict):
            raise FalhaDaEdicao("campo_invalido", "Agrupamento inválido.")
        atual = novo.get("colecao") if isinstance(novo.get("colecao"), dict) else {}
        if "id" in colecao and colecao["id"] != atual.get("id"):
            raise FalhaDaEdicao(
                "campo_imutavel", "O id do agrupamento não é editável no painel."
            )
        if "nome" in colecao:
            atual["nome"] = _texto(colecao["nome"], "O nome do agrupamento")
        if "icone" in colecao:
            atual["icone"] = _texto(colecao["icone"], "O ícone do agrupamento")
        novo["colecao"] = atual

    if "cards" in proposta:
        propostos = proposta["cards"]
        cards_originais = original.get("cards") or []
        if not isinstance(propostos, list) or len(propostos) != len(cards_originais):
            raise FalhaDaEdicao(
                "cards_alterados",
                "Cards não podem ser acrescentados nem removidos nesta edição.",
            )
        cards = []
        for card_original, card_proposto in zip(cards_originais, propostos):
            if not isinstance(card_proposto, dict):
                raise FalhaDaEdicao("campo_invalido", "Card inválido.")
            if card_proposto.get("id") != card_original.get("id"):
                raise FalhaDaEdicao("campo_imutavel", "O id de um card não pode mudar.")
            if (
                "respostaId" in card_proposto
                and card_proposto["respostaId"] != card_original.get("respostaId")
            ):
                raise FalhaDaEdicao("campo_imutavel", "O respostaId não pode mudar.")
            cards.append(aplicar_edicao_no_card(card_original, card_proposto))
        novo["cards"] = cards

    assegurar_identidade_preservada(original, novo)
    return novo


def _ids_de_fato(card):
    banco = card.get("bancoDeDicas")
    if not isinstance(banco, list):
        return None
    return [fato.get("id") for fato in banco if isinstance(fato, dict)]


def assegurar_identidade_preservada(original, novo):
    """Ultima barreira antes de salvar ou aplicar: nada de identidade mudou."""
    for campo in ("id", "estado", "versao"):
        if original.get(campo) != novo.get(campo):
            raise FalhaDaEdicao("identidade_alterada", "Identidade do baralho alterada.")
    colecao_original = original.get("colecao") or {}
    colecao_nova = novo.get("colecao") or {}
    if colecao_original.get("id") != colecao_nova.get("id"):
        raise FalhaDaEdicao("identidade_alterada", "Id do agrupamento alterado.")
    cards_originais = original.get("cards") or []
    cards_novos = novo.get("cards") or []
    if len(cards_originais) != len(cards_novos):
        raise FalhaDaEdicao("identidade_alterada", "Conjunto de cards alterado.")
    for card_original, card_novo in zip(cards_originais, cards_novos):
        if card_original.get("id") != card_novo.get("id"):
            raise FalhaDaEdicao("identidade_alterada", "Id de card alterado.")
        if card_original.get("respostaId") != card_novo.get("respostaId"):
            raise FalhaDaEdicao("identidade_alterada", "respostaId alterado.")
        if _ids_de_fato(card_original) != _ids_de_fato(card_novo):
            raise FalhaDaEdicao("identidade_alterada", "Ids do banco de dicas alterados.")


def slug(texto, limite=48):
    """Slug estavel no formato dos ids reais: minusculo, sem acento, com hifen."""
    base = re.sub(r"[^a-z0-9]+", "-", normalizar(texto)).strip("-")
    return base[:limite].strip("-")


def criar_baralho_de_rascunho(nome, grupo, icone, categoria, sufixo=None):
    """Monta um baralho novo, vazio, apenas para o rascunho privado.

    Nao entra no indice do catalogo nem no asset: rascunho novo so sai daqui
    por exportacao deliberada, descrita no README.
    """
    nome = _texto(nome, "O nome do baralho")
    grupo = _texto(grupo, "O nome do agrupamento")
    icone = _texto(icone, "O ícone do agrupamento")
    if categoria not in CATEGORIAS:
        raise FalhaDaEdicao("campo_invalido", "Categoria desconhecida.")
    base = slug(nome)
    if not base:
        raise FalhaDaEdicao("campo_invalido", "O nome não gera um id válido.")
    identificador = "{}-{}".format(base, sufixo) if sufixo else base
    if not PADRAO_DE_ID.match(identificador):
        raise FalhaDaEdicao("campo_invalido", "O nome não gera um id válido.")
    return {
        "id": identificador,
        "nome": nome,
        "categoria": categoria,
        "colecao": {"id": slug(grupo) or base, "nome": grupo, "icone": icone},
        "versao": 1,
        "estado": ESTADO_EM_DESENVOLVIMENTO,
        "cards": [],
    }


def proximo_id_de_card(baralho):
    """Gera um id estável que não depende dos cards ainda presentes.

    Um sufixo aleatório evita reaproveitar o id de um card removido. Depois de
    criado, o id continua imutável pelas demais barreiras do editor.
    """
    prefixo = "{}-c".format((baralho.get("id") or "rascunho")[:48])
    existentes = {
        (card or {}).get("id") for card in (baralho.get("cards") or []) if isinstance(card, dict)
    }
    while True:
        identificador = "{}{}".format(prefixo, secrets.token_hex(6))
        if identificador not in existentes:
            return identificador


def acrescentar_card(baralho):
    """Acrescenta um card vazio ao rascunho e devolve (baralho, id do card)."""
    assegurar_editavel(baralho)
    cards = list(baralho.get("cards") or [])
    if len(cards) >= MAXIMO_DE_CARDS:
        raise FalhaDaEdicao(
            "teto_de_cards",
            "O teto é de {} cards por baralho.".format(MAXIMO_DE_CARDS),
        )
    novo = copy.deepcopy(baralho)
    identificador = proximo_id_de_card(novo)
    cards.append(
        {
            "id": identificador,
            "type": "PESSOA",
            "answer": "",
            "clues": [""] * QUANTIDADE_DE_DICAS,
        }
    )
    novo["cards"] = cards
    return novo, identificador


def remover_card_do_rascunho(baralho, card_id):
    """Remove um card do rascunho novo. Nao existe para baralho de origem."""
    assegurar_editavel(baralho)
    originais = baralho.get("cards") or []
    cards = [card for card in originais if card.get("id") != card_id]
    if len(cards) == len(originais):
        raise FalhaDaEdicao("card_ausente", "Card não encontrado no rascunho.")
    novo = copy.deepcopy(baralho)
    novo["cards"] = cards
    return novo


# ---- projeção do acervo editorial (trilha separada da edição manual) ------
#
# `aplicar_edicao_no_card` recusa qualquer banco com tamanho diferente do
# original de propósito (edição manual nunca ganha/perde fatos). A projeção
# do acervo editorial é o oposto: o banco REMOTO manda, e pode ter 10 a 500
# fatos, diferente do que está hoje embarcado no card. Ainda assim id do
# card, respostaId, resposta e tipo nunca mudam.

MAXIMO_DE_DICAS_NO_BANCO = 500


def substituir_banco_do_card(card_original, banco_de_dicas, clues):
    """Projeta um banco vindo do acervo editorial remoto para dentro do card.

    `banco_de_dicas` e `clues` já vêm prontos (montados por
    `acervo_editorial`/`acervo.js` a partir das dicas ATIVAS autorizadas pelo
    escopo do baralho); esta função só valida a forma e preserva identidade.
    """
    if not isinstance(banco_de_dicas, list) or not (
        QUANTIDADE_DE_DICAS <= len(banco_de_dicas) <= MAXIMO_DE_DICAS_NO_BANCO
    ):
        raise FalhaDaEdicao(
            "banco_invalido",
            "O banco projetado precisa ter entre {} e {} dicas.".format(
                QUANTIDADE_DE_DICAS, MAXIMO_DE_DICAS_NO_BANCO
            ),
        )
    ids_vistos = set()
    textos_por_id = {}
    for fato in banco_de_dicas:
        if not isinstance(fato, dict):
            raise FalhaDaEdicao("campo_invalido", "Fato do banco projetado inválido.")
        identificador = fato.get("id")
        texto = fato.get("texto")
        if not isinstance(identificador, str) or not identificador:
            raise FalhaDaEdicao("campo_invalido", "Fato do banco projetado sem id.")
        if identificador in ids_vistos:
            raise FalhaDaEdicao("campo_invalido", "Id de fato repetido no banco projetado.")
        if not isinstance(texto, str) or not texto.strip():
            raise FalhaDaEdicao("campo_invalido", "Fato do banco projetado sem texto.")
        ids_vistos.add(identificador)
        textos_por_id[identificador] = texto

    if not isinstance(clues, list) or len(clues) != QUANTIDADE_DE_DICAS:
        raise FalhaDaEdicao(
            "quantidade_de_dicas",
            "São exatamente {} dicas de compatibilidade.".format(QUANTIDADE_DE_DICAS),
        )
    textos_disponiveis = set(textos_por_id.values())
    for clue in clues:
        if clue not in textos_disponiveis:
            raise FalhaDaEdicao(
                "dica_sem_fato",
                "Uma dica de compatibilidade não pertence ao banco projetado.",
            )

    novo = copy.deepcopy(card_original)
    antigos = {f["id"]: f for f in card_original.get("bancoDeDicas", [])}
    novo["bancoDeDicas"] = [
        {**copy.deepcopy(antigos.get(f["id"], {})), **copy.deepcopy(f)}
        for f in banco_de_dicas
    ]
    novo["respostaId"] = card_original.get("respostaId") or normalizar(card_original.get("answer", ""))
    novo["clues"] = list(clues)
    return novo


def aplicar_projecao_no_baralho(baralho_original, projecoes_por_card_id):
    """Substitui banco/clues só dos cards presentes em `projecoes_por_card_id`.

    `projecoes_por_card_id`: ``{cardId: {"bancoDeDicas": [...], "clues": [...]}}``.
    Cards fora do mapa saem intactos. Nunca gera nem remove card.
    """
    assegurar_editavel(baralho_original)
    if not isinstance(projecoes_por_card_id, dict) or not projecoes_por_card_id:
        raise FalhaDaEdicao(
            "projecao_vazia", "Nenhuma projeção de banco foi enviada."
        )
    cards_originais = baralho_original.get("cards") or []
    ids_do_baralho = {card.get("id") for card in cards_originais}
    desconhecidos = set(projecoes_por_card_id) - ids_do_baralho
    if desconhecidos:
        raise FalhaDaEdicao(
            "card_ausente",
            "Card(s) fora deste baralho: {}.".format(", ".join(sorted(desconhecidos))),
        )
    cards = []
    for card_original in cards_originais:
        projecao = projecoes_por_card_id.get(card_original.get("id"))
        if projecao is None:
            cards.append(card_original)
            continue
        cards.append(
            substituir_banco_do_card(
                card_original,
                (projecao or {}).get("bancoDeDicas"),
                (projecao or {}).get("clues"),
            )
        )
    novo = copy.deepcopy(baralho_original)
    novo["cards"] = cards
    _assegurar_identidade_de_projecao(baralho_original, novo)
    return novo


def _assegurar_identidade_de_projecao(original, novo):
    """Mesma barreira final de `assegurar_identidade_preservada`, mas tolera
    banco/clues de tamanho diferente (é isso que a projeção existe para fazer).
    """
    for campo in ("id", "estado", "versao"):
        if original.get(campo) != novo.get(campo):
            raise FalhaDaEdicao("identidade_alterada", "Identidade do baralho alterada.")
    cards_originais = original.get("cards") or []
    cards_novos = novo.get("cards") or []
    if len(cards_originais) != len(cards_novos):
        raise FalhaDaEdicao("identidade_alterada", "Conjunto de cards alterado.")
    for card_original, card_novo in zip(cards_originais, cards_novos):
        if card_original.get("id") != card_novo.get("id"):
            raise FalhaDaEdicao("identidade_alterada", "Id de card alterado.")
        esperada = card_original.get("respostaId") or normalizar(card_original.get("answer", ""))
        if card_novo != card_original and esperada != card_novo.get("respostaId"):
            raise FalhaDaEdicao("identidade_alterada", "respostaId alterado.")
        if card_original.get("answer") != card_novo.get("answer"):
            raise FalhaDaEdicao("identidade_alterada", "Resposta do card alterada na projeção.")
        if card_original.get("type") != card_novo.get("type"):
            raise FalhaDaEdicao("identidade_alterada", "Tipo do card alterado na projeção.")


def assegurar_identidade_do_registro(original, registro):
    """Aceita somente as identidades de banco autorizadas pela rota de projeção.

    A edição manual continua estrita. O certificado é gerado no servidor,
    não recebido do formulário, e não aprova conteúdo: Gradle ainda valida
    o candidato exato antes da aplicação/publicação.
    """
    if registro["baralho"].get("categoria") != "ESPECIAIS" and any(
        f.get("escopo") == "PRIVADO" for c in registro["baralho"].get("cards", []) for f in c.get("bancoDeDicas", [])
    ):
        raise FalhaDaEdicao("escopo_privado", "Banco com dicas privadas não pode ser público.")
    esperado = copy.deepcopy(original)
    autorizadas = registro.get("projecoesDoAcervo", {})
    cards = {c["id"]: c for c in esperado.get("cards", [])}
    for card_id, autorizacao in autorizadas.items():
        card = cards.get(card_id)
        if card is None:
            raise FalhaDaEdicao("card_ausente", "Projeção fora do baralho.")
        resposta_id = card.get("respostaId") or normalizar(card.get("answer", ""))
        if resposta_id != autorizacao["respostaId"]:
            raise FalhaDaEdicao("identidade_alterada", "Resposta da projeção alterada.")
        if registro["baralho"].get("categoria") != "ESPECIAIS" and "PRIVADO" in autorizacao["escopos"]:
            raise FalhaDaEdicao("escopo_privado", "Dicas privadas não podem entrar em baralho público.")
        card["respostaId"] = resposta_id
        card["bancoDeDicas"] = [{"id": id_} for id_ in autorizacao["ids"]]
    assegurar_identidade_preservada(esperado, registro["baralho"])
