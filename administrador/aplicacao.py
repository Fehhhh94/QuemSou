"""Plano de aplicacao e gravacao na origem local.

Aplicar escreve nas origens privadas, nunca no código ou no APK. Por isso ela
exige, nesta ordem: estado técnico compatível, identidade intacta, validação
positiva do candidato exato, ausencia de mudanca externa desde que o rascunho
foi aberto e confirmacao explicita do usuario.

Aplicar NAO finaliza e NAO publica: as origens ficam fora do Git
e a copia local do catalogo so chega aos celulares depois de uma publicacao
separada e confirmada no Firestore.
"""
from __future__ import annotations

import copy
import os
import shutil
import tempfile
from datetime import datetime

from edicao import FalhaDaEdicao, assegurar_editavel, assegurar_identidade_preservada, assegurar_identidade_do_registro
from fontes import (
    ORIGEM_ASSET,
    ORIGEM_CATALOGO,
    ORIGEM_RASCUNHO,
    forma_canonica,
    impressao_digital,
    interpretar_chave,
    ler_baralho_da_origem,
    ler_envelope_do_asset,
    ler_indice_do_catalogo,
    serializar,
)
from rascunhos import impressao_do_candidato

ALVO_BARALHO = "baralho"
ALVO_CATALOGO = "catalogo"
ALVO_FIRESTORE = "firestore"


class FalhaDaAplicacao(ValueError):
    """Erro previsto de aplicacao, com codigo estavel para a tela."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


class Plano:
    """Tudo que seria gravado, calculado antes de qualquer escrita."""

    def __init__(self, registro, candidato, origem=None, alvos=(ALVO_BARALHO,),
                 envelope=None, indice=None, caminho=None, hash_da_origem=None,
                 hash_do_indice=None,
                 aplicavel=False, motivo_de_nao_aplicavel="", conflito=False,
                 sem_alteracao=False, versao_anterior=None):
        self.registro = registro
        self.candidato = candidato
        self.origem = origem
        self.alvos = tuple(alvos)
        self.envelope = envelope
        self.indice = indice
        self.caminho = caminho
        self.hash_da_origem = hash_da_origem
        self.hash_do_indice = hash_do_indice
        self.aplicavel = aplicavel
        self.motivo_de_nao_aplicavel = motivo_de_nao_aplicavel
        self.conflito = conflito
        self.sem_alteracao = sem_alteracao
        self.versao_anterior = versao_anterior

    @property
    def hash_do_candidato(self):
        return impressao_do_candidato(self.candidato)

    @property
    def alvo_principal(self):
        return self.alvos[-1]

    @property
    def hash_da_validacao(self):
        """Impressão do candidato e de todos os arquivos validados com ele."""
        return impressao_do_candidato(
            {
                "alvo": self.alvo_principal,
                "candidato": self.candidato,
                "hashDaOrigem": self.hash_da_origem,
                "hashDoIndice": self.hash_do_indice,
                "indice": self.indice if ALVO_CATALOGO in self.alvos else None,
            }
        )

    def resumo(self):
        return {
            "alvos": list(self.alvos),
            "aplicavel": self.aplicavel,
            "motivo": self.motivo_de_nao_aplicavel,
            "conflito": self.conflito,
            "semAlteracao": self.sem_alteracao,
            "versaoAnterior": self.versao_anterior,
            "versaoDoCandidato": self.candidato.get("versao"),
            "quantidadeDeCards": len(self.candidato.get("cards") or []),
            "hashDoCandidato": self.hash_do_candidato,
            "hashDaValidacao": self.hash_da_validacao,
        }


def montar_plano(config, registro):
    """Calcula o candidato final (ja com a versao incrementada) e o que muda.

    A versao e incrementada aqui, antes da validacao, de proposito: o que o
    Gradle aprova e exatamente o que seria gravado depois.
    """
    baralho = registro.get("baralho")
    if not isinstance(baralho, dict):
        raise FalhaDaAplicacao("rascunho_invalido", "Rascunho sem conteúdo de baralho.")

    if registro.get("tipo") == "novo":
        return Plano(
            registro=registro,
            candidato=copy.deepcopy(baralho),
            origem=None,
            alvos=(ALVO_BARALHO,),
            aplicavel=False,
            motivo_de_nao_aplicavel=(
                "Rascunho novo: valide e exporte o JSON. A entrada no catálogo "
                "ou no app é uma integração deliberada, feita fora do painel."
            ),
        )

    origem, baralho_id = interpretar_chave(registro.get("chave"))
    if origem == ORIGEM_RASCUNHO:
        raise FalhaDaAplicacao("origem_invalida", "Rascunho local não tem origem para aplicar.")
    atual = ler_baralho_da_origem(config, origem, baralho_id)
    assegurar_editavel(atual.dados)
    assegurar_identidade_do_registro(atual.dados, registro)

    sem_alteracao = forma_canonica(atual.dados) == forma_canonica(baralho)
    conflito = bool(registro.get("hashDaOrigem")) and registro["hashDaOrigem"] != atual.hash_da_origem

    candidato = copy.deepcopy(baralho)
    versao_anterior = atual.dados.get("versao")
    candidato["versao"] = (versao_anterior or 0) + 1

    if origem == ORIGEM_ASSET:
        envelope, _ = ler_envelope_do_asset(config)
        envelope["version"] = (envelope.get("version") or 0) + 1
        envelope["baralhos"][atual.indice_na_lista] = candidato
        return Plano(
            registro=registro,
            candidato=candidato,
            origem=atual,
            alvos=(ALVO_BARALHO,),
            envelope=envelope,
            caminho=config.arquivo_do_asset,
            hash_da_origem=atual.hash_da_origem,
            aplicavel=not sem_alteracao and not conflito,
            motivo_de_nao_aplicavel=_motivo(sem_alteracao, conflito),
            conflito=conflito,
            sem_alteracao=sem_alteracao,
            versao_anterior=versao_anterior,
        )

    indice, texto_do_indice = ler_indice_do_catalogo(config)
    hash_do_indice = impressao_digital(texto_do_indice)
    conflito = conflito or (
        "hashDoIndice" in registro and registro.get("hashDoIndice") != hash_do_indice
    )
    entrada = None
    for item in indice["baralhos"]:
        if isinstance(item, dict) and item.get("id") == baralho_id:
            entrada = item
            break
    if entrada is None:
        return Plano(
            registro=registro,
            candidato=candidato,
            origem=atual,
            alvos=(ALVO_BARALHO,),
            caminho=atual.caminho,
            hash_da_origem=atual.hash_da_origem,
            hash_do_indice=hash_do_indice,
            aplicavel=False,
            motivo_de_nao_aplicavel=(
                "Este baralho não tem entrada no índice. O painel não insere itens "
                "novos no catálogo automaticamente."
            ),
            conflito=conflito,
            sem_alteracao=sem_alteracao,
            versao_anterior=versao_anterior,
        )
    _atualizar_entrada_do_indice(entrada, candidato)
    return Plano(
        registro=registro,
        candidato=candidato,
        origem=atual,
        alvos=(ALVO_BARALHO, ALVO_CATALOGO),
        indice=indice,
        caminho=atual.caminho,
        hash_da_origem=atual.hash_da_origem,
        hash_do_indice=hash_do_indice,
        aplicavel=not sem_alteracao and not conflito,
        motivo_de_nao_aplicavel=_motivo(sem_alteracao, conflito),
        conflito=conflito,
        sem_alteracao=sem_alteracao,
        versao_anterior=versao_anterior,
    )


def montar_plano_de_publicacao(config, registro):
    """Valida o conteúdo já aplicado, exatamente como será enviado à nuvem.

    A publicação no Firestore não grava na origem local nem incrementa versão.
    Por isso ela só pode usar um rascunho idêntico à origem atual e precisa de
    um hash de validação diferente do plano de aplicação local.
    """
    baralho = registro.get("baralho")
    if not isinstance(baralho, dict):
        raise FalhaDaAplicacao("rascunho_invalido", "Rascunho sem conteúdo de baralho.")
    if registro.get("tipo") == "novo":
        raise FalhaDaAplicacao(
            "somente_origem",
            "Aplique ou integre o rascunho a uma origem antes de publicar.",
        )

    origem, baralho_id = interpretar_chave(registro.get("chave"))
    if origem == ORIGEM_RASCUNHO:
        raise FalhaDaAplicacao("origem_invalida", "Rascunho local não pode ser publicado.")
    atual = ler_baralho_da_origem(config, origem, baralho_id)
    assegurar_editavel(atual.dados)
    assegurar_identidade_do_registro(atual.dados, registro)

    sem_alteracao = forma_canonica(atual.dados) == forma_canonica(baralho)
    conflito = bool(registro.get("hashDaOrigem")) and (
        registro["hashDaOrigem"] != atual.hash_da_origem
    )
    hash_do_indice = None
    if origem == ORIGEM_CATALOGO:
        if atual.entrada_do_indice is None:
            raise FalhaDaAplicacao(
                "fora_do_indice",
                "Este baralho precisa estar no índice local antes de ser publicado.",
            )
        _, texto_do_indice = ler_indice_do_catalogo(config)
        hash_do_indice = impressao_digital(texto_do_indice)
        conflito = conflito or (
            "hashDoIndice" in registro
            and registro.get("hashDoIndice") != hash_do_indice
        )

    return Plano(
        registro=registro,
        candidato=copy.deepcopy(baralho),
        origem=atual,
        alvos=(ALVO_FIRESTORE,),
        hash_da_origem=atual.hash_da_origem,
        hash_do_indice=hash_do_indice,
        aplicavel=False,
        motivo_de_nao_aplicavel="A publicação não altera a origem local.",
        conflito=conflito,
        sem_alteracao=sem_alteracao,
        versao_anterior=atual.dados.get("versao"),
    )


def _motivo(sem_alteracao, conflito):
    if conflito:
        return "O arquivo de origem mudou fora do painel desde que este rascunho foi aberto."
    if sem_alteracao:
        return "O rascunho está igual à origem: não há o que aplicar."
    return ""


def _atualizar_entrada_do_indice(entrada, candidato):
    """Sincroniza versao, quantidade, tamanho e rotulos com o arquivo gravado."""
    entrada["versao"] = candidato.get("versao")
    entrada["quantidadeDeCards"] = len(candidato.get("cards") or [])
    entrada["tamanhoEmBytes"] = len(serializar(candidato).encode("utf-8"))
    for campo in ("nome", "categoria", "estado"):
        if campo in candidato:
            entrada[campo] = candidato[campo]
    if isinstance(candidato.get("colecao"), dict):
        entrada["colecao"] = copy.deepcopy(candidato["colecao"])


def _gravar_atomico(caminho, texto):
    """Grava num arquivo ao lado e so entao troca, para nao deixar meio arquivo."""
    descritor, nome_temporario = tempfile.mkstemp(
        prefix=".{}-".format(caminho.name), suffix=".parcial", dir=str(caminho.parent)
    )
    try:
        with os.fdopen(descritor, "w", encoding="utf-8", newline="\n") as saida:
            saida.write(texto)
        os.replace(nome_temporario, caminho)
    except Exception:
        try:
            os.unlink(nome_temporario)
        except FileNotFoundError:
            pass
        raise


def _escritas_do_plano(config, plano):
    if plano.origem.origem == ORIGEM_ASSET:
        return [(config.arquivo_do_asset, serializar(plano.envelope))]
    escritas = [(plano.caminho, serializar(plano.candidato))]
    if plano.indice is not None:
        escritas.append((config.arquivo_do_indice, serializar(plano.indice)))
    return escritas


def _outros_baralhos_do_asset(envelope, alvo_id):
    return {
        baralho.get("id"): forma_canonica(baralho)
        for baralho in envelope.get("baralhos") or []
        if isinstance(baralho, dict) and baralho.get("id") != alvo_id
    }


def _outras_entradas_do_indice(indice, alvo_id):
    return {
        entrada.get("id"): forma_canonica(entrada)
        for entrada in indice.get("baralhos") or []
        if isinstance(entrada, dict) and entrada.get("id") != alvo_id
    }


def _restaurar(backups):
    for caminho, copia in backups.items():
        shutil.copy2(copia, caminho)


def _pasta_exclusiva_de_backup(config, origem, baralho_id, acao="aplicar"):
    config.pasta_de_backups.mkdir(parents=True, exist_ok=True)
    carimbo = datetime.now().strftime("%Y%m%d-%H%M%S")
    pasta = tempfile.mkdtemp(
        prefix="{}-{}-{}-{}-".format(carimbo, acao, origem, baralho_id),
        dir=str(config.pasta_de_backups),
    )
    return os.path.abspath(pasta)


def aplicar(config, registro, plano, gravar=None):
    """Grava o candidato na origem local, com backup e rollback.

    O chamador ja conferiu a aprovacao da validacao e a confirmacao explicita;
    aqui as barreiras sao repetidas contra o estado real do disco, porque o
    arquivo pode ter mudado entre validar e aplicar.
    """
    gravar = gravar or _gravar_atomico
    if plano.origem is None:
        raise FalhaDaAplicacao("sem_origem", "Este rascunho não tem origem para aplicar.")
    if not plano.aplicavel:
        raise FalhaDaAplicacao(
            "nao_aplicavel", plano.motivo_de_nao_aplicavel or "Nada a aplicar."
        )

    atual = ler_baralho_da_origem(config, plano.origem.origem, plano.origem.id)
    assegurar_editavel(atual.dados)
    if atual.hash_da_origem != plano.hash_da_origem:
        raise FalhaDaAplicacao(
            "conflito_externo",
            "O arquivo de origem mudou fora do painel. Reabra o baralho e valide de novo.",
        )
    if plano.indice is not None and atual.hash_do_indice != plano.hash_do_indice:
        raise FalhaDaAplicacao(
            "conflito_externo",
            "O índice do catálogo mudou fora do painel. Reabra o baralho e valide de novo.",
        )
    assegurar_identidade_do_registro(atual.dados, plano.registro)

    escritas = _escritas_do_plano(config, plano)
    pasta_do_backup = _pasta_exclusiva_de_backup(
        config, plano.origem.origem, plano.origem.id
    )
    backups = {}
    for caminho, _ in escritas:
        if caminho == config.arquivo_do_indice:
            nome_da_copia = "indice-{}".format(caminho.name)
        elif plano.origem.origem == ORIGEM_ASSET:
            nome_da_copia = "asset-{}".format(caminho.name)
        else:
            nome_da_copia = "baralho-{}".format(caminho.name)
        copia = os.path.join(pasta_do_backup, nome_da_copia)
        shutil.copy2(caminho, copia)
        backups[caminho] = copia

    gravados = []
    try:
        for caminho, texto in escritas:
            gravar(caminho, texto)
            gravados.append(caminho)
        _conferir_vizinhanca(config, plano)
    except FalhaDaAplicacao:
        _restaurar(backups)
        raise
    except Exception:
        _restaurar(backups)
        raise FalhaDaAplicacao(
            "gravacao_falhou",
            "A gravação falhou. Os arquivos foram restaurados do backup em {}.".format(
                pasta_do_backup
            ),
        )

    return {
        "arquivos": [str(caminho) for caminho, _ in escritas],
        "backup": pasta_do_backup,
        "versaoAnterior": plano.versao_anterior,
        "versaoNova": plano.candidato.get("versao"),
        "versaoDoEnvelope": (plano.envelope or {}).get("version"),
        "avisos": _avisos_da_origem(plano.origem.origem),
    }


def remover_baralho(config, registro, gravar=None, apagar=None):
    """Remove um baralho da origem local, sempre com backup e rollback.

    A confirmacao textual e a revisao sao barreiras da camada de servico. Esta
    funcao repete as verificacoes contra o disco e preserva todos os vizinhos.
    """
    gravar = gravar or _gravar_atomico
    apagar = apagar or os.unlink
    origem, baralho_id = interpretar_chave(registro.get("chave"))
    if origem == ORIGEM_RASCUNHO or registro.get("tipo") == "novo":
        raise FalhaDaAplicacao(
            "sem_origem", "Rascunho novo deve ser descartado, não removido de uma origem."
        )

    atual = ler_baralho_da_origem(config, origem, baralho_id)
    if atual.hash_da_origem != registro.get("hashDaOrigem"):
        raise FalhaDaAplicacao(
            "conflito_externo",
            "O arquivo de origem mudou fora do painel. Reabra o baralho antes de remover.",
        )

    pasta_do_backup = _pasta_exclusiva_de_backup(
        config, origem, baralho_id, acao="remover"
    )

    if origem == ORIGEM_ASSET:
        envelope, _ = ler_envelope_do_asset(config)
        outros = [
            forma_canonica(baralho)
            for baralho in envelope.get("baralhos") or []
            if isinstance(baralho, dict) and baralho.get("id") != baralho_id
        ]
        candidato = copy.deepcopy(envelope)
        candidato["baralhos"] = [
            baralho
            for baralho in candidato.get("baralhos") or []
            if not (isinstance(baralho, dict) and baralho.get("id") == baralho_id)
        ]
        if len(candidato["baralhos"]) == len(envelope.get("baralhos") or []):
            raise FalhaDaAplicacao("baralho_ausente", "O baralho não existe mais no asset.")
        candidato["version"] = (envelope.get("version") or 0) + 1
        copia = os.path.join(pasta_do_backup, "asset-cards.json")
        shutil.copy2(config.arquivo_do_asset, copia)
        backups = {config.arquivo_do_asset: copia}
        try:
            gravar(config.arquivo_do_asset, serializar(candidato))
            gravado, _ = ler_envelope_do_asset(config)
            ids = [
                item.get("id") for item in gravado.get("baralhos") or [] if isinstance(item, dict)
            ]
            vizinhos = [
                forma_canonica(baralho)
                for baralho in gravado.get("baralhos") or []
                if isinstance(baralho, dict) and baralho.get("id") != baralho_id
            ]
            if baralho_id in ids or vizinhos != outros:
                raise FalhaDaAplicacao(
                    "vizinhanca_alterada", "Outro baralho do arquivo do app teria mudado."
                )
        except FalhaDaAplicacao:
            _restaurar(backups)
            raise
        except Exception:
            _restaurar(backups)
            raise FalhaDaAplicacao(
                "remocao_falhou",
                "A remoção falhou. O asset foi restaurado do backup em {}.".format(
                    pasta_do_backup
                ),
            )
        return {
            "baralhoId": baralho_id,
            "origem": origem,
            "arquivos": [str(config.arquivo_do_asset)],
            "backup": pasta_do_backup,
            "versaoDoEnvelope": candidato.get("version"),
            "avisos": _avisos_da_origem(origem),
        }

    indice, texto_do_indice = ler_indice_do_catalogo(config)
    hash_do_indice = impressao_digital(texto_do_indice)
    if hash_do_indice != registro.get("hashDoIndice"):
        raise FalhaDaAplicacao(
            "conflito_externo",
            "O índice do catálogo mudou fora do painel. Reabra o baralho antes de remover.",
        )
    outros = [
        forma_canonica(item)
        for item in indice.get("baralhos") or []
        if isinstance(item, dict) and item.get("id") != baralho_id
    ]
    indice_novo = copy.deepcopy(indice)
    indice_novo["baralhos"] = [
        item
        for item in indice_novo.get("baralhos") or []
        if not (isinstance(item, dict) and item.get("id") == baralho_id)
    ]
    tinha_entrada = len(indice_novo["baralhos"]) != len(indice.get("baralhos") or [])

    copia_baralho = os.path.join(pasta_do_backup, "baralho-{}".format(atual.caminho.name))
    copia_indice = os.path.join(pasta_do_backup, "indice-{}".format(config.arquivo_do_indice.name))
    shutil.copy2(atual.caminho, copia_baralho)
    shutil.copy2(config.arquivo_do_indice, copia_indice)
    backups = {atual.caminho: copia_baralho, config.arquivo_do_indice: copia_indice}
    try:
        if tinha_entrada:
            gravar(config.arquivo_do_indice, serializar(indice_novo))
        apagar(atual.caminho)
        if atual.caminho.exists():
            raise FalhaDaAplicacao("remocao_falhou", "O arquivo do baralho ainda existe.")
        indice_gravado, _ = ler_indice_do_catalogo(config)
        ids = [
            item.get("id")
            for item in indice_gravado.get("baralhos") or []
            if isinstance(item, dict)
        ]
        vizinhos = [
            forma_canonica(item)
            for item in indice_gravado.get("baralhos") or []
            if isinstance(item, dict) and item.get("id") != baralho_id
        ]
        if baralho_id in ids or vizinhos != outros:
            raise FalhaDaAplicacao(
                "vizinhanca_alterada", "Outra entrada do índice teria mudado."
            )
    except FalhaDaAplicacao:
        _restaurar(backups)
        raise
    except Exception:
        _restaurar(backups)
        raise FalhaDaAplicacao(
            "remocao_falhou",
            "A remoção falhou. Catálogo e índice foram restaurados do backup em {}.".format(
                pasta_do_backup
            ),
        )
    arquivos = [str(atual.caminho)]
    if tinha_entrada:
        arquivos.append(str(config.arquivo_do_indice))
    return {
        "baralhoId": baralho_id,
        "origem": origem,
        "arquivos": arquivos,
        "backup": pasta_do_backup,
        "avisos": _avisos_da_origem(origem),
    }


def _conferir_vizinhanca(config, plano):
    """Confere, depois de gravar, que nenhum outro baralho mudou de conteudo.

    O candidato e montado sobre uma leitura fresca do arquivo inteiro, entao
    isto nao deveria falhar nunca. E justamente por isso que vale a pena: se
    falhar, alguma coisa saiu do previsto e o rollback e acionado.
    """
    alvo_id = plano.candidato.get("id")
    if plano.origem.origem == ORIGEM_ASSET:
        esperado = _outros_baralhos_do_asset(plano.envelope, alvo_id)
        gravado, _ = ler_envelope_do_asset(config)
        if _outros_baralhos_do_asset(gravado, alvo_id) != esperado:
            raise FalhaDaAplicacao(
                "vizinhanca_alterada", "Outro baralho do arquivo do app teria mudado."
            )
        return
    if plano.indice is not None:
        esperado = _outras_entradas_do_indice(plano.indice, alvo_id)
        gravado, _ = ler_indice_do_catalogo(config)
        if _outras_entradas_do_indice(gravado, alvo_id) != esperado:
            raise FalhaDaAplicacao(
                "vizinhanca_alterada", "Outra entrada do índice teria mudado."
            )


def _avisos_da_origem(origem):
    if origem == ORIGEM_ASSET:
        return [
            "Alteração gravada na biblioteca privada desta máquina, fora do Git.",
            "Para distribuir aos celulares, valide e publique explicitamente no Firestore.",
        ]
    return [
        "Alteração gravada apenas na cópia local do catálogo, nesta máquina.",
        "Publicar no Firestore é uma ação separada, com validação e confirmação.",
    ]


def exportar_rascunho(registro):
    """JSON de um rascunho novo, para integracao deliberada em outro momento."""
    baralho = registro.get("baralho")
    if not isinstance(baralho, dict):
        raise FalhaDaAplicacao("rascunho_invalido", "Rascunho sem conteúdo de baralho.")
    return serializar(baralho)
