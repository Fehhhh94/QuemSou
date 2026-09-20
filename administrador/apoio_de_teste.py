"""Fixtures temporarias dos testes. Nenhum teste toca os acervos reais.

Monta, dentro de um diretorio temporario, um repositorio de app e um catalogo
com os casos que interessam: estado legado, baralho atualizavel com banco
de dicas e campo desconhecido, id repetido em duas origens, arquivo local fora
do indice, entrada do indice sem arquivo e baralho tecnico de teste.
"""
from __future__ import annotations

import json

from config import Configuracao


def card_simples(identificador, resposta):
    return {
        "id": identificador,
        "type": "PESSOA",
        "answer": resposta,
        "clues": ["pista {} do card {}".format(numero, identificador) for numero in range(1, 11)],
    }


def card_com_banco(identificador, resposta, extras=3):
    clues = ["pista {} do card {}".format(numero, identificador) for numero in range(1, 11)]
    banco = [
        {"id": "{}:f{}".format(identificador, numero), "texto": texto}
        for numero, texto in enumerate(clues, start=1)
    ]
    banco += [
        {
            "id": "{}:x{}".format(identificador, numero),
            "texto": "fato extra {} do card {}".format(numero, identificador),
        }
        for numero in range(1, extras + 1)
    ]
    return {
        "id": identificador,
        "type": "COISA",
        "answer": resposta,
        "respostaId": "resposta-{}".format(identificador),
        "clues": clues,
        "bancoDeDicas": banco,
        "campoDesconhecido": "precisa sobreviver",
    }


def baralho(identificador, nome, estado, cards, colecao_id="demo", categoria="ESPECIAIS",
            versao=1, icone="⭐"):
    return {
        "id": identificador,
        "nome": nome,
        "categoria": categoria,
        "colecao": {"id": colecao_id, "nome": colecao_id.replace("-", " ").title(), "icone": icone},
        "versao": versao,
        "estado": estado,
        "cards": cards,
    }


def _escrever(caminho, dados):
    caminho.parent.mkdir(parents=True, exist_ok=True)
    caminho.write_text(
        json.dumps(dados, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n"
    )


ENVELOPE_INICIAL = 3


def montar_ambiente(raiz, com_catalogo=True):
    """Cria app + catalogo de mentira em "raiz" e devolve a Configuracao."""
    app = raiz / "app"
    catalogo = raiz / "catalogo"
    privado = raiz / "privado"

    _escrever(
        privado / "origens" / "embarcados-legado.json",
        {
            "version": ENVELOPE_INICIAL,
            "baralhos": [
                baralho(
                    "demo-final",
                    "Demo — Estado Legado",
                    "FINALIZADO",
                    [card_simples("df-001", "Resposta Final")],
                    colecao_id="demo-classico",
                ),
                baralho(
                    "demo-evolucao",
                    "Demo — Em Evolução",
                    "EM_DESENVOLVIMENTO",
                    [
                        card_com_banco("de-001", "Resposta Viva"),
                        card_simples("de-002", "Resposta Legada"),
                    ],
                ),
            ],
        },
    )
    app.mkdir(parents=True, exist_ok=True)
    (app / "gradlew.bat").write_text("@echo off\n", encoding="utf-8")

    if com_catalogo:
        _escrever(
            catalogo / "baralhos" / "demo-catalogo.json",
            baralho(
                "demo-catalogo",
                "Demo — Catálogo",
                "EM_DESENVOLVIMENTO",
                [card_simples("dc-001", "Resposta do Catálogo")],
                colecao_id="demo-catalogo",
                versao=2,
            ),
        )
        _escrever(
            catalogo / "baralhos" / "demo-evolucao.json",
            baralho(
                "demo-evolucao",
                "Demo — Em Evolução (catálogo)",
                "EM_DESENVOLVIMENTO",
                [card_simples("de-001", "Resposta Viva")],
            ),
        )
        _escrever(
            catalogo / "baralhos" / "demo-orfao.json",
            baralho(
                "demo-orfao",
                "Demo — Fora do Índice",
                "EM_DESENVOLVIMENTO",
                [card_simples("do-001", "Resposta Órfã")],
                colecao_id="demo-orfao",
            ),
        )
        _escrever(
            catalogo / "baralhos" / "baralho-de-teste-1.json",
            baralho(
                "baralho-de-teste-1",
                "Baralho de Teste — Edição 1",
                "EM_DESENVOLVIMENTO",
                [card_simples("bt-001", "Resposta Técnica")],
                colecao_id="baralho-de-teste",
                icone="🧪",
            ),
        )
        _escrever(catalogo / "indice.json", {"baralhos": _entradas_do_indice()})

    config = Configuracao(
        raiz_do_app=app,
        raiz_do_catalogo=catalogo,
        diretorio_privado=privado,
        porta=0,
        tempo_limite_da_validacao=30,
    )
    config.preparar_diretorio_privado()
    return config


def _entrada(identificador, nome, estado, quantidade, versao, colecao_id):
    return {
        "id": identificador,
        "nome": nome,
        "categoria": "ESPECIAIS",
        "colecao": {
            "id": colecao_id,
            "nome": colecao_id.replace("-", " ").title(),
            "icone": "⭐",
        },
        "versao": versao,
        "estado": estado,
        "quantidadeDeCards": quantidade,
        "url": "https://exemplo.invalido/{}.json".format(identificador),
        "descricao": "Fixture de teste.",
        "tamanhoEmBytes": 1234,
    }


def _entradas_do_indice():
    return [
        _entrada("demo-catalogo", "Demo — Catálogo", "EM_DESENVOLVIMENTO", 1, 2, "demo-catalogo"),
        _entrada("demo-evolucao", "Demo — Em Evolução (catálogo)", "EM_DESENVOLVIMENTO", 1, 1, "demo"),
        _entrada("demo-ausente", "Demo — Sem Arquivo", "EM_DESENVOLVIMENTO", 9, 1, "demo-ausente"),
        _entrada(
            "baralho-de-teste-1",
            "Baralho de Teste — Edição 1",
            "EM_DESENVOLVIMENTO",
            1,
            1,
            "baralho-de-teste",
        ),
    ]


def validador_simulado(codigo=0, saida="✓ 1 card(s) válido(s). Baralho aprovado."):
    """SIMULADO: substitui o subprocesso do Gradle nos testes rapidos.

    Nenhuma regra editorial e avaliada aqui. Os testes que usam esta funcao
    provam o fluxo do painel (fila, aprovacao presa ao hash, bloqueio da
    aplicacao), nao a regua do app. A regua real so e exercida quando o Gradle
    de verdade roda, fora da suite rapida.
    """
    chamadas = []

    def executar(config, comando):
        chamadas.append(comando)
        return codigo, saida

    executar.chamadas = chamadas
    return executar
