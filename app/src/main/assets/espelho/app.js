/*
 * Cliente do espelho de leitura — Fase 4A, parte 1: SÓ o pareamento.
 *
 * O jogador abre o endereço anunciado pelo anfitrião, toca no próprio nome e
 * fica na tela de espera. Nenhuma dica e nenhuma resposta trafegam aqui
 * ainda: o canal /estado já está aberto e recebendo {"fase":"aguardando"},
 * mas o conteúdo da vez de ler chega só na parte 2.
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

  // --- Canal de eventos (a estrutura da parte 2 já nasce aqui) ---

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
      // Parte 1: a única fase possível é "aguardando" e a tela já é essa.
      // A parte 2 troca este ponto pelo desenho da dica e da resposta.
      if (estado.fase !== 'aguardando') return;
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
