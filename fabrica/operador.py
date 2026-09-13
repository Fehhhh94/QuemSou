"""Preparação privada do host. Não publica porta, instala VPN nem autentica o Codex."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
from urllib.parse import urlsplit


def preparar(pasta, url, codex="codex"):
    partes = urlsplit(url)
    if (partes.scheme != "https" or not partes.hostname or partes.username or partes.password
            or partes.query or partes.fragment or "|" in url):
        raise ValueError("Informe o endereço HTTPS privado, sem credenciais, query ou fragmento")
    pasta = Path(pasta)
    pasta.mkdir(parents=True, exist_ok=True)
    # Diretório deve ser privado da conta de serviço. Nunca sobrescrever uma conexão existente.
    if any(pasta.iterdir()):
        raise ValueError("A pasta já contém arquivos; preserve a configuração existente")
    token = secrets.token_urlsafe(32)
    clientes = {hashlib.sha256(token.encode()).hexdigest(): "pessoal"}
    (pasta / "clientes.json").write_text(json.dumps(clientes), encoding="utf-8")
    (pasta / "conexao.txt").write_text(url.rstrip("/") + "|" + token, encoding="utf-8")
    (pasta / "config.json").write_text(json.dumps({"codex": codex}, indent=2), encoding="utf-8")
    if os.name != "nt":
        pasta.chmod(0o700)
        for arquivo in pasta.iterdir():
            arquivo.chmod(0o600)


def verificar(pasta):
    pasta = Path(pasta)
    config = json.loads((pasta / "config.json").read_text(encoding="utf-8"))
    clientes = json.loads((pasta / "clientes.json").read_text(encoding="utf-8"))
    if not clientes or not all(len(k) == 64 and isinstance(v, str) and v for k, v in clientes.items()):
        raise ValueError("Configuração de clientes inválida")
    if not shutil.which(config["codex"]):
        raise ValueError("Codex não encontrado na conta executora")
    return config


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("acao", choices=["preparar", "verificar", "iniciar"])
    parser.add_argument("--pasta", type=Path, default=Path(os.environ.get("LOCALAPPDATA", str(Path.home() / ".local/share"))) / "QuemSou/fabrica")
    parser.add_argument("--url")
    parser.add_argument("--codex", default="codex")
    args = parser.parse_args()
    try:
        if args.acao == "preparar":
            if not args.url:
                raise ValueError("Falta --url com o HTTPS privado já configurado")
            preparar(args.pasta, args.url, args.codex)
            print("Configuração criada. O código privado está em", args.pasta / "conexao.txt")
            return
        config = verificar(args.pasta)
        if args.acao == "verificar":
            print("Arquivos e executável encontrados. Ainda é preciso validar login do Codex, isolamento da conta e HTTPS.")
            return
        os.environ["QUEMSOU_CLIENTS_FILE"] = str(args.pasta / "clientes.json")
        os.environ["QUEMSOU_DB"] = str(args.pasta / "fila.sqlite3")
        os.environ["QUEMSOU_CODEX_BIN"] = config["codex"]
        import server
        server.main()
    except (ValueError, OSError, KeyError):
        parser.exit(1, "Não foi possível preparar ou iniciar. Confira os argumentos, arquivos privados e executável. Nenhuma credencial foi exibida.\n")


if __name__ == "__main__":
    main()
