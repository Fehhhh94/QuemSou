# Regras do Jogo

Este arquivo é o dono das regras do QuemSou: qualquer regra implementada no
código deve estar registrada aqui.

## O card

- Todo card tem uma resposta secreta (pessoa, lugar ou coisa) e **exatamente
  10 dicas** autossuficientes. As dicas **não têm curva de dificuldade** —
  cada card mistura dicas fáceis e difíceis.
- **Grid 1–10 às cegas**: as dicas são apresentadas num grid de 1 a 10 e a
  escolha do número é às cegas — o app **embaralha a posição** das dicas a
  cada partida (implementação na Fase 3, no domínio, com
  `clues.shuffled(random)`).
- **Card queimado** (ninguém acertou): é **descartado** — não volta ao
  baralho (`RegrasPartida.descartarCardQueimado`, padrão **SIM**).

## Jogadores

- Mínimo **2**, máximo **4** jogadores por partida: 1 leitor + 1 a 3
  adivinhadores por rodada.
- **Modo de jogo**: individual ou em times, configurável antes da partida
  (os dois modos estão na v1).

## Pontuação

- Quem acerta tendo usado **N** dicas (N de 1 a 10) ganha **11 − N** pontos:
  acertou com 1 dica usada → 10 pontos; com 5 → 6 pontos; com 10 → 1 ponto.
- **Leitor**: configurável por partida (`RegrasPartida.leitorPontua`, padrão
  **SIM**). Quando ativo, o leitor ganha os **mesmos pontos** do acertador;
  quando desativado, o leitor ganha 0.
- **Ninguém acertou**: ninguém pontua (0 para o acertador, 0 para o leitor).

## Partida

- A partida tem um número configurável de rodadas (`RegrasPartida.numeroDeRodadas`,
  padrão 5).
- O baralho é embaralhado de forma determinística a partir do código da partida
  (ex.: "LOBO"), como embaralhamento interno do anfitrião (Fase 4 — Nearby
  Connections); veja `docs/CLAUDE.md` para a arquitetura de multiplayer.

## Placar

- O placar é exibido em **todos os aparelhos** e sincronizado pelo anfitrião
  via Nearby Connections (multiplayer, Fase 4).
