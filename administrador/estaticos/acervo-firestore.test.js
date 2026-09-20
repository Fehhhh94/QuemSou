// Testes de lógica real com um TRANSPORTE FALSO que executa o MESMO código
// de acervo-firestore.js (não é busca por string). Simula o suficiente da
// API REST do Firestore (get, createDocument, :commit com precondição) para
// provar create-only idempotente, conflito de concorrência e o guardrail de
// tamanho de commit. Um emulador local real é complementar (opt-in, Codex).
//
// Rodar com: node --test administrador/estaticos/acervo-firestore.test.js
"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");

const acervoFirestore = require("./acervo-firestore.js");

const CONFIG = { projectId: "demo-projeto", databaseId: "(default)" };
const CREDENCIAL = { idToken: "token-falso" };

/** Fake mínimo da API REST do Firestore: get, createDocument (?documentId=)
 * e :commit (com currentDocument.updateTime/exists). Guarda documentos por
 * caminho relativo ("acervoEditorial/x/dicas/y").
 */
class FirestoreFalso {
  constructor() {
    this.documentos = new Map();
    this.sequencia = 0;
  }

  _novoUpdateTime() {
    this.sequencia += 1;
    return "2026-01-01T00:00:00." + String(this.sequencia).padStart(6, "0") + "Z";
  }

  _raiz() {
    return "projects/" + CONFIG.projectId + "/databases/" + CONFIG.databaseId + "/documents/";
  }

  _caminhoRelativo(nomeCompleto) {
    // Um servidor HTTP real decodifica o path da URL uma vez antes de rotear;
    // sem isso, o "%2F" que representa uma barra LITERAL no id (ver
    // idDoDocumento) chegaria como "%252F" e nunca bateria com o que
    // `_criar` guardou.
    const relativo = nomeCompleto.slice(nomeCompleto.indexOf(this._raiz()) + this._raiz().length);
    return decodeURIComponent(relativo);
  }

  get transporte() {
    return async (url, opcoes) => {
      const metodo = (opcoes && opcoes.method) || "GET";
      if (url.includes(":commit")) {
        return this._commit(JSON.parse(opcoes.body).writes);
      }
      if (metodo === "POST" && url.includes("?documentId=")) {
        return this._criar(url, JSON.parse(opcoes.body));
      }
      return this._ler(url);
    };
  }

  _respostaJson(status, corpo) {
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => corpo,
    };
  }

  _ler(url) {
    const semQuery = url.split("?")[0];
    const caminho = this._caminhoRelativo(semQuery);
    if (semQuery.endsWith("/dicas")) {
      // Listagem de subcoleção.
      const prefixo = caminho + "/";
      const documentos = [...this.documentos.entries()]
        .filter(([chave]) => chave.startsWith(prefixo) && !chave.slice(prefixo.length).includes("/"))
        .map(([chave, doc]) => ({
          name: this._raiz() + chave,
          fields: doc.fields,
          updateTime: doc.updateTime,
        }));
      return this._respostaJson(200, { documents: documentos });
    }
    const documento = this.documentos.get(caminho);
    if (!documento) {
      return this._respostaJson(404, { error: { status: "NOT_FOUND", message: "ausente" } });
    }
    return this._respostaJson(200, {
      name: this._raiz() + caminho,
      fields: documento.fields,
      updateTime: documento.updateTime,
    });
  }

  _criar(url, corpo) {
    const semQuery = url.split("?")[0];
    const parentPath = this._caminhoRelativo(semQuery);
    const idDocumento = decodeURIComponent(url.split("documentId=")[1]);
    const caminho = (parentPath ? parentPath + "/" : "") + idDocumento;
    if (this.documentos.has(caminho)) {
      return this._respostaJson(409, { error: { status: "ALREADY_EXISTS", message: "já existe" } });
    }
    const updateTime = this._novoUpdateTime();
    this.documentos.set(caminho, { fields: corpo.fields, updateTime });
    return this._respostaJson(200, { name: this._raiz() + caminho, fields: corpo.fields, updateTime });
  }

  _commit(writes) {
    // Verifica TODAS as precondições antes de aplicar qualquer escrita
    // (mesma semântica atômica do Firestore real).
    for (const write of writes) {
      const caminho = this._caminhoRelativo(write.update.name);
      const existente = this.documentos.get(caminho);
      if (write.currentDocument && "updateTime" in write.currentDocument) {
        if (!existente || existente.updateTime !== write.currentDocument.updateTime) {
          return this._respostaJson(400, {
            error: { status: "FAILED_PRECONDITION", message: "updateTime não confere" },
          });
        }
      }
      if (write.currentDocument && write.currentDocument.exists === false && existente) {
        return this._respostaJson(400, {
          error: { status: "ALREADY_EXISTS", message: "já existe" },
        });
      }
    }
    const respostas = writes.map((write) => {
      const caminho = this._caminhoRelativo(write.update.name);
      const updateTime = this._novoUpdateTime();
      this.documentos.set(caminho, { fields: write.update.fields, updateTime });
      return { updateTime };
    });
    return this._respostaJson(200, { writeResults: respostas });
  }
}

function resposta(id, texto, tipo, origem) {
  return { schemaVersion: 1, respostaId: id, texto, tipo, origem };
}

function dicaDoc(id, texto, escopo, status) {
  return {
    schemaVersion: 1,
    dicaId: id,
    texto,
    escopo,
    status: status || "ATIVA",
    origem: "EDITORIAL",
    revisaoTecnica: 1,
  };
}

test("criarSeAusente cria na primeira vez e não sobrescreve na segunda (idempotente)", async () => {
  const banco = new FirestoreFalso();
  const primeira = await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "shakira",
    resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO")
  );
  assert.equal(primeira.criado, true);
  assert.equal(primeira.jaExistia, false);

  const segunda = await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "shakira",
    resposta("shakira", "OUTRO TEXTO", "PESSOA", "ALIAS_LEGADO")
  );
  assert.equal(segunda.criado, false);
  assert.equal(segunda.jaExistia, true);

  const lido = await acervoFirestore.lerDocumento(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("shakira")
  );
  assert.equal(lido.dados.texto, "Shakira"); // o conteúdo original nunca foi sobrescrito.
});

test("migrarPreviaParaRemoto cria só as respostas seguras e não duplica ao rodar de novo", async () => {
  const banco = new FirestoreFalso();
  const previa = {
    respostas: {
      shakira: resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO"),
      "com-conflito": resposta("com-conflito", "X", "PESSOA", "EDITORIAL"),
    },
    dicas: {
      shakira: { "legado:d1": dicaDoc("legado:d1", "Dica 1", "PUBLICO") },
      "com-conflito": { d1: dicaDoc("d1", "Dica", "PUBLICO") },
    },
  };
  const seguras = ["shakira"]; // "com-conflito" fica de fora deliberadamente.

  const primeiro = await acervoFirestore.migrarPreviaParaRemoto(
    banco.transporte, CONFIG, CREDENCIAL, previa, seguras
  );
  assert.deepEqual(primeiro.criadas, ["shakira"]);
  assert.deepEqual(primeiro.jaExistentes, []);
  assert.equal(banco.documentos.has("acervoEditorial/com-conflito"), false);

  const segundo = await acervoFirestore.migrarPreviaParaRemoto(
    banco.transporte, CONFIG, CREDENCIAL, previa, seguras
  );
  assert.deepEqual(segundo.criadas, []);
  assert.deepEqual(segundo.jaExistentes, ["shakira"]);
});

test("carregarRespostaRemota devolve null quando a resposta não existe", async () => {
  const banco = new FirestoreFalso();
  const remoto = await acervoFirestore.carregarRespostaRemota(
    banco.transporte, CONFIG, CREDENCIAL, "inexistente"
  );
  assert.equal(remoto, null);
});

test("carregarRespostaRemota lê a resposta e todo o banco de dicas", async () => {
  const banco = new FirestoreFalso();
  await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "shakira",
    resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO")
  );
  await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("shakira") + "/dicas",
    "dicaId", "legado:d1", dicaDoc("legado:d1", "Dica 1", "PUBLICO")
  );
  const remoto = await acervoFirestore.carregarRespostaRemota(
    banco.transporte, CONFIG, CREDENCIAL, "shakira"
  );
  assert.equal(remoto.resposta.texto, "Shakira");
  assert.equal(Object.keys(remoto.dicas).length, 1);
  assert.equal(remoto.dicas["legado:d1"].texto, "Dica 1");
});

test("salvarRascunhoRemoto grava com precondição e detecta conflito de concorrência", async () => {
  const banco = new FirestoreFalso();
  await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "shakira",
    resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO")
  );
  const lido1 = await acervoFirestore.lerDocumento(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("shakira")
  );

  // Primeira gravação com a precondição correta: passa.
  await acervoFirestore.salvarRascunhoRemoto(
    banco.transporte, CONFIG, CREDENCIAL, "shakira",
    { ...lido1.dados, _updateTime: lido1.updateTime, notasEditoriais: "primeira nota" },
    []
  );

  // Segunda gravação usando a MESMA (agora desatualizada) precondição: falha.
  await assert.rejects(
    acervoFirestore.salvarRascunhoRemoto(
      banco.transporte, CONFIG, CREDENCIAL, "shakira",
      { ...lido1.dados, _updateTime: lido1.updateTime, notasEditoriais: "segunda nota" },
      []
    ),
    (erro) => erro instanceof acervoFirestore.ConflitoDeConcorrencia
  );

  const final = await acervoFirestore.lerDocumento(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("shakira")
  );
  assert.equal(final.dados.notasEditoriais, "primeira nota"); // nunca sobrescrito silenciosamente.
});

test("salvarRascunhoRemoto cria com precondição exists:false quando é novo", async () => {
  const banco = new FirestoreFalso();
  await acervoFirestore.salvarRascunhoRemoto(
    banco.transporte, CONFIG, CREDENCIAL, "nova-resposta",
    resposta("nova-resposta", "Nova", "PESSOA", "EDITORIAL"),
    [dicaDoc("d1", "Dica nova", "PUBLICO")]
  );
  const lida = await acervoFirestore.lerDocumento(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("nova-resposta")
  );
  assert.equal(lida.dados.texto, "Nova");
});

test("commit nunca é parcial: se uma precondição falha, nenhuma escrita do lote aplica", async () => {
  const banco = new FirestoreFalso();
  await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "shakira",
    resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO")
  );
  // dica ainda não existe remotamente, mas passamos um _updateTime inventado
  // para forçar a precondição a falhar.
  await assert.rejects(
    acervoFirestore.salvarRascunhoRemoto(
      banco.transporte, CONFIG, CREDENCIAL, "shakira",
      { ...resposta("shakira", "Shakira", "PESSOA", "ALIAS_LEGADO"), _updateTime: undefined },
      [{ ...dicaDoc("d1", "Dica", "PUBLICO"), _updateTime: "tempo-que-nao-existe" }]
    ),
    (erro) => erro instanceof acervoFirestore.ConflitoDeConcorrencia
  );
  const dicaFoiCriada = banco.documentos.has("acervoEditorial/shakira/dicas/d1");
  assert.equal(dicaFoiCriada, false);
});

test("ids codificados no documento e no campo ficam sempre iguais", async () => {
  const banco = new FirestoreFalso();
  await acervoFirestore.criarSeAusente(
    banco.transporte, CONFIG, CREDENCIAL, "acervoEditorial", "respostaId", "algo/com/barra",
    resposta("algo/com/barra", "Texto", "COISA", "EDITORIAL")
  );
  const lido = await acervoFirestore.lerDocumento(
    banco.transporte, CONFIG, CREDENCIAL, acervoFirestore.caminhoDaResposta("algo/com/barra")
  );
  assert.equal(lido.existe, true);
  assert.equal(lido.dados.respostaId, require("./acervo.js").idDoDocumento("algo/com/barra"));
});

test("executarCommit recusa lote maior que o teto de escritas", async () => {
  const banco = new FirestoreFalso();
  const writes = Array.from({ length: acervoFirestore.LIMITE_DE_ESCRITAS_POR_COMMIT + 1 }, (_, i) => ({
    update: { name: "projects/p/databases/(default)/documents/x/" + i, fields: {} },
  }));
  await assert.rejects(acervoFirestore.executarCommit(banco.transporte, CONFIG, CREDENCIAL, writes));
});

test("campoFirestore/valorFirestore fazem ida e volta para string, número, booleano e mapa", () => {
  const original = { texto: "Olá", numero: 42, ativo: true, aninhado: { a: 1 } };
  const campos = acervoFirestore.camposFirestore(original);
  const devolvido = acervoFirestore.objetoDosCampos(campos);
  assert.deepEqual(devolvido, original);
});
