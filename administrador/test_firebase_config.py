"""A configuração do Firebase servida ao painel é mínima e validada."""
import json
import tempfile
import unittest
from pathlib import Path

from apoio_de_teste import montar_ambiente
from firebase_config import configuracao_publica


class TesteDaConfiguracaoFirebase(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))

    def test_ausente_mantem_o_painel_local_funcionando(self):
        self.assertFalse(configuracao_publica(self.config)["configurado"])

    def test_expoe_so_o_necessario_do_cliente_android_correto(self):
        caminho = self.config.arquivo_google_services
        caminho.parent.mkdir(parents=True, exist_ok=True)
        caminho.write_text(
            json.dumps(
                {
                    "project_info": {"project_id": "borajogar-app", "storage_bucket": "nao-expor"},
                    "client": [
                        {
                            "client_info": {
                                "mobilesdk_app_id": "app-id-nao-necessario",
                                "android_client_info": {"package_name": "com.quemsou.app"},
                            },
                            "api_key": [{"current_key": "chave-publica-do-apk"}],
                        }
                    ],
                }
            ),
            encoding="utf-8",
        )
        resultado = configuracao_publica(self.config)
        self.assertEqual(
            resultado,
            {
                "configurado": True,
                "projectId": "borajogar-app",
                "apiKey": "chave-publica-do-apk",
                "databaseId": "(default)",
            },
        )
        self.assertNotIn("storage_bucket", resultado)
        self.assertNotIn("mobilesdk_app_id", resultado)


if __name__ == "__main__":
    unittest.main()
