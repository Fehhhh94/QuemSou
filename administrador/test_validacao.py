"""Validacao: comandos, arquivos temporarios, estados e concorrencia.

O subprocesso do Gradle e SIMULADO em toda esta suite. Nenhuma regra editorial
e exercida aqui; isso so acontece quando o Gradle real roda.
"""
import json
import subprocess
import tempfile
import threading
import unittest
from pathlib import Path

import aplicacao
import validacao
from apoio_de_teste import montar_ambiente, validador_simulado
from servico import Central
from validacao import Validacoes


class BaseDeValidacao(unittest.TestCase):
    def setUp(self):
        self.temporario = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporario.cleanup)
        self.config = montar_ambiente(Path(self.temporario.name))

    def plano(self, chave, alteracoes=None, executor=None):
        central = Central(self.config, Validacoes(self.config, executar=executor or validador_simulado()))
        revisao = central.abrir(chave)["revisao"]
        central.salvar_rascunho(
            chave, alteracoes or {"nome": "Alterado"}, revisao
        )
        registro = central.rascunhos.ler(chave)
        return central, aplicacao.montar_plano(self.config, registro)


class TesteDeComandos(BaseDeValidacao):
    def test_comando_de_baralho_usa_a_tarefa_do_app(self):
        comando = validacao.comando_de_baralho(self.config, Path("/tmp/candidato.json"))
        self.assertTrue(comando[0].endswith("gradlew.bat"))
        self.assertEqual(comando[1], "validarBaralho")
        self.assertTrue(comando[2].startswith("-Parquivo="))
        self.assertIn("--offline", comando)

    def test_comando_de_catalogo_usa_a_tarefa_do_app(self):
        comando = validacao.comando_de_catalogo(self.config, Path("/tmp/staging"))
        self.assertEqual(comando[1], "validarCatalogo")
        self.assertTrue(comando[2].startswith("-Ppasta="))

    def test_recusa_caminho_com_caractere_de_shell(self):
        for perigoso in ("C:/tmp/a&b", "C:/tmp/a|b", 'C:/tmp/a"b', "C:/tmp/a\nb", "C:/tmp/a%b"):
            with self.subTest(perigoso=perigoso):
                with self.assertRaises(validacao.FalhaDaValidacao):
                    validacao.argumento_seguro(perigoso)

    def test_ambiente_nao_perde_o_ambiente_base(self):
        ambiente = validacao.ambiente_do_gradle({"MARCADOR": "1"})
        self.assertEqual(ambiente["MARCADOR"], "1")


class TesteDeArquivosTemporarios(BaseDeValidacao):
    def test_candidato_vai_para_o_diretorio_privado(self):
        _, plano = self.plano("asset:demo-evolucao")
        pasta = self.config.pasta_temporaria / "teste"
        arquivo, staging = validacao.preparar_arquivos(self.config, plano, pasta)
        self.assertIsNone(staging)
        dados = json.loads(arquivo.read_text(encoding="utf-8"))
        self.assertEqual(dados["versao"], plano.candidato["versao"])
        self.assertTrue(str(arquivo).startswith(str(self.config.diretorio_privado)))

    def test_staging_do_catalogo_tem_indice_e_os_demais_baralhos(self):
        _, plano = self.plano("catalogo:demo-catalogo")
        pasta = self.config.pasta_temporaria / "teste-catalogo"
        _, staging = validacao.preparar_arquivos(self.config, plano, pasta)
        self.assertTrue((staging / "indice.json").is_file())
        self.assertTrue((staging / "baralhos" / "demo-orfao.json").is_file())
        candidato = json.loads(
            (staging / "baralhos" / "demo-catalogo.json").read_text(encoding="utf-8")
        )
        self.assertEqual(candidato["nome"], "Alterado")

    def test_staging_nao_toca_o_catalogo_real(self):
        antes = (self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json").read_text(
            encoding="utf-8"
        )
        _, plano = self.plano("catalogo:demo-catalogo")
        validacao.preparar_arquivos(self.config, plano, self.config.pasta_temporaria / "t2")
        depois = (self.config.pasta_de_baralhos_do_catalogo / "demo-catalogo.json").read_text(
            encoding="utf-8"
        )
        self.assertEqual(antes, depois)

    def test_staging_recusa_link_para_fora_do_catalogo(self):
        _, plano = self.plano("catalogo:demo-catalogo")
        externo = Path(self.temporario.name) / "externo.json"
        externo.write_text("{}", encoding="utf-8")
        link = self.config.pasta_de_baralhos_do_catalogo / "escape.json"
        try:
            link.symlink_to(externo)
        except OSError as erro:
            self.skipTest("Links simbólicos indisponíveis nesta máquina: {}".format(erro))
        with self.assertRaises(validacao.FalhaDaValidacao) as contexto:
            validacao.preparar_arquivos(
                self.config, plano, self.config.pasta_temporaria / "com-link"
            )
        self.assertEqual(contexto.exception.codigo, "caminho_inseguro")


class TesteDeEstados(BaseDeValidacao):
    def _rodar(self, executor, chave="asset:demo-evolucao"):
        central, plano = self.plano(chave, executor=executor)
        trabalho = central.validacoes.iniciar(plano, chave)
        return central.validacoes.aguardar(trabalho["id"])

    def test_saida_zero_aprova(self):
        resultado = self._rodar(validador_simulado(0))
        self.assertEqual(resultado["estado"], "aprovado")
        self.assertTrue(resultado["simulado"])

    def test_saida_um_reprova_e_mostra_as_violacoes(self):
        saida = (
            "✗ 2 violação(ões) encontrada(s):\n"
            "  - card 'x': dica vazia.\n"
            "  - card 'y': erro.\nBaralho reprovado."
        )
        resultado = self._rodar(validador_simulado(1, saida))
        self.assertEqual(resultado["estado"], "reprovado")
        self.assertIn("card 'x': dica vazia.", resultado["mensagens"])

    def test_saida_um_de_ferramenta_nao_e_chamada_de_reprovacao_editorial(self):
        resultado = self._rodar(
            validador_simulado(1, "Exception in thread main: acesso negado")
        )
        self.assertEqual(resultado["estado"], "falha")

    def test_saida_dois_e_falha_de_ferramenta(self):
        resultado = self._rodar(validador_simulado(2, "Uso incorreto."))
        self.assertEqual(resultado["estado"], "falha")

    def test_gradlew_ausente_vira_falha_legivel(self):
        def sem_gradle(config, comando):
            return None, "O gradlew.bat não foi encontrado."

        resultado = self._rodar(sem_gradle)
        self.assertEqual(resultado["estado"], "falha")

    def test_tempo_esgotado(self):
        def estourar(config, comando):
            raise subprocess.TimeoutExpired(comando, config.tempo_limite_da_validacao)

        resultado = self._rodar(estourar)
        self.assertEqual(resultado["estado"], "tempo_esgotado")

    def test_catalogo_roda_as_duas_tarefas(self):
        executor = validador_simulado(0)
        self._rodar(executor, chave="catalogo:demo-catalogo")
        tarefas = [comando[1] for comando in executor.chamadas]
        self.assertEqual(tarefas, ["validarBaralho", "validarCatalogo"])

    def test_limpa_os_arquivos_temporarios(self):
        self._rodar(validador_simulado(0))
        self.assertEqual(list(self.config.pasta_temporaria.glob("validacao-*")), [])


class TesteDeConcorrencia(BaseDeValidacao):
    def test_um_gradle_por_vez_sem_travar_o_resto(self):
        simultaneos = []
        maximo = []
        trava = threading.Lock()
        liberar = threading.Event()

        def executor(config, comando):
            with trava:
                simultaneos.append(1)
                maximo.append(len(simultaneos))
            liberar.wait(5)
            with trava:
                simultaneos.pop()
            return 0, "ok"

        central, plano = self.plano("asset:demo-evolucao", executor=executor)
        primeiro = central.validacoes.iniciar(plano, "asset:demo-evolucao")
        segundo = central.validacoes.iniciar(plano, "asset:demo-evolucao")
        # O painel continua respondendo enquanto o Gradle simulado esta preso.
        self.assertIsNotNone(central.inventario())
        liberar.set()
        central.validacoes.aguardar(primeiro["id"])
        central.validacoes.aguardar(segundo["id"])
        self.assertEqual(max(maximo), 1)
        self.assertEqual(central.validacoes.estado(primeiro["id"])["estado"], "aprovado")
        self.assertEqual(central.validacoes.estado(segundo["id"])["estado"], "aprovado")


if __name__ == "__main__":
    unittest.main()
