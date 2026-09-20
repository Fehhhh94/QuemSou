// Central de Baralhos - cliente offline, sem framework e sem CDN.
// Todo texto vindo do servidor entra por textContent; nada de innerHTML.
"use strict";

const TOKEN = document.querySelector('meta[name="central-token"]').content;

const estado = {
  inventario: [],
  avisos: [],
  origens: [],
  chave: null,
  aberto: null,
  alterado: false,
  ocupado: false,
  validando: false,
  aprovado: false,
  plano: null,
  feedbacksGlobais: { quantidadeDeListas: 0, quantidadeDeDicas: 0, listas: [] },
  firebase: { configurado: false, status: "carregando", uid: null, motivo: "" },
  feedbacksNuvem: [],
  feedbacksNuvemPorCard: {},
  assinaturaFeedbacksNuvem: "",
  sequenciaDeAbertura: 0,
};

const elementos = {
  estadoGlobal: document.getElementById("estado-global"),
  avisos: document.getElementById("avisos"),
  busca: document.getElementById("busca"),
  filtroOrigem: document.getElementById("filtro-origem"),
  filtroGrupo: document.getElementById("filtro-grupo"),
  filtroSituacao: document.getElementById("filtro-situacao"),
  mostrarTecnicos: document.getElementById("mostrar-tecnicos"),
  resumo: document.getElementById("resumo-biblioteca"),
  lista: document.getElementById("lista"),
  editor: document.getElementById("editor"),
  editorVazio: document.getElementById("editor-vazio"),
  formNovo: document.getElementById("form-novo"),
  arquivoFeedback: document.getElementById("arquivo-feedback"),
  importarFeedback: document.getElementById("importar-feedback"),
  resumoFeedbacks: document.getElementById("resumo-feedbacks"),
  listasFeedbacks: document.getElementById("listas-feedbacks"),
  estadoNuvem: document.getElementById("estado-nuvem"),
  atualizarNuvem: document.getElementById("atualizar-nuvem"),
  copiarUidNuvem: document.getElementById("copiar-uid-nuvem"),
};

const CHAVE_DA_SESSAO_FIREBASE = "quemsou.firebase.anonymous.v1";

function avisar(mensagem, tom) {
  elementos.estadoGlobal.textContent = mensagem || "";
  if (tom) {
    elementos.estadoGlobal.dataset.tom = tom;
  } else {
    delete elementos.estadoGlobal.dataset.tom;
  }
}

function criar(tag, texto, classe) {
  const elemento = document.createElement(tag);
  if (texto !== undefined && texto !== null) elemento.textContent = String(texto);
  if (classe) elemento.className = classe;
  return elemento;
}

function campo(rotulo, controle, identificador) {
  const caixa = criar("div", null, "campo");
  const label = criar("label", rotulo);
  label.htmlFor = identificador;
  controle.id = identificador;
  caixa.append(label, controle);
  return caixa;
}

async function pedir(caminho) {
  const resposta = await fetch(caminho, { headers: { Accept: "application/json" } });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) throw new Error(dados.mensagem || "Falha na comunicação com o painel.");
  return dados;
}

async function enviar(caminho, corpo) {
  const resposta = await fetch(caminho, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Token-Central": TOKEN },
    body: JSON.stringify(corpo),
  });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) throw new Error(dados.mensagem || "Falha na comunicação com o painel.");
  return dados;
}

function marcarAlterado() {
  estado.alterado = true;
  estado.aprovado = false;
  avisar("Alterações não salvas neste rascunho.", "atencao");
  atualizarAcoes();
}

window.addEventListener("beforeunload", (evento) => {
  if (estado.alterado) evento.preventDefault();
});

// ---- biblioteca -----------------------------------------------------------

async function carregarInventario(silencioso = false) {
  if (!silencioso) avisar("Carregando a biblioteca...", "ocupado");
  const dados = await pedir("/api/inventario");
  estado.inventario = dados.baralhos || [];
  estado.avisos = dados.avisos || [];
  estado.origens = dados.origens || [];
  estado.feedbacksGlobais = dados.feedbacks || {
    quantidadeDeListas: 0,
    quantidadeDeDicas: 0,
    listas: [],
  };
  montarFiltros();
  renderizarAvisos();
  renderizarLista();
  renderizarResumoDeFeedbacks();
  if (!silencioso) avisar("Biblioteca carregada.", "ok");
  return dados;
}

function renderizarResumoDeFeedbacks() {
  const resumo = estado.feedbacksGlobais;
  const quantidadeNuvem = estado.feedbacksNuvem.length;
  elementos.resumoFeedbacks.textContent =
    resumo.quantidadeDeListas + " lista(s), " + resumo.quantidadeDeDicas +
    " avaliação(ões) importada(s) • " + quantidadeNuvem + " recebida(s) da nuvem.";
  elementos.listasFeedbacks.replaceChildren();
  (resumo.listas || []).forEach((lista) => {
    elementos.listasFeedbacks.append(
      criar(
        "li",
        lista.nome + " — " + lista.quantidadeDeDicas + " dica(s)" +
          (lista.quantidadeIgnorada ? " • " + lista.quantidadeIgnorada + " feedback(s) gerais" : "")
      )
    );
  });
}

function renderizarEstadoDaNuvem() {
  const firebase = estado.firebase;
  atualizarAcoes();
  elementos.copiarUidNuvem.hidden = !firebase.uid;
  elementos.atualizarNuvem.disabled = !firebase.configurado || firebase.status === "carregando";
  if (!firebase.configurado) {
    elementos.estadoNuvem.textContent = firebase.motivo || "Sincronização em nuvem não configurada.";
    return;
  }
  if (firebase.status === "sem_permissao") {
    elementos.estadoNuvem.textContent =
      "Painel conectado, mas ainda sem permissão de leitura. Autorize o UID " + firebase.uid +
      " em admins/{uid} no Firestore.";
    return;
  }
  if (firebase.status === "erro") {
    elementos.estadoNuvem.textContent = firebase.motivo || "Não foi possível consultar o Firestore.";
    return;
  }
  if (firebase.status === "pronto") {
    elementos.estadoNuvem.textContent =
      "Firestore conectado • " + estado.feedbacksNuvem.length + " feedback(s) sincronizado(s).";
    return;
  }
  elementos.estadoNuvem.textContent = "Consultando o Firestore…";
}

function lerCredencialFirebase() {
  try {
    const dados = JSON.parse(localStorage.getItem(CHAVE_DA_SESSAO_FIREBASE) || "null");
    return dados && typeof dados === "object" ? dados : null;
  } catch (_) {
    return null;
  }
}

function salvarCredencialFirebase(credencial) {
  localStorage.setItem(CHAVE_DA_SESSAO_FIREBASE, JSON.stringify(credencial));
  return credencial;
}

async function respostaFirebase(resposta, mensagemPadrao) {
  const dados = await resposta.json().catch(() => ({}));
  if (resposta.ok) return dados;
  const mensagem = ((dados.error || {}).message || mensagemPadrao);
  const erro = new Error(mensagem);
  erro.status = resposta.status;
  throw erro;
}

async function criarCredencialAnonima(config) {
  const resposta = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" +
      encodeURIComponent(config.apiKey),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ returnSecureToken: true }),
    }
  );
  const dados = await respostaFirebase(resposta, "Não foi possível autenticar o painel.");
  return salvarCredencialFirebase({
    idToken: dados.idToken,
    refreshToken: dados.refreshToken,
    uid: dados.localId,
    expiraEm: Date.now() + Number(dados.expiresIn || 3600) * 1000,
  });
}

async function renovarCredencial(config, anterior) {
  const resposta = await fetch(
    "https://securetoken.googleapis.com/v1/token?key=" + encodeURIComponent(config.apiKey),
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: anterior.refreshToken,
      }),
    }
  );
  const dados = await respostaFirebase(resposta, "Não foi possível renovar a sessão do painel.");
  return salvarCredencialFirebase({
    idToken: dados.access_token,
    refreshToken: dados.refresh_token,
    uid: dados.user_id,
    expiraEm: Date.now() + Number(dados.expires_in || 3600) * 1000,
  });
}

async function credencialFirebase(config, forcarRenovacao = false) {
  const anterior = lerCredencialFirebase();
  if (!forcarRenovacao && anterior && anterior.idToken && anterior.expiraEm > Date.now() + 60000) {
    return anterior;
  }
  if (anterior && anterior.refreshToken) {
    try {
      return await renovarCredencial(config, anterior);
    } catch (_) {
      localStorage.removeItem(CHAVE_DA_SESSAO_FIREBASE);
    }
  }
  return criarCredencialAnonima(config);
}

async function comCredencialFirebase(acao) {
  const config = estado.firebase.config;
  if (!config || !config.configurado) {
    throw new Error("A configuração do Firebase não está disponível neste painel.");
  }
  let credencial = await credencialFirebase(config);
  estado.firebase.uid = credencial.uid;
  try {
    return await acao(config, credencial);
  } catch (erro) {
    if (erro.status !== 401) throw erro;
    credencial = await credencialFirebase(config, true);
    estado.firebase.uid = credencial.uid;
    return acao(config, credencial);
  }
}

function valorFirestore(campo) {
  if (!campo || typeof campo !== "object") return null;
  if (Object.prototype.hasOwnProperty.call(campo, "stringValue")) return campo.stringValue;
  if (Object.prototype.hasOwnProperty.call(campo, "integerValue")) return Number(campo.integerValue);
  if (Object.prototype.hasOwnProperty.call(campo, "timestampValue")) return campo.timestampValue;
  if (Object.prototype.hasOwnProperty.call(campo, "booleanValue")) return campo.booleanValue;
  return null;
}

function feedbackDoDocumento(documento) {
  const campos = documento.fields || {};
  const ler = (nome) => valorFirestore(campos[nome]);
  return {
    documentoId: String(documento.name || "").split("/").pop(),
    baralhoId: ler("baralhoId"),
    cardId: ler("cardId"),
    dicaId: ler("dicaId"),
    textoAvaliado: ler("textoAvaliado"),
    voto: ler("voto"),
    comentario: ler("comentario") || null,
    rodada: ler("rodada"),
    posicao: ler("posicao"),
    versaoDoBaralho: ler("versaoDoBaralho"),
    criadoEm: ler("criadoEm"),
    atualizadoEm: ler("atualizadoEm"),
    listaId: "firestore",
    listaNome: "Firestore — automático",
  };
}

async function listarFeedbacksNoFirestore(config, credencial) {
  const base =
    "https://firestore.googleapis.com/v1/projects/" + encodeURIComponent(config.projectId) +
    "/databases/" + encodeURIComponent(config.databaseId || "(default)") +
    "/documents/feedbacks?pageSize=1000";
  const documentos = [];
  let pagina = "";
  do {
    const resposta = await fetch(base + (pagina ? "&pageToken=" + encodeURIComponent(pagina) : ""), {
      headers: { Authorization: "Bearer " + credencial.idToken },
    });
    const dados = await respostaFirebase(resposta, "Não foi possível ler os feedbacks do Firestore.");
    documentos.push(...(dados.documents || []));
    pagina = dados.nextPageToken || "";
  } while (pagina && documentos.length < 50000);
  return documentos.map(feedbackDoDocumento).filter(
    (item) => item.baralhoId && item.cardId && item.dicaId && item.textoAvaliado
  );
}

function reindexarFeedbacksNuvem() {
  const baralho = ((estado.aberto || {}).baralho || {});
  const cards = baralho.cards || [];
  const porCard = {};
  const cardsPorId = new Map();
  cards.forEach((card) => {
    cardsPorId.set(card.id, card);
    porCard[card.id] = (card.clues || []).map(() => []);
  });
  estado.feedbacksNuvem.forEach((item) => {
    if (item.baralhoId !== baralho.id) return;
    const card = cardsPorId.get(item.cardId);
    if (!card) return;
    const fato = (card.bancoDeDicas || []).find((candidato) => candidato.id === item.dicaId);
    const textoDeReferencia = fato ? fato.texto : item.textoAvaliado;
    const posicao = (card.clues || []).findIndex(
      (texto) => normalizar(texto) === normalizar(textoDeReferencia)
    );
    if (posicao >= 0) porCard[card.id][posicao].push(item);
  });
  estado.feedbacksNuvemPorCard = porCard;
}

async function atualizarFeedbacksDaNuvem(silencioso = false) {
  const config = estado.firebase.config;
  if (!config || !config.configurado || estado.firebase.status === "carregando") return;
  estado.firebase.status = "carregando";
  if (!silencioso) renderizarEstadoDaNuvem();
  try {
    let credencial = await credencialFirebase(config);
    estado.firebase.uid = credencial.uid;
    let itens;
    try {
      itens = await listarFeedbacksNoFirestore(config, credencial);
    } catch (erro) {
      if (erro.status !== 401) throw erro;
      credencial = await credencialFirebase(config, true);
      estado.firebase.uid = credencial.uid;
      itens = await listarFeedbacksNoFirestore(config, credencial);
    }
    itens.sort((a, b) => String(b.atualizadoEm || b.criadoEm).localeCompare(
      String(a.atualizadoEm || a.criadoEm)
    ));
    const assinatura = JSON.stringify(itens);
    const mudou = assinatura !== estado.assinaturaFeedbacksNuvem;
    estado.assinaturaFeedbacksNuvem = assinatura;
    estado.feedbacksNuvem = itens;
    estado.firebase.status = "pronto";
    estado.firebase.motivo = "";
    reindexarFeedbacksNuvem();
    renderizarResumoDeFeedbacks();
    renderizarEstadoDaNuvem();
    if (mudou && estado.aberto) renderizarEditor();
  } catch (erro) {
    estado.firebase.status = erro.status === 403 ? "sem_permissao" : "erro";
    estado.firebase.motivo = erro.message === "OPERATION_NOT_ALLOWED"
      ? "Ative o provedor Anônimo no Firebase Authentication."
      : erro.message;
    renderizarEstadoDaNuvem();
  }
}

async function iniciarFeedbacksDaNuvem() {
  const config = await pedir("/api/firebase");
  estado.firebase.config = config;
  estado.firebase.configurado = Boolean(config.configurado);
  window.dispatchEvent(new Event("central-firebase-configurada"));
  estado.firebase.motivo = config.motivo || "";
  estado.firebase.status = config.configurado ? "aguardando" : "erro";
  renderizarEstadoDaNuvem();
  if (!config.configurado) return;
  await atualizarFeedbacksDaNuvem();
}

function raizDeDocumentosFirestore(config) {
  return (
    "projects/" + config.projectId + "/databases/" +
    (config.databaseId || "(default)") + "/documents"
  );
}

function campoFirestore(valor) {
  if (typeof valor === "string") return { stringValue: valor };
  if (typeof valor === "boolean") return { booleanValue: valor };
  if (Number.isInteger(valor)) return { integerValue: String(valor) };
  throw new Error("Tipo incompatível com a publicação no Firestore.");
}

function camposFirestore(objeto) {
  return Object.fromEntries(
    Object.entries(objeto).map(([nome, valor]) => [nome, campoFirestore(valor)])
  );
}

function nomeDoDocumentoFirestore(config, caminho) {
  return raizDeDocumentosFirestore(config) + "/" + caminho;
}

async function obterDocumentoFirestore(config, credencial, caminho) {
  const url = "https://firestore.googleapis.com/v1/" + nomeDoDocumentoFirestore(config, caminho);
  const resposta = await fetch(url, {
    headers: { Authorization: "Bearer " + credencial.idToken },
  });
  if (resposta.status === 404) return null;
  return respostaFirebase(resposta, "Não foi possível consultar o baralho no Firestore.");
}

async function executarCommitFirestore(config, credencial, writes) {
  if (new TextEncoder().encode(JSON.stringify({ writes })).length > 9 * 1024 * 1024) {
    throw new Error("Este baralho excede o tamanho seguro de publicação atômica (9 MiB). Nenhum dado foi enviado.");
  }
  const url =
    "https://firestore.googleapis.com/v1/projects/" + encodeURIComponent(config.projectId) +
    "/databases/" + encodeURIComponent(config.databaseId || "(default)") +
    "/documents:commit";
  const resposta = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: "Bearer " + credencial.idToken,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ writes: writes }),
  });
  return respostaFirebase(resposta, "Não foi possível publicar o baralho no Firestore.");
}

async function sha256Hex(texto) {
  const bytes = new TextEncoder().encode(texto);
  const hash = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(hash)].map((valor) => valor.toString(16).padStart(2, "0")).join("");
}

function baralhoParaPublicacao() {
  const baralho = estado.aberto.baralho;
  const colecao = baralho.colecao || {};
  return {
    id: String(baralho.id || ""),
    nome: String(baralho.nome || ""),
    categoria: String(baralho.categoria || ""),
    colecao: {
      id: String(colecao.id || ""),
      nome: String(colecao.nome || ""),
      icone: String(colecao.icone || ""),
    },
    versao: Number(baralho.versao),
    estado: String(baralho.estado || "EM_DESENVOLVIMENTO"),
    cards: (baralho.cards || []).map((card) => {
      const saida = {
        id: String(card.id || ""),
        type: String(card.type || ""),
        answer: String(card.answer || ""),
        clues: [...(card.clues || [])],
      };
      if (card.respostaId) saida.respostaId = card.respostaId;
      if (Array.isArray(card.bancoDeDicas) && card.bancoDeDicas.length) {
        saida.bancoDeDicas = card.bancoDeDicas.map((fato) => ({
          id: fato.id,
          texto: fato.texto,
        }));
      }
      return saida;
    }),
  };
}

function escritaDeDocumento(nome, campos) {
  return { update: { name: nome, fields: camposFirestore(campos) } };
}

async function prepararPublicacaoNoFirestore(config) {
  const baralho = baralhoParaPublicacao();
  const conteudo = JSON.stringify(baralho);
  const hashDoConteudo = await sha256Hex(conteudo);
  const versaoId = "v" + baralho.versao;
  const grupos = [];
  let atual = [];
  for (const card of baralho.cards) {
    const candidato = [...atual, card];
    const bytes = new TextEncoder().encode(JSON.stringify(candidato)).length;
    if (atual.length && (atual.length >= 25 || bytes > 700000)) {
      grupos.push(atual);
      atual = [card];
    } else {
      atual = candidato;
    }
    if (new TextEncoder().encode(JSON.stringify(atual)).length > 700000) {
      throw new Error("Um card isolado ultrapassa o limite seguro de publicação no Firestore.");
    }
  }
  if (atual.length) grupos.push(atual);
  if (grupos.length > 250) {
    throw new Error("O baralho exige blocos demais para uma publicação atômica.");
  }
  const blocos = [];
  for (const cards of grupos) {
    const conteudoJson = JSON.stringify(cards);
    blocos.push({
      schemaVersion: 1,
      ordem: blocos.length,
      quantidadeDeCards: cards.length,
      conteudoJson: conteudoJson,
      hashDoConteudo: await sha256Hex(conteudoJson),
    });
  }
  const comum = {
    schemaVersion: 1,
    id: baralho.id,
    nome: baralho.nome,
    categoria: baralho.categoria,
    colecaoId: baralho.colecao.id,
    colecaoNome: baralho.colecao.nome,
    colecaoIcone: baralho.colecao.icone,
    versao: baralho.versao,
    versaoId: versaoId,
    estado: baralho.estado,
    quantidadeDeCards: baralho.cards.length,
    quantidadeDeBlocos: blocos.length,
    hashDoConteudo: hashDoConteudo,
  };
  const entrada = estado.aberto.entradaDoIndice || {};
  const manifesto = {
    ...comum,
    descricao: String(entrada.descricao || ""),
    tamanhoEmBytes: new TextEncoder().encode(conteudo).length,
    visibilidade: baralho.categoria === "ESPECIAIS" ? "PRIVADO" : "PUBLICO",
    publicado: true,
  };
  const raiz = raizDeDocumentosFirestore(config);
  const base = "catalogo/" + encodeURIComponent(baralho.id);
  const caminhoDaVersao = base + "/versoes/" + versaoId;
  return {
    baralho: baralho,
    manifesto: manifesto,
    hashDoConteudo: hashDoConteudo,
    caminhoDoManifesto: base,
    writesDaVersao: [
      escritaDeDocumento(raiz + "/" + caminhoDaVersao, comum),
      ...blocos.map((bloco) => escritaDeDocumento(
        raiz + "/" + caminhoDaVersao + "/blocos/" + String(bloco.ordem).padStart(2, "0"),
        bloco
      )),
    ],
  };
}

function escritaDoManifesto(config, preparado, publicado = true) {
  const campos = { ...preparado.manifesto, publicado: publicado };
  return {
    update: {
      name: nomeDoDocumentoFirestore(config, preparado.caminhoDoManifesto),
      fields: camposFirestore(campos),
    },
    updateTransforms: [
      { fieldPath: "atualizadoEm", setToServerValue: "REQUEST_TIME" },
    ],
  };
}

async function publicarBaralhoNoFirestore() {
  if (estado.ocupado) return;
  const dados = estado.aberto;
  const origemAplicada =
    dados && dados.tipo !== "novo" && estado.plano && estado.plano.semAlteracao &&
    !estado.plano.conflito;
  if (
    !origemAplicada || estado.alterado || !estado.aprovado ||
    estado.firebase.status !== "pronto"
  ) {
    avisar(
      "Aplique as alterações na origem, valide o conteúdo exato e conecte o Firestore antes de publicar.",
      "erro"
    );
    return;
  }
  let preparado;
  try {
    preparado = await prepararPublicacaoNoFirestore(estado.firebase.config);
  } catch (erro) {
    avisar(erro.message, "erro");
    return;
  }
  const seguir = window.confirm(
    "Publicar no Firestore o baralho “" + preparado.baralho.nome + "” (versão " +
      preparado.baralho.versao + ", " + preparado.baralho.cards.length + " cards, acesso " +
      preparado.manifesto.visibilidade.toLowerCase() + ")?\n\n" +
      "Depois disso, o aplicativo poderá baixá-lo pelo catálogo em nuvem."
  );
  if (!seguir) return;
  definirOcupado(true);
  avisar("Publicando o baralho no Firestore...", "ocupado");
  try {
    await comCredencialFirebase(async (config, credencial) => {
      const existente = await obterDocumentoFirestore(
        config, credencial, preparado.caminhoDoManifesto
      );
      const versaoExistente = existente
        ? Number(valorFirestore((existente.fields || {}).versao))
        : 0;
      const hashExistente = existente
        ? valorFirestore((existente.fields || {}).hashDoConteudo)
        : null;
      if (versaoExistente > preparado.baralho.versao) {
        throw new Error(
          "O Firestore já tem uma versão mais nova. Atualize a origem local antes de publicar."
        );
      }
      if (versaoExistente === preparado.baralho.versao && hashExistente !== preparado.hashDoConteudo) {
        throw new Error(
          "A mesma versão já existe com outro conteúdo. Incremente a versão local antes de publicar."
        );
      }
      const writes = versaoExistente === preparado.baralho.versao
        ? [escritaDoManifesto(config, preparado)]
        : [...preparado.writesDaVersao, escritaDoManifesto(config, preparado)];
      await executarCommitFirestore(config, credencial, writes);
    });
    avisar(
      "Baralho publicado no Firestore: versão " + preparado.baralho.versao +
        ", " + preparado.baralho.cards.length + " cards.",
      "ok"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function retirarBaralhoDoFirestore() {
  if (estado.ocupado) return;
  const baralho = estado.aberto.baralho;
  const confirmacao = window.prompt(
    "Para retirar este baralho do catálogo em nuvem, digite exatamente o id:\n\n" + baralho.id
  );
  if (confirmacao === null) return;
  if (confirmacao !== baralho.id) {
    avisar("O id digitado não confere. Nada foi alterado no Firestore.", "erro");
    return;
  }
  definirOcupado(true);
  avisar("Retirando o baralho do catálogo em nuvem...", "ocupado");
  try {
    await comCredencialFirebase(async (config, credencial) => {
      const caminho = "catalogo/" + encodeURIComponent(baralho.id);
      const existente = await obterDocumentoFirestore(config, credencial, caminho);
      if (!existente) throw new Error("Este baralho ainda não existe no Firestore.");
      const write = {
        update: {
          name: nomeDoDocumentoFirestore(config, caminho),
          fields: { publicado: { booleanValue: false } },
        },
        updateMask: { fieldPaths: ["publicado"] },
        updateTransforms: [
          { fieldPath: "atualizadoEm", setToServerValue: "REQUEST_TIME" },
        ],
      };
      await executarCommitFirestore(config, credencial, [write]);
    });
    avisar("Baralho retirado do catálogo em nuvem. A versão permanece recuperável.", "ok");
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

function montarFiltros() {
  const origem = elementos.filtroOrigem;
  const grupo = elementos.filtroGrupo;
  const origemAtual = origem.value;
  const grupoAtual = grupo.value;
  origem.replaceChildren(criar("option", "Todas"));
  origem.firstChild.value = "";
  estado.origens.forEach((item) => {
    const opcao = criar("option", item.rotulo);
    opcao.value = item.origem;
    origem.append(opcao);
  });
  const grupos = [...new Set(estado.inventario.map((item) => item.grupo))].sort();
  grupo.replaceChildren(criar("option", "Todos"));
  grupo.firstChild.value = "";
  grupos.forEach((nome) => {
    const opcao = criar("option", nome);
    opcao.value = nome;
    grupo.append(opcao);
  });
  origem.value = origemAtual;
  grupo.value = grupoAtual;
}

function renderizarAvisos() {
  elementos.avisos.replaceChildren();
  estado.avisos.forEach((texto) => elementos.avisos.append(criar("li", texto)));
}

function combina(item, termo) {
  if (!termo) return true;
  const alvo = [item.nome, item.grupo, item.categoria, ...(item.respostas || [])]
    .join(" ")
    .toLowerCase();
  return alvo.includes(termo);
}

function filtrar() {
  const termo = elementos.busca.value.trim().toLowerCase();
  const origem = elementos.filtroOrigem.value;
  const grupo = elementos.filtroGrupo.value;
  const situacao = elementos.filtroSituacao.value;
  const tecnicos = elementos.mostrarTecnicos.checked;
  return estado.inventario.filter((item) => {
    if (item.tecnico && !tecnicos) return false;
    if (origem && item.origem !== origem) return false;
    if (grupo && item.grupo !== grupo) return false;
    if (situacao === "editavel" && !item.editavel) return false;
    if (situacao === "protegido" && !item.protegido) return false;
    if (situacao === "rascunho" && !item.temRascunho) return false;
    return combina(item, termo);
  });
}

function selo(texto, classe) {
  return criar("span", texto, "selo " + classe);
}

function renderizarLista() {
  const itens = filtrar();
  elementos.lista.replaceChildren();
  const ocultos = estado.inventario.filter((item) => item.tecnico).length;
  elementos.resumo.textContent =
    itens.length + " de " + estado.inventario.length + " baralho(s)" +
    (elementos.mostrarTecnicos.checked || ocultos === 0
      ? ""
      : " — " + ocultos + " técnico(s) oculto(s)");
  if (itens.length === 0) {
    elementos.lista.append(criar("li", "Nenhum baralho com esses filtros.", "vazio"));
    return;
  }
  itens.forEach((item) => {
    const botao = criar("button", null, "item");
    botao.type = "button";
    botao.disabled = estado.ocupado;
    botao.setAttribute("aria-current", item.chave === estado.chave ? "true" : "false");
    botao.append(criar("span", item.nome, "nome"));
    const selos = criar("span");
    selos.append(selo(item.origemRotulo, "selo-origem"));
    if (item.protegido) selos.append(selo("Estado incompatível", "selo-protegido"));
    if (item.temRascunho) selos.append(selo("Rascunho", "selo-rascunho"));
    if (item.tecnico) selos.append(selo("Técnico", "selo-tecnico"));
    botao.append(selos);
    botao.append(
      criar(
        "span",
        item.grupo + " • " + item.quantidadeDeCards + " card(s) • " + item.estadoAmigavel,
        "meta"
      )
    );
    (item.observacoes || []).forEach((texto) => botao.append(criar("span", texto, "meta")));
    botao.addEventListener("click", () => abrirBaralho(item.chave));
    const linha = criar("li");
    linha.append(botao);
    elementos.lista.append(linha);
  });
}

// ---- editor ---------------------------------------------------------------

async function abrirBaralho(chave) {
  if (estado.ocupado) {
    avisar("Aguarde a operação atual terminar.", "atencao");
    return false;
  }
  if (estado.alterado) {
    const seguir = window.confirm(
      "Há alterações não salvas neste baralho. Abrir novamente descarta o que está na tela. Continuar?"
    );
    if (!seguir) return false;
  }
  const sequencia = ++estado.sequenciaDeAbertura;
  avisar("Abrindo o baralho...", "ocupado");
  try {
    const dados = await pedir("/api/baralho?chave=" + encodeURIComponent(chave));
    if (sequencia !== estado.sequenciaDeAbertura) return false;
    adotarBaralhoAberto(chave, dados);
    avisar(dados.temRascunho ? "Rascunho recuperado." : "Baralho aberto.", "ok");
    return true;
  } catch (erro) {
    if (sequencia === estado.sequenciaDeAbertura) avisar(erro.message, "erro");
    return false;
  }
}

function adotarBaralhoAberto(chave, dados) {
  estado.chave = chave;
  estado.aberto = dados;
  estado.alterado = false;
  estado.aprovado = Boolean(dados.aprovado);
  estado.plano = dados.plano;
  reindexarFeedbacksNuvem();
  renderizarEditor();
  renderizarLista();
}

function adotarRespostaDoRascunho(dados) {
  estado.aberto.baralho = dados.baralho;
  estado.aberto.revisao = dados.revisao;
  estado.aberto.atualizadoEm = dados.atualizadoEm;
  estado.aberto.temRascunho = true;
  estado.alterado = false;
  estado.aprovado = Boolean(dados.aprovado);
  estado.plano = dados.plano;
}

function detalhesTecnicos(dados) {
  const bloco = document.createElement("details");
  bloco.append(criar("summary", "Detalhes técnicos"));
  const lista = document.createElement("dl");
  const linhas = [
    ["Id do baralho", dados.baralho.id],
    ["Versão", dados.baralho.versao],
    ["Categoria interna", dados.baralho.categoria],
    ["Estado", dados.baralho.estado],
    ["Id do agrupamento", (dados.baralho.colecao || {}).id],
  ];
  if (dados.entradaDoIndice) {
    linhas.push(["Índice: versão", dados.entradaDoIndice.versao]);
    linhas.push(["Índice: cards", dados.entradaDoIndice.quantidadeDeCards]);
    linhas.push(["Índice: bytes", dados.entradaDoIndice.tamanhoEmBytes]);
  }
  linhas.forEach(([rotulo, valor]) => {
    lista.append(criar("dt", rotulo), criar("dd", valor === undefined ? "—" : valor));
  });
  bloco.append(lista);
  return bloco;
}

function cabecalhoDoEditor(dados) {
  const caixa = document.createElement("div");
  caixa.append(criar("h3", dados.baralho.nome || dados.baralho.id));
  caixa.append(criar("p", dados.origemRotulo + " — " + dados.origemDetalhe, "nota"));
  if (dados.protegido) {
    caixa.append(
      criar(
        "p",
        "O estado técnico deste arquivo não é reconhecido. O painel mostra o conteúdo, mas não edita.",
        "nota"
      )
    );
  }
  if (dados.tipo === "novo") {
    caixa.append(
      criar(
        "p",
        "Rascunho local. Validar e exportar funcionam; entrar no catálogo ou no app é uma integração deliberada, feita fora daqui.",
        "nota"
      )
    );
  }
  return caixa;
}

function metadadosDoBaralho(dados) {
  const baralho = dados.baralho;
  const caixa = document.createElement("div");
  const somenteLeitura = dados.protegido;

  const nome = document.createElement("input");
  nome.value = baralho.nome || "";
  nome.maxLength = 120;
  nome.disabled = somenteLeitura;
  nome.addEventListener("input", () => {
    baralho.nome = nome.value;
    marcarAlterado();
  });
  caixa.append(campo("Nome do baralho", nome, "baralho-nome"));

  const colecao = baralho.colecao || (baralho.colecao = {});
  const grupo = document.createElement("input");
  grupo.value = colecao.nome || "";
  grupo.maxLength = 120;
  grupo.disabled = somenteLeitura;
  grupo.addEventListener("input", () => {
    colecao.nome = grupo.value;
    marcarAlterado();
  });
  caixa.append(campo("Nome do agrupamento", grupo, "baralho-grupo"));

  const icone = document.createElement("input");
  icone.value = colecao.icone || "";
  icone.maxLength = 8;
  icone.disabled = somenteLeitura;
  icone.addEventListener("input", () => {
    colecao.icone = icone.value;
    marcarAlterado();
  });
  caixa.append(campo("Ícone do agrupamento", icone, "baralho-icone"));

  caixa.append(
    criar("p", "Id, versão e estado não são editáveis: são a identidade do conteúdo.", "nota")
  );
  return caixa;
}

function normalizar(texto) {
  return String(texto || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

function feedbacksDaDica(cardId, indice) {
  const porCard = ((estado.aberto || {}).feedbacks || {}).porCard || {};
  const dicas = porCard[cardId] || [];
  const locais = dicas[indice] || [];
  const nuvem = (estado.feedbacksNuvemPorCard[cardId] || [])[indice] || [];
  const unicos = new Map();
  [...locais, ...nuvem].forEach((item) => {
    const chave = [item.dicaId, item.voto, item.comentario || "", item.criadoEm || ""].join("|");
    if (!unicos.has(chave) || item.listaId === "firestore") unicos.set(chave, item);
  });
  return [...unicos.values()].sort(
    (a, b) => String(b.atualizadoEm || b.criadoEm).localeCompare(
      String(a.atualizadoEm || a.criadoEm)
    )
  );
}

function formatarData(valor) {
  if (!valor) return "data não informada";
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? valor : data.toLocaleString("pt-BR");
}

function pedidoParaCodex(card, indice, textoAtual, itens) {
  const baralho = estado.aberto.baralho;
  const dicaId = itens.map((item) => item.dicaId).find(Boolean) || "sem id estável";
  const feedbacks = itens.map((item, posicao) => {
    const comentario = item.comentario || "sem comentário";
    const snapshot = item.textoAvaliado && item.textoAvaliado !== textoAtual
      ? " | texto avaliado: " + item.textoAvaliado
      : "";
    return (
      (posicao + 1) + ". [" + item.listaNome + "] " + item.voto +
      " em " + (item.criadoEm || "data não informada") + ": " + comentario + snapshot
    );
  }).join("\n");
  return [
    "Você está trabalhando no repositório QuemSou.",
    "Leia docs/CARDS_GUIDE.md e as regras editoriais vigentes antes de responder.",
    "Reescreva somente a dica abaixo; não altere o id do fato, o card ou as outras dicas.",
    "",
    "Baralho: " + (baralho.nome || "") + " (" + baralho.id + ")",
    "Card: " + card.id,
    "Resposta correta: " + (card.answer || ""),
    "Dica: " + (indice + 1),
    "Id da dica/fato: " + dicaId,
    "Texto atual: " + textoAtual,
    "",
    "Feedbacks recebidos:",
    feedbacks,
    "",
    "Requisitos:",
    "- produzir texto original, sem copiar fala ou trecho de obra;",
    "- não incluir a resposta, nem uma variação óbvia dela, na dica;",
    "- corrigir o problema apontado sem tornar a dica ambígua ou fácil demais;",
    "- preservar o id da dica/fato;",
    "- devolver exatamente uma sugestão, seguida de uma justificativa curta.",
    "",
    "Formato da resposta:",
    "Dica revisada: <texto>",
    "Justificativa: <uma frase>",
  ].join("\n");
}

function baixarPedido(texto, cardId, indice) {
  const blob = new Blob([texto], { type: "text/markdown;charset=utf-8" });
  const endereco = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = "pedido-codex-" + cardId + "-dica-" + (indice + 1) + ".md";
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(endereco);
}

async function copiarPedidoParaCodex(card, indice, textoAtual, itens) {
  const texto = pedidoParaCodex(card, indice, textoAtual, itens);
  try {
    await navigator.clipboard.writeText(texto);
    avisar("Pedido copiado. Cole no Codex; nenhuma dica foi alterada automaticamente.", "ok");
  } catch (_) {
    baixarPedido(texto, card.id, indice);
    avisar("O navegador bloqueou a cópia. O pedido foi baixado em arquivo .md.", "atencao");
  }
}

function painelDeFeedbacks(card, indice, obterTextoAtual) {
  const itens = feedbacksDaDica(card.id, indice);
  const textoAtual = obterTextoAtual();
  if (itens.length === 0) {
    return criar("p", "Sem feedback para esta dica.", "feedback-vazio");
  }
  const caixa = document.createElement("details");
  caixa.className = "feedback-da-dica";
  caixa.open = true;
  const listas = new Set(itens.map((item) => item.listaId));
  caixa.append(
    criar(
      "summary",
      itens.length + " feedback(s) em " + listas.size + " fonte(s)"
    )
  );
  const grupos = new Map();
  itens.forEach((item) => {
    if (!grupos.has(item.listaId)) grupos.set(item.listaId, []);
    grupos.get(item.listaId).push(item);
  });
  grupos.forEach((feedbacks) => {
    const grupo = criar("section", null, "grupo-de-feedbacks");
    grupo.append(criar("h5", feedbacks[0].listaNome));
    feedbacks.forEach((item) => {
      const linha = criar("article", null, "feedback-item");
      linha.dataset.voto = item.voto;
      linha.append(criar("strong", item.voto === "BOM" ? "Boa" : "Precisa melhorar"));
      linha.append(criar("span", formatarData(item.criadoEm), "feedback-data"));
      linha.append(criar("p", item.comentario || "Sem comentário."));
      if (item.textoAvaliado && item.textoAvaliado !== textoAtual) {
        linha.append(criar("p", "Texto avaliado: “" + item.textoAvaliado + "”", "nota"));
      }
      grupo.append(linha);
    });
    caixa.append(grupo);
  });
  const copiar = botao("Copiar pedido para o Codex", "pedido-codex", () =>
    copiarPedidoParaCodex(card, indice, obterTextoAtual(), itens)
  );
  caixa.append(copiar);
  return caixa;
}

function editorDeCard(card, posicao, somenteLeitura, permitirRemover) {
  const bloco = document.createElement("fieldset");
  bloco.className = "card";
  bloco.append(criar("legend", "Card " + (posicao + 1) + " — " + card.id));

  const resposta = document.createElement("input");
  resposta.value = card.answer || "";
  resposta.maxLength = 500;
  resposta.disabled = somenteLeitura;
  resposta.addEventListener("input", () => {
    card.answer = resposta.value;
    marcarAlterado();
  });
  bloco.append(campo("Resposta", resposta, "card-" + posicao + "-resposta"));

  const tipo = document.createElement("select");
  ["PESSOA", "LUGAR", "COISA"].forEach((valor) => {
    const opcao = criar("option", valor);
    opcao.value = valor;
    tipo.append(opcao);
  });
  tipo.value = card.type || "PESSOA";
  tipo.disabled = somenteLeitura;
  tipo.addEventListener("change", () => {
    card.type = tipo.value;
    marcarAlterado();
  });
  bloco.append(campo("Tipo", tipo, "card-" + posicao + "-tipo"));

  const dicas = criar("div", null, "dicas");
  const clues = card.clues || (card.clues = []);
  for (let indice = 0; indice < 10; indice += 1) {
    if (clues[indice] === undefined) clues[indice] = "";
    const area = document.createElement("textarea");
    area.value = clues[indice];
    area.rows = 2;
    area.maxLength = 500;
    area.disabled = somenteLeitura;
    area.addEventListener("input", () => {
      clues[indice] = area.value;
      marcarAlterado();
    });
    const blocoDaDica = campo(
      "Dica " + (indice + 1),
      area,
      "card-" + posicao + "-dica-" + indice
    );
    blocoDaDica.append(painelDeFeedbacks(card, indice, () => area.value));
    dicas.append(blocoDaDica);
  }
  bloco.append(dicas);

  const banco = card.bancoDeDicas;
  if (Array.isArray(banco) && banco.length > 0) {
    const usadas = new Set(clues.map(normalizar));
    const extras = banco.filter((fato) => !usadas.has(normalizar(fato.texto)));
    const caixa = document.createElement("details");
    caixa.append(
      criar(
        "summary",
        "Banco de dicas: " + banco.length + " fatos (" + extras.length + " além das dez)"
      )
    );
    caixa.append(
      criar(
        "p",
        "Corrigir o texto mantém o id do fato. As dez dicas acima também são fatos deste banco e ficam sincronizadas.",
        "nota"
      )
    );
    extras.forEach((fato, ordem) => {
      const area = document.createElement("textarea");
      area.value = fato.texto || "";
      area.rows = 2;
      area.maxLength = 500;
      area.disabled = somenteLeitura;
      area.addEventListener("input", () => {
        fato.texto = area.value;
        marcarAlterado();
      });
      caixa.append(campo("Fato " + fato.id, area, "card-" + posicao + "-fato-" + ordem));
    });
    bloco.append(caixa);
  }

  if (permitirRemover) {
    const remover = criar("button", "Remover este card", "perigo");
    remover.type = "button";
    remover.addEventListener("click", () => removerCard(card.id));
    bloco.append(remover);
  }
  return bloco;
}

// ---- acoes ----------------------------------------------------------------

function botao(texto, classe, acao) {
  const elemento = criar("button", texto, classe);
  elemento.type = "button";
  elemento.addEventListener("click", () => acao());
  return elemento;
}

function acoesDoEditor(dados) {
  const caixa = criar("div", null, "acoes");
  const botoes = {};
  botoes.salvar = botao("Salvar rascunho", "principal", salvarRascunho);
  botoes.descartar = botao("Descartar rascunho", "perigo", descartarRascunho);
  botoes.validar = botao("Validar pela régua do app", "", validar);
  caixa.append(botoes.salvar, botoes.descartar, botoes.validar);
  if (dados.tipo === "novo") {
    botoes.card = botao("Adicionar card", "", adicionarCard);
    botoes.exportar = botao("Exportar JSON", "", exportar);
    caixa.append(botoes.card, botoes.exportar);
  } else {
    botoes.aplicar = botao("Aplicar na origem local", "", aplicar);
    botoes.removerBaralho = botao(
      "Remover baralho da origem local",
      "perigo",
      removerBaralho
    );
    caixa.append(botoes.aplicar, botoes.removerBaralho);
  }
  botoes.publicarFirestore = botao(
    "Publicar/atualizar no Firestore",
    "",
    publicarBaralhoNoFirestore
  );
  botoes.retirarFirestore = botao(
    "Retirar do catálogo no Firestore",
    "perigo",
    retirarBaralhoDoFirestore
  );
  caixa.append(botoes.publicarFirestore, botoes.retirarFirestore);
  estado.botoes = botoes;
  const nota = criar("p", "", "nota");
  nota.id = "nota-acoes";
  caixa.append(nota);
  estado.notaDeAcoes = nota;
  return caixa;
}

function motivoDeNaoAplicar() {
  const dados = estado.aberto;
  if (!dados || dados.tipo === "novo") return "";
  if (dados.protegido) return "Estado técnico incompatível: nada é aplicado.";
  if (estado.alterado) return "Salve e valide antes de aplicar.";
  if (estado.plano && estado.plano.conflito) return estado.plano.motivo;
  if (estado.plano && estado.plano.semAlteracao) return estado.plano.motivo;
  if (!estado.aprovado) return "Valide este conteúdo para liberar a aplicação.";
  return "";
}

function atualizarAcoes() {
  const botoes = estado.botoes;
  const dados = estado.aberto;
  if (!botoes || !dados) return;
  botoes.salvar.disabled = dados.protegido || !estado.alterado || estado.ocupado;
  botoes.descartar.disabled = !dados.temRascunho || estado.ocupado;
  botoes.validar.disabled = estado.ocupado;
  if (botoes.aplicar) {
    const motivo = motivoDeNaoAplicar();
    botoes.aplicar.disabled = Boolean(motivo) || estado.ocupado;
    if (estado.notaDeAcoes) estado.notaDeAcoes.textContent = motivo;
  }
  if (botoes.card) botoes.card.disabled = estado.ocupado;
  if (botoes.removerBaralho) {
    botoes.removerBaralho.disabled = estado.ocupado || estado.alterado;
    botoes.removerBaralho.title = estado.alterado
      ? "Salve ou descarte as alterações antes de remover a origem."
      : "Cria backup e remove somente esta origem local.";
  }
  if (botoes.exportar) {
    botoes.exportar.disabled = estado.ocupado || estado.alterado || !estado.aprovado;
  }
  const nuvemIndisponivel = !estado.firebase.configurado || estado.firebase.status !== "pronto";
  if (botoes.publicarFirestore) {
    const origemAplicada =
      dados.tipo !== "novo" && estado.plano && estado.plano.semAlteracao &&
      !estado.plano.conflito;
    botoes.publicarFirestore.disabled =
      estado.ocupado || estado.alterado || !estado.aprovado || dados.protegido ||
      !origemAplicada || nuvemIndisponivel;
    botoes.publicarFirestore.title = !origemAplicada
      ? "Aplique primeiro as alterações na origem local e valide novamente."
      : estado.aprovado
        ? "Publica uma versão imutável e aponta o catálogo para ela."
        : "Valide o conteúdo exato antes de publicar.";
  }
  if (botoes.retirarFirestore) {
    botoes.retirarFirestore.disabled =
      estado.ocupado || dados.tipo === "novo" || nuvemIndisponivel;
    botoes.retirarFirestore.title =
      "Retira do catálogo sem apagar as versões armazenadas.";
  }
}

function definirOcupado(valor) {
  estado.ocupado = valor;
  elementos.importarFeedback.disabled = valor;
  elementos.atualizarNuvem.disabled = valor || !estado.firebase.configurado;
  atualizarAcoes();
  renderizarLista();
}

function montarAlteracoes() {
  const baralho = estado.aberto.baralho;
  const colecao = baralho.colecao || {};
  return {
    nome: baralho.nome,
    colecao: { nome: colecao.nome, icone: colecao.icone },
    cards: (baralho.cards || []).map((card) => {
      const saida = {
        id: card.id,
        answer: card.answer,
        type: card.type,
        clues: card.clues,
      };
      if (Array.isArray(card.bancoDeDicas)) {
        saida.bancoDeDicas = card.bancoDeDicas.map((fato) => ({
          id: fato.id,
          texto: fato.texto,
        }));
      }
      return saida;
    }),
  };
}

async function salvarRascunho(interno = false) {
  if (!interno && estado.ocupado) return false;
  if (!interno) definirOcupado(true);
  avisar("Salvando o rascunho...", "ocupado");
  try {
    const dados = await enviar("/api/rascunho", {
      chave: estado.chave,
      revisao: estado.aberto.revisao,
      alteracoes: montarAlteracoes(),
    });
    adotarRespostaDoRascunho(dados);
    await carregarInventario(true);
    renderizarEditor();
    if (!interno) {
      avisar("Rascunho salvo no diretório privado. Nenhuma origem foi alterada.", "ok");
    }
    return true;
  } catch (erro) {
    avisar(erro.message, "erro");
    return false;
  } finally {
    if (!interno) definirOcupado(false);
  }
}

async function descartarRascunho() {
  const seguir = window.confirm(
    "Descartar o rascunho apaga só o trabalho não aplicado. O baralho de origem continua intacto. Continuar?"
  );
  if (!seguir) return;
  if (estado.ocupado) return;
  definirOcupado(true);
  const chave = estado.chave;
  const eraNovo = estado.aberto.tipo === "novo";
  try {
    await enviar("/api/rascunho/descartar", {
      chave: chave,
      revisao: estado.aberto.revisao,
    });
    await carregarInventario(true);
    if (eraNovo) {
      estado.chave = null;
      estado.aberto = null;
      estado.alterado = false;
      estado.aprovado = false;
      estado.plano = null;
      renderizarEditor();
      renderizarLista();
    } else {
      const dados = await pedir("/api/baralho?chave=" + encodeURIComponent(chave));
      adotarBaralhoAberto(chave, dados);
    }
    avisar("Rascunho descartado. A origem não foi tocada.", "ok");
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function validar() {
  if (estado.ocupado) return;
  definirOcupado(true);
  try {
    if (estado.alterado && !(await salvarRascunho(true))) return;
    estado.validando = true;
    atualizarAcoes();
    avisar("Validando com a régua do app (Gradle). Isso pode demorar.", "ocupado");
    const trabalho = await enviar("/api/validar", {
      chave: estado.chave,
      revisao: estado.aberto.revisao,
    });
    const resultado = await acompanharValidacao(trabalho.id);
    const dados = await pedir("/api/baralho?chave=" + encodeURIComponent(estado.chave));
    adotarBaralhoAberto(estado.chave, dados);
    const detalhe = (resultado.mensagens || []).slice(0, 5).join(" | ");
    avisar(
      resultado.resumo + (detalhe ? " " + detalhe : ""),
      resultado.estado === "aprovado" ? "ok" : "erro"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    estado.validando = false;
    definirOcupado(false);
  }
}

async function acompanharValidacao(identificador) {
  const finais = ["aprovado", "reprovado", "falha", "tempo_esgotado"];
  for (;;) {
    await new Promise((resolve) => window.setTimeout(resolve, 1500));
    const trabalho = await pedir("/api/validacao?id=" + encodeURIComponent(identificador));
    if (!finais.includes(trabalho.estado)) {
      avisar(
        trabalho.estado === "na_fila"
          ? "Na fila: outra validação está usando o Gradle."
          : "Validando com a régua do app (Gradle)...",
        "ocupado"
      );
      continue;
    }
    return trabalho;
  }
}

async function aplicar() {
  const dados = estado.aberto;
  const seguir = window.confirm(
    "Aplicar em: " + dados.origemRotulo + ".\n\n" +
      dados.origemDetalhe + "\n\n" +
      "O arquivo local será reescrito, com backup e com a versão incrementada. Continuar?"
  );
  if (!seguir) return;
  if (estado.ocupado) return;
  definirOcupado(true);
  avisar("Aplicando na origem local...", "ocupado");
  try {
    const relatorio = await enviar("/api/aplicar", {
      chave: estado.chave,
      revisao: estado.aberto.revisao,
      confirmacao: true,
    });
    await carregarInventario(true);
    const dados = await pedir("/api/baralho?chave=" + encodeURIComponent(estado.chave));
    adotarBaralhoAberto(estado.chave, dados);
    avisar(
      "Aplicado. Versão " + relatorio.versaoAnterior + " → " + relatorio.versaoNova +
        ". Backup em " + relatorio.backup + ". " + (relatorio.avisos || []).join(" "),
      "ok"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function adicionarCard() {
  if (estado.ocupado) return;
  definirOcupado(true);
  try {
    if (estado.alterado && !(await salvarRascunho(true))) return;
    const dados = await enviar("/api/rascunho/card", {
      chave: estado.chave,
      revisao: estado.aberto.revisao,
    });
    adotarRespostaDoRascunho(dados);
    await carregarInventario(true);
    renderizarEditor();
    avisar("Card " + dados.cardId + " criado no rascunho.", "ok");
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function removerCard(cardId) {
  const seguir = window.confirm("Remover o card " + cardId + " deste rascunho?");
  if (!seguir) return;
  if (estado.ocupado) return;
  definirOcupado(true);
  try {
    if (estado.alterado && !(await salvarRascunho(true))) return;
    const dados = await enviar("/api/rascunho/card/remover", {
      chave: estado.chave,
      revisao: estado.aberto.revisao,
      cardId: cardId,
    });
    adotarRespostaDoRascunho(dados);
    await carregarInventario(true);
    renderizarEditor();
    avisar("Card removido do rascunho.", "ok");
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function removerBaralho() {
  if (estado.ocupado) return;
  if (estado.alterado) {
    avisar("Salve ou descarte as alterações antes de remover o baralho.", "atencao");
    return;
  }
  const dados = estado.aberto;
  const baralhoId = dados.baralho.id;
  const seguir = window.confirm(
    "Remover “" + (dados.baralho.nome || baralhoId) + "” de " + dados.origemRotulo + "?\n\n" +
      "O painel criará um backup antes da remoção. Isso não altera o GitHub nem o telefone."
  );
  if (!seguir) return;
  const confirmacao = window.prompt(
    "Para confirmar, digite exatamente o id do baralho:\n" + baralhoId
  );
  if (confirmacao === null) return;
  definirOcupado(true);
  avisar("Criando backup e removendo o baralho da origem local...", "ocupado");
  try {
    const relatorio = await enviar("/api/baralho/remover", {
      chave: estado.chave,
      revisao: dados.revisao,
      confirmacao: confirmacao,
    });
    estado.chave = null;
    estado.aberto = null;
    estado.alterado = false;
    estado.aprovado = false;
    estado.plano = null;
    await carregarInventario(true);
    renderizarEditor();
    renderizarLista();
    avisar(
      "Baralho removido da origem local. Backup recuperável em " + relatorio.backup + ".",
      "ok"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

async function importarListasDeFeedback(arquivos) {
  if (estado.ocupado || arquivos.length === 0) return;
  definirOcupado(true);
  const resultados = [];
  try {
    for (const arquivo of arquivos) {
      let conteudo;
      try {
        conteudo = JSON.parse(await arquivo.text());
      } catch (_) {
        throw new Error(arquivo.name + " não é um JSON válido.");
      }
      resultados.push(
        await enviar("/api/feedbacks/importar", { nome: arquivo.name, conteudo: conteudo })
      );
    }
    await carregarInventario(true);
    if (estado.chave && estado.aberto) {
      const atualizado = await pedir("/api/baralho?chave=" + encodeURIComponent(estado.chave));
      estado.aberto.feedbacks = atualizado.feedbacks;
      renderizarEditor();
    }
    const novas = resultados.filter((item) => !item.repetida).length;
    const repetidas = resultados.length - novas;
    avisar(
      novas + " lista(s) importada(s)" +
        (repetidas ? "; " + repetidas + " já existia(m) e não foi(ram) duplicada(s)." : "."),
      "ok"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    elementos.arquivoFeedback.value = "";
    definirOcupado(false);
  }
}

async function exportar() {
  if (estado.ocupado) return;
  definirOcupado(true);
  try {
    if (estado.alterado && !(await salvarRascunho(true))) return;
    const resposta = await fetch("/api/exportar", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Token-Central": TOKEN },
      body: JSON.stringify({ chave: estado.chave, revisao: estado.aberto.revisao }),
    });
    if (!resposta.ok) {
      const falha = await resposta.json().catch(() => ({}));
      throw new Error(falha.mensagem || "Não foi possível exportar o JSON.");
    }
    const nomeCabecalho = resposta.headers.get("Content-Disposition") || "";
    const encontrado = /filename="([a-z0-9._-]+)"/i.exec(nomeCabecalho);
    const nome = encontrado ? encontrado[1] : "baralho.json";
    const endereco = URL.createObjectURL(await resposta.blob());
    const link = document.createElement("a");
    link.href = endereco;
    link.download = nome;
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(endereco);
    avisar(
      "JSON validado e exportado. Integrar ao app ou ao catálogo continua sendo um passo deliberado, fora do painel.",
      "ok"
    );
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
}

function renderizarEditor() {
  const dados = estado.aberto;
  elementos.editorVazio.hidden = Boolean(dados);
  elementos.editor.hidden = !dados;
  if (!dados) return;
  const raiz = elementos.editor;
  raiz.replaceChildren();
  raiz.append(cabecalhoDoEditor(dados));
  raiz.append(acoesDoEditor(dados));
  if (dados.tipo !== "novo") {
    const projetar = criar("button", "Trazer bancos de dicas do Firestore para este baralho");
    projetar.type = "button";
    projetar.id = "projetar-acervo-no-baralho";
    projetar.addEventListener("click", () => projetarAcervoNoBaralhoAberto());
    raiz.append(projetar);
  }
  raiz.append(detalhesTecnicos(dados));
  raiz.append(metadadosDoBaralho(dados));
  const cards = dados.baralho.cards || [];
  if (cards.length === 0) {
    raiz.append(criar("p", "Este baralho ainda não tem cards.", "vazio"));
  }
  cards.forEach((card, posicao) => {
    raiz.append(editorDeCard(card, posicao, dados.protegido, dados.tipo === "novo"));
  });
  atualizarAcoes();
}

// ---- inicio ---------------------------------------------------------------

[elementos.busca, elementos.filtroOrigem, elementos.filtroGrupo, elementos.filtroSituacao].forEach(
  (controle) => controle.addEventListener("input", renderizarLista)
);
elementos.mostrarTecnicos.addEventListener("change", renderizarLista);
elementos.importarFeedback.addEventListener("click", () => elementos.arquivoFeedback.click());
elementos.arquivoFeedback.addEventListener("change", () =>
  importarListasDeFeedback([...elementos.arquivoFeedback.files])
);
elementos.atualizarNuvem.addEventListener("click", () => atualizarFeedbacksDaNuvem());
elementos.copiarUidNuvem.addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(estado.firebase.uid || "");
    avisar("UID do painel copiado.", "ok");
  } catch (_) {
    avisar("Não foi possível copiar o UID; selecione-o no aviso da nuvem.", "atencao");
  }
});

elementos.formNovo.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  if (estado.ocupado) return;
  if (estado.alterado) {
    const seguir = window.confirm(
      "Há alterações não salvas no editor. Criar outro rascunho descarta o que está na tela. Continuar?"
    );
    if (!seguir) return;
  }
  definirOcupado(true);
  try {
    const dados = await enviar("/api/rascunho/novo", {
      nome: document.getElementById("novo-nome").value,
      grupo: document.getElementById("novo-grupo").value,
      icone: document.getElementById("novo-icone").value,
      categoria: document.getElementById("novo-categoria").value,
    });
    elementos.formNovo.reset();
    document.getElementById("novo-icone").value = "⭐";
    await carregarInventario(true);
    const aberto = await pedir("/api/baralho?chave=" + encodeURIComponent(dados.chave));
    adotarBaralhoAberto(dados.chave, aberto);
    avisar("Rascunho criado. Ele só existe nesta máquina.", "ok");
  } catch (erro) {
    avisar(erro.message, "erro");
  } finally {
    definirOcupado(false);
  }
});

carregarInventario().catch((erro) => avisar(erro.message, "erro"));
iniciarFeedbacksDaNuvem().catch((erro) => {
  estado.firebase.status = "erro";
  estado.firebase.motivo = erro.message;
  renderizarEstadoDaNuvem();
});
setInterval(() => {
  if (!document.hidden) atualizarFeedbacksDaNuvem(true);
}, 30000);
