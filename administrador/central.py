"""Ponto de entrada da Central de Baralhos.

Uso tipico (o Abrir-Administrador.cmd faz isto):

    python administrador/central.py

Opcoes: --porta N, --sem-navegador, --dados <pasta>, --app <pasta>,
--catalogo <pasta>. O servidor escuta somente em 127.0.0.1 e o navegador e
aberto na propria maquina; nada e exposto na rede.
"""
from __future__ import annotations

import sys
import threading
import webbrowser

from config import detectar_configuracao
from servidor import criar_servidor

NOME = "Central de Baralhos"


def _opcoes(argumentos):
    opcoes = {}
    restantes = list(argumentos)
    while restantes:
        atual = restantes.pop(0)
        if atual == "--sem-navegador":
            opcoes["sem_navegador"] = True
        elif atual in ("--porta", "--dados", "--app", "--catalogo"):
            if not restantes:
                raise SystemExit("Falta o valor de {}.".format(atual))
            opcoes[atual[2:]] = restantes.pop(0)
        else:
            raise SystemExit("Opção desconhecida: {}".format(atual))
    return opcoes


def _avisos(config):
    avisos = []
    if not config.arquivo_do_asset.is_file():
        avisos.append(
            "Não encontrei {} — a biblioteca local legada não será listada.".format(
                config.arquivo_do_asset
            )
        )
    if not config.arquivo_do_indice.is_file():
        avisos.append(
            "Não encontrei {} — a cópia local do catálogo não será listada.".format(
                config.arquivo_do_indice
            )
        )
    if not config.gradlew.is_file():
        avisos.append(
            "Não encontrei {} — a validação pela régua do app ficará indisponível.".format(
                config.gradlew
            )
        )
    return avisos


def principal(argumentos=None):
    opcoes = _opcoes(sys.argv[1:] if argumentos is None else argumentos)
    config = detectar_configuracao(opcoes)
    print("{} — painel local".format(NOME))
    print("  App:      {}".format(config.raiz_do_app))
    print("  Catálogo: {}".format(config.raiz_do_catalogo))
    print("  Dados privados: {}".format(config.diretorio_privado))
    for aviso in _avisos(config):
        print("  Aviso: {}".format(aviso))

    try:
        servidor, _ = criar_servidor(config)
    except OSError as erro:
        print(
            "\nNão foi possível abrir a porta {}: {}".format(config.porta, erro.strerror or erro)
        )
        print("Feche o que estiver usando a porta ou rode com --porta <outra>.")
        return 1

    endereco = "http://127.0.0.1:{}/".format(servidor.server_address[1])
    print("\nAberto em {}".format(endereco))
    print("Feche esta janela ou pressione Ctrl+C para encerrar.\n")
    if not opcoes.get("sem_navegador"):
        threading.Timer(0.5, webbrowser.open, (endereco,)).start()
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        print("\n{} encerrada.".format(NOME))
    finally:
        servidor.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(principal())
