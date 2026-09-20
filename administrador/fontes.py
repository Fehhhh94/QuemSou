"""Leitura das origens de baralho e inventario consolidado da biblioteca.

Duas origens reais:

* "asset"    - origens/embarcados-legado.json no diretorio privado, um
  envelope {"version": N, "baralhos": [...]};
* "catalogo" - a copia privada do catalogo, com indice.json na
  raiz e um arquivo por baralho em baralhos/.

Ha ainda a origem "rascunho", que so existe no diretorio privado.

O inventario enumera as duas origens por completo: o catalogo lista tanto as
entradas do indice quanto os arquivos locais que nao estao nele, para que nada
fique invisivel. O formato dos arquivos e o de docs/CATALOG_FORMAT.md; nada
aqui reimplementa a regua do app - a autoridade continua sendo o
ValidadorEditorial, acionado por Gradle em validacao.py.
"""
from __future__ import annotations

import hashlib
import json
import re
import unicodedata

ORIGEM_ASSET = "asset"
ORIGEM_CATALOGO = "catalogo"
ORIGEM_RASCUNHO = "rascunho"
ORIGENS_DE_FONTE = (ORIGEM_ASSET, ORIGEM_CATALOGO)

ROTULO_DA_ORIGEM = {
    ORIGEM_ASSET: "Biblioteca local (legada)",
    # Nunca afirmar publicacao: o painel nao consulta o GitHub.
    ORIGEM_CATALOGO: "Cópia local do catálogo",
    ORIGEM_RASCUNHO: "Rascunho local",
}

DETALHE_DA_ORIGEM = {
    ORIGEM_ASSET: "Cópia privada fora do Git. Para distribuir, valide e publique no Firestore.",
    ORIGEM_CATALOGO: (
        "Pasta privada de baralhos, fora do Git. Aplicar aqui não altera o que já "
        "está publicado; a publicação no Firestore é uma ação separada."
    ),
    ORIGEM_RASCUNHO: "Só existe nesta máquina, no diretório privado.",
}

ESTADO_EM_DESENVOLVIMENTO = "EM_DESENVOLVIMENTO"
ESTADO_FINALIZADO = "FINALIZADO"
ESTADOS_COMPATIVEIS = frozenset({ESTADO_EM_DESENVOLVIMENTO, ESTADO_FINALIZADO})

ESTADO_AMIGAVEL = {
    ESTADO_EM_DESENVOLVIMENTO: "Atualizável",
    ESTADO_FINALIZADO: "Atualizável (formato legado)",
}

# Conteudo tecnico de teste; nao e baralho de jogo e fica oculto por padrao.
IDS_TECNICOS = frozenset({"baralho-de-teste-1"})
COLECOES_TECNICAS = frozenset({"baralho-de-teste"})

# Ids sao slugs estaveis; isto tambem impede travessia de caminho.
PADRAO_DE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}$")


class FalhaDaFonte(ValueError):
    """Erro previsto de leitura ou identificacao de uma origem."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


def normalizar(texto):
    """Minusculas sem acento e sem pontuacao, para comparar textos de dica."""
    decomposto = unicodedata.normalize("NFD", texto or "").lower()
    return " ".join(
        "".join(
            c if c.isalnum() else " "
            for c in decomposto
            if unicodedata.category(c) != "Mn"
        ).split()
    )


def impressao_digital(texto):
    """SHA-256 de um texto, usada para detectar mudanca externa do arquivo."""
    return hashlib.sha256(texto.encode("utf-8")).hexdigest()


def serializar(dados):
    """Serializa no formato dos arquivos reais: 2 espacos, UTF-8, linha final."""
    return json.dumps(dados, ensure_ascii=False, indent=2) + "\n"


def forma_canonica(dados):
    """Texto estavel de um baralho, para casar aprovacao de validacao e conteudo."""
    return json.dumps(dados, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def chave_do_baralho(origem, baralho_id):
    """Chave opaca usada pelo navegador. O cliente nunca envia caminho."""
    return "{}:{}".format(origem, baralho_id)


def interpretar_chave(chave):
    """Devolve (origem, id) de uma chave, validando os dois."""
    if not isinstance(chave, str) or chave.count(":") != 1:
        raise FalhaDaFonte("chave_invalida", "Baralho desconhecido.")
    origem, baralho_id = chave.split(":", 1)
    if origem not in (ORIGEM_ASSET, ORIGEM_CATALOGO, ORIGEM_RASCUNHO):
        raise FalhaDaFonte("chave_invalida", "Baralho desconhecido.")
    if not PADRAO_DE_ID.match(baralho_id):
        raise FalhaDaFonte("chave_invalida", "Baralho desconhecido.")
    return origem, baralho_id


def caminho_do_baralho_do_catalogo(config, baralho_id):
    """Resolve baralhos/<id>.json dentro da pasta do catalogo.

    O id ja passou por PADRAO_DE_ID; ainda assim o caminho resolvido e
    conferido contra a pasta resolvida, o que tambem barra symlink apontando
    para fora do catalogo.
    """
    if not PADRAO_DE_ID.match(baralho_id or ""):
        raise FalhaDaFonte("chave_invalida", "Baralho desconhecido.")
    pasta = config.pasta_de_baralhos_do_catalogo.resolve()
    caminho = (pasta / "{}.json".format(baralho_id)).resolve()
    if caminho.parent != pasta:
        raise FalhaDaFonte("caminho_invalido", "Baralho fora da pasta do catálogo.")
    return caminho


def _ler_json(caminho, rotulo):
    try:
        texto = caminho.read_text(encoding="utf-8")
    except FileNotFoundError:
        raise FalhaDaFonte("arquivo_ausente", "{} não foi encontrado.".format(rotulo))
    except OSError:
        raise FalhaDaFonte("leitura_falhou", "Não foi possível ler {}.".format(rotulo))
    try:
        return json.loads(texto), texto
    except json.JSONDecodeError as erro:
        raise FalhaDaFonte(
            "json_invalido",
            "{} não é um JSON válido (linha {}).".format(rotulo, erro.lineno),
        )


def ler_envelope_do_asset(config):
    """Devolve o envelope da biblioteca privada, nunca o asset do APK."""
    envelope, texto = _ler_json(config.arquivo_do_asset, "A biblioteca local legada")
    if not isinstance(envelope, dict) or not isinstance(envelope.get("baralhos"), list):
        raise FalhaDaFonte("formato_inesperado", "A biblioteca local não tem a lista de baralhos.")
    return envelope, texto


def ler_indice_do_catalogo(config):
    """Devolve (indice, texto) de indice.json."""
    indice, texto = _ler_json(config.arquivo_do_indice, "O índice do catálogo")
    if not isinstance(indice, dict) or not isinstance(indice.get("baralhos"), list):
        raise FalhaDaFonte(
            "formato_inesperado", "O índice do catálogo não tem a lista de baralhos."
        )
    return indice, texto


class BaralhoNaOrigem:
    """Baralho lido de uma origem, com o que e preciso para editar com seguranca."""

    def __init__(self, origem, dados, hash_da_origem, caminho=None,
                 entrada_do_indice=None, indice_na_lista=None,
                 hash_do_indice=None):
        self.origem = origem
        self.dados = dados
        self.hash_da_origem = hash_da_origem
        self.caminho = caminho
        self.entrada_do_indice = entrada_do_indice
        self.indice_na_lista = indice_na_lista
        self.hash_do_indice = hash_do_indice

    @property
    def id(self):
        return self.dados.get("id")

    @property
    def chave(self):
        return chave_do_baralho(self.origem, self.id)

    @property
    def protegido(self):
        return self.dados.get("estado") not in ESTADOS_COMPATIVEIS


def ler_baralho_da_origem(config, origem, baralho_id):
    """Le um baralho especifico de "asset" ou "catalogo".

    hash_da_origem cobre o arquivo inteiro que seria reescrito - o envelope do
    asset ou o arquivo do baralho no catalogo -, porque e ele que denuncia
    edicao externa entre abrir e aplicar.
    """
    if origem == ORIGEM_ASSET:
        envelope, texto = ler_envelope_do_asset(config)
        for posicao, baralho in enumerate(envelope["baralhos"]):
            if isinstance(baralho, dict) and baralho.get("id") == baralho_id:
                return BaralhoNaOrigem(
                    origem=ORIGEM_ASSET,
                    dados=baralho,
                    hash_da_origem=impressao_digital(texto),
                    caminho=config.arquivo_do_asset,
                    indice_na_lista=posicao,
                )
        raise FalhaDaFonte("baralho_ausente", "Este baralho não está na biblioteca local.")

    if origem == ORIGEM_CATALOGO:
        caminho = caminho_do_baralho_do_catalogo(config, baralho_id)
        dados, texto = _ler_json(caminho, "O arquivo do baralho no catálogo")
        if not isinstance(dados, dict):
            raise FalhaDaFonte(
                "formato_inesperado", "O arquivo do baralho não é um objeto JSON."
            )
        entrada = None
        hash_do_indice = None
        try:
            indice, texto_do_indice = ler_indice_do_catalogo(config)
            hash_do_indice = impressao_digital(texto_do_indice)
            for item in indice["baralhos"]:
                if isinstance(item, dict) and item.get("id") == baralho_id:
                    entrada = item
                    break
        except FalhaDaFonte:
            entrada = None
        return BaralhoNaOrigem(
            origem=ORIGEM_CATALOGO,
            dados=dados,
            hash_da_origem=impressao_digital(texto),
            caminho=caminho,
            entrada_do_indice=entrada,
            hash_do_indice=hash_do_indice,
        )

    raise FalhaDaFonte("origem_invalida", "Origem desconhecida.")


def _e_tecnico(baralho_id, colecao):
    colecao_id = (colecao or {}).get("id")
    return baralho_id in IDS_TECNICOS or colecao_id in COLECOES_TECNICAS


def _respostas(baralho):
    cards = baralho.get("cards")
    if not isinstance(cards, list):
        return []
    return [card.get("answer", "") for card in cards if isinstance(card, dict)]


def _item_do_inventario(origem, baralho, extras=None):
    colecao = baralho.get("colecao") if isinstance(baralho.get("colecao"), dict) else {}
    baralho_id = baralho.get("id") or ""
    estado = baralho.get("estado") or ""
    cards = baralho.get("cards") if isinstance(baralho.get("cards"), list) else []
    item = {
        "chave": chave_do_baralho(origem, baralho_id),
        "origem": origem,
        "origemRotulo": ROTULO_DA_ORIGEM[origem],
        "origemDetalhe": DETALHE_DA_ORIGEM[origem],
        "id": baralho_id,
        "nome": baralho.get("nome") or baralho_id,
        "grupo": colecao.get("nome") or "Sem agrupamento",
        "grupoId": colecao.get("id") or "",
        "icone": colecao.get("icone") or "",
        "categoria": baralho.get("categoria") or "",
        "quantidadeDeCards": len(cards),
        "estado": estado,
        "estadoAmigavel": ESTADO_AMIGAVEL.get(estado, estado or "Desconhecido"),
        "protegido": estado not in ESTADOS_COMPATIVEIS,
        "tecnico": _e_tecnico(baralho_id, colecao),
        "versao": baralho.get("versao"),
        "respostas": _respostas(baralho),
        "editavel": estado in ESTADOS_COMPATIVEIS,
        "observacoes": [],
        "duplicadoEm": [],
        "temRascunho": False,
    }
    if extras:
        item.update(extras)
    return item


def inventariar(config, rascunhos=None):
    """Monta a biblioteca consolidada das duas origens mais os rascunhos.

    Devolve {"baralhos": [...], "avisos": [...]}. Um erro em uma origem vira
    aviso e nao derruba as outras - o painel precisa continuar util quando,
    por exemplo, a pasta do catalogo nao existe nesta maquina.
    """
    itens = []
    avisos = []

    try:
        envelope, _ = ler_envelope_do_asset(config)
        for baralho in envelope["baralhos"]:
            if isinstance(baralho, dict):
                itens.append(_item_do_inventario(ORIGEM_ASSET, baralho))
    except FalhaDaFonte as falha:
        avisos.append("Biblioteca local legada: {}".format(falha.mensagem))

    itens.extend(_inventariar_catalogo(config, avisos))

    if rascunhos is not None:
        itens.extend(_inventariar_rascunhos(config, rascunhos, itens))

    _marcar_duplicados(itens)
    itens.sort(key=lambda item: (item["tecnico"], item["grupo"].lower(), item["nome"].lower()))
    return {"baralhos": itens, "avisos": avisos}


def _ler_arquivos_do_catalogo(config, avisos):
    pasta = config.pasta_de_baralhos_do_catalogo
    pasta_real = pasta.resolve()
    arquivos = {}
    if not pasta.is_dir():
        avisos.append("Cópia local do catálogo: a pasta baralhos/ não existe nesta máquina.")
        return arquivos
    for caminho in sorted(pasta.glob("*.json")):
        try:
            caminho_real = caminho.resolve(strict=True)
        except OSError:
            avisos.append(
                "Cópia local do catálogo: {} não pôde ser resolvido com segurança.".format(
                    caminho.name
                )
            )
            continue
        if caminho_real.parent != pasta_real or not caminho_real.is_file():
            avisos.append(
                "Cópia local do catálogo: {} aponta para fora de baralhos/ e foi ignorado.".format(
                    caminho.name
                )
            )
            continue
        try:
            dados = json.loads(caminho_real.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            avisos.append(
                "Cópia local do catálogo: {} não pôde ser lido.".format(caminho.name)
            )
            continue
        if isinstance(dados, dict) and dados.get("id"):
            arquivos[dados["id"]] = dados
    return arquivos


def _divergencias_do_indice(entrada, dados, item):
    """Compara a entrada do indice com o arquivo real do baralho."""
    if entrada.get("versao") != dados.get("versao"):
        item["observacoes"].append(
            "Índice e arquivo discordam da versão ({} contra {}).".format(
                entrada.get("versao"), dados.get("versao")
            )
        )
    if entrada.get("quantidadeDeCards") != item["quantidadeDeCards"]:
        item["observacoes"].append(
            "Índice declara {} card(s); o arquivo tem {}.".format(
                entrada.get("quantidadeDeCards"), item["quantidadeDeCards"]
            )
        )
    colecao = entrada.get("colecao") if isinstance(entrada.get("colecao"), dict) else {}
    if colecao.get("id") and colecao.get("id") != item["grupoId"]:
        item["observacoes"].append(
            "Índice agrupa em {}; o arquivo agrupa em {}.".format(
                colecao.get("nome"), item["grupo"]
            )
        )


def ler_todos_os_baralhos_do_catalogo(config):
    """Wrapper público: ({id: dados}, avisos) de baralhos/*.json.

    Uso interno de ferramentas (ex.: migração do acervo editorial) que
    precisam do conteúdo real E de saber quando uma origem falhou, em vez de
    silenciar o problema.
    """
    avisos = []
    arquivos = _ler_arquivos_do_catalogo(config, avisos)
    return arquivos, avisos


def _inventariar_catalogo(config, avisos):
    """Enumera o indice E os arquivos locais, inclusive os que faltam nele."""
    itens = []
    entradas = []
    try:
        indice, _ = ler_indice_do_catalogo(config)
        entradas = [item for item in indice["baralhos"] if isinstance(item, dict)]
    except FalhaDaFonte as falha:
        avisos.append("Cópia local do catálogo: {}".format(falha.mensagem))

    arquivos = _ler_arquivos_do_catalogo(config, avisos)
    ids_do_indice = set()

    for entrada in entradas:
        entrada_id = entrada.get("id") or ""
        ids_do_indice.add(entrada_id)
        dados = arquivos.get(entrada_id)
        if dados is None:
            colecao = entrada.get("colecao") if isinstance(entrada.get("colecao"), dict) else {}
            item = _item_do_inventario(
                ORIGEM_CATALOGO,
                {
                    "id": entrada_id,
                    "nome": entrada.get("nome"),
                    "categoria": entrada.get("categoria"),
                    "colecao": colecao,
                    "versao": entrada.get("versao"),
                    "estado": entrada.get("estado"),
                    "cards": [],
                },
                extras={
                    "quantidadeDeCards": entrada.get("quantidadeDeCards") or 0,
                    "noIndice": True,
                    "arquivoLocal": False,
                    "editavel": False,
                },
            )
            item["observacoes"].append(
                "O índice cita este baralho, mas o arquivo não existe na cópia local."
            )
            itens.append(item)
            continue
        item = _item_do_inventario(
            ORIGEM_CATALOGO,
            dados,
            extras={
                "noIndice": True,
                "arquivoLocal": True,
                "descricao": entrada.get("descricao", ""),
            },
        )
        _divergencias_do_indice(entrada, dados, item)
        itens.append(item)

    for baralho_id, dados in arquivos.items():
        if baralho_id in ids_do_indice:
            continue
        item = _item_do_inventario(
            ORIGEM_CATALOGO, dados, extras={"noIndice": False, "arquivoLocal": True}
        )
        item["observacoes"].append(
            "Arquivo local sem entrada no índice: não aparece no catálogo do app."
        )
        itens.append(item)

    return itens


def _inventariar_rascunhos(config, rascunhos, itens_das_fontes):
    """Rascunhos novos viram itens; edicoes so marcam mudancas reais.

    A validacao para publicacao pode manter um registro privado identico a
    origem apenas para guardar a aprovacao. Esse registro tecnico nao e uma
    alteracao pendente e, portanto, nao deve aparecer como "Rascunho".
    """
    novos = []
    por_chave = {item["chave"]: item for item in itens_das_fontes}
    for registro in rascunhos.listar():
        if registro.get("tipo") == "novo":
            item = _item_do_inventario(
                ORIGEM_RASCUNHO,
                registro.get("baralho") or {},
                extras={"editavel": True, "temRascunho": True, "arquivoLocal": False},
            )
            item["observacoes"].append(
                "Rascunho local: nunca foi publicado nem inserido no índice do catálogo."
            )
            novos.append(item)
            continue
        alvo = por_chave.get(registro.get("chave") or "")
        if alvo is not None:
            try:
                origem, baralho_id = interpretar_chave(registro.get("chave"))
                atual = ler_baralho_da_origem(config, origem, baralho_id)
                sem_alteracao = forma_canonica(registro.get("baralho")) == forma_canonica(
                    atual.dados
                )
            except (FalhaDaFonte, TypeError, ValueError):
                sem_alteracao = False
            if sem_alteracao:
                continue
            alvo["temRascunho"] = True
            alvo["observacoes"].append("Há alterações em rascunho ainda não aplicadas.")
    return novos


def _marcar_duplicados(itens):
    """Mesmo id em origens diferentes: agrupar na tela, nunca escrever nas duas."""
    por_id = {}
    for item in itens:
        por_id.setdefault(item["id"], []).append(item)
    for irmaos in por_id.values():
        if len(irmaos) < 2:
            continue
        for item in irmaos:
            item["duplicadoEm"] = [
                outro["origemRotulo"] for outro in irmaos if outro is not item
            ]
            item["observacoes"].append(
                "Mesmo id em mais de uma origem. Cada origem é editada separadamente."
            )
