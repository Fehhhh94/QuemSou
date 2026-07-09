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

## Jogadores e grupos

- Mínimo **2**, máximo **4** jogadores por partida: 1 leitor + 1 a 3
  adivinhadores por rodada.
- **Grupos (especificação v4)** — não existe mais "modo de jogo"
  (Individual/Times): todo jogador pertence a um **grupo**. Por padrão, cada
  jogador nasce em um grupo próprio de tamanho 1 — o antigo "individual" é
  apenas esse estado padrão, não um modo à parte. Jogar "em times" é
  simplesmente agrupar 2+ jogadores num mesmo grupo.
- **Grupos mistos são permitidos**, sem validação especial: uma partida pode
  ter um grupo de 2 e dois jogadores solo, por exemplo. Não há limite de
  quantidade de grupos — o teto natural é o número de jogadores (2–4).
- **Nome de exibição do grupo**: o nome do jogador, se o grupo tem 1 membro;
  os nomes concatenados (ex.: "Ana & Bruno"), se tem 2+.
- O **rodízio de leitor e de escolhedor continua por jogador individual** —
  o grupo não muda quem lê nem quem escolhe a dica. Companheiros de grupo do
  leitor adivinham normalmente.

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

## Modo Shot (regra opcional)

- Configurável por partida: `RegrasPartida.modoShot`, padrão **NÃO**;
  quantidade em `RegrasPartida.quantidadeDeShots` (**1 a 3**, padrão **2**).
- Com o modo ligado, cada turno tem `quantidadeDeShots` posições do grid com
  shot, sorteadas **sem repetição** e de forma determinística: a seed dos
  shots deriva da seed das dicas com um **fator próprio**
  (`Partida.seedDosShots` = seed das dicas × 31 + 7) — mesma partida, mesmas
  posições, independentes do embaralhamento das dicas do mesmo turno.
- O shot é **pedágio, não armadilha**: quem escolheu a posição bebe, toca
  "Bebi!" e a dica é revelada **normalmente**. A **pontuação não muda em
  nada** — 11 − N, os pontos do leitor e a invariante dos 10 pontos por turno
  seguem exatamente iguais.
- O grid **nunca marca** a posição com shot antes do toque — surpresa,
  coerente com o grid às cegas.
- Quem bebe: **sempre quem escolheu o número**, sem exceções.
- 🔞 Modo para maiores de idade. Beba com responsabilidade — vale combinar
  shot sem álcool.

## Pontuação (especificação v3 — "cabo de guerra")

- Quem acerta tendo usado **N** dicas (N de 1 a 10) ganha **11 − N** pontos:
  acertou com 1 dica usada → 10 pontos; com 5 → 6 pontos; com 10 → 1 ponto.
  **Inalterado** desde a v1.
- **Leitor**: configurável por partida (`RegrasPartida.leitorPontua`, padrão
  **SIM**). Quando ativo, o leitor ganha **1 ponto por dica revelada sem
  acerto** — ou seja, acerto na dica N dá ao leitor **N − 1** pontos:
  - acerto na dica 3 → acertador 8, leitor 2.
  - acerto na dica 10 → acertador 1, leitor 9.
  - acerto na dica 1 → acertador 10, leitor **0** (nenhuma dica foi revelada
    sem acerto).

  Quando desativado, o leitor ganha 0.
- **Card queimado** (10 dicas sem acerto, ou desistência): o acertador não
  pontua e o leitor ganha **10 pontos** (0 se `leitorPontua` estiver
  desligado) — as 10 dicas foram todas reveladas sem ninguém acertar.
- **Destino dos pontos (v4)**: os pontos calculados para acertador e leitor
  são creditados ao **grupo** de cada um — o jogador é quem age no turno, o
  grupo é quem acumula. Se acertador e leitor forem do mesmo grupo, os dois
  créditos vão para ele.
- **Invariante**: com o leitor pontuando, todo turno distribui **exatamente
  10 pontos** no total — acertador + leitor somam 10 num acerto; o card
  queimado dá os 10 pontos inteiros ao leitor. Na v4, a invariante vale
  **somando por grupo**.

## Partida

- **Baralhos**: a partida usa **1 ou mais baralhos** do catálogo, escolhidos
  na configuração; o monte da partida é a **união dos cards** dos baralhos
  selecionados. A categoria (Personagem de filme, Mundo da música) é
  **metadado do baralho** — etiqueta/filtro visual, herdada pelos cards. O
  espírito da antiga "Livre" sobrevive como "selecionar todos os baralhos".
- **União determinística**: os cards da união são ordenados por chave
  estável — id do baralho, depois id do card — **antes** do embaralhamento
  por seed. Mesma seleção + mesmo código → mesmo monte, em qualquer aparelho
  e em qualquer ordem de download/instalação dos baralhos.
- A partida tem um número configurável de rodadas (`RegrasPartida.numeroDeRodadas`,
  padrão 5).
- O monte é embaralhado de forma determinística a partir do código da partida
  (ex.: "LOBO"), como embaralhamento interno do anfitrião (Fase 4 — Nearby
  Connections); veja `docs/CLAUDE.md` para a arquitetura de multiplayer.

## Placar

- O placar é exibido em **todos os aparelhos** e sincronizado pelo anfitrião
  via Nearby Connections (multiplayer, Fase 4).
- O placar é **agregado por grupo** e exibido com o nome de exibição do grupo
  (nome do jogador se solo, nomes concatenados se time). O grupo acumula os
  pontos de todos os seus jogadores.
- **Empate**: declarado — todos os grupos empatados na maior pontuação são
  vencedores; **não há critério de desempate na v1**.

### Exemplo de partida mista

Partida de 3 rodadas com o grupo **"Ana & Bruno"** e os jogadores solo
**Caio** e **Dani** (leitor pontua ligado):

1. Ana lê; Caio acerta na 1ª dica → grupo de Caio **+10** (Ana ganha 0 — não
   houve dica revelada sem acerto).
2. Bruno lê; Ana acerta na 3ª dica → "Ana & Bruno" **+8** (acertadora) **e
   +2** (o leitor Bruno é do mesmo grupo) — os 10 pontos do turno inteiros
   para o grupo.
3. Caio lê; card queimado → grupo de Caio **+10**.

Placar final: **Caio 20 · Ana & Bruno 10 · Dani 0** — Caio vence.
