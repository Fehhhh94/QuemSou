// Acervo editorial - camada de DOM. Depende de acervo.js e acervo-firestore.js
// (carregados antes) e reaproveita TOKEN/pedir/avisar/comCredencialFirebase/
// estado.firebase de app.js (mesmo escopo global, sem módulo). Nenhuma
// escrita remota acontece sem uma ação explícita do formulário.
"use strict";

const estadoDoAcervo = {
  previa: null,
  buscaDeResposta: "",
  paginaDeRespostas: 1,
  respostaId: null,
  aberto: null,
  remoto: null, // { resposta, dicas } quando já existe no Firestore; null senão.
  estadoRemoto: "nao_verificado", // nao_verificado | carregando | ausente | presente | erro
  buscaDeDica: "",
  paginaDeDicas: 1,
  ocupado: false,
  // Edições em andamento, preservadas entre re-renders (busca, paginação,
  // salvar outra dica, poll de feedback) até salvar com sucesso ou trocar
  // de resposta.
  edicoesPendentes: new Map(), // dicaId -> {texto, escopo}
  rascunhoDeNovaDica: { texto: "", escopo: null },
};

const elementosDoAcervo = {
  secao: document.getElementById("acervo-editorial"),
  resumo: document.getElementById("acervo-resumo"),
  conflitos: document.getElementById("acervo-conflitos"),
  avisos: document.getElementById("acervo-avisos"),
  atualizarPrevia: document.getElementById("acervo-atualizar-previa"),
  mostrarTecnicos: document.getElementById("acervo-mostrar-tecnicos"),
  migrarPrevia: document.getElementById("acervo-migrar-previa"),
  buscaResposta: document.getElementById("acervo-busca-resposta"),
  listaDeRespostas: document.getElementById("acervo-lista-respostas"),
  paginacaoRespostas: document.getElementById("acervo-paginacao-respostas"),
  editor: document.getElementById("acervo-editor"),
  editorVazio: document.getElementById("acervo-editor-vazio"),
};

function avisarAcervo(mensagem, tom) {
  if (typeof avisar === "function") avisar(mensagem, tom);
}

async function pedirAcervo(caminho) {
  const resposta = await fetch(caminho, { headers: { Accept: "application/json" } });
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new Error((dados && dados.mensagem) || "Falha ao consultar o acervo editorial.");
  }
  return dados;
}

async function enviarAcervo(caminho, corpo) {
  const resposta = await fetch(caminho, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Token-Central": TOKEN },
    body: JSON.stringify(corpo),
  });
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new Error((dados && dados.mensagem) || "Falha ao gravar no acervo editorial.");
  }
  return dados;
}

function definirOcupadoDoAcervo(valor) {
  estadoDoAcervo.ocupado = valor;
}

function firestoreDisponivel() {
  return Boolean(
    typeof estado !== "undefined" &&
    estado.firebase &&
    estado.firebase.configurado &&
    estado.firebase.config
  );
}

// ---- prévia local -----------------------------------------------------------

async function carregarPreviaDoAcervo() {
  const incluirTecnicos = elementosDoAcervo.mostrarTecnicos && elementosDoAcervo.mostrarTecnicos.checked;
  estadoDoAcervo.previa = await pedirAcervo(
    "/api/acervo/previa" + (incluirTecnicos ? "?tecnicos=1" : "")
  );
  renderizarResumoDoAcervo();
  renderizarListaDeRespostas();
}

function renderizarResumoDoAcervo() {
  const previa = estadoDoAcervo.previa;
  if (!previa) {
    elementosDoAcervo.resumo.textContent = "Carregando prévia da migração…";
    return;
  }
  elementosDoAcervo.resumo.textContent =
    previa.quantidadeDeRespostas + " resposta(s) • " + previa.quantidadeDeDicas +
    " dica(s) • " + previa.respostasComAliasLegado + " ainda sem respostaId explícito" +
    (previa.segura ? "" : " • respostas com conflito ficam fora da migração; veja abaixo");

  elementosDoAcervo.avisos.innerHTML = "";
  (previa.avisos || []).forEach((aviso) => {
    const item = document.createElement("li");
    item.textContent = aviso;
    elementosDoAcervo.avisos.append(item);
  });

  elementosDoAcervo.conflitos.innerHTML = "";
  (previa.conflitos || []).slice(0, 50).forEach((conflito) => {
    const item = document.createElement("li");
    item.textContent =
      conflito.tipo + ": " + (conflito.respostaId || (conflito.respostaIds || []).join(", ")) +
      (conflito.dicaId ? " / dica " + conflito.dicaId : "") +
      (conflito.baralhoId ? " (baralho " + conflito.baralhoId + ")" : "");
    elementosDoAcervo.conflitos.append(item);
  });
  (previa.bancosAcimaDoLimite || []).forEach((item) => {
    const linha = document.createElement("li");
    linha.textContent =
      "banco acima do teto: " + item.respostaId + " tem " + item.quantidade + " dicas (máximo 500).";
    elementosDoAcervo.conflitos.append(linha);
  });

  if (elementosDoAcervo.migrarPrevia) {
    elementosDoAcervo.migrarPrevia.disabled = !firestoreDisponivel();
  }
}

function respostasFiltradas() {
  const previa = estadoDoAcervo.previa;
  if (!previa) return [];
  const termo = AcervoEditorial.normalizar(estadoDoAcervo.buscaDeResposta);
  return Object.values(previa.respostas)
    .filter((resposta) => {
      if (!termo) return true;
      return (
        AcervoEditorial.normalizar(resposta.texto).includes(termo) ||
        resposta.respostaId.toLowerCase().includes(termo.replace(/ /g, ""))
      );
    })
    .sort((a, b) => a.texto.localeCompare(b.texto, "pt-BR"));
}

function botaoDePaginacao(rotulo, habilitado, aoClicar) {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.textContent = rotulo;
  botao.disabled = !habilitado;
  botao.addEventListener("click", aoClicar);
  return botao;
}

function renderizarListaDeRespostas() {
  const lista = respostasFiltradas();
  const pagina = AcervoEditorial.paginar(lista, estadoDoAcervo.paginaDeRespostas, 30);
  estadoDoAcervo.paginaDeRespostas = pagina.pagina;
  elementosDoAcervo.listaDeRespostas.innerHTML = "";
  pagina.itens.forEach((resposta) => {
    const item = document.createElement("li");
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "item";
    botao.setAttribute("aria-current", String(resposta.respostaId === estadoDoAcervo.respostaId));
    const nome = document.createElement("span");
    nome.className = "nome";
    nome.textContent = resposta.texto || resposta.respostaId;
    const meta = document.createElement("span");
    meta.className = "meta";
    meta.textContent =
      resposta.respostaId + " • " +
      (resposta.origem === "ALIAS_LEGADO" ? "alias legado" : "editorial");
    botao.append(nome, meta);
    botao.addEventListener("click", () => abrirRespostaNoAcervo(resposta.respostaId));
    item.append(botao);
    elementosDoAcervo.listaDeRespostas.append(item);
  });

  elementosDoAcervo.paginacaoRespostas.innerHTML = "";
  const resumoTexto = document.createElement("span");
  resumoTexto.textContent =
    lista.length === 0
      ? "Nenhuma resposta encontrada."
      : "Página " + pagina.pagina + " de " + pagina.totalDePaginas + " (" + pagina.total + " no total).";
  elementosDoAcervo.paginacaoRespostas.append(
    resumoTexto,
    botaoDePaginacao("« Anterior", pagina.pagina > 1, () => {
      estadoDoAcervo.paginaDeRespostas -= 1;
      renderizarListaDeRespostas();
    }),
    botaoDePaginacao("Próxima »", pagina.pagina < pagina.totalDePaginas, () => {
      estadoDoAcervo.paginaDeRespostas += 1;
      renderizarListaDeRespostas();
    })
  );
}

// ---- abrir uma resposta ------------------------------------------------------

async function abrirRespostaNoAcervo(respostaId) {
  if (estadoDoAcervo.ocupado) return;
  if (temEdicaoPendenteNoAcervo() && !window.confirm("Há dicas não salvas. Descartar o que está na tela e abrir a resposta?")) return;
  definirOcupadoDoAcervo(true);
  try {
    estadoDoAcervo.respostaId = respostaId;
    estadoDoAcervo.remoto = null;
    estadoDoAcervo.estadoRemoto = "nao_verificado";
    estadoDoAcervo.edicoesPendentes = new Map();
    estadoDoAcervo.rascunhoDeNovaDica = { texto: "", escopo: null };
    estadoDoAcervo.aberto = await pedirAcervo(
      "/api/acervo/resposta?respostaId=" + encodeURIComponent(respostaId)
    );
    estadoDoAcervo.buscaDeDica = "";
    estadoDoAcervo.paginaDeDicas = 1;
    renderizarListaDeRespostas();
    renderizarEditorDaResposta();
    await carregarBancoRemoto(respostaId);
  } catch (erro) {
    avisarAcervo(erro.message, "erro");
  } finally {
    definirOcupadoDoAcervo(false);
  }
}

async function recarregarRespostaAberta(preservarPendentes) {
  if (!estadoDoAcervo.respostaId) return;
  if (!preservarPendentes) estadoDoAcervo.edicoesPendentes = new Map();
  estadoDoAcervo.aberto = await pedirAcervo(
    "/api/acervo/resposta?respostaId=" + encodeURIComponent(estadoDoAcervo.respostaId)
  );
  renderizarEditorDaResposta();
}

/** Carrega o banco remoto completo antes de liberar a edição. Rascunhos
 * locais exigem reconciliação explícita; a revisão carregada é a base do
 * salvamento otimista, sem sobrescrever correções remotas silenciosamente.
 */
async function carregarBancoRemoto(respostaId) {
  if (!firestoreDisponivel()) {
    estadoDoAcervo.estadoRemoto = "indisponivel";
    renderizarStatusRemoto();
    return;
  }
  estadoDoAcervo.estadoRemoto = "carregando";
  renderizarStatusRemoto();
  try {
    const config = estado.firebase.config;
    const remoto = await comCredencialFirebase(async (cfg, credencial) =>
      AcervoFirestore.carregarRespostaRemota(fetch, cfg, credencial, respostaId)
    );
    if (estadoDoAcervo.respostaId !== respostaId) return; // usuário já trocou de resposta
    estadoDoAcervo.remoto = remoto;
    if (remoto && !remoto.completo) throw new Error("Migração incompleta. Execute novamente a migração antes de editar/projetar.");
    if (remoto && estadoDoAcervo.aberto.temRascunhoLocal) {
      const aceito = window.confirm("Há um rascunho local salvo. Usar o banco do Firestore para editar? O rascunho local será preservado no disco, sem ser enviado nem substituído.");
      if (!aceito) throw new Error("Escolha pendente: abra novamente para usar o remoto. Rascunho local preservado.");
    }
    estadoDoAcervo.estadoRemoto = remoto ? "presente" : "ausente";
    if (remoto) adotarBancoRemoto(remoto);
  } catch (erro) {
    if (estadoDoAcervo.respostaId !== respostaId) return;
    estadoDoAcervo.estadoRemoto = "erro";
    estadoDoAcervo.erroRemoto = erro.message;
  }
  renderizarEditorDaResposta();
}

function temEdicaoPendenteNoAcervo() {
  return estadoDoAcervo.edicoesPendentes.size > 0 || Boolean(estadoDoAcervo.rascunhoDeNovaDica.texto.trim());
}

function exigirAcervoEditavel() {
  if (firestoreDisponivel() && !["presente", "ausente"].includes(estadoDoAcervo.estadoRemoto)) {
    throw new Error("Reabra a resposta e confira o Firestore antes de editar. Nenhum rascunho foi descartado.");
  }
}

function adotarBancoRemoto(remoto) {
  estadoDoAcervo.remoto = remoto;
  const dicas = structuredClone(remoto.dicas);
  estadoDoAcervo.aberto = { ...estadoDoAcervo.aberto, resposta: structuredClone(remoto.resposta), dicas,
    quantidadeDeDicasAtivas: AcervoEditorial.contarAtivas(dicas),
    prontaParaPublicarPublico: AcervoEditorial.prontaParaPublicar(dicas, "PUBLICO"),
    prontaParaPublicarPrivado: AcervoEditorial.prontaParaPublicar(dicas, "PRIVADO"),
    escopoPadraoParaNovaDica: AcervoEditorial.escopoPadraoParaNovaDica(dicas), temRascunhoLocal: false };
}

async function gravarBancoRemoto(dicas) {
  exigirAcervoEditavel();
  const aberto = estadoDoAcervo.aberto;
  const base = estadoDoAcervo.remoto;
  try {
    await comCredencialFirebase(async (cfg, credencial) => {
      await AcervoFirestore.salvarRascunhoRemoto(fetch, cfg, credencial, aberto.respostaId,
        base ? base.resposta : aberto.resposta, Object.values(dicas), base);
      // Adota apenas após o commit. Falha de leitura mantém o formulário e
      // exige reabertura, nunca usa revisão nova para reenviar texto antigo.
      adotarBancoRemoto(await AcervoFirestore.carregarRespostaRemota(fetch, cfg, credencial, aberto.respostaId));
    });
    estadoDoAcervo.estadoRemoto = "presente";
  } catch (erro) {
    estadoDoAcervo.estadoRemoto = "erro";
    estadoDoAcervo.erroRemoto = "Não foi possível confirmar o salvamento. Preserve seu texto e reabra para conferir/reconciliar. " + erro.message;
    renderizarStatusRemoto();
    throw erro;
  }
}

window.addEventListener("beforeunload", evento => {
  if (temEdicaoPendenteNoAcervo() || estadoDoAcervo.ocupado) {
    evento.preventDefault();
    evento.returnValue = "";
  }
});

function renderizarStatusRemoto() {
  const elemento = document.getElementById("acervo-status-remoto");
  if (!elemento) return;
  const textos = {
    nao_verificado: "Verificando o Firestore…",
    indisponivel: "Sincronização com o Firestore não configurada neste painel (ver google-services.json).",
    carregando: "Consultando o banco remoto…",
    ausente: "Esta resposta ainda não existe no Firestore (trabalhando só com a prévia local).",
    presente:
      "Registrada no Firestore com " +
      Object.keys((estadoDoAcervo.remoto || {}).dicas || {}).length + " dica(s) remota(s).",
    erro: "Não foi possível consultar o Firestore: " + (estadoDoAcervo.erroRemoto || ""),
  };
  elemento.textContent = textos[estadoDoAcervo.estadoRemoto] || "";
}

// ---- lista de dicas -----------------------------------------------------------

function dicasFiltradasDoAcervo() {
  const aberto = estadoDoAcervo.aberto;
  if (!aberto) return [];
  const ordenadas = Object.values(aberto.dicas).sort((a, b) =>
    a.dicaId.localeCompare(b.dicaId, "pt-BR")
  );
  return AcervoEditorial.filtrarDicas(ordenadas, estadoDoAcervo.buscaDeDica);
}

/** Mescla feedback local (contingência importada) com o Firestore ao vivo
 * (já carregado por app.js em `estado.feedbacksNuvem`).
 *
 * O documento `feedbacks` no Firestore NÃO tem `respostaId` (só
 * `baralhoId`+`cardId`+`dicaId` — ver `SincronizacaoDeFeedback.kt`), e um
 * `dicaId` só é único DENTRO de uma resposta. Por isso o escopo aqui é
 * `referencias` (os pares baralhoId/cardId que originaram esta resposta na
 * migração) PRIMEIRO, `dicaId` depois — nunca dicaId sozinho.
 */
function feedbacksDaDicaAcervo(dicaId) {
  const aberto = estadoDoAcervo.aberto;
  const locais = ((aberto.feedbacks || {}).porDica || {})[dicaId] || [];
  const referencias = (aberto.resposta && aberto.resposta.referencias) || [];
  const nuvem = (typeof estado !== "undefined" ? estado.feedbacksNuvem || [] : [])
    .filter((item) =>
      item.dicaId === dicaId &&
      referencias.some((ref) => ref.baralhoId === item.baralhoId && ref.cardId === item.cardId)
    )
    .map((item) => ({ ...item, listaNome: "Firestore — automático" }));
  const unicos = new Map();
  [...locais, ...nuvem].forEach((item) => {
    const chave = [item.voto, item.comentario || "", item.criadoEm || ""].join("|");
    if (!unicos.has(chave) || item.listaNome === "Firestore — automático") unicos.set(chave, item);
  });
  return [...unicos.values()].sort((a, b) =>
    String(b.criadoEm || "").localeCompare(String(a.criadoEm || ""))
  );
}

function textoDeProntidao(aberto) {
  const publico = aberto.prontaParaPublicarPublico ? "sim" : "não";
  const privado = aberto.prontaParaPublicarPrivado ? "sim" : "não";
  return (
    aberto.quantidadeDeDicasAtivas + " dica(s) ativa(s) • pronta para publicar em PÚBLICO: " +
    publico + " • em PRIVADO: " + privado
  );
}

function linhaDeFeedback(item) {
  const linha = document.createElement("div");
  linha.className = "feedback-item";
  linha.dataset.voto = item.voto;
  const cabecalho = document.createElement("strong");
  cabecalho.textContent = item.voto === "BOM" ? "Boa dica" : "Dica fraca";
  const data = document.createElement("span");
  data.className = "feedback-data";
  data.textContent = " — " + (item.listaNome || "") + " — " + (item.criadoEm || "");
  linha.append(cabecalho, data);
  if (item.comentario) {
    const paragrafo = document.createElement("p");
    paragrafo.textContent = item.comentario;
    linha.append(paragrafo);
  }
  return linha;
}

function montarLinhaDeDica(dica, resposta, feedbacksDaDica) {
  const pendente = estadoDoAcervo.edicoesPendentes.get(dica.dicaId);
  const valorDoTexto = pendente ? pendente.texto : dica.texto;
  const valorDoEscopo = pendente ? pendente.escopo : dica.escopo;

  const artigo = document.createElement("fieldset");
  artigo.className = "card";
  const legenda = document.createElement("legend");
  legenda.textContent = dica.dicaId + (dica.status === "REMOVIDA" ? " (desativada)" : "");
  artigo.append(legenda);

  const idTextarea = "acervo-dica-texto-" + dica.dicaId;
  const idSelect = "acervo-dica-escopo-" + dica.dicaId;

  const rotuloTexto = document.createElement("label");
  rotuloTexto.setAttribute("for", idTextarea);
  rotuloTexto.textContent = "Texto da dica " + dica.dicaId;
  rotuloTexto.className = "campo-visualmente-oculto";

  const area = document.createElement("textarea");
  area.id = idTextarea;
  area.value = valorDoTexto;
  area.maxLength = AcervoEditorial.MAXIMO_DE_TEXTO_DA_DICA;
  const bloqueada = firestoreDisponivel() && !["presente", "ausente"].includes(estadoDoAcervo.estadoRemoto);
  area.disabled = dica.status === "REMOVIDA" || bloqueada;
  const contador = document.createElement("p");
  contador.className = "nota";
  const atualizarContador = () => {
    contador.textContent = area.value.length + "/" + AcervoEditorial.MAXIMO_DE_TEXTO_DA_DICA + " caracteres";
  };
  const marcarPendente = () => {
    estadoDoAcervo.edicoesPendentes.set(dica.dicaId, {
      texto: area.value,
      escopo: selecaoDeEscopo.value,
    });
  };
  area.addEventListener("input", () => {
    atualizarContador();
    marcarPendente();
  });
  atualizarContador();

  const rotuloEscopo = document.createElement("label");
  rotuloEscopo.setAttribute("for", idSelect);
  rotuloEscopo.textContent = "Escopo da dica " + dica.dicaId;
  rotuloEscopo.className = "campo-visualmente-oculto";

  const selecaoDeEscopo = document.createElement("select");
  selecaoDeEscopo.id = idSelect;
  [AcervoEditorial.ESCOPO_PUBLICO, AcervoEditorial.ESCOPO_PRIVADO].forEach((valor) => {
    const opcao = document.createElement("option");
    opcao.value = valor;
    opcao.textContent = valor;
    opcao.selected = valorDoEscopo === valor;
    selecaoDeEscopo.append(opcao);
  });
  selecaoDeEscopo.disabled = dica.status === "REMOVIDA" || bloqueada;
  selecaoDeEscopo.addEventListener("change", marcarPendente);

  const acoes = document.createElement("div");
  acoes.className = "acoes";

  const salvar = document.createElement("button");
  salvar.type = "button";
  salvar.textContent = "Salvar dica";
  salvar.disabled = bloqueada || dica.status === "REMOVIDA";
  salvar.addEventListener("click", () =>
    salvarEdicaoDeDica(dica.dicaId, area.value, selecaoDeEscopo.value)
  );

  const alternarStatus = document.createElement("button");
  alternarStatus.type = "button";
  alternarStatus.className = dica.status === "REMOVIDA" ? "" : "perigo";
  alternarStatus.textContent = dica.status === "REMOVIDA" ? "Reativar" : "Desativar";
  alternarStatus.disabled = bloqueada;
  alternarStatus.addEventListener("click", () => alternarStatusDaDica(dica.dicaId, dica.status));

  const copiarPedido = document.createElement("button");
  copiarPedido.className = "pedido-codex";
  copiarPedido.type = "button";
  copiarPedido.textContent = "Copiar pedido para o Codex";
  copiarPedido.addEventListener("click", () => copiarPedidoDoAcervo(resposta, dica, feedbacksDaDica));

  acoes.append(salvar, alternarStatus, copiarPedido);
  artigo.append(rotuloTexto, area, contador, rotuloEscopo, selecaoDeEscopo, acoes);

  if (feedbacksDaDica.length) {
    const grupo = document.createElement("div");
    grupo.className = "grupo-de-feedbacks";
    feedbacksDaDica.forEach((item) => grupo.append(linhaDeFeedback(item)));
    artigo.append(grupo);
  } else {
    const vazio = document.createElement("p");
    vazio.className = "feedback-vazio";
    vazio.textContent = "Sem feedback registrado para esta dica.";
    artigo.append(vazio);
  }
  return artigo;
}

function renderizarEditorDaResposta() {
  const aberto = estadoDoAcervo.aberto;
  if (!aberto) {
    elementosDoAcervo.editorVazio.hidden = false;
    elementosDoAcervo.editor.hidden = true;
    return;
  }
  elementosDoAcervo.editorVazio.hidden = true;
  elementosDoAcervo.editor.hidden = false;
  elementosDoAcervo.editor.innerHTML = "";

  const titulo = document.createElement("h3");
  titulo.textContent = aberto.resposta.texto + " (" + aberto.respostaId + ")";
  const prontidao = document.createElement("p");
  prontidao.className = "resumo";
  prontidao.textContent = textoDeProntidao(aberto);
  const statusRemoto = document.createElement("p");
  statusRemoto.className = "nota";
  statusRemoto.id = "acervo-status-remoto";

  const acoesRemotas = document.createElement("div");
  acoesRemotas.className = "acoes";
  const salvarRemoto = document.createElement("button");
  salvarRemoto.type = "button";
  salvarRemoto.id = "acervo-salvar-remoto";
  salvarRemoto.textContent = "Salvar alterações no Firestore";
  salvarRemoto.disabled = !firestoreDisponivel();
  salvarRemoto.addEventListener("click", salvarRascunhoRemotoDaResposta);
  const prepararBaralho = document.createElement("button");
  prepararBaralho.type = "button";
  prepararBaralho.textContent = "Reabrir banco do Firestore";
  prepararBaralho.disabled = !firestoreDisponivel();
  prepararBaralho.addEventListener("click", () => abrirRespostaNoAcervo(estadoDoAcervo.respostaId));
  acoesRemotas.append(salvarRemoto, prepararBaralho);

  const busca = document.createElement("input");
  busca.type = "search";
  busca.setAttribute("aria-label", "Buscar dica por texto ou id");
  busca.placeholder = "Buscar dica por texto ou id";
  busca.value = estadoDoAcervo.buscaDeDica;
  busca.addEventListener("input", () => {
    estadoDoAcervo.buscaDeDica = busca.value;
    estadoDoAcervo.paginaDeDicas = 1;
    renderizarListaDeDicas();
  });

  const listaDeDicas = document.createElement("div");
  listaDeDicas.className = "dicas";
  listaDeDicas.id = "acervo-lista-dicas";

  const paginacao = document.createElement("p");
  paginacao.className = "resumo";
  paginacao.id = "acervo-paginacao-dicas";

  const formNovaDica = document.createElement("form");
  formNovaDica.className = "campo";
  const rotuloNova = document.createElement("label");
  rotuloNova.setAttribute("for", "acervo-nova-dica-texto");
  rotuloNova.textContent = "Nova dica";
  const areaNova = document.createElement("textarea");
  areaNova.id = "acervo-nova-dica-texto";
  areaNova.maxLength = AcervoEditorial.MAXIMO_DE_TEXTO_DA_DICA;
  areaNova.required = true;
  areaNova.disabled = firestoreDisponivel() && !["presente", "ausente"].includes(estadoDoAcervo.estadoRemoto);
  areaNova.value = estadoDoAcervo.rascunhoDeNovaDica.texto;
  areaNova.addEventListener("input", () => {
    estadoDoAcervo.rascunhoDeNovaDica.texto = areaNova.value;
  });

  const rotuloEscopoNovo = document.createElement("label");
  rotuloEscopoNovo.setAttribute("for", "acervo-nova-dica-escopo");
  rotuloEscopoNovo.textContent = "Escopo da nova dica";
  rotuloEscopoNovo.className = "campo-visualmente-oculto";
  const escopoNovo = document.createElement("select");
  escopoNovo.id = "acervo-nova-dica-escopo";
  const escopoPadrao =
    estadoDoAcervo.rascunhoDeNovaDica.escopo ||
    aberto.escopoPadraoParaNovaDica ||
    AcervoEditorial.escopoPadraoParaNovaDica(aberto.dicas);
  [AcervoEditorial.ESCOPO_PUBLICO, AcervoEditorial.ESCOPO_PRIVADO].forEach((valor) => {
    const opcao = document.createElement("option");
    opcao.value = valor;
    opcao.textContent = "Escopo " + valor;
    opcao.selected = valor === escopoPadrao;
    escopoNovo.append(opcao);
  });
  escopoNovo.addEventListener("change", () => {
    estadoDoAcervo.rascunhoDeNovaDica.escopo = escopoNovo.value;
  });

  const enviarNova = document.createElement("button");
  enviarNova.type = "submit";
  enviarNova.className = "principal";
  enviarNova.textContent = "Acrescentar dica";
  formNovaDica.append(rotuloNova, areaNova, rotuloEscopoNovo, escopoNovo, enviarNova);
  formNovaDica.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const sucesso = await acrescentarDicaNoAcervo(areaNova.value, escopoNovo.value);
    if (sucesso) {
      estadoDoAcervo.rascunhoDeNovaDica = { texto: "", escopo: null };
      renderizarEditorDaResposta();
    }
    // Em falha ou quando ocupado, o texto digitado permanece na tela.
  });

  elementosDoAcervo.editor.append(
    titulo, prontidao, statusRemoto, acoesRemotas, busca, listaDeDicas, paginacao, formNovaDica
  );
  renderizarStatusRemoto();
  renderizarListaDeDicas();
}

function renderizarListaDeDicas() {
  const lista = dicasFiltradasDoAcervo();
  const pagina = AcervoEditorial.paginar(lista, estadoDoAcervo.paginaDeDicas, 25);
  estadoDoAcervo.paginaDeDicas = pagina.pagina;
  const container = document.getElementById("acervo-lista-dicas");
  const paginacao = document.getElementById("acervo-paginacao-dicas");
  if (!container || !paginacao) return;
  container.innerHTML = "";
  pagina.itens.forEach((dica) => {
    container.append(
      montarLinhaDeDica(dica, {
        respostaId: estadoDoAcervo.aberto.respostaId,
        texto: estadoDoAcervo.aberto.resposta.texto,
        tipo: estadoDoAcervo.aberto.resposta.tipo,
      }, feedbacksDaDicaAcervo(dica.dicaId))
    );
  });

  paginacao.innerHTML = "";
  const resumoTexto = document.createElement("span");
  resumoTexto.textContent =
    lista.length === 0
      ? "Nenhuma dica encontrada."
      : "Página " + pagina.pagina + " de " + pagina.totalDePaginas + " (" + pagina.total + " dica(s)).";
  paginacao.append(
    resumoTexto,
    botaoDePaginacao("« Anterior", pagina.pagina > 1, () => {
      estadoDoAcervo.paginaDeDicas -= 1;
      renderizarListaDeDicas();
    }),
    botaoDePaginacao("Próxima »", pagina.pagina < pagina.totalDePaginas, () => {
      estadoDoAcervo.paginaDeDicas += 1;
      renderizarListaDeDicas();
    })
  );
}

// ---- ações locais (servidor da Central) --------------------------------------

async function comOcupadoNoAcervo(acao) {
  if (estadoDoAcervo.ocupado) return false;
  definirOcupadoDoAcervo(true);
  try {
    await acao();
    return true;
  } catch (erro) {
    avisarAcervo(erro.message, "erro");
    return false;
  } finally {
    definirOcupadoDoAcervo(false);
  }
}

async function acrescentarDicaNoAcervo(texto, escopo) {
  return comOcupadoNoAcervo(async () => {
    exigirAcervoEditavel();
    AcervoEditorial.validarTextoDaDica(texto);
    if (estadoDoAcervo.estadoRemoto === "presente") {
      const dicaId = "dica-" + crypto.randomUUID();
      const dica = { schemaVersion: 1, dicaId, texto: texto.trim(), escopo, status: "ATIVA", origem: "EDITORIAL", revisaoTecnica: 1 };
      await gravarBancoRemoto({ ...estadoDoAcervo.aberto.dicas, [dicaId]: dica });
      estadoDoAcervo.rascunhoDeNovaDica = { texto: "", escopo: null };
      renderizarEditorDaResposta();
      avisarAcervo("Dica acrescentada no Firestore. Os baralhos só mudam após projeção e publicação.", "ok");
      return;
    }
    await enviarAcervo("/api/acervo/dica", {
      respostaId: estadoDoAcervo.respostaId,
      texto,
      escopo,
      revisao: estadoDoAcervo.aberto.revisao,
    });
    await recarregarRespostaAberta(true);
    avisarAcervo("Dica acrescentada ao rascunho local do acervo.", "ok");
  });
}

/** Texto + escopo numa ÚNICA requisição atômica (mesma revisão nas duas
 * mudanças); nunca duas chamadas sequenciais, que deixariam a segunda falhar
 * com revisão obsoleta depois que a primeira já tivesse avançado a revisão.
 */
async function salvarEdicaoDeDica(dicaId, texto, escopo) {
  return comOcupadoNoAcervo(async () => {
    exigirAcervoEditavel();
    AcervoEditorial.validarTextoDaDica(texto);
    if (estadoDoAcervo.estadoRemoto === "presente") {
      const dica = { ...estadoDoAcervo.aberto.dicas[dicaId], texto: texto.trim(), escopo };
      await gravarBancoRemoto({ ...estadoDoAcervo.aberto.dicas, [dicaId]: dica });
      estadoDoAcervo.edicoesPendentes.delete(dicaId);
      renderizarEditorDaResposta();
      avisarAcervo("Dica salva no Firestore.", "ok");
      return;
    }
    await enviarAcervo("/api/acervo/dica/editar", {
      respostaId: estadoDoAcervo.respostaId,
      dicaId,
      texto,
      escopo,
      revisao: estadoDoAcervo.aberto.revisao,
    });
    estadoDoAcervo.edicoesPendentes.delete(dicaId);
    await recarregarRespostaAberta(true);
    avisarAcervo("Dica salva no rascunho local do acervo.", "ok");
  });
}

async function alternarStatusDaDica(dicaId, statusAtual) {
  return comOcupadoNoAcervo(async () => {
    exigirAcervoEditavel();
    if (estadoDoAcervo.estadoRemoto === "presente") {
      const dica = { ...estadoDoAcervo.aberto.dicas[dicaId], status: statusAtual === "REMOVIDA" ? "ATIVA" : "REMOVIDA" };
      await gravarBancoRemoto({ ...estadoDoAcervo.aberto.dicas, [dicaId]: dica });
      renderizarEditorDaResposta();
      avisarAcervo("Status salvo no Firestore; identidade e histórico preservados.", "ok");
      return;
    }
    const caminho =
      statusAtual === "REMOVIDA" ? "/api/acervo/dica/reativar" : "/api/acervo/dica/desativar";
    await enviarAcervo(caminho, {
      respostaId: estadoDoAcervo.respostaId,
      dicaId,
      revisao: estadoDoAcervo.aberto.revisao,
    });
    await recarregarRespostaAberta(true);
    avisarAcervo("Status da dica atualizado.", "ok");
  });
}

async function copiarPedidoDoAcervo(resposta, dica, feedbacksDaDica) {
  const texto = AcervoEditorial.pedidoParaCodexDaResposta(resposta, dica, feedbacksDaDica);
  try {
    await navigator.clipboard.writeText(texto);
    avisarAcervo("Pedido copiado. Cole no Codex; nenhuma dica foi alterada automaticamente.", "ok");
  } catch (_) {
    const blob = new Blob([texto], { type: "text/markdown;charset=utf-8" });
    const endereco = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = endereco;
    link.download = "pedido-codex-" + dica.dicaId + ".md";
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(endereco);
    avisarAcervo("O navegador bloqueou a cópia. O pedido foi baixado em arquivo .md.", "atencao");
  }
}

// ---- ações remotas (Firestore) -----------------------------------------------

async function salvarRascunhoRemotoDaResposta() {
  if (!firestoreDisponivel()) return avisarAcervo("Firestore não configurado.", "erro");
  await comOcupadoNoAcervo(async () => {
    exigirAcervoEditavel();
    const dicas = structuredClone(estadoDoAcervo.aberto.dicas);
    estadoDoAcervo.edicoesPendentes.forEach((edicao, id) => {
      dicas[id] = { ...dicas[id], ...edicao };
    });
    await gravarBancoRemoto(dicas);
    estadoDoAcervo.edicoesPendentes.clear();
    renderizarEditorDaResposta();
    avisarAcervo("Banco salvo no Firestore. Dicas novas ainda em digitação devem ser acrescentadas pelo formulário.", "ok");
  });
}

// Projeção só cria um rascunho local: validar, aplicar e publicar permanecem
// etapas separadas. O botão vive no baralho para deixar o destino explícito.
async function projetarAcervoNoBaralhoAberto() {
  if (!estado.aberto || estado.ocupado || estadoDoAcervo.ocupado) return;
  if (estado.alterado) return avisarAcervo("Salve as alterações do baralho antes de projetar.", "atencao");
  if (!firestoreDisponivel()) return avisarAcervo("Firestore não configurado.", "erro");
  const chave = estado.chave;
  const aberto = estado.aberto;
  if (!window.confirm("Preparar no rascunho de “" + aberto.baralho.nome +
    "” os bancos ativos do Firestore? Isso não aplica nem publica o baralho.")) return;
  definirOcupado(true);
  await comOcupadoNoAcervo(async () => {
    const respostas = {};
    await comCredencialFirebase(async (cfg, credencial) => {
      for (const card of aberto.baralho.cards) {
        const id = card.respostaId || AcervoEditorial.aliasDeResposta(card.answer);
        if (!(id in respostas)) respostas[id] = await AcervoFirestore.carregarRespostaRemota(fetch, cfg, credencial, id);
      }
    });
    const resultado = AcervoEditorial.prepararProjecaoDoBaralho(
      aberto.baralho.cards, respostas, AcervoEditorial.escopoDoBaralho(aberto.baralho));
    if (!Object.keys(resultado.projecoes).length) throw new Error("Nenhum banco remoto completo com 10 dicas ativas neste escopo.");
    const dados = await enviarAcervo("/api/acervo/projetar", {
      chave, revisao: aberto.revisao, projecoes: resultado.projecoes
    });
    adotarBaralhoAberto(chave, dados);
    avisarAcervo("Bancos projetados no rascunho; " + resultado.ignorados.length +
      " card(s) mantidos sem alteração. Agora valide e aplique; publicação é separada.", "ok");
  });
  definirOcupado(false);
}

async function migrarPreviaParaFirestore() {
  if (!firestoreDisponivel()) {
    avisarAcervo("Firestore não configurado neste painel.", "erro");
    return;
  }
  const previa = estadoDoAcervo.previa;
  if (!previa) return;
  const seguras = AcervoEditorial.respostasSegurasParaMigrar(previa);
  if (!seguras.length) {
    avisarAcervo("Nenhuma resposta segura para migrar (resolva os conflitos primeiro).", "atencao");
    return;
  }
  const confirmar = window.confirm(
    "Criar no Firestore " + seguras.length + " resposta(s) segura(s) (create-only; nada é " +
    "sobrescrito)? Respostas com conflito ficam de fora."
  );
  if (!confirmar) return;
  await comOcupadoNoAcervo(async () => {
    const relatorio = await comCredencialFirebase(async (config, credencial) =>
      AcervoFirestore.migrarPreviaParaRemoto(fetch, config, credencial, previa, seguras)
    );
    avisarAcervo(
      "Migração: " + relatorio.criadas.length + " criada(s), " +
      relatorio.jaExistentes.length + " já existiam, " + relatorio.erros.length + " erro(s).",
      relatorio.erros.length ? "atencao" : "ok"
    );
  });
}

// ---- inicialização ------------------------------------------------------------

if (elementosDoAcervo.secao) {
  window.addEventListener("central-firebase-configurada", () => {
    renderizarResumoDoAcervo();
    if (estadoDoAcervo.aberto) renderizarEditorDaResposta();
  });
  elementosDoAcervo.atualizarPrevia.addEventListener("click", () =>
    carregarPreviaDoAcervo().catch((erro) => avisarAcervo(erro.message, "erro"))
  );
  if (elementosDoAcervo.mostrarTecnicos) {
    elementosDoAcervo.mostrarTecnicos.addEventListener("change", () =>
      carregarPreviaDoAcervo().catch((erro) => avisarAcervo(erro.message, "erro"))
    );
  }
  if (elementosDoAcervo.migrarPrevia) {
    elementosDoAcervo.migrarPrevia.addEventListener("click", migrarPreviaParaFirestore);
  }
  elementosDoAcervo.buscaResposta.addEventListener("input", () => {
    estadoDoAcervo.buscaDeResposta = elementosDoAcervo.buscaResposta.value;
    estadoDoAcervo.paginaDeRespostas = 1;
    renderizarListaDeRespostas();
  });
  carregarPreviaDoAcervo().catch((erro) => avisarAcervo(erro.message, "erro"));

  // Feedback sincronizado (Firestore, via app.js) some/aparece sem destruir
  // edição em andamento: só re-renderiza a lista de dicas com os dados mais
  // recentes de estado.feedbacksNuvem, preservando estadoDoAcervo.edicoesPendentes.
  setInterval(() => {
    if (!document.hidden && estadoDoAcervo.aberto && !estadoDoAcervo.ocupado && !temEdicaoPendenteNoAcervo()) {
      renderizarListaDeDicas();
    }
  }, 30000);
}
