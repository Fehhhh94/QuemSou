"""Regressões estáticas mínimas do cliente, complementadas pelo smoke real."""
import unittest
from pathlib import Path


class TesteDaInterface(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.javascript = (
            Path(__file__).resolve().parent / "estaticos" / "app.js"
        ).read_text(encoding="utf-8")

    def test_clique_nao_injeta_evento_como_argumento_da_acao(self):
        self.assertIn('addEventListener("click", () => acao())', self.javascript)

    def test_salvamento_adota_o_baralho_autoritativo_do_servidor(self):
        self.assertIn("estado.aberto.baralho = dados.baralho", self.javascript)

    def test_exportacao_usa_post_e_so_anuncia_depois_da_resposta(self):
        self.assertIn('fetch("/api/exportar", {', self.javascript)
        self.assertNotIn('window.location.href = "/api/exportar', self.javascript)

    def test_feedback_aparece_sob_a_dica_e_prepara_pedido_para_codex(self):
        self.assertIn("painelDeFeedbacks(card, indice, () => area.value)", self.javascript)
        self.assertIn("Copiar pedido para o Codex", self.javascript)
        self.assertIn("navigator.clipboard.writeText", self.javascript)

    def test_feedback_da_nuvem_entra_sem_importacao_manual(self):
        self.assertIn("accounts:signUp", self.javascript)
        self.assertIn("listarFeedbacksNoFirestore", self.javascript)
        self.assertIn("setInterval", self.javascript)
        self.assertIn("Firestore — automático", self.javascript)

    def test_remocao_exige_id_digitado_e_usa_endpoint_dedicado(self):
        self.assertIn("window.prompt", self.javascript)
        self.assertIn('enviar("/api/baralho/remover"', self.javascript)

    def test_publicacao_firestore_e_versionada_em_blocos_e_exige_validacao(self):
        self.assertIn("prepararPublicacaoNoFirestore", self.javascript)
        self.assertIn("atual.length >= 25", self.javascript)
        self.assertIn("bytes > 700000", self.javascript)
        self.assertIn("grupos.length > 250", self.javascript)
        self.assertIn('baralho.categoria === "ESPECIAIS" ? "PRIVADO" : "PUBLICO"', self.javascript)
        self.assertIn('setToServerValue: "REQUEST_TIME"', self.javascript)
        self.assertIn("!estado.aprovado", self.javascript)
        self.assertIn('dados.tipo !== "novo"', self.javascript)
        self.assertIn("estado.plano.semAlteracao", self.javascript)
        self.assertIn("Aplique primeiro as alterações na origem local", self.javascript)
        self.assertIn("mesma versão já existe com outro conteúdo", self.javascript)

    def test_retirada_da_nuvem_nao_apaga_as_versoes(self):
        self.assertIn("retirarBaralhoDoFirestore", self.javascript)
        self.assertIn('fieldPaths: ["publicado"]', self.javascript)
        self.assertNotIn("delete: { name:", self.javascript)


if __name__ == "__main__":
    unittest.main()
