// Transporte REST do acervo editorial no Firestore. Funcoes puras o
// suficiente para testar com um `transporte` falso (mesma assinatura do
// `fetch` do navegador: `(url, opcoes) => Promise<{ok, status, json()}>`);
// em producao/emulador, `acervo-ui.js` injeta o `fetch` real e a credencial
// ja obtida pelo mesmo fluxo anonimo de `app.js` (`comCredencialFirebase`).
//
// Cria-apenas na migracao: nunca sobrescreve um documento remoto ja
// existente. Edicao explicita usa precondicao `currentDocument.updateTime`:
// se o documento mudou remotamente entre ler e salvar, a escrita falha com
// `ConflitoDeConcorrencia` em vez de sobrescrever silenciosamente.
"use strict";

(function (raiz, dependencias) {
  const acervo = dependencias.AcervoEditorial;
  if (!acervo) {
    throw new Error("acervo-firestore.js depende de acervo.js carregado antes.");
  }

  const BASE_PADRAO = "https://firestore.googleapis.com/v1";

  // Teto operacional nosso: uma resposta + até 500 dicas, atomicamente.
  // A quota oficial REST é 10 MiB/requisição; usamos margem de 9 MiB.
  const LIMITE_DE_ESCRITAS_POR_COMMIT = 501;
  const LIMITE_DE_BYTES_POR_COMMIT = 9 * 1024 * 1024;

  class ErroDoFirestore extends Error {
    constructor(codigo, mensagem, status) {
      super(mensagem);
      this.codigo = codigo;
      this.mensagem = mensagem;
      this.status = status;
    }
  }

  class ConflitoDeConcorrencia extends ErroDoFirestore {
    constructor(mensagem) {
      super("conflito_de_concorrencia", mensagem, 409);
    }
  }

  // ---- conversão de campos (mesmo formato usado por app.js) ---------------

  function campoFirestore(valor) {
    if (valor === null || valor === undefined) return { nullValue: null };
    if (typeof valor === "string") return { stringValue: valor };
    if (typeof valor === "boolean") return { booleanValue: valor };
    if (typeof valor === "number" && Number.isInteger(valor)) {
      return { integerValue: String(valor) };
    }
    if (typeof valor === "number") return { doubleValue: valor };
    if (Array.isArray(valor)) {
      return { arrayValue: { values: valor.map(campoFirestore) } };
    }
    if (typeof valor === "object") {
      return { mapValue: { fields: camposFirestore(valor) } };
    }
    throw new Error("Tipo incompatível com o Firestore: " + typeof valor);
  }

  function camposFirestore(objeto) {
    return Object.fromEntries(
      Object.entries(objeto || {})
        .filter(([, valor]) => valor !== undefined)
        .map(([nome, valor]) => [nome, campoFirestore(valor)])
    );
  }

  function valorFirestore(campo) {
    if (!campo || typeof campo !== "object") return null;
    if ("stringValue" in campo) return campo.stringValue;
    if ("integerValue" in campo) return Number(campo.integerValue);
    if ("doubleValue" in campo) return campo.doubleValue;
    if ("booleanValue" in campo) return campo.booleanValue;
    if ("timestampValue" in campo) return campo.timestampValue;
    if ("nullValue" in campo) return null;
    if ("arrayValue" in campo) {
      return ((campo.arrayValue && campo.arrayValue.values) || []).map(valorFirestore);
    }
    if ("mapValue" in campo) {
      return objetoDosCampos((campo.mapValue && campo.mapValue.fields) || {});
    }
    return null;
  }

  function objetoDosCampos(campos) {
    return Object.fromEntries(
      Object.entries(campos || {}).map(([nome, campo]) => [nome, valorFirestore(campo)])
    );
  }

  // ---- caminhos -------------------------------------------------------------

  function raizDeDocumentos(config) {
    return (
      "projects/" + config.projectId +
      "/databases/" + (config.databaseId || "(default)") +
      "/documents"
    );
  }

  function urlBase(config) {
    return config.baseUrl || BASE_PADRAO;
  }

  function caminhoDaResposta(respostaId) {
    return "acervoEditorial/" + encodeURIComponent(acervo.idDoDocumento(respostaId));
  }

  function caminhoDaDica(respostaId, dicaId) {
    return caminhoDaResposta(respostaId) + "/dicas/" + encodeURIComponent(acervo.idDoDocumento(dicaId));
  }

  function nomeDoDocumento(config, caminho) {
    return raizDeDocumentos(config) + "/" + caminho;
  }

  function urlDoDocumento(config, caminho) {
    return urlBase(config) + "/" + nomeDoDocumento(config, caminho);
  }

  // ---- leitura ---------------------------------------------------------------

  async function respostaJson(resposta, mensagemDeErro) {
    let corpo = null;
    try {
      corpo = await resposta.json();
    } catch (_) {
      corpo = null;
    }
    if (!resposta.ok) {
      const detalhe = Array.isArray(corpo) ? corpo[0] : corpo;
      const status = (detalhe && detalhe.error && detalhe.error.status) || "";
      throw new ErroDoFirestore(
        status || "erro_http",
        (detalhe && detalhe.error && detalhe.error.message) || mensagemDeErro,
        resposta.status
      );
    }
    return corpo;
  }

  /** {existe:false} em 404; senão {existe:true, dados, updateTime, nome}. */
  async function lerDocumento(transporte, config, credencial, caminho) {
    const resposta = await transporte(urlDoDocumento(config, caminho), {
      headers: { Authorization: "Bearer " + credencial.idToken },
    });
    if (resposta.status === 404) return { existe: false };
    const documento = await respostaJson(resposta, "Não foi possível ler o documento.");
    return {
      existe: true,
      dados: objetoDosCampos(documento.fields || {}),
      updateTime: documento.updateTime,
      nome: documento.name,
    };
  }

  async function listarSubcolecao(transporte, config, credencial, caminhoDoPai, nomeDaSubcolecao) {
    const base = urlBase(config) + "/" + nomeDoDocumento(config, caminhoDoPai) + "/" +
      nomeDaSubcolecao + "?pageSize=300";
    const documentos = [];
    let pagina = "";
    do {
      const url = base + (pagina ? "&pageToken=" + encodeURIComponent(pagina) : "");
      const resposta = await transporte(url, {
        headers: { Authorization: "Bearer " + credencial.idToken },
      });
      if (resposta.status === 404) break;
      const dados = await respostaJson(resposta, "Não foi possível listar a subcoleção.");
      documentos.push(...(dados.documents || []));
      pagina = dados.nextPageToken || "";
    } while (pagina && documentos.length < 5000);
    return documentos.map((documento) => ({
      dados: objetoDosCampos(documento.fields || {}),
      updateTime: documento.updateTime,
      nome: documento.name,
    }));
  }

  /** Lê uma resposta remota completa (resposta + banco). null se não existir. */
  async function carregarRespostaRemota(transporte, config, credencial, respostaId) {
    const resposta = await lerDocumento(transporte, config, credencial, caminhoDaResposta(respostaId));
    if (!resposta.existe) return null;
    const documentosDeDicas = await listarSubcolecao(
      transporte, config, credencial, caminhoDaResposta(respostaId), "dicas"
    );
    const dicas = {};
    documentosDeDicas.forEach((doc) => {
      const dicaId = acervo.idLogicoDoDocumento(doc.dados.dicaId);
      dicas[dicaId] = { ...doc.dados, dicaId, _updateTime: doc.updateTime };
    });
    // Uma consulta paginada não é snapshot: toda escrita toca o pai no mesmo
    // commit. A segunda leitura detecta qualquer alteração entre as páginas.
    const depois = await lerDocumento(transporte, config, credencial, caminhoDaResposta(respostaId));
    if (!depois.existe || depois.updateTime !== resposta.updateTime) {
      throw new ConflitoDeConcorrencia("Banco mudou durante a leitura. Abra novamente.");
    }
    if (acervo.idLogicoDoDocumento(resposta.dados.respostaId) !== respostaId) throw new Error("Identidade remota divergente.");
    const esperados = resposta.dados.dicaIds;
    const completo = Array.isArray(esperados) && esperados.length === Object.keys(dicas).length &&
      esperados.every(id => Object.hasOwn(dicas, acervo.idLogicoDoDocumento(id)));
    return { resposta: { ...resposta.dados, respostaId, _updateTime: resposta.updateTime }, dicas, completo };
  }

  // ---- escrita: criar-apenas (migração) --------------------------------------

  /**
   * POST cria só se ausente; ALREADY_EXISTS vira {criado:false, jaExistia:true}.
   *
   * `nomeDoCampoDeId` (`"respostaId"` ou `"dicaId"`) é sempre sobrescrito com
   * a forma CODIFICADA do id — o mesmo valor que vira o segmento do caminho
   * do documento. Sem isso, um id lógico com caractere especial (raro, mas
   * possível em id explícito legado) faria o campo divergir do segmento do
   * caminho, e a regra `dados.respostaId == respostaId`/`dicaId == dicaId`
   * do `firestore.rules` rejeitaria a escrita.
   */
  async function criarSeAusente(
    transporte, config, credencial, caminhoDoPai, nomeDoCampoDeId, idLogico, campos
  ) {
    const idDocumento = acervo.idDoDocumento(idLogico);
    const url =
      urlBase(config) + "/" + nomeDoDocumento(config, caminhoDoPai) +
      "?documentId=" + encodeURIComponent(idDocumento);
    const camposCodificados = { ...semCamposInternos(campos), [nomeDoCampoDeId]: idDocumento };
    const resposta = await transporte(url, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + credencial.idToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ fields: camposFirestore(camposCodificados) }),
    });
    if (resposta.status === 409) {
      return { criado: false, jaExistia: true };
    }
    await respostaJson(resposta, "Não foi possível criar o documento.");
    return { criado: true, jaExistia: false };
  }

  function tamanhoEmBytes(objeto) {
    return new TextEncoder().encode(JSON.stringify(objeto)).length;
  }

  function dividirEmLotes(lista, tamanho) {
    const lotes = [];
    for (let inicio = 0; inicio < lista.length; inicio += tamanho) {
      lotes.push(lista.slice(inicio, inicio + tamanho));
    }
    return lotes;
  }

  /**
   * Migra as respostas SEGURAS de uma prévia (sem conflito, sem banco acima
   * do teto — ver `respostasSeguras`) para o Firestore, criando só o que
   * ainda não existe remotamente. Idempotente: rodar de novo não duplica
   * nem sobrescreve o que já foi criado, mesmo que o conteúdo local tenha
   * mudado (quem manda depois de criado é o remoto — corrigir por lá).
   */
  async function migrarPreviaParaRemoto(transporte, config, credencial, previa, respostasSeguras) {
    const relatorio = { criadas: [], jaExistentes: [], puladas: [], erros: [] };
    for (const respostaId of respostasSeguras) {
      const resposta = previa.respostas[respostaId];
      const dicas = previa.dicas[respostaId] || {};
      if (!resposta) continue;
      try {
        const existente = await carregarRespostaRemota(transporte, config, credencial, respostaId);
        if (existente && existente.completo) {
          relatorio.jaExistentes.push(respostaId);
          continue;
        }
        // Recupera também migração provisória incompleta: só cria faltantes;
        // qualquer dica já corrigida remotamente prevalece sobre a prévia.
        const banco = { ...dicas, ...(existente ? existente.dicas : {}) };
        await salvarRascunhoRemoto(transporte, config, credencial, respostaId,
          existente ? existente.resposta : resposta, Object.values(banco), existente);
        relatorio.criadas.push(respostaId);
      } catch (erro) {
        relatorio.erros.push({ respostaId, mensagem: erro.mensagem || erro.message });
      }
    }
    return relatorio;
  }

  // ---- escrita: edição com precondição (rascunho remoto) ---------------------

  async function executarCommit(transporte, config, credencial, writes) {
    if (writes.length > LIMITE_DE_ESCRITAS_POR_COMMIT) {
      throw new Error(
        "Lote grande demais para um commit (" + writes.length + " escritas; teto " +
        LIMITE_DE_ESCRITAS_POR_COMMIT + "). Divida em lotes menores."
      );
    }
    const tamanho = tamanhoEmBytes(writes);
    if (tamanho > LIMITE_DE_BYTES_POR_COMMIT) {
      throw new Error(
        "Lote grande demais para um commit (" + tamanho + " bytes; teto " +
        LIMITE_DE_BYTES_POR_COMMIT + "). Divida em lotes menores."
      );
    }
    const url =
      urlBase(config) + "/projects/" + encodeURIComponent(config.projectId) +
      "/databases/" + encodeURIComponent(config.databaseId || "(default)") + "/documents:commit";
    const resposta = await transporte(url, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + credencial.idToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ writes }),
    });
    const corpo = await resposta.json().catch(() => null);
    if (!resposta.ok) {
      const detalhe = Array.isArray(corpo) ? corpo[0] : corpo;
      const status = (detalhe && detalhe.error && detalhe.error.status) || "";
      if (["FAILED_PRECONDITION", "ALREADY_EXISTS", "ABORTED", "NOT_FOUND"].includes(status)) {
        throw new ConflitoDeConcorrencia(
          "O documento mudou remotamente. Recarregue antes de salvar de novo."
        );
      }
      throw new ErroDoFirestore(
        status || "erro_http",
        (detalhe && detalhe.error && detalhe.error.message) || "Não foi possível salvar no Firestore.",
        resposta.status
      );
    }
    return corpo;
  }

  /** Remove campos de controle interno (prefixo "_") antes de gravar no Firestore. */
  function semCamposInternos(objeto) {
    return Object.fromEntries(Object.entries(objeto || {}).filter(([nome]) => !nome.startsWith("_")));
  }

  function escritaComPrecondicao(config, caminho, nomeDoCampoDeId, idLogico, campos, updateTimeEsperado) {
    const idDocumento = acervo.idDoDocumento(idLogico);
    const camposCodificados = { ...semCamposInternos(campos), [nomeDoCampoDeId]: idDocumento };
    const write = {
      update: { name: nomeDoDocumento(config, caminho), fields: camposFirestore(camposCodificados) },
    };
    write.currentDocument = updateTimeEsperado
      ? { updateTime: updateTimeEsperado }
      : { exists: false };
    return write;
  }

  /**
   * Salva rascunho remoto de UMA resposta + só as dicas efetivamente
   * alteradas, tudo num único commit atômico (ou tudo aplica, ou nada
   * aplica). Recebe o banco completo e calcula o diff contra a base lida
   * quando o editor abriu. Precondições do pai e das dicas impedem alterações
   * concorrentes; a base nunca é atualizada automaticamente em um conflito.
   */
  async function salvarRascunhoRemoto(
    transporte, config, credencial, respostaId, resposta, dicasDoBanco, baseRemota
  ) {
    const banco = dicasDoBanco || [];
    const ids = new Set(banco.map(d => d.dicaId));
    if (banco.length > 500 || ids.size !== banco.length) throw new Error("Banco exige ids únicos e até 500 dicas (incluindo desativadas).");
    banco.forEach(d => {
      acervo.idDoDocumento(d.dicaId);
      acervo.validarTextoDaDica(d.texto);
      if (!["PUBLICO", "PRIVADO"].includes(d.escopo) || !["ATIVA", "REMOVIDA"].includes(d.status)) throw new Error("Escopo/status inválido.");
    });
    const base = baseRemota === undefined
      ? await carregarRespostaRemota(transporte, config, credencial, respostaId) : baseRemota;
    if ((base?.resposta._updateTime || null) !== (resposta._updateTime || null)) {
      throw new ConflitoDeConcorrencia("Banco remoto mudou. Reabra e reconcilie suas alterações.");
    }
    const anteriores = base?.dicas || {};
    if (Object.keys(anteriores).some(id => !ids.has(id))) throw new Error("Dicas não podem ser apagadas; use Desativar.");
    const alteradas = banco.filter(d => {
      const antiga = anteriores[d.dicaId];
      return !antiga || ["texto", "escopo", "status"].some(k => d[k] !== antiga[k]);
    });
    // Só campos editoriais mutáveis vêm do formulário. Identidade/metadados
    // desconhecidos do remoto são conservados; revisão avança no mesmo commit.
    const pai = { ...(base?.resposta || resposta),
      notasEditoriais: resposta.notasEditoriais || "",
      revisaoTecnica: (base?.resposta.revisaoTecnica || 0) + 1,
      dicaIds: [...ids].map(acervo.idDoDocumento).sort() };
    const writes = [
      escritaComPrecondicao(
        config, caminhoDaResposta(respostaId), "respostaId", respostaId, pai, resposta._updateTime
      ),
    ];
    alteradas.forEach((dica) => {
      const antiga = anteriores[dica.dicaId];
      const nova = { ...(antiga || dica), texto: dica.texto, escopo: dica.escopo, status: dica.status,
        revisaoTecnica: (antiga?.revisaoTecnica || 0) + 1 };
      writes.push(
        escritaComPrecondicao(
          config, caminhoDaDica(respostaId, dica.dicaId), "dicaId", dica.dicaId, nova, antiga?._updateTime
        )
      );
    });
    try {
      return await executarCommit(transporte, config, credencial, writes);
    } catch (erro) {
      // Rules podem avaliar revisão crescente antes da precondição do REST.
      // Releitura apenas classifica o erro; nunca adota a nova base nem reenvia.
      if (base && erro.status === 403) {
        try {
          const atual = await lerDocumento(transporte, config, credencial, caminhoDaResposta(respostaId));
          if (atual.existe && atual.updateTime !== base.resposta._updateTime) {
            throw new ConflitoDeConcorrencia("Banco mudou remotamente. Reabra e reconcilie suas alterações.");
          }
        } catch (leitura) {
          if (leitura instanceof ConflitoDeConcorrencia) throw leitura;
        }
      }
      throw erro;
    }
  }

  const api = {
    BASE_PADRAO,
    LIMITE_DE_ESCRITAS_POR_COMMIT,
    LIMITE_DE_BYTES_POR_COMMIT,
    ErroDoFirestore,
    ConflitoDeConcorrencia,
    campoFirestore,
    camposFirestore,
    valorFirestore,
    objetoDosCampos,
    raizDeDocumentos,
    caminhoDaResposta,
    caminhoDaDica,
    lerDocumento,
    listarSubcolecao,
    carregarRespostaRemota,
    criarSeAusente,
    migrarPreviaParaRemoto,
    tamanhoEmBytes,
    dividirEmLotes,
    executarCommit,
    salvarRascunhoRemoto,
  };

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  } else {
    raiz.AcervoFirestore = api;
  }
})(
  typeof window !== "undefined" ? window : globalThis,
  typeof module !== "undefined" && module.exports
    ? { AcervoEditorial: require("./acervo.js") }
    : { AcervoEditorial: (typeof window !== "undefined" ? window : globalThis).AcervoEditorial }
);
