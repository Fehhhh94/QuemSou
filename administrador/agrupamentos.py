"""Taxonomia ampla da Central; não contém conteúdo editorial ou baralhos.

Os ids das coleções-base são legados. Grupos vazios existem apenas no cadastro
da Central: o app continua derivando sua lista dos baralhos publicados.
"""
from copy import deepcopy

_AGRUPAMENTOS = (
    {"id": "cinema-classico", "nome": "Cinema e TV", "icone": "🎬", "categoriaPadrao": "PERSONAGEM_FILME"},
    {"id": "mundo-da-musica", "nome": "Música", "icone": "🎵", "categoriaPadrao": "MUNDO_DA_MUSICA"},
    {"id": "esportes", "nome": "Esportes", "icone": "⚽", "categoriaPadrao": "PERSONAGEM_FILME"},
    {"id": "conhecimentos-gerais", "nome": "Conhecimentos Gerais", "icone": "💡", "categoriaPadrao": "PERSONAGEM_FILME"},
    {"id": "lugares-e-natureza", "nome": "Lugares e Natureza", "icone": "🌍", "categoriaPadrao": "PERSONAGEM_FILME"},
    {"id": "especiais", "nome": "Especiais", "icone": "⭐", "categoriaPadrao": "ESPECIAIS"},
)


def listar_agrupamentos():
    return deepcopy(list(_AGRUPAMENTOS))


def obter_agrupamento(valor):
    """Aceita id ou nome exibido, preservando chamadas antigas de Especiais."""
    for item in _AGRUPAMENTOS:
        if valor in (item["id"], item["nome"]):
            return deepcopy(item)
    raise ValueError("Selecione um dos seis agrupamentos cadastrados.")


def colecao_do_agrupamento(valor):
    item = obter_agrupamento(valor)
    return {campo: item[campo] for campo in ("id", "nome", "icone")}
