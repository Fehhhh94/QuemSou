# Regras do Jogo

Este arquivo é o dono das regras do QuemSou: qualquer regra implementada no
código deve estar registrada aqui.

## Tema, resposta, baralho e carta

Conceito aprovado por Felipe em 2026-09-12, antes da continuação da fábrica:

| Conceito | Responsabilidade |
|---|---|
| Tema | Assunto do conteúdo; as categorias atuais continuam sendo as etiquetas disponíveis. |
| Resposta | Identidade do que deve ser adivinhado; pode aparecer em diferentes temas e baralhos. |
| Banco de dicas | Acervo de fatos daquela resposta, compartilhado entre os baralhos instalados. |
| Baralho | Coleção editorial de respostas para uma experiência de jogo, reutilizável entre partidas. |
| Carta | Uma resposta com exatamente dez dicas disponíveis, preparada para uma rodada. |
| Partida | Sequência de respostas distintas escolhidas dos baralhos selecionados conforme o histórico local. |

`AcervoDeRespostas` reúne referências por id canônico ou nome normalizado,
inclusive aliases transitivos. As dicas de todos os baralhos instalados
alimentam o banco da resposta, mesmo quando apenas um deles foi selecionado.
Para correções do mesmo id de dica, prevalece a versão editorial mais alta;
empates usam id do baralho/card. Id e texto normalizado eliminam duplicatas.
Nomes homônimos/aliases precisam de curadoria: o app não infere identidade
semântica. A variedade não estima dificuldade; o desafio depende da curadoria.

## A carta e o consumo de dicas

- A resposta pode voltar em outra partida; as dicas utilizadas neste celular
  não voltam. O acervo pode conter 60 ou mais dicas por resposta, mas a carta
  preparada para jogar continua tendo exatamente dez.
- Ao preparar a rodada, as dez dicas ficam **reservadas**, sem consumo.
  Cada revelação salva o histórico e o checkpoint da partida na mesma
  transação **antes de mostrar o texto**. Falha de gravação não exibe a dica;
  tentar novamente recupera o último estado persistido.
- Acerto, queima ou abandono liberam as dicas ocultas. Exemplo: banco com
  60 dicas, acerto após três revelações → três usadas e **57 disponíveis**,
  incluindo as sete que estavam reservadas e não foram vistas.
- Queimar com menos de dez revelações não consome as restantes. O anúncio
  revela a resposta, sem mostrar automaticamente as dicas ocultas.
- A mesma rodada restaurada recupera carta, posições, fase, pontos e Shot
  pendente. Dicas já reveladas continuam visíveis nessa rodada; não entram
  em cartas futuras. Um checkpoint Room prevalece sobre SavedState atrasado.
- Existe uma partida ativa por instalação. Abandono confirmado libera suas
  reservas; iniciar outra sessão também encerra reservas da anterior. Apenas
  montar o monte não marca respostas nem dicas como jogadas.
- Com menos de dez dicas disponíveis, a resposta fica fora de novas partidas.
  Não existe reciclagem automática. Pedir mais dicas amplia seu acervo.
  As nove (ou menos) restantes são preservadas para quando o acervo crescer.
- O histórico cruza baralhos por identidade editorial e texto normalizado.
  Renomear a edição não reinicia esse histórico. Paráfrases precisam ser
  barradas na revisão editorial.

- Todo card tem uma resposta secreta (pessoa, lugar ou coisa) e **exatamente
  10 dicas** autossuficientes. As dicas **não têm curva de dificuldade** —
  cada card mistura dicas fáceis e difíceis.
- **Grid 1–10 às cegas**: as dicas são apresentadas num grid de 1 a 10 e a
  escolha do número é às cegas — o app **embaralha a posição** das dicas a
  cada turno (embaralhamento determinístico por seed em `Turno.criar`, no
  domínio; a posição não indica dificuldade).
- **Card queimado** (ninguém acertou): é **descartado** — não volta ao
  monte dessa partida (`RegrasPartida.descartarCardQueimado`, padrão **SIM**).
  A resposta pode voltar em outra partida conforme a rotação e as dicas livres.

O histórico é do celular anfitrião/instalação, compartilhado pelos jogadores
locais. Reinstalar/limpar dados pode apagá-lo. No upgrade da versão experimental
Room v5, o consumo antigo de dez dicas é preservado: sem registro de quais
foram vistas, não é seguro devolver as sete retroativamente.

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
  desligado), inclusive na desistência antecipada. Isso não transforma dicas
  ocultas em dicas utilizadas.
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
  por seed. A preparação filtra respostas sem dez dicas inéditas e evita a
  mesma resposta duas vezes na partida. Mesma seleção, conteúdo, histórico e
  código produzem o mesmo monte; aparelhos com históricos diferentes podem
  receber cartas diferentes. O snapshot local preserva a restauração.
- **Rotação de respostas**: inéditas primeiro, depois a menor ordem de última
  aparição. Empates mantêm o embaralhamento pela seed. A aparição é registrada
  uma única vez quando a rodada abre; preparar/restaurar não avança o rodízio.
  O histórico usado para ordenar o monte fica congelado com a sessão.
- Com 40 respostas elegíveis e partidas completas de dez rodadas, as quatro
  primeiras percorrem as 40 antes de repetir; o ciclo seguinte começa pelas
  ausentes há mais tempo. Com acervo pequeno, a frequência possível depende
  da quantidade de respostas elegíveis; não existe intervalo fixo artificial.
- Se houver menos respostas distintas elegíveis que rodadas, bloquear a
  partida e orientar reduzir rodadas ou escolher mais baralhos. Nunca completar
  o monte repetindo uma resposta na mesma partida.
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
