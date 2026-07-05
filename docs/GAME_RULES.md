# Regras do Jogo

Este arquivo é o dono das regras do QuemSou: qualquer regra implementada no
código deve estar registrada aqui.

## O card

- Todo card tem uma resposta secreta (pessoa, lugar ou coisa) e **exatamente
  10 dicas** autossuficientes. As dicas **não têm curva de dificuldade** —
  cada card mistura dicas fáceis e difíceis.
- **Grid 1–10 às cegas**: as dicas são apresentadas num grid de 1 a 10 e a
  escolha do número é às cegas — o app **embaralha a posição** das dicas a
  cada turno (embaralhamento determinístico por seed em `Turno.criar`, no
  domínio; a posição não indica dificuldade).
- **Card queimado** (ninguém acertou): é **descartado** — não volta ao
  baralho (`RegrasPartida.descartarCardQueimado`, padrão **SIM**).

## Jogadores

- Mínimo **2**, máximo **4** jogadores por partida: 1 leitor + 1 a 3
  adivinhadores por rodada.
- **Modo de jogo**: individual ou em times, configurável antes da partida
  (os dois modos estão na v1).

## O turno

- O leitor da rodada lê as dicas; os demais jogadores são os adivinhadores
  (1 a 3). O leitor **gira a cada rodada**, em rodízio circular.
- **Rodízio do escolhedor**: quem escolhe a posição no grid gira **a cada dica
  revelada**, circular entre os adivinhadores (funciona com 1, 2 ou 3);
  o leitor nunca escolhe.
- Posição já revelada não pode ser escolhida de novo.
- O turno termina com **acerto**, **desistência** (card queimado) ou após a
  **10ª dica sem acerto** (queima automaticamente). O fim de turno é anunciado
  com a **resposta revelada**, as dicas usadas e os pontos de cada um.

## Pontuação

- Quem acerta tendo usado **N** dicas (N de 1 a 10) ganha **11 − N** pontos:
  acertou com 1 dica usada → 10 pontos; com 5 → 6 pontos; com 10 → 1 ponto.
- **Leitor**: configurável por partida (`RegrasPartida.leitorPontua`, padrão
  **SIM**). Quando ativo, o leitor ganha os **mesmos pontos** do acertador;
  quando desativado, o leitor ganha 0.
- **Ninguém acertou**: ninguém pontua (0 para o acertador, 0 para o leitor).

## Partida

- **Categoria**: filtra o baralho da partida — Personagem de filme, Mundo da
  música, ou Livre (união de todas as categorias; não há cards exclusivos de
  Livre).
- A partida tem um número configurável de rodadas (`RegrasPartida.numeroDeRodadas`,
  padrão 5).
- O baralho é embaralhado de forma determinística a partir do código da partida
  (ex.: "LOBO"), como embaralhamento interno do anfitrião (Fase 4 — Nearby
  Connections); veja `docs/CLAUDE.md` para a arquitetura de multiplayer.

## Placar

- O placar é exibido em **todos os aparelhos** e sincronizado pelo anfitrião
  via Nearby Connections (multiplayer, Fase 4).
- No modo **TIMES**, o placar do time é a **soma** dos pontos dos jogadores
  do time.
- **Empate**: declarado — todos os empatados na maior pontuação são
  vencedores; **não há critério de desempate na v1**.
