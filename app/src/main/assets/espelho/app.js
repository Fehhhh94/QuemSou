/*
 * Cliente do espelho de leitura — pareamento e acompanhamento da partida.
 *
 * O jogador abre o endereço anunciado pelo anfitrião, toca no próprio nome e
 * acompanha a partida pelo canal /estado. O servidor só envia resposta e
 * dica ao navegador autenticado como leitor da rodada.
 *
 * Sem framework e sem dependência externa — tudo vem do aparelho da mesa.
 */
(function () {
  'use strict';

  var CHAVE_TOKEN = 'quemsou.espelho.token';
  var CHAVE_JOGADOR = 'quemsou.espelho.jogador';
  var INTERVALO_DA_LISTA_MS = 3000;

  var app = document.getElementById('app');
  var temporizadorDaLista = null;
  var canalDeEstado = null;

  // --- Armazenamento local (tolerante a navegador com storage bloqueado) ---

  function ler(chave) {
    try {
      return window.localStorage.getItem(chave);
    } catch (erro) {
      return null;
    }
  }

  function gravar(chave, valor) {
    try {
      window.localStorage.setItem(chave, valor);
    } catch (erro) {
      /* Sessão só na memória desta aba: aceitável, o pareamento segue. */
    }
  }

  function esquecer() {
    try {
      window.localStorage.removeItem(CHAVE_TOKEN);
      window.localStorage.removeItem(CHAVE_JOGADOR);
    } catch (erro) {
      /* Nada a fazer. */
    }
  }

  // --- Rede ---

  function buscarJogadores() {
    return fetch('/jogadores', { cache: 'no-store' }).then(function (resposta) {
      if (!resposta.ok) throw new Error('lista indisponível');
      return resposta.json();
    });
  }

  function entrar(jogadorId) {
    var corpo = { jogadorId: jogadorId };
    var token = ler(CHAVE_TOKEN);
    if (token) corpo.token = token;
    return fetch('/entrar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(corpo)
    }).then(function (resposta) {
      return resposta.json().then(function (dados) {
        return { ok: resposta.ok, dados: dados };
      });
    });
  }

  // --- Telas ---

  function limpar() {
    if (temporizadorDaLista) {
      window.clearInterval(temporizadorDaLista);
      temporizadorDaLista = null;
    }
    app.textContent = '';
  }

  function nomeExibido(jogador, indice) {
    var nome = (jogador.nome || '').trim();
    return nome !== '' ? nome : 'Jogador ' + (indice + 1);
  }

  function mostrarSelecao(jogadores, aviso) {
    limpar();

    var cabecalho = document.createElement('header');
    cabecalho.className = 'cabecalho';
    var titulo = document.createElement('h1');
    titulo.className = 'titulo';
    titulo.textContent = 'Espelho de leitura';
    var subtitulo = document.createElement('p');
    subtitulo.className = 'subtitulo';
    subtitulo.textContent = 'Toque no seu nome. Na sua vez de ler, a dica aparece aqui.';
    cabecalho.appendChild(titulo);
    cabecalho.appendChild(subtitulo);
    app.appendChild(cabecalho);

    var caixaDeAviso = document.createElement('p');
    caixaDeAviso.className = 'aviso';
    caixaDeAviso.hidden = !aviso;
    if (aviso) caixaDeAviso.textContent = aviso;
    app.appendChild(caixaDeAviso);

    var lista = document.createElement('ul');
    lista.className = 'lista';
    jogadores.forEach(function (jogador, indice) {
      var item = document.createElement('li');
      var botao = document.createElement('button');
      botao.className = 'nome';
      botao.type = 'button';
      botao.disabled = jogador.emUso;

      var rotulo = document.createElement('span');
      rotulo.textContent = nomeExibido(jogador, indice);
      botao.appendChild(rotulo);

      if (jogador.emUso) {
        var marca = document.createElement('span');
        marca.className = 'marca';
        marca.textContent = 'já entrou';
        botao.appendChild(marca);
      }

      botao.addEventListener('click', function () {
        botao.disabled = true;
        aoEscolher(jogador.id);
      });

      item.appendChild(botao);
      lista.appendChild(item);
    });
    app.appendChild(lista);

    var rodape = document.createElement('p');
    rodape.className = 'rodape';
    rodape.textContent = 'Este espelho vive no celular do anfitrião, na sua rede.';
    app.appendChild(rodape);

    // A lista se atualiza sozinha: quem entrou no celular do lado precisa
    // aparecer esmaecido aqui sem ninguém recarregar a página.
    temporizadorDaLista = window.setInterval(function () {
      buscarJogadores()
        .then(function (dados) {
          atualizarMarcas(lista, dados.jogadores || []);
        })
        .catch(function () {
          /* Piscada de rede: a próxima volta resolve. */
        });
    }, INTERVALO_DA_LISTA_MS);
  }

  function atualizarMarcas(lista, jogadores) {
    var botoes = lista.querySelectorAll('button.nome');
    if (botoes.length !== jogadores.length) {
      // O anfitrião mexeu na lista de jogadores: redesenha do zero.
      mostrarSelecao(jogadores, null);
      return;
    }
    jogadores.forEach(function (jogador, indice) {
      var botao = botoes[indice];
      botao.disabled = jogador.emUso;
      botao.firstChild.textContent = nomeExibido(jogador, indice);
      var marca = botao.querySelector('.marca');
      if (jogador.emUso && !marca) {
        marca = document.createElement('span');
        marca.className = 'marca';
        marca.textContent = 'já entrou';
        botao.appendChild(marca);
      } else if (!jogador.emUso && marca) {
        botao.removeChild(marca);
      }
    });
  }

  function mostrarEspera(nome, jogadorId) {
    limpar();

    var caixa = document.createElement('section');
    caixa.className = 'espera';

    var titulo = document.createElement('h1');
    titulo.className = 'espera-nome';
    titulo.textContent = nome;

    var ponto = document.createElement('span');
    ponto.className = 'ponto';

    var texto = document.createElement('p');
    texto.className = 'espera-texto';
    texto.textContent = 'Aguardando a partida começar';

    var trocar = document.createElement('button');
    trocar.className = 'trocar';
    trocar.type = 'button';
    trocar.textContent = 'Não sou eu — escolher outro nome';
    trocar.addEventListener('click', function () {
      fecharCanal();
      carregarSelecao(null);
    });

    caixa.appendChild(titulo);
    caixa.appendChild(ponto);
    caixa.appendChild(texto);
    caixa.appendChild(trocar);
    app.appendChild(caixa);

    abrirCanal(jogadorId);
  }

  function mostrarErro(mensagem) {
    limpar();
    var caixa = document.createElement('section');
    caixa.className = 'espera';
    var texto = document.createElement('p');
    texto.className = 'espera-texto';
    texto.textContent = mensagem;
    caixa.appendChild(texto);
    app.appendChild(caixa);
  }

  function adicionarTexto(pai, classe, texto) {
    if (!texto) return;
    var elemento = document.createElement('p');
    if (classe) elemento.className = classe;
    elemento.textContent = texto;
    pai.appendChild(elemento);
  }

  function adicionarCartao(pai, rotulo, texto, classe) {
    if (!texto) return;
    var cartao = document.createElement('section');
    cartao.className = 'cartao';
    adicionarTexto(cartao, 'cartao-rotulo', rotulo);
    adicionarTexto(cartao, classe, texto);
    pai.appendChild(cartao);
  }

  function tituloDaFase(estado) {
    switch (estado.fase) {
      case 'vez_de_jogar': return estado.suaVez ? 'Sua vez de ler' : estado.leitor + ' vai ler';
      case 'grid': return estado.suaVez ? 'Prepare a leitura' : 'Escolhendo uma dica';
      case 'shot': return 'Pausa para o shot';
      case 'dica_revelada': return estado.suaVez ? 'Leia em voz alta' : 'Hora de adivinhar';
      case 'quem_acertou': return 'Quem acertou?';
      case 'anuncio': return 'Resposta revelada';
      case 'placar_final': return 'Placar final';
      case 'indisponivel': return 'Partida indisponível';
      default: return 'Espelho de leitura';
    }
  }

  function mensagemDaFase(estado) {
    if (estado.mensagem) return estado.mensagem;
    switch (estado.fase) {
      case 'vez_de_jogar':
        return estado.suaVez
          ? 'Pegue este celular. A partida começa no aparelho principal.'
          : 'Aguarde o leitor começar a rodada.';
      case 'grid':
        return (estado.escolhedor || 'Outro jogador') + ' escolhe uma posição no aparelho principal.';
      case 'dica_revelada':
        return estado.suaVez
          ? 'Mantenha a resposta em segredo.'
          : (estado.leitor || 'O leitor') + ' está lendo a dica.';
      case 'quem_acertou': return 'O aparelho principal está registrando o acerto.';
      case 'indisponivel': return 'Volte ao aparelho principal para ajustar a partida.';
      default: return null;
    }
  }

  function mostrarEstadoDaPartida(estado) {
    limpar();
    var caixa = document.createElement('section');
    caixa.className = 'partida';
    caixa.setAttribute('aria-live', 'polite');

    if (estado.rodada && estado.totalDeRodadas) {
      adicionarTexto(caixa, 'rodada', 'Rodada ' + estado.rodada + ' de ' + estado.totalDeRodadas);
    }
    adicionarTexto(caixa, 'partida-titulo', tituloDaFase(estado));
    adicionarTexto(caixa, 'partida-texto', mensagemDaFase(estado));

    adicionarCartao(caixa, 'Resposta', estado.resposta, 'resposta');
    if (estado.dica) {
      var rotulo = estado.valor ? 'Dica — vale ' + estado.valor + ' pontos' : 'Dica';
      adicionarCartao(caixa, rotulo, estado.dica, 'dica');
    }

    if (estado.fase === 'placar_final' && estado.ranking) {
      var lista = document.createElement('ol');
      lista.className = 'placar';
      estado.ranking.forEach(function (linha) {
        var item = document.createElement('li');
        adicionarTexto(item, '', linha.nome);
        adicionarTexto(item, '', linha.pontos + ' pts');
        lista.appendChild(item);
      });
      caixa.appendChild(lista);
    }
    app.appendChild(caixa);
  }

  // --- Canal de eventos ---

  function fecharCanal() {
    if (canalDeEstado) {
      canalDeEstado.close();
      canalDeEstado = null;
    }
  }

  function abrirCanal(jogadorId) {
    fecharCanal();
    if (!window.EventSource) return;
    var token = ler(CHAVE_TOKEN);
    if (!token) return;
    canalDeEstado = new EventSource(
      '/estado?jogador=' + encodeURIComponent(jogadorId) +
      '&token=' + encodeURIComponent(token)
    );
    canalDeEstado.onmessage = function (evento) {
      var estado;
      try {
        estado = JSON.parse(evento.data);
      } catch (erro) {
        return;
      }
      if (estado.fase === 'aguardando') return;
      mostrarEstadoDaPartida(estado);
    };
    canalDeEstado.onerror = function () {
      /* O EventSource reconecta sozinho com o retry anunciado pelo servidor. */
    };
  }

  // --- Fluxo ---

  function aoEscolher(jogadorId) {
    entrar(jogadorId)
      .then(function (resultado) {
        if (resultado.ok) {
          gravar(CHAVE_TOKEN, resultado.dados.token);
          gravar(CHAVE_JOGADOR, resultado.dados.jogadorId);
          mostrarEspera(resultado.dados.nome, resultado.dados.jogadorId);
          return;
        }
        var aviso = resultado.dados && resultado.dados.erro === 'ja_tomado'
          ? 'Esse nome já foi escolhido em outro celular. Pegue o seu.'
          : 'Esse nome saiu da lista. Escolha outro.';
        carregarSelecao(aviso);
      })
      .catch(function () {
        carregarSelecao('Não deu para entrar. Confira se está no mesmo Wi-Fi e tente de novo.');
      });
  }

  function carregarSelecao(aviso) {
    buscarJogadores()
      .then(function (dados) {
        mostrarSelecao(dados.jogadores || [], aviso);
      })
      .catch(function () {
        mostrarErro('Sem contato com o celular do anfitrião. Confira o Wi-Fi e recarregue.');
      });
  }

  function comecar() {
    var token = ler(CHAVE_TOKEN);
    var jogadorId = ler(CHAVE_JOGADOR);
    if (!token || !jogadorId) {
      carregarSelecao(null);
      return;
    }
    // Reconexão: o mesmo token retoma o próprio lugar sem roubar o de
    // ninguém. Se o anfitrião reiniciou o espelho, o token velho não vale
    // mais e a resposta manda de volta para a lista.
    entrar(jogadorId)
      .then(function (resultado) {
        if (resultado.ok) {
          gravar(CHAVE_TOKEN, resultado.dados.token);
          gravar(CHAVE_JOGADOR, resultado.dados.jogadorId);
          mostrarEspera(resultado.dados.nome, resultado.dados.jogadorId);
        } else {
          esquecer();
          carregarSelecao(null);
        }
      })
      .catch(function () {
        carregarSelecao(null);
      });
  }

  comecar();
})();
