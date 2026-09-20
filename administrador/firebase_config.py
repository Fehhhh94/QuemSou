"""Leitura mínima da configuração pública do app Android no Firebase.

O painel nunca serve o arquivo inteiro. Expõe somente projeto e chave Web API,
valores que já ficam incorporados ao APK, para o navegador autenticar a sua
própria sessão anônima diretamente no Firebase.
"""
from __future__ import annotations

import json


PACOTE_ANDROID = "com.quemsou.app"


def configuracao_publica(config):
    caminho = config.arquivo_google_services
    if not caminho.is_file():
        return {
            "configurado": False,
            "motivo": "Coloque google-services.json em app/ para ligar a sincronização.",
        }
    try:
        bruto = caminho.read_text(encoding="utf-8")
        if len(bruto) > 128 * 1024:
            raise ValueError("arquivo grande")
        dados = json.loads(bruto)
        projeto = dados["project_info"]["project_id"]
        clientes = dados["client"]
    except (OSError, ValueError, KeyError, TypeError):
        return {"configurado": False, "motivo": "google-services.json é inválido."}

    for cliente in clientes if isinstance(clientes, list) else []:
        try:
            pacote = cliente["client_info"]["android_client_info"]["package_name"]
            chaves = cliente["api_key"]
        except (KeyError, TypeError):
            continue
        if pacote != PACOTE_ANDROID or not isinstance(chaves, list):
            continue
        chave = next(
            (
                item.get("current_key")
                for item in chaves
                if isinstance(item, dict) and isinstance(item.get("current_key"), str)
            ),
            None,
        )
        if chave and isinstance(projeto, str) and projeto:
            return {
                "configurado": True,
                "projectId": projeto,
                "apiKey": chave,
                "databaseId": "(default)",
            }
    return {
        "configurado": False,
        "motivo": "google-services.json não contém o app com.quemsou.app.",
    }
