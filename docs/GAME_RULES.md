# Regras do Jogo

Este arquivo é o dono das regras do QuemSou: qualquer regra implementada no
código deve estar registrada aqui.

## O card

- Todo card tem uma resposta secreta (pessoa, lugar ou coisa) e **exatamente
  10 dicas**, reveladas uma a uma, da mais difícil para a mais fácil.

## Pontuação

- Quem acerta com **N** dicas reveladas (N de 1 a 10) ganha **11 − N** pontos:
  acertou na dica 1 → 10 pontos; na dica 5 → 6 pontos; na dica 10 → 1 ponto.
- **Leitor**: configurável por partida (`RegrasPartida.leitorPontua`, padrão
  **SIM**). Quando ativo, o leitor ganha os **mesmos pontos** do acertador;
  quando desativado, o leitor ganha 0.
- **Ninguém acertou**: ninguém pontua (0 para o acertador, 0 para o leitor).

## Partida

- A partida tem um número configurável de rodadas (`RegrasPartida.numeroDeRodadas`,
  padrão 5).
- O baralho é embaralhado de forma determinística a partir do código da partida
  (ex.: "LOBO"): o mesmo código gera a mesma ordem de cards em qualquer
  aparelho — base do multiplayer offline.

## Decisões em aberto

- Destino do card queimado (volta ao baralho? é descartado?) — sem campo em
  `RegrasPartida` até a decisão ser fechada.
