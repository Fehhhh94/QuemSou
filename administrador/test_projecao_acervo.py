"""Fluxo integrado do serviço em fontes temporárias. Gradle real é opt-in separado."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

import aplicacao
import edicao
from apoio_de_teste import montar_ambiente, validador_simulado
from fontes import normalizar
from servico import Central, FalhaDoServico
from validacao import Validacoes


class TesteDaProjecaoIntegrada(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.config = montar_ambiente(Path(tmp.name))
        self.central = Central(self.config, Validacoes(self.config, executar=validador_simulado()))

    def projetar(self, card_id, tamanho, escopo="PUBLICO"):
        chave = "asset:demo-evolucao"
        aberto = self.central.abrir(chave)
        card = next(c for c in aberto["baralho"]["cards"] if c["id"] == card_id)
        banco = copy.deepcopy(card.get("bancoDeDicas") or [
            {"id": "legado:" + normalizar(t), "texto": t} for t in card["clues"]
        ])
        banco += [{"id": "novo-{}".format(i), "texto": "Fato adicional {}.".format(i)} for i in range(tamanho-len(banco))]
        for fato in banco:
            fato["escopo"] = escopo
        return self.central.projetar_do_acervo(chave, {card_id: {"bancoDeDicas": banco, "clues": [f["texto"] for f in banco[:10]]}}, aberto["revisao"])

    def test_legado_e_editorial_ate_500_validam_aplicam_e_preparam_publicacao(self):
        for card_id, tamanho in (("de-002", 11), ("de-001", 500)):
            with self.subTest(card=card_id):
                antes = self.central.abrir("asset:demo-evolucao")["baralho"]
                salvo = self.projetar(card_id, tamanho)
                self.assertTrue(salvo["plano"]["aplicavel"])
                novo = next(c for c in salvo["baralho"]["cards"] if c["id"] == card_id)
                self.assertEqual(len(novo["bancoDeDicas"]), tamanho)
                self.assertEqual(novo["respostaId"], "resposta-de-001" if card_id == "de-001" else "resposta legada")
                self.assertEqual([c for c in antes["cards"] if c["id"] != card_id], [c for c in salvo["baralho"]["cards"] if c["id"] != card_id])
                trabalho = self.central.validar(salvo["chave"], salvo["revisao"])
                self.central.validacoes.aguardar(trabalho["id"])
                self.assertEqual(self.central.estado_da_validacao(trabalho["id"])["estado"], "aprovado")
                self.central.aplicar(salvo["chave"], True, salvo["revisao"])
                reg, _ = self.central._registro_da_origem(salvo["chave"])
                plano = aplicacao.montar_plano_de_publicacao(self.config, reg)
                self.assertTrue(plano.sem_alteracao)
                self.assertEqual(plano.candidato["versao"], antes["versao"]+1)

    def test_edicao_manual_nao_ganha_permissao_de_trocar_identidade(self):
        salvo = self.projetar("de-002", 11)
        reg = self.central.rascunhos.ler(salvo["chave"])
        reg["baralho"]["cards"][1]["bancoDeDicas"][0]["id"] = "renomeado"
        with self.assertRaises(edicao.FalhaDaEdicao):
            aplicacao.montar_plano(self.config, reg)
        alteracoes = copy.deepcopy(salvo["baralho"])
        alteracoes["cards"][1]["respostaId"] = "renomeada"
        with self.assertRaises(edicao.FalhaDaEdicao):
            self.central.salvar_rascunho(salvo["chave"], alteracoes, salvo["revisao"])

    def test_privado_nao_vira_publico_apos_projecao(self):
        salvo = self.projetar("de-002", 11, "PRIVADO")
        reg = self.central.rascunhos.ler(salvo["chave"])
        reg["baralho"]["categoria"] = "PERSONAGEM_FILME"
        with self.assertRaises(edicao.FalhaDaEdicao):
            aplicacao.montar_plano(self.config, reg)

    def test_projecao_em_publico_recusa_escopo_privado(self):
        envelope = json.loads(self.config.arquivo_do_asset.read_text(encoding="utf-8"))
        envelope["baralhos"][1]["categoria"] = "PERSONAGEM_FILME"
        self.config.arquivo_do_asset.write_text(json.dumps(envelope), encoding="utf-8")
        with self.assertRaises(FalhaDoServico):
            self.projetar("de-002", 11, "PRIVADO")

    def test_escopo_privado_sobrevive_aplicacao_e_bloqueia_publicacao_publica(self):
        salvo = self.projetar("de-002", 11, "PRIVADO")
        trabalho = self.central.validar(salvo["chave"], salvo["revisao"])
        self.central.validacoes.aguardar(trabalho["id"])
        self.central.estado_da_validacao(trabalho["id"])
        self.central.aplicar(salvo["chave"], True, salvo["revisao"])
        registro, _ = self.central._registro_da_origem(salvo["chave"])
        self.assertNotIn("projecoesDoAcervo", registro)
        self.assertEqual(registro["baralho"]["cards"][1]["bancoDeDicas"][0]["escopo"], "PRIVADO")
        registro["baralho"]["categoria"] = "PERSONAGEM_FILME"
        with self.assertRaises(edicao.FalhaDaEdicao):
            aplicacao.montar_plano_de_publicacao(self.config, registro)
