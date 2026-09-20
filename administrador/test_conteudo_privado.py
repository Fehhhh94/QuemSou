"""Regressões da separação entre código e origens editoriais privadas."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from config import Configuracao, detectar_configuracao
from migrar_conteudo_local import migrar


class TesteDoConteudoPrivado(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.raiz = Path(self.tmp.name)
        self.app = self.raiz / "app"
        self.catalogo = self.raiz / "catalogo-legado"
        self.dados = self.raiz / "privado"
        self.asset = self.app / "app/src/main/assets/cards.json"
        self.asset.parent.mkdir(parents=True)
        self.asset.write_text('{"version":7,"baralhos":[]}\n', encoding="utf-8")
        (self.app / ".git").mkdir()
        (self.catalogo / ".git").mkdir(parents=True)
        (self.catalogo / "baralhos").mkdir()
        (self.catalogo / "indice.json").write_text('{"baralhos":[]}', encoding="utf-8")
        (self.catalogo / "baralhos/demo.json").write_text('{"id":"demo"}', encoding="utf-8")

    def test_copia_verificada_e_reexecucao_sem_alterar_originais(self):
        antes = self.asset.read_bytes()
        resultado = migrar(self.app, self.catalogo, self.dados)
        self.assertEqual(resultado["arquivos"], 3)
        self.assertEqual(migrar(self.app, self.catalogo, self.dados), resultado)
        self.assertEqual(self.asset.read_bytes(), antes)
        self.assertEqual((self.dados / "origens/embarcados-legado.json").read_bytes(), antes)
        manifesto = json.loads((Path(resultado["backup"]) / "manifesto.json").read_text())
        self.assertEqual(len(manifesto["arquivos"]), 3)

    def test_conflito_recusado_antes_de_copiar_qualquer_arquivo(self):
        destino = self.dados / "origens/catalogo/indice.json"
        destino.parent.mkdir(parents=True)
        destino.write_text("nao sobrescrever", encoding="utf-8")
        with self.assertRaises(ValueError):
            migrar(self.app, self.catalogo, self.dados)
        self.assertEqual(destino.read_text(), "nao sobrescrever")
        self.assertFalse((self.dados / "backups").exists())

    def test_destino_dentro_do_git_recusado_inclusive_worktree(self):
        worktree = self.raiz / "worktree"
        worktree.mkdir()
        (worktree / ".git").write_text("gitdir: qualquer", encoding="utf-8")
        for destino in (self.app / "privado", worktree / "privado"):
            with self.subTest(destino=destino), self.assertRaises(ValueError):
                migrar(self.app, self.catalogo, destino)

    def test_configuracao_padrao_nao_aponta_para_asset_nem_catalogo_git(self):
        with patch.dict("os.environ", {}, clear=True):
            config = detectar_configuracao({"app": str(self.app), "dados": str(self.dados)})
        self.assertEqual(config.arquivo_do_asset, self.dados / "origens/embarcados-legado.json")
        self.assertEqual(config.raiz_do_catalogo, self.dados / "origens/catalogo")
        with self.assertRaises(ValueError):
            Configuracao(self.app, self.catalogo, self.dados)

    def test_asset_do_apk_nao_muda_ao_editar_origem_privada(self):
        migrar(self.app, self.catalogo, self.dados)
        config = Configuracao(self.app, self.dados / "origens/catalogo", self.dados)
        antes = self.asset.read_bytes()
        config.arquivo_do_asset.write_text('{"version":8,"baralhos":[]}', encoding="utf-8")
        self.assertEqual(self.asset.read_bytes(), antes)
