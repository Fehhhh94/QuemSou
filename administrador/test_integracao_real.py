"""Integração opt-in com o Gradle real, sempre sobre uma cópia do catálogo.

Executar somente com as três variáveis abaixo. A suíte comum ignora este caso
para não tornar testes rápidos dependentes do Android/Gradle.
"""
import json
import copy
import os
import unittest
from pathlib import Path

from config import Configuracao
from servico import Central
from validacao import Validacoes
from fontes import normalizar


@unittest.skipUnless(
    os.environ.get("QUEMSOU_ADMIN_INTEGRACAO_REAL") == "1",
    "Integração Gradle real é opt-in e exige catálogo de staging.",
)
class TesteDeIntegracaoReal(unittest.TestCase):
    def test_projecao_legada_e_editorial_com_500_passam_gradle_real(self):
        app = Path(os.environ["QUEMSOU_ADMIN_APP_REAL"]).resolve()
        catalogo = Path(os.environ["QUEMSOU_ADMIN_CATALOGO_STAGING"]).resolve()
        self.assertNotEqual(catalogo, Path("C:/Dev/QuemSou-Baralhos").resolve())
        config = Configuracao(app, catalogo, Path(os.environ["QUEMSOU_ADMIN_DADOS_STAGING"]).resolve())
        central = Central(config, Validacoes(config))
        arquivo = config.pasta_de_baralhos_do_catalogo / "mundo-dos-bruxos-1.json"
        original = json.loads(arquivo.read_text(encoding="utf-8"))
        # Só no staging: um card editorial explícito e um legado na mesma origem.
        editorial = original["cards"][1]
        editorial["respostaId"] = "qa-editorial-estavel"
        editorial["bancoDeDicas"] = [{"id": "qa-f{}".format(i), "texto": t} for i,t in enumerate(editorial["clues"])]
        arquivo.write_text(json.dumps(original, ensure_ascii=False), encoding="utf-8")
        vizinhos = copy.deepcopy(original["cards"][2:])
        chave = "catalogo:mundo-dos-bruxos-1"
        aberto = central.abrir(chave)
        projecoes = {}
        for card, tamanho in ((original["cards"][0],500),(editorial,11)):
            banco = copy.deepcopy(card.get("bancoDeDicas") or [{"id":"legado:"+normalizar(t),"texto":t} for t in card["clues"]])
            banco += [{"id":"qa-novo-{}".format(i),"texto":"Pista complementar de teste número {}.".format(i)} for i in range(tamanho-len(banco))]
            for fato in banco:
                fato["escopo"]="PUBLICO"
            projecoes[card["id"]]={"bancoDeDicas":banco,"clues":[f["texto"] for f in banco[:10]]}
        salvo=central.projetar_do_acervo(chave,projecoes,aberto["revisao"])
        trabalho=central.validar(chave,salvo["revisao"])
        resultado=central.validacoes.aguardar(trabalho["id"],tempo=950)
        self.assertEqual(resultado["estado"],"aprovado",resultado)
        central.estado_da_validacao(trabalho["id"])
        central.aplicar(chave,True,salvo["revisao"])
        aplicado=central.abrir(chave)
        self.assertEqual(aplicado["baralho"]["cards"][2:],vizinhos)
        self.assertEqual(len(aplicado["baralho"]["cards"][0]["bancoDeDicas"]),500)
        self.assertEqual(aplicado["baralho"]["cards"][1]["respostaId"],"qa-editorial-estavel")
        # Validação separada do conteúdo aplicado para preparar a publicação,
        # sem enviar nenhum dado à nuvem.
        trabalho=central.validar(chave,aplicado["revisao"])
        resultado=central.validacoes.aguardar(trabalho["id"],tempo=950)
        self.assertEqual(resultado["estado"],"aprovado",resultado)

    def test_valida_e_aplica_somente_na_copia_do_catalogo(self):
        raiz_do_app = Path(os.environ["QUEMSOU_ADMIN_APP_REAL"]).resolve()
        catalogo = Path(os.environ["QUEMSOU_ADMIN_CATALOGO_STAGING"]).resolve()
        dados = Path(os.environ["QUEMSOU_ADMIN_DADOS_STAGING"]).resolve()
        self.assertNotEqual(catalogo, Path("C:/Dev/QuemSou-Baralhos").resolve())
        config = Configuracao(
            raiz_do_app=raiz_do_app,
            raiz_do_catalogo=catalogo,
            diretorio_privado=dados,
            tempo_limite_da_validacao=900,
        )
        config.preparar_diretorio_privado()
        central = Central(config, Validacoes(config))

        chave = "catalogo:mundo-dos-bruxos-1"
        aberto = central.abrir(chave)
        nome_original = aberto["baralho"]["nome"]
        versao_original = aberto["baralho"]["versao"]
        salvo = central.salvar_rascunho(
            chave, {"nome": nome_original + " — QA temporária"}, aberto["revisao"]
        )
        trabalho = central.validar(chave, salvo["revisao"])
        resultado = central.validacoes.aguardar(trabalho["id"], tempo=950)
        self.assertEqual(resultado["estado"], "aprovado", resultado)
        central.estado_da_validacao(trabalho["id"])
        relatorio = central.aplicar(chave, True, salvo["revisao"])

        arquivo = config.pasta_de_baralhos_do_catalogo / "mundo-dos-bruxos-1.json"
        gravado = json.loads(arquivo.read_text(encoding="utf-8"))
        indice = json.loads(config.arquivo_do_indice.read_text(encoding="utf-8"))
        entrada = next(item for item in indice["baralhos"] if item["id"] == gravado["id"])
        self.assertEqual(gravado["nome"], nome_original + " — QA temporária")
        self.assertEqual(gravado["versao"], versao_original + 1)
        self.assertEqual(entrada["versao"], gravado["versao"])
        self.assertTrue(Path(relatorio["backup"]).is_dir())


if __name__ == "__main__":
    unittest.main()
