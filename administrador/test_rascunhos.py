"""Rascunho privado: persistencia, recuperacao, descarte e aprovacao."""
import copy
import tempfile
import unittest
from pathlib import Path

import rascunhos
from apoio_de_teste import montar_ambiente
from servico import Central
from servico import FalhaDoServico


class TesteDeRascunhos(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.raiz = Path(self.temporario.name)
        self.config = montar_ambiente(self.raiz)
        self.central = Central(self.config)

    def _texto_da_origem(self):
        return self.config.arquivo_do_asset.read_text(encoding="utf-8")

    def salvar(self, chave, alteracoes):
        revisao = self.central.abrir(chave)["revisao"]
        return self.central.salvar_rascunho(chave, alteracoes, revisao)

    def test_salva_trabalho_incompleto_sem_tocar_a_origem(self):
        antes = self._texto_da_origem()
        self.salvar(
            "asset:demo-evolucao", {"nome": "Demo — trabalho em andamento"}
        )
        self.assertEqual(self._texto_da_origem(), antes)

    def test_recupera_depois_de_reiniciar_o_painel(self):
        self.salvar("asset:demo-evolucao", {"nome": "Nome parcial"})
        outra_sessao = Central(self.config)
        aberto = outra_sessao.abrir("asset:demo-evolucao")
        self.assertTrue(aberto["temRascunho"])
        self.assertEqual(aberto["baralho"]["nome"], "Nome parcial")

    def test_descartar_apaga_so_o_rascunho(self):
        antes = self._texto_da_origem()
        salvo = self.salvar("asset:demo-evolucao", {"nome": "Nome parcial"})
        self.central.descartar_rascunho("asset:demo-evolucao", salvo["revisao"])
        self.assertEqual(self._texto_da_origem(), antes)
        aberto = self.central.abrir("asset:demo-evolucao")
        self.assertFalse(aberto["temRascunho"])
        self.assertEqual(aberto["baralho"]["nome"], "Demo — Em Evolução")

    def test_rascunho_novo_aparece_no_inventario_como_nao_publicado(self):
        criado = self.central.criar_rascunho("Tema Piloto", "Especiais", "⭐", "ESPECIAIS")
        itens = {item["chave"]: item for item in self.central.inventario()["baralhos"]}
        item = itens[criado["chave"]]
        self.assertEqual(item["origem"], "rascunho")
        self.assertTrue(any("nunca foi publicado" in nota for nota in item["observacoes"]))

    def test_rascunho_de_edicao_marca_o_baralho_de_origem(self):
        self.salvar("asset:demo-evolucao", {"nome": "Nome parcial"})
        itens = {item["chave"]: item for item in self.central.inventario()["baralhos"]}
        self.assertTrue(itens["asset:demo-evolucao"]["temRascunho"])
        self.assertFalse(itens["catalogo:demo-evolucao"]["temRascunho"])

    def test_aprovacao_sem_alteracao_nao_aparece_como_rascunho(self):
        chave = "asset:demo-evolucao"
        registro, _ = self.central._registro_da_origem(chave, criar=True)
        rascunhos.marcar_aprovacao(registro, "firestore", "conteudo", "validacao")
        self.central.rascunhos.salvar(registro)

        itens = {item["chave"]: item for item in self.central.inventario()["baralhos"]}
        self.assertFalse(itens[chave]["temRascunho"])
        self.assertFalse(self.central.abrir(chave)["temRascunho"])

    def test_edicao_de_rascunho_nao_altera_a_outra_origem(self):
        antes = (self.config.pasta_de_baralhos_do_catalogo / "demo-evolucao.json").read_text(
            encoding="utf-8"
        )
        self.salvar("asset:demo-evolucao", {"nome": "Só no asset"})
        depois = (self.config.pasta_de_baralhos_do_catalogo / "demo-evolucao.json").read_text(
            encoding="utf-8"
        )
        self.assertEqual(antes, depois)
        aberto = self.central.abrir("catalogo:demo-evolucao")
        self.assertEqual(aberto["baralho"]["nome"], "Demo — Em Evolução (catálogo)")

    def test_resposta_autoritativa_sustenta_duas_correcoes_seguidas(self):
        chave = "asset:demo-evolucao"
        aberto = self.central.abrir(chave)
        cards = copy.deepcopy(aberto["baralho"]["cards"])
        cards[0]["clues"][0] = "primeira correção"
        primeiro = self.central.salvar_rascunho(
            chave, {"cards": cards}, aberto["revisao"]
        )
        self.assertEqual(
            primeiro["baralho"]["cards"][0]["bancoDeDicas"][0]["texto"],
            "primeira correção",
        )
        cards = copy.deepcopy(primeiro["baralho"]["cards"])
        cards[0]["clues"][1] = "segunda correção"
        segundo = self.central.salvar_rascunho(
            chave, {"cards": cards}, primeiro["revisao"]
        )
        self.assertEqual(segundo["baralho"]["cards"][0]["clues"][0], "primeira correção")
        self.assertEqual(segundo["baralho"]["cards"][0]["clues"][1], "segunda correção")

    def test_novo_rascunho_nao_colide_com_id_de_qualquer_origem(self):
        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.criar_rascunho(
                "Demo Evolução", "Especiais", "⭐", "ESPECIAIS"
            )
        self.assertEqual(contexto.exception.codigo, "id_existente")


class TesteDeAprovacao(unittest.TestCase):
    def test_aprovacao_deixa_de_valer_quando_o_candidato_muda(self):
        registro = {"chave": "asset:x", "baralho": {"id": "x"}}
        rascunhos.marcar_aprovacao(
            registro, "baralho", "hash-do-candidato", "hash-validacao", "ok"
        )
        self.assertTrue(
            rascunhos.aprovacao_valida(
                registro, "baralho", "hash-do-candidato", "hash-validacao"
            )
        )
        self.assertFalse(
            rascunhos.aprovacao_valida(
                registro, "baralho", "outro-hash", "hash-validacao"
            )
        )

    def test_aprovacao_de_um_alvo_nao_vale_para_outro(self):
        registro = {"chave": "catalogo:x", "baralho": {"id": "x"}}
        rascunhos.marcar_aprovacao(registro, "baralho", "hash", "validacao", "ok")
        self.assertFalse(
            rascunhos.aprovacao_valida(registro, "catalogo", "hash", "validacao")
        )

    def test_impressao_muda_com_qualquer_edicao(self):
        primeiro = rascunhos.impressao_do_candidato({"id": "x", "nome": "A"})
        segundo = rascunhos.impressao_do_candidato({"id": "x", "nome": "B"})
        self.assertNotEqual(primeiro, segundo)


if __name__ == "__main__":
    unittest.main()
