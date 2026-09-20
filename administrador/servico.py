"""Orquestracao da Central de Baralhos, sem nenhuma dependencia de HTTP.

O servidor traduz requisicao para chamada daqui e resposta para JSON. Toda a
decisao de seguranca de conteudo (compatibilidade de estado, preservacao de ids,
exigencia de validacao antes de aplicar) mora neste modulo e nos que ele usa,
nao no navegador.
"""
from __future__ import annotations

import copy
import secrets
import threading

import aplicacao
import acervo_editorial
import fontes
from feedbacks import ArmazemDeFeedbacks
from edicao import (
    FalhaDaEdicao,
    acrescentar_card,
    aplicar_edicao_no_baralho,
    aplicar_projecao_no_baralho,
    assegurar_editavel,
    criar_baralho_de_rascunho,
    remover_card_do_rascunho,
)
from rascunhos import Rascunhos, aprovacao_valida, marcar_aprovacao
from validacao import ESTADO_APROVADO, Validacoes


class FalhaDoServico(ValueError):
    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo
        self.mensagem = mensagem


class Central:
    """Fachada da central. Uma instancia por processo do servidor."""

    def __init__(self, config, validacoes=None):
        self.config = config
        self.rascunhos = Rascunhos(config.pasta_de_rascunhos)
        self.acervo = Rascunhos(config.pasta_do_acervo)
        self.feedbacks = ArmazemDeFeedbacks(config.pasta_de_feedbacks)
        self.validacoes = validacoes or Validacoes(config)
        self._trava = threading.Lock()

    # ---- biblioteca -----------------------------------------------------

    def inventario(self):
        dados = fontes.inventariar(self.config, self.rascunhos)
        dados["origens"] = [
            {
                "origem": origem,
                "rotulo": fontes.ROTULO_DA_ORIGEM[origem],
                "detalhe": fontes.DETALHE_DA_ORIGEM[origem],
            }
            for origem in (fontes.ORIGEM_ASSET, fontes.ORIGEM_CATALOGO, fontes.ORIGEM_RASCUNHO)
        ]
        dados["feedbacks"] = self.feedbacks.resumo()
        return dados

    def _registro_da_origem(self, chave, criar=False):
        """Registro do rascunho, ou um registro efemero espelhando a origem."""
        registro = self.rascunhos.ler(chave)
        if registro is not None:
            return registro, False
        origem, baralho_id = fontes.interpretar_chave(chave)
        if origem == fontes.ORIGEM_RASCUNHO:
            raise FalhaDoServico("rascunho_ausente", "Este rascunho não existe mais.")
        atual = fontes.ler_baralho_da_origem(self.config, origem, baralho_id)
        registro = {
            "chave": chave,
            "tipo": "edicao",
            "origem": origem,
            "baralhoId": baralho_id,
            "baralho": copy.deepcopy(atual.dados),
            "hashDaOrigem": atual.hash_da_origem,
            "hashDoIndice": atual.hash_do_indice,
            "numeroDaRevisao": 0,
            "aprovacao": None,
        }
        if criar:
            registro = self.rascunhos.salvar(registro)
            return registro, True
        return registro, True

    @staticmethod
    def _revisao(registro):
        """Token opaco de concorrência otimista para abas e respostas atrasadas."""
        conteudo = {
            "chave": registro.get("chave"),
            "tipo": registro.get("tipo"),
            "numero": registro.get("numeroDaRevisao", 0),
            "baralho": registro.get("baralho"),
            "hashDaOrigem": registro.get("hashDaOrigem"),
            "hashDoIndice": registro.get("hashDoIndice"),
        }
        return fontes.impressao_digital(fontes.forma_canonica(conteudo))

    def _exigir_revisao(self, registro, revisao):
        if not isinstance(revisao, str) or not revisao:
            raise FalhaDoServico(
                "revisao_ausente", "Reabra o baralho antes de alterar este rascunho."
            )
        if not secrets.compare_digest(revisao, self._revisao(registro)):
            raise FalhaDoServico(
                "revisao_obsoleta",
                "Este rascunho mudou em outra aba ou requisição. Reabra para não perder edições.",
            )

    def _assegurar_origem_inalterada(self, registro):
        if registro.get("tipo") == "novo":
            return
        origem, baralho_id = fontes.interpretar_chave(registro.get("chave"))
        atual = fontes.ler_baralho_da_origem(self.config, origem, baralho_id)
        if registro.get("hashDaOrigem") != atual.hash_da_origem:
            raise FalhaDoServico(
                "conflito_externo",
                "O arquivo de origem mudou fora do painel. Descarte ou reabra o rascunho.",
            )
        if registro.get("hashDoIndice") != atual.hash_do_indice:
            raise FalhaDoServico(
                "conflito_externo",
                "O índice do catálogo mudou fora do painel. Descarte ou reabra o rascunho.",
            )

    def _resposta_do_registro(self, registro):
        plano = self._plano(registro)
        resposta = {
            "chave": registro["chave"],
            "atualizadoEm": registro.get("atualizadoEm"),
            "baralho": registro["baralho"],
            "revisao": self._revisao(registro),
            "plano": plano.resumo() if plano is not None else None,
            "aprovado": self._aprovado(registro, plano),
        }
        resposta["feedbacks"] = self.feedbacks.para_baralho(registro["baralho"])
        return resposta

    def importar_feedbacks(self, nome, conteudo):
        """Guarda uma exportacao sanitizada no diretorio privado do painel."""
        with self._trava:
            return self.feedbacks.importar(nome, conteudo)

    def remover_baralho(self, chave, confirmacao, revisao):
        """Remove uma origem real após confirmação textual exata e cria backup."""
        with self._trava:
            registro, _ = self._registro_da_origem(chave)
            self._exigir_revisao(registro, revisao)
            if registro.get("tipo") == "novo":
                raise FalhaDoServico(
                    "somente_origem",
                    "Este item é um rascunho novo. Use Descartar rascunho.",
                )
            baralho_id = registro.get("baralhoId")
            if not isinstance(confirmacao, str) or not secrets.compare_digest(
                confirmacao, baralho_id or ""
            ):
                raise FalhaDoServico(
                    "confirmacao_incorreta",
                    "Digite exatamente o id do baralho para confirmar a remoção.",
                )
            self._assegurar_origem_inalterada(registro)
            relatorio = aplicacao.remover_baralho(self.config, registro)
            self.rascunhos.descartar(chave)
        return relatorio

    def abrir(self, chave):
        """Conteudo para o editor: origem, rascunho quando houver e o plano."""
        registro, efemero = self._registro_da_origem(chave)
        origem_atual = None
        entrada_do_indice = None
        if registro.get("tipo") != "novo":
            origem, baralho_id = fontes.interpretar_chave(chave)
            origem_atual = fontes.ler_baralho_da_origem(self.config, origem, baralho_id)
            entrada_do_indice = origem_atual.entrada_do_indice
        tem_alteracoes_em_rascunho = not efemero and (
            registro.get("tipo") == "novo"
            or origem_atual is None
            or fontes.forma_canonica(registro.get("baralho"))
            != fontes.forma_canonica(origem_atual.dados)
        )
        plano = self._plano(registro)
        plano_da_validacao = self._plano_para_validacao(registro, plano)
        return {
            "chave": chave,
            "tipo": registro.get("tipo"),
            "origem": registro.get("origem"),
            "origemRotulo": fontes.ROTULO_DA_ORIGEM[registro.get("origem")],
            "origemDetalhe": fontes.DETALHE_DA_ORIGEM[registro.get("origem")],
            "baralho": registro["baralho"],
            "revisao": self._revisao(registro),
            "protegido": registro["baralho"].get("estado") not in fontes.ESTADOS_COMPATIVEIS,
            "temRascunho": tem_alteracoes_em_rascunho,
            "atualizadoEm": registro.get("atualizadoEm"),
            "entradaDoIndice": entrada_do_indice,
            "conteudoDaOrigem": origem_atual.dados if origem_atual is not None else None,
            "plano": plano.resumo() if plano is not None else None,
            "aprovado": self._aprovado(registro, plano_da_validacao),
            "feedbacks": self.feedbacks.para_baralho(registro["baralho"]),
        }

    def _plano(self, registro):
        try:
            return aplicacao.montar_plano(self.config, registro)
        except (aplicacao.FalhaDaAplicacao, FalhaDaEdicao, fontes.FalhaDaFonte):
            return None

    def _plano_para_validacao(self, registro, plano_local=None):
        """Escolhe entre validar uma alteração local ou a publicação da origem."""
        plano_local = plano_local or self._plano(registro)
        if plano_local is None:
            return None
        if (
            registro.get("tipo") != "novo"
            and plano_local.sem_alteracao
            and not plano_local.conflito
        ):
            try:
                return aplicacao.montar_plano_de_publicacao(self.config, registro)
            except (aplicacao.FalhaDaAplicacao, FalhaDaEdicao, fontes.FalhaDaFonte):
                return None
        return plano_local

    def _aprovado(self, registro, plano):
        if plano is None:
            return False
        return (
            not plano.conflito
            and aprovacao_valida(
                registro,
                plano.alvo_principal,
                plano.hash_do_candidato,
                plano.hash_da_validacao,
            )
        )

    # ---- rascunho -------------------------------------------------------

    def salvar_rascunho(self, chave, alteracoes, revisao):
        """Guarda o trabalho no diretorio privado. Nenhuma origem e tocada."""
        with self._trava:
            registro, _ = self._registro_da_origem(chave)
            self._exigir_revisao(registro, revisao)
            self._assegurar_origem_inalterada(registro)
            base = registro["baralho"]
            assegurar_editavel(base)
            registro["baralho"] = aplicar_edicao_no_baralho(base, alteracoes)
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.rascunhos.salvar(registro)
        return self._resposta_do_registro(registro)

    def descartar_rascunho(self, chave, revisao):
        """Apaga o rascunho. A origem continua exatamente como estava."""
        with self._trava:
            registro = self.rascunhos.ler(chave)
            if registro is None:
                raise FalhaDoServico("rascunho_ausente", "Este rascunho não existe mais.")
            self._exigir_revisao(registro, revisao)
            existia = self.rascunhos.descartar(chave)
        return {"chave": chave, "descartado": existia}

    def criar_rascunho(self, nome, grupo, icone, categoria):
        """Cria um baralho novo que vive apenas no diretorio privado."""
        with self._trava:
            baralho = criar_baralho_de_rascunho(nome, grupo, icone, categoria)
            chave = fontes.chave_do_baralho(fontes.ORIGEM_RASCUNHO, baralho["id"])
            ids_existentes = {item["id"] for item in self.inventario()["baralhos"]}
            if baralho["id"] in ids_existentes:
                raise FalhaDoServico(
                    "id_existente",
                    "Já existe um baralho com esse id em uma das origens. Use outro nome.",
                )
            registro = self.rascunhos.salvar(
                {
                    "chave": chave,
                    "tipo": "novo",
                    "origem": fontes.ORIGEM_RASCUNHO,
                    "baralhoId": baralho["id"],
                    "baralho": baralho,
                    "hashDaOrigem": None,
                    "hashDoIndice": None,
                    "numeroDaRevisao": 1,
                    "aprovacao": None,
                }
            )
        return self._resposta_do_registro(registro)

    def acrescentar_card(self, chave, revisao):
        """Card novo com id estavel, so em rascunho novo."""
        with self._trava:
            registro = self.rascunhos.ler(chave)
            if registro is None or registro.get("tipo") != "novo":
                raise FalhaDoServico(
                    "somente_rascunho_novo",
                    "Cards só podem ser acrescentados em um rascunho novo.",
                )
            self._exigir_revisao(registro, revisao)
            registro["baralho"], identificador = acrescentar_card(registro["baralho"])
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.rascunhos.salvar(registro)
        resposta = self._resposta_do_registro(registro)
        resposta["cardId"] = identificador
        return resposta

    def remover_card(self, chave, card_id, revisao):
        with self._trava:
            registro = self.rascunhos.ler(chave)
            if registro is None or registro.get("tipo") != "novo":
                raise FalhaDoServico(
                    "somente_rascunho_novo",
                    "Cards só podem ser removidos em um rascunho novo.",
                )
            self._exigir_revisao(registro, revisao)
            registro["baralho"] = remover_card_do_rascunho(registro["baralho"], card_id)
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.rascunhos.salvar(registro)
        return self._resposta_do_registro(registro)

    def exportar(self, chave, revisao):
        """JSON do rascunho novo, para integracao deliberada depois."""
        registro = self.rascunhos.ler(chave)
        if registro is None or registro.get("tipo") != "novo":
            raise FalhaDoServico(
                "somente_rascunho_novo", "Só rascunhos novos são exportados por aqui."
            )
        self._exigir_revisao(registro, revisao)
        plano = aplicacao.montar_plano(self.config, registro)
        if not aprovacao_valida(
            registro, plano.alvo_principal, plano.hash_do_candidato, plano.hash_da_validacao
        ):
            raise FalhaDoServico(
                "sem_validacao",
                "Valide este conteúdo exato antes de exportar. Qualquer edição exige validar de novo.",
            )
        nome = "{}.json".format(registro["baralho"].get("id") or "rascunho")
        return aplicacao.exportar_rascunho(registro), nome

    # ---- validacao e aplicacao -----------------------------------------

    def validar(self, chave, revisao):
        """Dispara a regua real do app. Nao espera o Gradle terminar."""
        registro, _ = self._registro_da_origem(chave, criar=True)
        self._exigir_revisao(registro, revisao)
        self._assegurar_origem_inalterada(registro)
        plano_local = aplicacao.montar_plano(self.config, registro)
        plano = self._plano_para_validacao(registro, plano_local)
        if plano is None:
            raise FalhaDoServico("nao_aplicavel", "Nada a validar.")
        if (
            registro.get("tipo") != "novo"
            and not plano_local.aplicavel
            and plano.alvo_principal != aplicacao.ALVO_FIRESTORE
        ):
            raise FalhaDoServico(
                "nao_aplicavel", plano_local.motivo_de_nao_aplicavel or "Nada a validar."
            )
        return self.validacoes.iniciar(plano, chave)

    def estado_da_validacao(self, identificador):
        trabalho = self.validacoes.estado(identificador)
        if trabalho is None:
            raise FalhaDoServico("validacao_desconhecida", "Validação não encontrada.")
        if trabalho["estado"] == ESTADO_APROVADO:
            self._registrar_aprovacao(trabalho)
        return trabalho

    def _registrar_aprovacao(self, trabalho):
        """Guarda a aprovacao presa ao hash exato do candidato validado."""
        with self._trava:
            registro = self.rascunhos.ler(trabalho["chave"])
            if registro is None:
                return
            atual = (registro.get("aprovacao") or {}).get("hashDaValidacao")
            if atual == trabalho["hashDaValidacao"]:
                return
            plano = self._plano_para_validacao(registro)
            if plano is None or plano.hash_da_validacao != trabalho["hashDaValidacao"]:
                # O rascunho ou a origem mudou depois de validar: nao vale.
                return
            marcar_aprovacao(
                registro,
                plano.alvo_principal,
                trabalho["hashDoCandidato"],
                trabalho["hashDaValidacao"],
                trabalho["resumo"],
            )
            self.rascunhos.salvar(registro)

    def aplicar(self, chave, confirmacao, revisao):
        """Grava na origem local. Exige confirmacao e validacao do candidato exato."""
        if confirmacao is not True:
            raise FalhaDoServico("sem_confirmacao", "Confirmação explícita necessária.")
        with self._trava:
            registro = self.rascunhos.ler(chave)
            if registro is None:
                raise FalhaDoServico(
                    "sem_rascunho", "Não há alterações em rascunho para aplicar."
                )
            self._exigir_revisao(registro, revisao)
            self._assegurar_origem_inalterada(registro)
            plano = aplicacao.montar_plano(self.config, registro)
            if not aprovacao_valida(
                registro,
                plano.alvo_principal,
                plano.hash_do_candidato,
                plano.hash_da_validacao,
            ):
                raise FalhaDoServico(
                    "sem_validacao",
                    "Valide este conteúdo antes de aplicar. Qualquer edição depois "
                    "da validação exige validar de novo.",
                )
            relatorio = aplicacao.aplicar(self.config, registro, plano)
            self.rascunhos.descartar(chave)
        return relatorio

    # ---- acervo editorial (respostas e bancos de dicas) ------------------

    def previa_do_acervo(self, incluir_tecnicos=False):
        """Dry-run local da migração: contagens e conflitos, sem gravar nada."""
        return acervo_editorial.gerar_previa_da_migracao(
            self.config, incluir_tecnicos=incluir_tecnicos
        )

    @staticmethod
    def _chave_da_resposta(resposta_id):
        return "resposta:{}".format(resposta_id)

    def _registro_da_resposta(self, resposta_id, criar=False):
        chave = self._chave_da_resposta(resposta_id)
        registro = self.acervo.ler(chave)
        if registro is not None:
            return registro, False
        previa = self.previa_do_acervo()
        resposta = previa["respostas"].get(resposta_id)
        if resposta is None:
            raise FalhaDoServico(
                "resposta_ausente", "Esta resposta não existe nas origens locais."
            )
        registro = {
            "chave": chave,
            "respostaId": resposta_id,
            "resposta": copy.deepcopy(resposta),
            "dicas": copy.deepcopy(previa["dicas"].get(resposta_id, {})),
            "numeroDaRevisao": 0,
        }
        if criar:
            registro = self.acervo.salvar(registro)
            return registro, True
        return registro, True

    @staticmethod
    def _revisao_do_acervo(registro):
        conteudo = {
            "chave": registro.get("chave"),
            "numero": registro.get("numeroDaRevisao", 0),
            "resposta": registro.get("resposta"),
            "dicas": registro.get("dicas"),
        }
        return fontes.impressao_digital(fontes.forma_canonica(conteudo))

    def _exigir_revisao_do_acervo(self, registro, revisao):
        if not isinstance(revisao, str) or not revisao:
            raise FalhaDoServico(
                "revisao_ausente", "Reabra a resposta antes de alterar este rascunho."
            )
        if not secrets.compare_digest(revisao, self._revisao_do_acervo(registro)):
            raise FalhaDoServico(
                "revisao_obsoleta",
                "Esta resposta mudou em outra aba ou requisição. Reabra para não perder edições.",
            )

    def _resposta_do_registro_de_acervo(self, registro):
        dicas = registro["dicas"]
        return {
            "chave": registro["chave"],
            "respostaId": registro["respostaId"],
            "resposta": registro["resposta"],
            "dicas": dicas,
            "quantidadeDeDicasAtivas": sum(
                1 for dica in dicas.values() if dica.get("status") == acervo_editorial.STATUS_ATIVA
            ),
            "prontaParaPublicarPublico": acervo_editorial.pronta_para_publicar(
                dicas, acervo_editorial.ESCOPO_PUBLICO
            ),
            "prontaParaPublicarPrivado": acervo_editorial.pronta_para_publicar(
                dicas, acervo_editorial.ESCOPO_PRIVADO
            ),
            "revisao": self._revisao_do_acervo(registro),
            "atualizadoEm": registro.get("atualizadoEm"),
            "escopoPadraoParaNovaDica": acervo_editorial.escopo_padrao_para_nova_dica(dicas),
            "feedbacks": self.feedbacks.para_dicas(registro["respostaId"], None, registro["resposta"].get("referencias", [])),
            "temRascunhoLocal": registro.get("numeroDaRevisao", 0) > 0,
        }

    def abrir_resposta(self, resposta_id):
        """Conteúdo para o editor: acervo local salvo, ou a prévia da migração."""
        registro, _ = self._registro_da_resposta(resposta_id)
        return self._resposta_do_registro_de_acervo(registro)

    def acrescentar_dica(self, resposta_id, texto, escopo, revisao):
        with self._trava:
            registro, _ = self._registro_da_resposta(resposta_id, criar=True)
            self._exigir_revisao_do_acervo(registro, revisao)
            nova = acervo_editorial.acrescentar_dica(resposta_id, registro["dicas"], texto, escopo)
            registro["dicas"][nova["dicaId"]] = nova
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.acervo.salvar(registro)
        return self._resposta_do_registro_de_acervo(registro)

    def _alterar_dica(self, resposta_id, dica_id, revisao, transformar):
        with self._trava:
            registro, _ = self._registro_da_resposta(resposta_id, criar=True)
            self._exigir_revisao_do_acervo(registro, revisao)
            dica = registro["dicas"].get(dica_id)
            if dica is None:
                raise FalhaDoServico("dica_ausente", "Esta dica não existe mais.")
            registro["dicas"][dica_id] = transformar(dica)
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.acervo.salvar(registro)
        return self._resposta_do_registro_de_acervo(registro)

    def editar_texto_da_dica(self, resposta_id, dica_id, novo_texto, revisao):
        return self._alterar_dica(
            resposta_id, dica_id, revisao,
            lambda dica: acervo_editorial.editar_texto_da_dica(dica, novo_texto),
        )

    def editar_dica(self, resposta_id, dica_id, novo_texto, escopo, revisao):
        """Texto e escopo numa ÚNICA operação/revisão — nunca duas chamadas.

        Duas requisições sequenciais com a mesma revisão sempre deixariam a
        segunda falhar com `revisao_obsoleta` depois que a primeira mudasse o
        número da revisão, resultando em salvamento parcial. Campo que não
        mudou não gera transformação (e portanto não teria por que existir
        como uma escrita própria).
        """

        def transformar(dica):
            atualizada = dica
            if novo_texto is not None and novo_texto != atualizada.get("texto"):
                atualizada = acervo_editorial.editar_texto_da_dica(atualizada, novo_texto)
            if escopo is not None and escopo != atualizada.get("escopo"):
                atualizada = acervo_editorial.alterar_escopo_da_dica(atualizada, escopo)
            return atualizada

        return self._alterar_dica(resposta_id, dica_id, revisao, transformar)

    def alterar_escopo_da_dica(self, resposta_id, dica_id, escopo, revisao):
        return self._alterar_dica(
            resposta_id, dica_id, revisao,
            lambda dica: acervo_editorial.alterar_escopo_da_dica(dica, escopo),
        )

    def desativar_dica(self, resposta_id, dica_id, revisao):
        return self._alterar_dica(resposta_id, dica_id, revisao, acervo_editorial.desativar_dica)

    def reativar_dica(self, resposta_id, dica_id, revisao):
        return self._alterar_dica(resposta_id, dica_id, revisao, acervo_editorial.reativar_dica)

    def descartar_rascunho_da_resposta(self, resposta_id, revisao):
        with self._trava:
            chave = self._chave_da_resposta(resposta_id)
            registro = self.acervo.ler(chave)
            if registro is None:
                raise FalhaDoServico("rascunho_ausente", "Este rascunho não existe mais.")
            self._exigir_revisao_do_acervo(registro, revisao)
            existia = self.acervo.descartar(chave)
        return {"chave": chave, "descartado": existia}

    # ---- ponte de publicação: projetar banco do acervo num baralho -------

    def projetar_do_acervo(self, chave_do_baralho, projecoes_por_card, revisao):
        """Grava, só no RASCUNHO do baralho, o banco/clues vindos do acervo.

        Não toca o acervo editorial nem publica nada — o rascunho resultante
        passa pelo MESMO fluxo de validar/aplicar/publicar já existente para
        baralhos (`/api/validar`, `/api/aplicar`, botão "Publicar no
        Firestore"). `projecoes_por_card` já vem pronto do navegador,
        montado a partir das dicas ATIVAS autorizadas pelo escopo do
        baralho (ver `estaticos/acervo.js#prepararProjecaoDoBaralho`).
        """
        if not isinstance(projecoes_por_card, dict) or not projecoes_por_card:
            raise FalhaDoServico(
                "projecao_vazia", "Nenhuma projeção de banco foi enviada."
            )
        with self._trava:
            registro, _ = self._registro_da_origem(chave_do_baralho)
            self._exigir_revisao(registro, revisao)
            self._assegurar_origem_inalterada(registro)
            base = registro["baralho"]
            assegurar_editavel(base)
            for projecao in projecoes_por_card.values():
                for fato in projecao.get("bancoDeDicas", []):
                    if fato.get("escopo") not in ("PUBLICO", "PRIVADO"):
                        raise FalhaDoServico("escopo_ausente", "Projeção exige o escopo de cada dica.")
                    if base.get("categoria") != "ESPECIAIS" and fato["escopo"] == "PRIVADO":
                        raise FalhaDoServico("escopo_privado", "Dica privada em baralho público.")
            registro["baralho"] = aplicar_projecao_no_baralho(base, projecoes_por_card)
            certificados = registro.setdefault("projecoesDoAcervo", {})
            for card in registro["baralho"]["cards"]:
                if card["id"] in projecoes_por_card:
                    certificados[card["id"]] = {
                        "respostaId": card["respostaId"],
                        "ids": [f["id"] for f in card["bancoDeDicas"]],
                        "escopos": [f["escopo"] for f in projecoes_por_card[card["id"]]["bancoDeDicas"]],
                    }
            registro["numeroDaRevisao"] = registro.get("numeroDaRevisao", 0) + 1
            registro = self.rascunhos.salvar(registro)
        return self._resposta_do_registro(registro)
