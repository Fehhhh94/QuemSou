"""Inventario: origens, duplicados, ausencias e recusa de travessia de caminho."""
import json
import tempfile
import unittest
from pathlib import Path

import fontes
from apoio_de_teste import montar_ambiente


class TesteDeInventario(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))
        self.inventario = fontes.inventariar(self.config)
        self.por_chave = {item["chave"]: item for item in self.inventario["baralhos"]}

    def test_lista_as_duas_origens(self):
        self.assertIn("asset:demo-final", self.por_chave)
        self.assertIn("asset:demo-evolucao", self.por_chave)
        self.assertIn("catalogo:demo-catalogo", self.por_chave)

    def test_arquivo_local_fora_do_indice_aparece(self):
        item = self.por_chave["catalogo:demo-orfao"]
        self.assertFalse(item["noIndice"])
        self.assertTrue(any("sem entrada no índice" in nota for nota in item["observacoes"]))

    def test_entrada_do_indice_sem_arquivo_aparece_e_nao_e_editavel(self):
        item = self.por_chave["catalogo:demo-ausente"]
        self.assertFalse(item["editavel"])
        self.assertTrue(any("não existe na cópia local" in nota for nota in item["observacoes"]))

    def test_mesmo_id_em_duas_origens_fica_marcado_sem_fundir(self):
        do_asset = self.por_chave["asset:demo-evolucao"]
        do_catalogo = self.por_chave["catalogo:demo-evolucao"]
        self.assertEqual(do_asset["id"], do_catalogo["id"])
        self.assertTrue(do_asset["duplicadoEm"])
        self.assertTrue(do_catalogo["duplicadoEm"])
        self.assertNotEqual(do_asset["chave"], do_catalogo["chave"])

    def test_baralho_tecnico_fica_marcado(self):
        self.assertTrue(self.por_chave["catalogo:baralho-de-teste-1"]["tecnico"])
        self.assertFalse(self.por_chave["catalogo:demo-catalogo"]["tecnico"])

    def test_finalizado_legado_e_editavel(self):
        self.assertFalse(self.por_chave["asset:demo-final"]["protegido"])
        self.assertTrue(self.por_chave["asset:demo-final"]["editavel"])

    def test_respostas_entram_na_busca(self):
        self.assertIn("Resposta Viva", self.por_chave["asset:demo-evolucao"]["respostas"])

    def test_rotulo_do_catalogo_nao_afirma_publicacao(self):
        rotulo = self.por_chave["catalogo:demo-catalogo"]["origemRotulo"].lower()
        self.assertIn("local", rotulo)
        self.assertNotIn("publicad", rotulo)

    def test_link_para_fora_da_pasta_e_ignorado(self):
        externo = Path(self.temporario.name) / "externo.json"
        externo.write_text(
            json.dumps({"id": "escape", "nome": "Escape", "cards": []}),
            encoding="utf-8",
        )
        link = self.config.pasta_de_baralhos_do_catalogo / "escape.json"
        try:
            link.symlink_to(externo)
        except OSError as erro:
            self.skipTest("Links simbólicos indisponíveis nesta máquina: {}".format(erro))
        inventario = fontes.inventariar(self.config)
        self.assertNotIn(
            "catalogo:escape", {item["chave"] for item in inventario["baralhos"]}
        )
        self.assertTrue(any("aponta para fora" in aviso for aviso in inventario["avisos"]))


class TesteDeCatalogoAusente(unittest.TestCase):
    def test_pasta_inexistente_vira_aviso_sem_derrubar_o_asset(self):
        with tempfile.TemporaryDirectory() as pasta:
            config = montar_ambiente(Path(pasta), com_catalogo=False)
            inventario = fontes.inventariar(config)
            chaves = [item["chave"] for item in inventario["baralhos"]]
            self.assertIn("asset:demo-final", chaves)
            self.assertTrue(inventario["avisos"])


class TesteDeChaves(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))

    def test_chave_valida(self):
        self.assertEqual(
            fontes.interpretar_chave("catalogo:demo-catalogo"), ("catalogo", "demo-catalogo")
        )

    def test_chave_recusa_travessia_e_lixo(self):
        for chave in (
            "catalogo:../../segredo",
            "catalogo:..",
            "catalogo:sub/dir",
            "catalogo:C:/Windows/system32",
            "arquivo:demo",
            "demo-catalogo",
            "catalogo:demo:extra",
            None,
            42,
        ):
            with self.subTest(chave=chave):
                with self.assertRaises(fontes.FalhaDaFonte):
                    fontes.interpretar_chave(chave)

    def test_caminho_do_catalogo_fica_dentro_da_pasta(self):
        caminho = fontes.caminho_do_baralho_do_catalogo(self.config, "demo-catalogo")
        self.assertEqual(
            caminho.parent, self.config.pasta_de_baralhos_do_catalogo.resolve()
        )

    def test_caminho_recusa_id_fora_do_padrao(self):
        for identificador in ("../indice", "a/b", "..", "MAIUSCULO", ""):
            with self.subTest(identificador=identificador):
                with self.assertRaises(fontes.FalhaDaFonte):
                    fontes.caminho_do_baralho_do_catalogo(self.config, identificador)


if __name__ == "__main__":
    unittest.main()
