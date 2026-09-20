"""Copia origens legadas para fora do Git, sem apagar/publicar/sobrescrever.

Executar explicitamente ANTES de retirar o conteúdo do asset. Uma cópia
imutável com manifesto SHA-256 permite recuperação; as origens de trabalho
mantêm os bytes originais para preservar revisões de rascunhos existentes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from config import Configuracao, exigir_fora_do_git


def migrar(app, catalogo_legado, dados):
    app, catalogo_legado, dados = map(lambda p: Path(p).resolve(), (app, catalogo_legado, dados))
    config = Configuracao(app, dados / "origens" / "catalogo", dados)
    fontes = {"embarcados-legado.json": app / "app/src/main/assets/cards.json",
              "catalogo/indice.json": catalogo_legado / "indice.json"}
    pasta = catalogo_legado / "baralhos"
    if not pasta.is_dir():
        raise ValueError("Pasta de baralhos legada ausente; migração interrompida.")
    for caminho in sorted(pasta.glob("*.json")):
        if caminho.resolve().parent != pasta.resolve():
            raise ValueError("Origem aponta para fora do catálogo.")
        fontes["catalogo/baralhos/" + caminho.name] = caminho
    conteudos = {nome: caminho.read_bytes() for nome, caminho in fontes.items()}
    for bruto in conteudos.values():
        json.loads(bruto)  # Não copiar silenciosamente um arquivo truncado.
    backup = dados / "backups" / "separacao-git-v1"
    manifesto = {
        "formato": 1,
        "arquivos": [{"arquivo": nome, "bytes": len(bruto),
                      "sha256": hashlib.sha256(bruto).hexdigest()}
                     for nome, bruto in sorted(conteudos.items())],
    }
    manifesto_bytes = (json.dumps(manifesto, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    destinos = [(backup / nome, bruto) for nome, bruto in conteudos.items()]
    destinos += [(backup / "manifesto.json", manifesto_bytes)]
    destinos += [(dados / "origens" / nome, bruto) for nome, bruto in conteudos.items()]
    # Preflight completo antes da primeira escrita; conflito exige decisão humana.
    for destino, bruto in destinos:
        exigir_fora_do_git(destino, app)
        if destino.exists() and destino.read_bytes() != bruto:
            raise ValueError("Destino existente diverge; nada será sobrescrito: " + str(destino))
    for destino, bruto in destinos:
        destino.parent.mkdir(parents=True, exist_ok=True)
        if not destino.exists():
            with destino.open("xb") as arquivo:
                arquivo.write(bruto)
        if destino.read_bytes() != bruto:
            raise OSError("Falha ao verificar a cópia: " + str(destino))
    return {"arquivos": len(conteudos), "backup": str(backup), "origens": str(dados / "origens")}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", type=Path, required=True)
    parser.add_argument("--catalogo-legado", type=Path, required=True)
    parser.add_argument("--dados", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(migrar(args.app, args.catalogo_legado, args.dados), ensure_ascii=False))
