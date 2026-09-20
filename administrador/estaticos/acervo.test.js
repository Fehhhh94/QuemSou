// Testes de lógica real (node:test), não apenas checagem de sintaxe.
// Rodar com: node --test administrador/estaticos/acervo.test.js
"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");

const acervo = require("./acervo.js");

test("normalizar remove acento e pontuação como o app e o painel Python", () => {
  assert.equal(acervo.normalizar("É Verde!"), "e verde");
  assert.equal(acervo.normalizar("  Pessoa Inventada  "), "pessoa inventada");
});

test("normalizar é Unicode-aware (letra/número, não só a-z0-9 ASCII)", () => {
  // "ß" não decompõe pelo NFD (não é letra+marca); um filtro ASCII a
  // apagaria, divergindo de \p{L} no Kotlin e isalnum() Unicode no Python.
  assert.equal(acervo.normalizar("Straße"), "straße");
  // Letras de outros alfabetos (aqui, cirílico) também precisam sobreviver.
  assert.equal(acervo.normalizar("Москва"), "москва");
  // Dígito Unicode fora de 0-9 ASCII continua sendo dígito (\p{N}).
  assert.equal(acervo.normalizar("card ١٢٣"), "card ١٢٣");
  // Pontuação e símbolos continuam virando espaço, como antes.
  assert.equal(acervo.normalizar("Olá, mundo!!"), "ola mundo");
});

test("aliasDeResposta usa o nome normalizado, igual a SelecionadorDeDicas.resposta()", () => {
  assert.equal(acervo.aliasDeResposta("Pessoa Inventada"), "pessoa inventada");
  assert.throws(() => acervo.aliasDeResposta("   "), { codigo: "resposta_sem_texto" });
});

test("aliasDeDica usa o prefixo legado:, igual a SelecionadorDeDicas.banco()", () => {
  const texto = "Uma pista fictícia com acentuação e pontuação.";
  assert.equal(acervo.aliasDeDica(texto), "legado:" + acervo.normalizar(texto));
});

test("card fictício legado mantém identidade e associação da décima dica", () => {
  const card = {id: "demo-028", answer: "Pessoa Inventada",
    clues: Array.from({length: 10}, (_, i) => "Fato sintético número " + i)};
  assert.equal(acervo.aliasDeResposta(card.answer), "pessoa inventada");
  assert.equal(acervo.aliasDeDica(card.clues[9]), "legado:" + acervo.normalizar(card.clues[9]));
});

test("escopoDoBaralho: ESPECIAIS é privado, os demais são públicos", () => {
  assert.equal(acervo.escopoDoBaralho({ categoria: "ESPECIAIS" }), acervo.ESCOPO_PRIVADO);
  assert.equal(acervo.escopoDoBaralho({ categoria: "PERSONAGEM_FILME" }), acervo.ESCOPO_PUBLICO);
});

test("validarTextoDaDica recusa vazio e texto longo demais", () => {
  assert.throws(() => acervo.validarTextoDaDica("   "), { codigo: "campo_invalido" });
  assert.throws(() => acervo.validarTextoDaDica("a".repeat(501)), { codigo: "campo_invalido" });
  assert.equal(acervo.validarTextoDaDica("  Texto ok  "), "Texto ok");
});

function dica(dicaId, texto, escopo, status) {
  return { dicaId, texto, escopo, status: status || acervo.STATUS_ATIVA };
}

test("dicasParaPublicacao nunca deixa PRIVADO vazar para PUBLICO", () => {
  const dicas = {
    d1: dica("d1", "Pública 1", acervo.ESCOPO_PUBLICO),
    d2: dica("d2", "Privada 1", acervo.ESCOPO_PRIVADO),
  };
  const publico = acervo.dicasParaPublicacao(dicas, acervo.ESCOPO_PUBLICO);
  assert.deepEqual(publico.map((d) => d.dicaId), ["d1"]);
  const privado = acervo.dicasParaPublicacao(dicas, acervo.ESCOPO_PRIVADO);
  assert.deepEqual(new Set(privado.map((d) => d.dicaId)), new Set(["d1", "d2"]));
});

test("dicasParaPublicacao ignora dicas removidas e deduplica texto normalizado", () => {
  const dicas = {
    d1: dica("d1", "É Verde!", acervo.ESCOPO_PUBLICO),
    d2: dica("d2", "e verde", acervo.ESCOPO_PUBLICO),
    d3: dica("d3", "Removida", acervo.ESCOPO_PUBLICO, acervo.STATUS_REMOVIDA),
  };
  const selecionadas = acervo.dicasParaPublicacao(dicas, acervo.ESCOPO_PUBLICO);
  assert.equal(selecionadas.length, 1);
});

test("prontaParaPublicar exige entre 10 e 500 dicas elegíveis", () => {
  const nove = {};
  for (let i = 0; i < 9; i += 1) nove["d" + i] = dica("d" + i, "Texto " + i, acervo.ESCOPO_PUBLICO);
  assert.equal(acervo.prontaParaPublicar(nove, acervo.ESCOPO_PUBLICO), false);
  nove.d9 = dica("d9", "Texto 9", acervo.ESCOPO_PUBLICO);
  assert.equal(acervo.prontaParaPublicar(nove, acervo.ESCOPO_PUBLICO), true);

  const quinhentas = {};
  for (let i = 0; i < 500; i += 1) {
    quinhentas["d" + i] = dica("d" + i, "Texto único " + i, acervo.ESCOPO_PUBLICO);
  }
  assert.equal(acervo.prontaParaPublicar(quinhentas, acervo.ESCOPO_PUBLICO), true);
  quinhentas.d500 = dica("d500", "Texto único 500", acervo.ESCOPO_PUBLICO);
  assert.equal(acervo.prontaParaPublicar(quinhentas, acervo.ESCOPO_PUBLICO), false);
});

test("filtrarDicas busca por texto normalizado ou por id", () => {
  const dicas = [
    dica("guit-01", "Toca guitarra desde criança.", acervo.ESCOPO_PUBLICO),
    dica("guit-02", "Nasceu no Texas.", acervo.ESCOPO_PUBLICO),
  ];
  assert.equal(acervo.filtrarDicas(dicas, "guitarra").length, 1);
  assert.equal(acervo.filtrarDicas(dicas, "texas").length, 1);
  assert.equal(acervo.filtrarDicas(dicas, "").length, 2);
});

test("paginar corta a lista em páginas para não gerar DOM massivo", () => {
  const lista = Array.from({ length: 120 }, (_, i) => dica("d" + i, "Texto " + i, acervo.ESCOPO_PUBLICO));
  const pagina1 = acervo.paginar(lista, 1, 50);
  assert.equal(pagina1.itens.length, 50);
  assert.equal(pagina1.totalDePaginas, 3);
  const pagina3 = acervo.paginar(lista, 3, 50);
  assert.equal(pagina3.itens.length, 20);
});

test("pedidoParaCodexDaResposta preserva id, texto e feedbacks sem chamar IA", () => {
  const resposta = { respostaId: "pessoa inventada", texto: "Pessoa Inventada", tipo: "PESSOA" };
  const alvo = dica("legado:abc", "Texto atual da dica.", acervo.ESCOPO_PUBLICO);
  const feedbacks = [{ listaNome: "Firestore — automático", voto: "FRACO", comentario: "Confuso", criadoEm: "2026-01-01" }];
  const pedido = acervo.pedidoParaCodexDaResposta(resposta, alvo, feedbacks);
  assert.match(pedido, /Id da dica: legado:abc/);
  assert.match(pedido, /Texto atual: Texto atual da dica\./);
  assert.match(pedido, /Confuso/);
  assert.doesNotMatch(pedido, /chamar|enviar automaticamente/i);
});

test("idDoDocumento/idLogicoDoDocumento são reversíveis, inclusive com barra", () => {
  assert.equal(acervo.idDoDocumento("ação / 100%"), "id_YcOnw6NvIC8gMTAwJQ");
  const ids = [".", "_.", "..", "_..", "%", "/", "__reservado__", "id_abc", "ação / 100%"];
  assert.equal(new Set(ids.map(acervo.idDoDocumento)).size, ids.length);
  ids.forEach(id => assert.equal(acervo.idLogicoDoDocumento(acervo.idDoDocumento(id)), id));
  const comBarra = "algo/com/barra";
  const codificado = acervo.idDoDocumento(comBarra);
  assert.ok(!codificado.includes("/"));
  assert.equal(acervo.idLogicoDoDocumento(codificado), comBarra);
  for (const reservado of [".", "..", "__reservado__"]) {
    const seguro = acervo.idDoDocumento(reservado);
    assert.notEqual(seguro, reservado);
    assert.equal(acervo.idLogicoDoDocumento(seguro), reservado);
  }
});

test("escopoPadraoParaNovaDica: privado quando a resposta já é majoritária/empatadamente privada", () => {
  assert.equal(acervo.escopoPadraoParaNovaDica({}), acervo.ESCOPO_PUBLICO);
  assert.equal(
    acervo.escopoPadraoParaNovaDica({ a: dica("a", "x", acervo.ESCOPO_PUBLICO) }),
    acervo.ESCOPO_PUBLICO
  );
  assert.equal(
    acervo.escopoPadraoParaNovaDica({
      a: dica("a", "x", acervo.ESCOPO_PRIVADO),
      b: dica("b", "y", acervo.ESCOPO_PUBLICO),
    }),
    acervo.ESCOPO_PRIVADO
  );
});

test("respostasSegurasParaMigrar exclui conflito e banco acima do limite", () => {
  const previa = {
    respostas: { a: {}, b: {}, c: {} },
    conflitos: [{ respostaId: "a", tipo: "x" }],
    bancosAcimaDoLimite: [{ respostaId: "b", quantidade: 501 }],
  };
  assert.deepEqual(acervo.respostasSegurasParaMigrar(previa), ["c"]);
});

test("prepararProjecaoDoBaralho monta banco/clues determinísticos por escopo", () => {
  const cards = [{ id: "c1", respostaId: "pessoa inventada" }, { id: "c2", respostaId: "sem-banco" }];
  const respostasRemotas = {
    "pessoa inventada": {
      dicas: {
        d3: dica("d3", "Terceira", acervo.ESCOPO_PUBLICO),
        d1: dica("d1", "Primeira", acervo.ESCOPO_PUBLICO),
        d2: dica("d2", "Segunda", acervo.ESCOPO_PRIVADO),
      },
    },
  };
  // Só 2 dicas públicas disponíveis: abaixo do mínimo de 10, então nenhuma
  // projeção é gerada para "pessoa inventada" nesta amostra pequena.
  const resultado = acervo.prepararProjecaoDoBaralho(cards, respostasRemotas, acervo.ESCOPO_PUBLICO);
  assert.equal(Object.keys(resultado.projecoes).length, 0);
  assert.equal(resultado.ignorados.length, 2);
  assert.equal(resultado.ignorados.find((i) => i.cardId === "c1").motivo, "banco_insuficiente");
  assert.equal(resultado.ignorados.find((i) => i.cardId === "c2").motivo, "sem_banco_remoto");
});

test("prepararProjecaoDoBaralho nunca inclui dica privada num baralho público", () => {
  const dicas = {};
  for (let i = 0; i < 9; i += 1) dicas["p" + i] = dica("p" + i, "Pública " + i, acervo.ESCOPO_PUBLICO);
  dicas.priv = dica("priv", "Privada única", acervo.ESCOPO_PRIVADO);
  const cards = [{ id: "c1", respostaId: "r" }];
  const resultado = acervo.prepararProjecaoDoBaralho(cards, { r: { dicas } }, acervo.ESCOPO_PUBLICO);
  // 9 públicas só: ainda abaixo do mínimo de 10, nenhuma projeção.
  assert.equal(Object.keys(resultado.projecoes).length, 0);
  dicas.p9 = dica("p9", "Pública 9", acervo.ESCOPO_PUBLICO);
  const resultado2 = acervo.prepararProjecaoDoBaralho(cards, { r: { dicas } }, acervo.ESCOPO_PUBLICO);
  const banco = resultado2.projecoes.c1.bancoDeDicas;
  assert.equal(banco.length, 10);
  assert.ok(!banco.some((fato) => fato.id === "priv"));
});

test("acrescentar/editar/desativar/reativar via edicao.py e acervo.js concordam no formato do id", () => {
  // acervo.js não gera id (isso é responsabilidade do servidor, que garante
  // unicidade global); este teste garante só que o contrato de status é o
  // mesmo dos dois lados.
  const original = dica("resposta-x-dabc123", "Texto original.", acervo.ESCOPO_PUBLICO);
  assert.equal(acervo.contarAtivas([original]), 1);
  const removida = { ...original, status: acervo.STATUS_REMOVIDA };
  assert.equal(acervo.contarAtivas([original, removida]), 1);
});
