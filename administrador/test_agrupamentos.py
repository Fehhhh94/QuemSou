"""Taxonomia ampla, sem dependência do conteúdo editorial privado."""
import unittest
from agrupamentos import listar_agrupamentos, colecao_do_agrupamento
from edicao import criar_baralho_de_rascunho, FalhaDaEdicao


class TesteDosAgrupamentos(unittest.TestCase):
    def test_seis_grupos_incluem_os_vazios(self):
        grupos = listar_agrupamentos()
        self.assertEqual(len(grupos), 6)
        self.assertEqual(len({g['id'] for g in grupos}), 6)
        self.assertEqual([g['nome'] for g in grupos], ['Cinema e TV', 'Música', 'Esportes', 'Conhecimentos Gerais', 'Lugares e Natureza', 'Especiais'])

    def test_listagem_nao_muta_cadastro(self):
        grupos = listar_agrupamentos()
        grupos[0]['nome'] = 'Alterado'
        self.assertEqual(listar_agrupamentos()[0]['nome'], 'Cinema e TV')

    def test_criacao_reutiliza_id_por_nome_ou_id(self):
        for grupo in listar_agrupamentos():
            for valor in (grupo['id'], grupo['nome']):
                with self.subTest(valor=valor):
                    deck = criar_baralho_de_rascunho('Piloto', valor, 'X', grupo['categoriaPadrao'])
                    self.assertEqual(deck['colecao'], colecao_do_agrupamento(grupo['id']))
                    self.assertEqual(deck['cards'], [])

    def test_grupo_arbitrario_nao_prolifera(self):
        with self.assertRaises(FalhaDaEdicao):
            criar_baralho_de_rascunho('Piloto', 'Uma franquia', 'X', 'PERSONAGEM_FILME')

    def test_especiais_nao_pode_ser_criado_publico(self):
        with self.assertRaises(FalhaDaEdicao):
            criar_baralho_de_rascunho('Piloto', 'especiais', 'X', 'PERSONAGEM_FILME')

    def test_categoria_privada_nao_vai_a_grupo_comum(self):
        with self.assertRaises(FalhaDaEdicao):
            criar_baralho_de_rascunho('Piloto', 'esportes', 'X', 'ESPECIAIS')
