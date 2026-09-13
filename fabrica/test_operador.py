import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from operador import preparar


class OperadorTest(unittest.TestCase):
    def test_configuracao_privada_corresponde_ao_token_sem_sobrescrever(self):
        with tempfile.TemporaryDirectory() as temp:
            p = Path(temp) / "privado"
            preparar(p, "https://gerador.example")
            url, token = (p / "conexao.txt").read_text().split("|")
            self.assertEqual(url, "https://gerador.example")
            self.assertGreaterEqual(len(token), 43)
            clientes = json.loads((p / "clientes.json").read_text())
            self.assertEqual(clientes[hashlib.sha256(token.encode()).hexdigest()], "pessoal")
            with self.assertRaises(ValueError):
                preparar(p, "https://outro.example")
            self.assertEqual((p / "conexao.txt").read_text(), url + "|" + token)

    def test_enderecos_inseguros_nao_criam_credenciais(self):
        with tempfile.TemporaryDirectory() as temp:
            for url in ["http://pc", "https://usuario:senha@pc", "https://pc/?token=x", "https://pc/#x", "https://"]:
                with self.assertRaises(ValueError):
                    preparar(Path(temp) / "privado", url)
            self.assertFalse((Path(temp) / "privado").exists())
