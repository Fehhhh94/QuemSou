"""Configuração da Central de Baralhos.

Painel local de administração dos baralhos. Nada aqui é executado pelo APK.
Dados privados (rascunhos, backups, arquivos temporários de validação) moram
fora do repositório, por padrão em ``%LOCALAPPDATA%/QuemSou/administrador``.
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

#: Porta padrão. Fora da faixa 8080-8089 usada pelo espelho de leitura.
PORTA_PADRAO = 8765

#: Tempo limite de uma execução do Gradle, em segundos.
TEMPO_LIMITE_DA_VALIDACAO = 900

#: Teto do corpo de uma requisição HTTP (o maior baralho real tem ~81 KB).
MAXIMO_DO_CORPO = 4 * 1024 * 1024


def exigir_fora_do_git(caminho, raiz_do_app=None):
    """Recusa conteúdo no código, inclusive worktrees e links para eles."""
    caminho = Path(caminho).resolve()
    if raiz_do_app and caminho.is_relative_to(Path(raiz_do_app).resolve()):
        raise ValueError("Conteúdo deve ficar fora da pasta do aplicativo.")
    if any((pasta / ".git").exists() for pasta in (caminho, *caminho.parents)):
        raise ValueError("Conteúdo deve ficar fora de qualquer repositório Git.")
    return caminho


@dataclass(frozen=True)
class Configuracao:
    """Caminhos e limites resolvidos uma única vez, na subida do servidor."""

    raiz_do_app: Path
    raiz_do_catalogo: Path
    diretorio_privado: Path
    porta: int = PORTA_PADRAO
    tempo_limite_da_validacao: int = TEMPO_LIMITE_DA_VALIDACAO

    def __post_init__(self):
        exigir_fora_do_git(self.diretorio_privado, self.raiz_do_app)
        exigir_fora_do_git(self.raiz_do_catalogo, self.raiz_do_app)

    @property
    def arquivo_do_asset(self):
        # Nome/chave "asset" mantido para não romper rascunhos já existentes.
        return exigir_fora_do_git(
            self.diretorio_privado / "origens" / "embarcados-legado.json", self.raiz_do_app
        )

    @property
    def arquivo_google_services(self):
        return self.raiz_do_app / "app" / "google-services.json"

    @property
    def arquivo_do_indice(self):
        return exigir_fora_do_git(self.raiz_do_catalogo / "indice.json", self.raiz_do_app)

    @property
    def pasta_de_baralhos_do_catalogo(self):
        return exigir_fora_do_git(self.raiz_do_catalogo / "baralhos", self.raiz_do_app)

    @property
    def gradlew(self):
        return self.raiz_do_app / "gradlew.bat"

    @property
    def pasta_de_rascunhos(self):
        return self.diretorio_privado / "rascunhos"

    @property
    def pasta_do_acervo(self):
        """Rascunhos locais do acervo editorial (respostas/dicas), fora do Git."""
        return self.diretorio_privado / "acervo"

    @property
    def pasta_de_backups(self):
        return self.diretorio_privado / "backups"

    @property
    def pasta_de_feedbacks(self):
        return self.diretorio_privado / "feedbacks"

    @property
    def pasta_temporaria(self):
        return self.diretorio_privado / "temporarios"

    def preparar_diretorio_privado(self):
        exigir_fora_do_git(self.diretorio_privado, self.raiz_do_app)
        exigir_fora_do_git(self.raiz_do_catalogo, self.raiz_do_app)
        for pasta in (
            self.pasta_de_rascunhos,
            self.pasta_do_acervo,
            self.pasta_de_backups,
            self.pasta_de_feedbacks,
            self.pasta_temporaria,
        ):
            exigir_fora_do_git(pasta, self.raiz_do_app)
            pasta.mkdir(parents=True, exist_ok=True)


def diretorio_privado_padrao():
    """``%LOCALAPPDATA%/QuemSou/administrador`` quando existir; senão, o perfil."""
    local = os.environ.get("LOCALAPPDATA")
    if local:
        return Path(local) / "QuemSou" / "administrador"
    return Path.home() / ".quemsou" / "administrador"


def java_home_sugerido():
    """JBR do Android Studio, quando instalada. ``None`` deixa o Gradle decidir."""
    jbr = Path("C:/Program Files/Android/Android Studio/jbr")
    return jbr if (jbr / "bin").is_dir() else None


def gradle_user_home_sugerido():
    """Cache local do Gradle, para a validação funcionar offline."""
    cache = Path.home() / ".gradle"
    return cache if cache.is_dir() else None


def detectar_configuracao(argumentos=None):
    """Resolve a configuração a partir do ambiente.

    Variáveis reconhecidas: ``QUEMSOU_ADMIN_APP``, ``QUEMSOU_ADMIN_CATALOGO``,
    ``QUEMSOU_ADMIN_DADOS`` e ``QUEMSOU_ADMIN_PORTA``. Sem elas, assume que
    este arquivo está dentro do repositório do app; todas as origens de
    conteúdo ficam sob o diretório privado, nunca no checkout antigo.
    """
    argumentos = argumentos or {}
    raiz_do_app = Path(
        argumentos.get("app")
        or os.environ.get("QUEMSOU_ADMIN_APP")
        or Path(__file__).resolve().parent.parent
    ).resolve()
    diretorio_privado = Path(
        argumentos.get("dados")
        or os.environ.get("QUEMSOU_ADMIN_DADOS")
        or diretorio_privado_padrao()
    ).resolve()
    raiz_do_catalogo = Path(
        argumentos.get("catalogo")
        or os.environ.get("QUEMSOU_ADMIN_CATALOGO")
        or diretorio_privado / "origens" / "catalogo"
    ).resolve()
    porta = int(argumentos.get("porta") or os.environ.get("QUEMSOU_ADMIN_PORTA") or PORTA_PADRAO)
    return Configuracao(
        raiz_do_app=raiz_do_app,
        raiz_do_catalogo=raiz_do_catalogo,
        diretorio_privado=diretorio_privado,
        porta=porta,
    )
