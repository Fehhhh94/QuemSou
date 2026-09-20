"""Aplicar na origem local: versao, indice, conflito, aprovacao e rollback.

Tudo acontece em fixtures temporarias. A validacao usada aqui e SIMULADA
(ver apoio_de_teste.validador_simulado): o que se prova e o fluxo do painel.
"""
import json
import tempfile
import unittest
from pathlib import Path

import aplicacao
from apoio_de_teste import montar_ambiente, validador_simulado
from servico import Central, FalhaDoServico
from validacao import Validacoes


class BaseDeAplicacao(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))
        self.executor = validador_simulado()
        self.central = Central(self.config, Validacoes(self.config, executar=self.executor))

    def asset(self):
        return json.loads(self.config.arquivo_do_asset.read_text(encoding="utf-8"))

    def indice(self):
        return json.loads(self.config.arquivo_do_indice.read_text(encoding="utf-8"))

    def arquivo_do_catalogo(self, identificador):
        caminho = self.config.pasta_de_baralhos_do_catalogo / "{}.json".format(identificador)
        return json.loads(caminho.read_text(encoding="utf-8"))

    def preparar_e_validar(self, chave, alteracoes):
        salvo = self.salvar(chave, alteracoes)
        trabalho = self.central.validar(chave, salvo["revisao"])
        self.central.validacoes.aguardar(trabalho["id"])
        return self.central.estado_da_validacao(trabalho["id"])

    def salvar(self, chave, alteracoes):
        revisao = self.central.abrir(chave)["revisao"]
        return self.central.salvar_rascunho(chave, alteracoes, revisao)

    def aplicar(self, chave, confirmacao=True):
        revisao = self.central.abrir(chave)["revisao"]
        return self.central.aplicar(chave, confirmacao, revisao)


class TesteDePlano(BaseDeAplicacao):
    def test_candidato_ja_vem_com_a_versao_incrementada(self):
        self.salvar("asset:demo-evolucao", {"nome": "Nome novo"})
        registro = self.central.rascunhos.ler("asset:demo-evolucao")
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertEqual(registro["baralho"]["versao"], 1)
        self.assertEqual(plano.candidato["versao"], 2)
        self.assertEqual(plano.envelope["version"], 4)

    def test_rascunho_igual_a_origem_nao_e_aplicavel(self):
        registro, _ = self.central._registro_da_origem("asset:demo-evolucao")
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertTrue(plano.sem_alteracao)
        self.assertFalse(plano.aplicavel)

    def test_plano_de_publicacao_preserva_a_versao_aplicada(self):
        registro, _ = self.central._registro_da_origem("asset:demo-evolucao")
        plano = aplicacao.montar_plano_de_publicacao(self.config, registro)
        self.assertEqual(plano.alvo_principal, aplicacao.ALVO_FIRESTORE)
        self.assertEqual(plano.candidato["versao"], registro["baralho"]["versao"])
        self.assertTrue(plano.sem_alteracao)
        self.assertFalse(plano.conflito)

    def test_estado_finalizado_legado_gera_plano_atualizavel(self):
        self.salvar("asset:demo-final", {"nome": "Demo atualizada"})
        registro = self.central.rascunhos.ler("asset:demo-final")
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertTrue(plano.aplicavel)
        self.assertEqual(plano.candidato["nome"], "Demo atualizada")

    def test_rascunho_novo_nao_e_aplicavel(self):
        criado = self.central.criar_rascunho("Tema Piloto", "Especiais", "⭐", "ESPECIAIS")
        registro = self.central.rascunhos.ler(criado["chave"])
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertFalse(plano.aplicavel)
        self.assertIn("integração deliberada", plano.motivo_de_nao_aplicavel)


class TesteDeAplicacaoNoAsset(BaseDeAplicacao):
    def test_aplica_incrementando_baralho_e_envelope(self):
        outros_antes = [b for b in self.asset()["baralhos"] if b["id"] != "demo-evolucao"]
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Demo — Revisada"})
        relatorio = self.aplicar("asset:demo-evolucao")
        envelope = self.asset()
        alvo = next(b for b in envelope["baralhos"] if b["id"] == "demo-evolucao")
        self.assertEqual(envelope["version"], 4)
        self.assertEqual(alvo["versao"], 2)
        self.assertEqual(alvo["nome"], "Demo — Revisada")
        self.assertEqual(relatorio["versaoAnterior"], 1)
        self.assertEqual(relatorio["versaoNova"], 2)

    def test_nao_altera_os_outros_baralhos(self):
        antes = [b for b in self.asset()["baralhos"] if b["id"] != "demo-evolucao"]
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Demo — Revisada"})
        self.aplicar("asset:demo-evolucao")
        depois = [b for b in self.asset()["baralhos"] if b["id"] != "demo-evolucao"]
        self.assertEqual(antes, depois)

    def test_avisa_que_distribuir_exige_publicar_no_firestore(self):
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Demo — Revisada"})
        relatorio = self.aplicar("asset:demo-evolucao")
        self.assertTrue(any("Firestore" in aviso for aviso in relatorio["avisos"]))
        self.assertTrue(any("fora do Git" in aviso for aviso in relatorio["avisos"]))

    def test_backup_guarda_o_arquivo_anterior(self):
        antes = self.config.arquivo_do_asset.read_text(encoding="utf-8")
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Demo — Revisada"})
        relatorio = self.aplicar("asset:demo-evolucao")
        copia = Path(relatorio["backup"]) / ("asset-" + self.config.arquivo_do_asset.name)
        self.assertEqual(copia.read_text(encoding="utf-8"), antes)

    def test_ids_de_card_e_banco_sobrevivem_a_aplicacao(self):
        antes = next(b for b in self.asset()["baralhos"] if b["id"] == "demo-evolucao")
        ids_antes = [c["id"] for c in antes["cards"]]
        fatos_antes = [f["id"] for f in antes["cards"][0]["bancoDeDicas"]]
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Demo — Revisada"})
        self.aplicar("asset:demo-evolucao")
        depois = next(b for b in self.asset()["baralhos"] if b["id"] == "demo-evolucao")
        self.assertEqual([c["id"] for c in depois["cards"]], ids_antes)
        self.assertEqual([f["id"] for f in depois["cards"][0]["bancoDeDicas"]], fatos_antes)
        self.assertEqual(depois["cards"][0]["campoDesconhecido"], "precisa sobreviver")


class TesteDeAplicacaoNoCatalogo(BaseDeAplicacao):
    def test_atualiza_arquivo_e_metadados_do_indice(self):
        self.preparar_e_validar("catalogo:demo-catalogo", {"nome": "Catálogo — Revisado"})
        self.aplicar("catalogo:demo-catalogo")
        arquivo = self.arquivo_do_catalogo("demo-catalogo")
        entrada = next(e for e in self.indice()["baralhos"] if e["id"] == "demo-catalogo")
        self.assertEqual(arquivo["versao"], 3)
        self.assertEqual(entrada["versao"], 3)
        self.assertEqual(entrada["nome"], "Catálogo — Revisado")
        self.assertEqual(entrada["quantidadeDeCards"], len(arquivo["cards"]))
        self.assertGreater(entrada["tamanhoEmBytes"], 100)

    def test_nao_mexe_nas_outras_entradas_do_indice(self):
        antes = [e for e in self.indice()["baralhos"] if e["id"] != "demo-catalogo"]
        self.preparar_e_validar("catalogo:demo-catalogo", {"nome": "Catálogo — Revisado"})
        self.aplicar("catalogo:demo-catalogo")
        depois = [e for e in self.indice()["baralhos"] if e["id"] != "demo-catalogo"]
        self.assertEqual(antes, depois)

    def test_avisa_que_publicar_continua_manual(self):
        self.preparar_e_validar("catalogo:demo-catalogo", {"nome": "Catálogo — Revisado"})
        relatorio = self.aplicar("catalogo:demo-catalogo")
        self.assertTrue(any("ação separada" in aviso for aviso in relatorio["avisos"]))

    def test_backup_do_catalogo_usa_nomes_distintos(self):
        self.preparar_e_validar(
            "catalogo:demo-catalogo", {"nome": "Catálogo — Revisado"}
        )
        relatorio = self.aplicar("catalogo:demo-catalogo")
        nomes = {item.name for item in Path(relatorio["backup"]).iterdir()}
        self.assertEqual(
            nomes, {"baralho-demo-catalogo.json", "indice-indice.json"}
        )

    def test_baralho_fora_do_indice_nao_e_inserido_automaticamente(self):
        self.salvar("catalogo:demo-orfao", {"nome": "Órfão revisado"})
        registro = self.central.rascunhos.ler("catalogo:demo-orfao")
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertFalse(plano.aplicavel)
        self.assertIn("não insere itens", plano.motivo_de_nao_aplicavel)


class TesteDeBarreiras(BaseDeAplicacao):
    def test_origem_sem_edicao_pode_ser_validada_para_publicacao(self):
        chave = "asset:demo-evolucao"
        aberto = self.central.abrir(chave)
        trabalho = self.central.validar(chave, aberto["revisao"])
        self.central.validacoes.aguardar(trabalho["id"])
        resultado = self.central.estado_da_validacao(trabalho["id"])
        depois = self.central.abrir(chave)
        registro = self.central.rascunhos.ler(chave)

        self.assertEqual(resultado["estado"], "aprovado")
        self.assertEqual(resultado["alvos"], [aplicacao.ALVO_FIRESTORE])
        self.assertTrue(depois["plano"]["semAlteracao"])
        self.assertTrue(depois["aprovado"])
        self.assertEqual(registro["aprovacao"]["alvo"], aplicacao.ALVO_FIRESTORE)
        self.assertEqual(
            registro["aprovacao"]["hashDoCandidato"],
            aplicacao.montar_plano_de_publicacao(self.config, registro).hash_do_candidato,
        )

        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.aplicar(chave, True, depois["revisao"])
        self.assertEqual(contexto.exception.codigo, "sem_validacao")

    def test_primeiro_salvamento_recusa_revisao_de_outra_aba(self):
        primeira = self.central.abrir("asset:demo-evolucao")["revisao"]
        segunda = self.central.abrir("asset:demo-evolucao")["revisao"]
        self.central.salvar_rascunho(
            "asset:demo-evolucao", {"nome": "Primeira aba"}, primeira
        )
        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.salvar_rascunho(
                "asset:demo-evolucao", {"nome": "Segunda aba"}, segunda
            )
        self.assertEqual(contexto.exception.codigo, "revisao_obsoleta")

    def test_primeiro_salvamento_recusa_indice_alterado_depois_de_abrir(self):
        revisao = self.central.abrir("catalogo:demo-catalogo")["revisao"]
        indice = self.indice()
        indice["marcadorExterno"] = True
        self.config.arquivo_do_indice.write_text(
            json.dumps(indice, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.salvar_rascunho(
                "catalogo:demo-catalogo", {"nome": "Não deve salvar"}, revisao
            )
        self.assertIn(contexto.exception.codigo, ("revisao_obsoleta", "conflito_externo"))

    def test_sem_validacao_nao_aplica(self):
        self.salvar("asset:demo-evolucao", {"nome": "Sem validar"})
        with self.assertRaises(FalhaDoServico) as contexto:
            self.aplicar("asset:demo-evolucao")
        self.assertEqual(contexto.exception.codigo, "sem_validacao")

    def test_editar_depois_de_validar_invalida_a_aprovacao(self):
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Validado"})
        self.salvar("asset:demo-evolucao", {"nome": "Editado depois"})
        with self.assertRaises(FalhaDoServico) as contexto:
            self.aplicar("asset:demo-evolucao")
        self.assertEqual(contexto.exception.codigo, "sem_validacao")

    def test_sem_confirmacao_nao_aplica(self):
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Validado"})
        with self.assertRaises(FalhaDoServico) as contexto:
            self.aplicar("asset:demo-evolucao", False)
        self.assertEqual(contexto.exception.codigo, "sem_confirmacao")

    def test_validacao_reprovada_nao_libera_aplicacao(self):
        central = Central(
            self.config, Validacoes(self.config, executar=validador_simulado(1, "  - violação"))
        )
        revisao = central.abrir("asset:demo-evolucao")["revisao"]
        salvo = central.salvar_rascunho(
            "asset:demo-evolucao", {"nome": "Reprovado"}, revisao
        )
        trabalho = central.validar("asset:demo-evolucao", salvo["revisao"])
        central.validacoes.aguardar(trabalho["id"])
        self.assertEqual(central.estado_da_validacao(trabalho["id"])["estado"], "reprovado")
        with self.assertRaises(FalhaDoServico):
            central.aplicar(
                "asset:demo-evolucao",
                True,
                central.abrir("asset:demo-evolucao")["revisao"],
            )

    def test_mudanca_externa_apos_validar_bloqueia(self):
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Validado"})
        envelope = self.asset()
        envelope["baralhos"][0]["nome"] = "Mexido por fora"
        self.config.arquivo_do_asset.write_text(
            json.dumps(envelope, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        with self.assertRaises((FalhaDoServico, aplicacao.FalhaDaAplicacao)):
            self.aplicar("asset:demo-evolucao")

    def test_mudanca_do_indice_apos_validar_bloqueia(self):
        self.preparar_e_validar(
            "catalogo:demo-catalogo", {"nome": "Catálogo validado"}
        )
        indice = self.indice()
        indice["marcadorExterno"] = True
        self.config.arquivo_do_indice.write_text(
            json.dumps(indice, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        with self.assertRaises(FalhaDoServico) as contexto:
            self.aplicar("catalogo:demo-catalogo")
        self.assertEqual(contexto.exception.codigo, "conflito_externo")

    def test_conflito_externo_aparece_no_plano(self):
        self.salvar("asset:demo-evolucao", {"nome": "Rascunho"})
        envelope = self.asset()
        envelope["version"] = 99
        self.config.arquivo_do_asset.write_text(
            json.dumps(envelope, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        registro = self.central.rascunhos.ler("asset:demo-evolucao")
        plano = aplicacao.montar_plano(self.config, registro)
        self.assertTrue(plano.conflito)
        self.assertFalse(plano.aplicavel)


class TesteDeRollback(BaseDeAplicacao):
    def test_temporario_preexistente_nao_e_sobrescrito(self):
        parcial = self.config.arquivo_do_asset.with_name("cards.json.parcial")
        parcial.write_text("preservar", encoding="utf-8")
        self.preparar_e_validar("asset:demo-evolucao", {"nome": "Nome novo"})
        self.aplicar("asset:demo-evolucao")
        self.assertEqual(parcial.read_text(encoding="utf-8"), "preservar")

    def test_falha_na_segunda_gravacao_restaura_as_duas(self):
        """Rollback SIMULADO: a gravacao do indice falha de proposito."""
        arquivo_antes = (
            self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json"
        ).read_text(encoding="utf-8")
        indice_antes = self.config.arquivo_do_indice.read_text(encoding="utf-8")
        self.salvar("catalogo:demo-catalogo", {"nome": "Vai falhar"})
        registro = self.central.rascunhos.ler("catalogo:demo-catalogo")
        plano = aplicacao.montar_plano(self.config, registro)

        gravacoes = []

        def gravar_com_falha(caminho, texto):
            gravacoes.append(caminho)
            if len(gravacoes) == 1:
                caminho.write_text(texto, encoding="utf-8", newline="\n")
                return
            raise OSError("falha simulada de disco")

        with self.assertRaises(aplicacao.FalhaDaAplicacao) as contexto:
            aplicacao.aplicar(self.config, registro, plano, gravar=gravar_com_falha)
        self.assertEqual(contexto.exception.codigo, "gravacao_falhou")
        self.assertEqual(
            (self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json").read_text(
                encoding="utf-8"
            ),
            arquivo_antes,
        )
        self.assertEqual(
            self.config.arquivo_do_indice.read_text(encoding="utf-8"), indice_antes
        )


class TesteDeRemocaoDeBaralho(BaseDeAplicacao):
    def test_remove_do_asset_incrementa_envelope_e_preserva_vizinhos(self):
        antes = [b for b in self.asset()["baralhos"] if b["id"] != "demo-evolucao"]
        aberto = self.central.abrir("asset:demo-evolucao")
        relatorio = self.central.remover_baralho(
            "asset:demo-evolucao", "demo-evolucao", aberto["revisao"]
        )
        envelope = self.asset()
        self.assertEqual(envelope["version"], 4)
        self.assertEqual(envelope["baralhos"], antes)
        backup = Path(relatorio["backup"]) / "asset-cards.json"
        self.assertTrue(backup.is_file())
        self.assertIn("demo-evolucao", backup.read_text(encoding="utf-8"))

    def test_remove_do_catalogo_apaga_arquivo_e_entrada_com_backup(self):
        indice_antes = [e for e in self.indice()["baralhos"] if e["id"] != "demo-catalogo"]
        aberto = self.central.abrir("catalogo:demo-catalogo")
        relatorio = self.central.remover_baralho(
            "catalogo:demo-catalogo", "demo-catalogo", aberto["revisao"]
        )
        caminho = self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json"
        self.assertFalse(caminho.exists())
        self.assertEqual(self.indice()["baralhos"], indice_antes)
        nomes = {item.name for item in Path(relatorio["backup"]).iterdir()}
        self.assertEqual(
            nomes, {"baralho-demo-catalogo.json", "indice-indice.json"}
        )

    def test_confirmacao_precisa_ser_o_id_exato(self):
        antes = self.config.arquivo_do_asset.read_text(encoding="utf-8")
        aberto = self.central.abrir("asset:demo-evolucao")
        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.remover_baralho(
                "asset:demo-evolucao", "DEMO-EVOLUCAO", aberto["revisao"]
            )
        self.assertEqual(contexto.exception.codigo, "confirmacao_incorreta")
        self.assertEqual(self.config.arquivo_do_asset.read_text(encoding="utf-8"), antes)

    def test_mudanca_externa_bloqueia_remocao(self):
        aberto = self.central.abrir("asset:demo-evolucao")
        envelope = self.asset()
        envelope["marcadorExterno"] = True
        self.config.arquivo_do_asset.write_text(
            json.dumps(envelope, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        with self.assertRaises(FalhaDoServico) as contexto:
            self.central.remover_baralho(
                "asset:demo-evolucao", "demo-evolucao", aberto["revisao"]
            )
        self.assertEqual(contexto.exception.codigo, "revisao_obsoleta")

    def test_falha_ao_apagar_catalogo_restaura_indice_e_arquivo(self):
        arquivo = self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json"
        arquivo_antes = arquivo.read_text(encoding="utf-8")
        indice_antes = self.config.arquivo_do_indice.read_text(encoding="utf-8")
        registro, _ = self.central._registro_da_origem("catalogo:demo-catalogo")

        def falhar_ao_apagar(_):
            raise OSError("falha simulada")

        with self.assertRaises(aplicacao.FalhaDaAplicacao) as contexto:
            aplicacao.remover_baralho(
                self.config, registro, apagar=falhar_ao_apagar
            )
        self.assertEqual(contexto.exception.codigo, "remocao_falhou")
        self.assertEqual(arquivo.read_text(encoding="utf-8"), arquivo_antes)
        self.assertEqual(
            self.config.arquivo_do_indice.read_text(encoding="utf-8"), indice_antes
        )


if __name__ == "__main__":
    unittest.main()
