// Acervo editorial - logica pura, compartilhada entre o navegador e os testes
// Node. Sem framework, sem CDN e sem DOM: quem manipula o documento e
// acervo-ui.js. Mesmo esquema de identidade legada que o app usa em jogo
// (domain/rules/SelecionadorDeDicas) e que administrador/acervo_editorial.py
// usa na previa de migracao - os tres precisam concordar.
"use strict";

(function (raiz) {
  const SCHEMA_VERSION = 1;

  const ESCOPO_PUBLICO = "PUBLICO";
  const ESCOPO_PRIVADO = "PRIVADO";
  const ESCOPOS = [ESCOPO_PUBLICO, ESCOPO_PRIVADO];

  const STATUS_ATIVA = "ATIVA";
  const STATUS_REMOVIDA = "REMOVIDA";

  const ORIGEM_EDITORIAL = "EDITORIAL";
  const ORIGEM_ALIAS_LEGADO = "ALIAS_LEGADO";

  const CATEGORIA_PRIVADA_POR_PADRAO = "ESPECIAIS";

  const MINIMO_DE_DICAS_PARA_PUBLICAR = 10;
  const MAXIMO_DE_DICAS = 500;
  const MAXIMO_DE_TEXTO_DA_DICA = 500;

  class FalhaDoAcervo extends Error {
    constructor(codigo, mensagem) {
      super(mensagem);
      this.codigo = codigo;
      this.mensagem = mensagem;
    }
  }

  // Precisa concordar EXATAMENTE com domain/rules/SelecionadorDeDicas.kt
  // (`\p{M}+` / `[^\p{L}\p{N}]+`) e com administrador/fontes.py (`isalnum()`
  // Unicode após NFD). Um filtro ASCII (`[^a-z0-9]+`) apagaria letras que não
  // decompõem em ASCII+marca (ex.: "ß", a maioria dos alfabetos não latinos),
  // divergindo da identidade real usada pelo app e quebrando a associação de
  // feedback em textos fora do português comum.
  function normalizar(texto) {
    return String(texto || "")
      .normalize("NFD")
      .replace(/\p{M}+/gu, "")
      .toLowerCase()
      .replace(/[^\p{L}\p{N}]+/gu, " ")
      .trim();
  }

  function escopoDoBaralho(baralho) {
    const categoria = (baralho || {}).categoria;
    return categoria === CATEGORIA_PRIVADA_POR_PADRAO ? ESCOPO_PRIVADO : ESCOPO_PUBLICO;
  }

  function aliasDeResposta(answer) {
    const identificador = normalizar(answer);
    if (!identificador) {
      throw new FalhaDoAcervo(
        "resposta_sem_texto", "Um card sem resposta não gera identidade editorial."
      );
    }
    return identificador;
  }

  function aliasDeDica(texto) {
    return "legado:" + normalizar(texto);
  }

  // UTF-8/base64url uniforme: '.', '_.' e percentuais nunca colidem.
  // Nenhum acervo editorial foi publicado usando o codec provisório anterior.
  function idDoDocumento(idLogico) {
    if (typeof idLogico !== "string" || !idLogico) throw new Error("Id vazio.");
    const bytes = new TextEncoder().encode(idLogico);
    const codificado = "id_" + btoa(String.fromCharCode(...bytes))
      .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    if (codificado.length > 1500) throw new Error("Id excede o tamanho permitido.");
    return codificado;
  }

  function idLogicoDoDocumento(idDocumento) {
    if (!/^id_[A-Za-z0-9_-]+$/.test(idDocumento)) throw new Error("Id remoto inválido.");
    const bytes = Uint8Array.from(atob(idDocumento.slice(3).replace(/-/g, "+").replace(/_/g, "/")), c => c.charCodeAt(0));
    const texto = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    if (idDoDocumento(texto) !== idDocumento) throw new Error("Id remoto não canônico.");
    return texto;
  }

  function validarTextoDaDica(texto) {
    if (typeof texto !== "string") {
      throw new FalhaDoAcervo("campo_invalido", "O texto da dica precisa ser texto.");
    }
    const aparado = texto.trim();
    if (!aparado) {
      throw new FalhaDoAcervo("campo_invalido", "A dica não pode ficar em branco.");
    }
    if (aparado.length > MAXIMO_DE_TEXTO_DA_DICA) {
      throw new FalhaDoAcervo(
        "campo_invalido", "A dica passa de " + MAXIMO_DE_TEXTO_DA_DICA + " caracteres."
      );
    }
    return aparado;
  }

  function listaDeDicas(dicas) {
    if (Array.isArray(dicas)) return dicas;
    if (dicas && typeof dicas === "object") return Object.values(dicas);
    return [];
  }

  function contarAtivas(dicas) {
    return listaDeDicas(dicas).filter((dica) => dica && dica.status === STATUS_ATIVA).length;
  }

  /** Nunca deixa PRIVADO entrar numa publicação PUBLICO, mesmo com resposta compartilhada. */
  function dicasParaPublicacao(dicas, visibilidadeDoBaralho) {
    const permitidos = visibilidadeDoBaralho === ESCOPO_PUBLICO ? [ESCOPO_PUBLICO] : ESCOPOS;
    const vistos = new Set();
    const selecionadas = [];
    listaDeDicas(dicas).forEach((dica) => {
      if (!dica || dica.status !== STATUS_ATIVA) return;
      if (!permitidos.includes(dica.escopo)) return;
      const chave = normalizar(dica.texto);
      if (!chave || vistos.has(chave)) return;
      vistos.add(chave);
      selecionadas.push(dica);
    });
    return selecionadas;
  }

  function prontaParaPublicar(dicas, visibilidadeDoBaralho) {
    const quantidade = dicasParaPublicacao(dicas, visibilidadeDoBaralho).length;
    return quantidade >= MINIMO_DE_DICAS_PARA_PUBLICAR && quantidade <= MAXIMO_DE_DICAS;
  }

  /** PRIVADO por padrão quando a resposta já é majoritária/empatadamente privada. */
  function escopoPadraoParaNovaDica(dicas) {
    const lista = listaDeDicas(dicas);
    if (!lista.length) return ESCOPO_PUBLICO;
    const privadas = lista.filter((d) => d && d.escopo === ESCOPO_PRIVADO).length;
    const publicas = lista.filter((d) => d && d.escopo === ESCOPO_PUBLICO).length;
    return privadas >= publicas ? ESCOPO_PRIVADO : ESCOPO_PUBLICO;
  }

  /**
   * Monta, para cada card ligado a uma resposta com banco remoto conhecido,
   * o novo `bancoDeDicas` (dicas ATIVAS autorizadas pelo escopo do baralho,
   * até 500) e as 10 `clues` de compatibilidade (ordem determinística por
   * dicaId, igual ao app: `sortedBy { it.id }`). Cards sem banco remoto para
   * a sua resposta ficam de fora do resultado (mantidos como estão).
   *
   * `respostasRemotas`: { [respostaId]: { resposta, dicas } } já carregado
   * do Firestore (ver acervo-firestore.js). Puro: não lê rede.
   */
  function prepararProjecaoDoBaralho(cards, respostasRemotas, visibilidadeDoBaralho) {
    const projecoes = {};
    const ignorados = [];
    (cards || []).forEach((card) => {
      const respostaId = card && (card.respostaId || aliasDeResposta(card.answer));
      const remoto = respostaId ? respostasRemotas[respostaId] : null;
      if (!remoto || remoto.completo === false) {
        ignorados.push({ cardId: card && card.id, motivo: "sem_banco_remoto" });
        return;
      }
      const selecionadas = dicasParaPublicacao(remoto.dicas, visibilidadeDoBaralho);
      if (selecionadas.length < MINIMO_DE_DICAS_PARA_PUBLICAR) {
        ignorados.push({ cardId: card.id, motivo: "banco_insuficiente" });
        return;
      }
      const banco = selecionadas
        .slice()
        .sort((a, b) => a.dicaId < b.dicaId ? -1 : a.dicaId > b.dicaId ? 1 : 0)
        .map((dica) => ({ id: dica.dicaId, texto: dica.texto, escopo: dica.escopo }));
      if (banco.length > MAXIMO_DE_DICAS) throw new Error("Banco excede 500 dicas.");
      const clues = banco.slice(0, 10).map((fato) => fato.texto);
      projecoes[card.id] = { bancoDeDicas: banco, clues };
    });
    return { projecoes, ignorados };
  }

  /**
   * Ids de resposta sem conflito e dentro do teto — só estas podem ser
   * gravadas no Firestore por uma migração automática; espelha
   * `acervo_editorial.respostas_seguras_para_migrar` (Python).
   */
  function respostasSegurasParaMigrar(previa) {
    const comProblema = new Set();
    (previa.conflitos || []).forEach((conflito) => {
      if (conflito.respostaId) comProblema.add(conflito.respostaId);
      (conflito.respostaIds || []).forEach((id) => comProblema.add(id));
    });
    (previa.bancosAcimaDoLimite || []).forEach((item) => comProblema.add(item.respostaId));
    return Object.keys(previa.respostas || {}).filter((id) => !comProblema.has(id));
  }

  /** Busca por texto ou id, para a lista de dicas do editor sem DOM massivo. */
  function filtrarDicas(dicas, termo) {
    const alvo = normalizar(termo);
    const lista = listaDeDicas(dicas);
    if (!alvo) return lista;
    return lista.filter((dica) => {
      if (!dica) return false;
      if (normalizar(dica.texto).includes(alvo)) return true;
      return String(dica.dicaId || "").toLowerCase().includes(alvo.replace(/ /g, ""));
    });
  }

  /** Uma página de cada vez: 500 dicas nunca viram 500 nós no DOM de uma vez. */
  function paginar(lista, pagina, tamanhoDaPagina) {
    const tamanho = tamanhoDaPagina > 0 ? tamanhoDaPagina : 50;
    const paginaAtual = pagina > 0 ? pagina : 1;
    const inicio = (paginaAtual - 1) * tamanho;
    const itens = (lista || []).slice(inicio, inicio + tamanho);
    return {
      itens,
      pagina: paginaAtual,
      totalDePaginas: Math.max(1, Math.ceil((lista || []).length / tamanho)),
      total: (lista || []).length,
    };
  }

  function pedidoParaCodexDaResposta(resposta, dica, feedbacks) {
    const itens = feedbacks || [];
    const listaDeFeedbacks = itens.length
      ? itens.map((item, posicao) => {
          const comentario = item.comentario || "sem comentário";
          const snapshot = item.textoAvaliado && item.textoAvaliado !== dica.texto
            ? " | texto avaliado: " + item.textoAvaliado
            : "";
          return (
            (posicao + 1) + ". [" + (item.listaNome || "sem lista") + "] " + item.voto +
            " em " + (item.criadoEm || "data não informada") + ": " + comentario + snapshot
          );
        }).join("\n")
      : "Nenhum feedback registrado para esta dica.";
    return [
      "Você está trabalhando no repositório QuemSou.",
      "Leia docs/CARDS_GUIDE.md e as regras editoriais vigentes antes de responder.",
      "Reescreva somente a dica abaixo; não altere o id da dica nem as demais dicas da resposta.",
      "",
      "Resposta: " + (resposta.texto || "") + " (" + resposta.respostaId + ")",
      "Tipo: " + (resposta.tipo || ""),
      "Id da dica: " + dica.dicaId,
      "Escopo: " + dica.escopo,
      "Texto atual: " + dica.texto,
      "",
      "Feedbacks recebidos:",
      listaDeFeedbacks,
      "",
      "Requisitos:",
      "- produzir texto original, sem copiar fala ou trecho de obra;",
      "- não incluir a resposta, nem uma variação óbvia dela, na dica;",
      "- corrigir o problema apontado sem tornar a dica ambígua ou fácil demais;",
      "- preservar o id da dica;",
      "- devolver exatamente uma sugestão, seguida de uma justificativa curta.",
      "",
      "Formato da resposta:",
      "Dica revisada: <texto>",
      "Justificativa: <uma frase>",
    ].join("\n");
  }

  const api = {
    SCHEMA_VERSION,
    ESCOPO_PUBLICO,
    ESCOPO_PRIVADO,
    ESCOPOS,
    STATUS_ATIVA,
    STATUS_REMOVIDA,
    ORIGEM_EDITORIAL,
    ORIGEM_ALIAS_LEGADO,
    MINIMO_DE_DICAS_PARA_PUBLICAR,
    MAXIMO_DE_DICAS,
    MAXIMO_DE_TEXTO_DA_DICA,
    FalhaDoAcervo,
    normalizar,
    escopoDoBaralho,
    aliasDeResposta,
    aliasDeDica,
    idDoDocumento,
    idLogicoDoDocumento,
    validarTextoDaDica,
    contarAtivas,
    dicasParaPublicacao,
    prontaParaPublicar,
    escopoPadraoParaNovaDica,
    prepararProjecaoDoBaralho,
    respostasSegurasParaMigrar,
    filtrarDicas,
    paginar,
    pedidoParaCodexDaResposta,
  };

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  } else {
    raiz.AcervoEditorial = api;
  }
})(typeof window !== "undefined" ? window : globalThis);
