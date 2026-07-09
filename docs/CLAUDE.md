# QuemSou — Guia do projeto para o Claude

## Histórico de versões

- **v1** — esqueleto do documento (Fase 0).
- **v2** (2026-07-04) — URL do GitHub, Fase 0 registrada como concluída, decisão
  "leitor pontua" resolvida, estado da Fase 1.
- **v3** (2026-07-04) — revisão de arquitetura: multiplayer via Nearby
  Connections + decisões de produto fechadas (jogadores 2–4, individual/times,
  placar em todos, card queimado descartado).
- **v4** (2026-07-04) — Fase 2 (parte técnica) concluída: Room + importador de
  cards versionado + campo `descartarCardQueimado` + `org.gradle.java.home`.
  Cards definitivos (trabalho editorial) pendentes; decisão "Livre" em aberto.
- **v5** (2026-07-04) — baralho editorial v2 (60 cards, 30+30); dicas sem
  curva de dificuldade (grid 1–10 às cegas, posições embaralhadas por
  partida); "Livre" resolvida como filtro; Fase 5 (fábrica de cards com
  Gemini) entra no plano.
- **v6** (2026-07-04) — sub-etapa 3.1 concluída: modelos
  Jogador/Partida/Turno/Placar, máquina de estados do turno, rodízios de
  leitor e escolhedor, grid de dicas embaralhado por seed, empate declarado.
  Erros do domínio: exceções (decisão documentada).
- **v7** (2026-07-04) — sub-etapa 3.2 concluída: navegação tipada com 3 rotas,
  SetupViewModel com validação viva, PartidaViewModel único por partida
  (fases do jogo = estados, não rotas), restauração pós-morte de processo por
  determinismo de seed. Telas reais ficam para a 3.3.
- **v8** (2026-07-04) — sub-etapa 3.3 concluída: telas reais da partida
  seguindo os mockups aprovados (placeholders removidos), componentes
  reutilizáveis em `ui/components`, tema claro/escuro, evento
  `reiniciarPartida`. Validação visual no aparelho ainda pendente (Felipe).
- **v9** (2026-07-04) — fechamento documental da Fase 3: código completo
  (3.1, 3.2, 3.3), 93 testes verdes, push feito. Precisões nas decisões (seed
  do grid = seed da partida × 31 + rodada, rodízio do escolhedor, empate
  declarado no placar, "Livre" via `buscarTodas()`) e novo item em aberto
  (nome do app). **Validação de jogo completo no Z Fold físico: PENDENTE** —
  a Fase 3 só fecha depois dela.
- **v10** (2026-07-08) — especificação v3 de pontuação ("cabo de guerra"),
  motivada pelo teste no Z Fold físico: o leitor deixa de ganhar os mesmos
  pontos do acertador e passa a ganhar 1 ponto por dica revelada sem acerto
  (acerto na dica N → leitor N − 1 pontos); card queimado passa a dar 10
  pontos ao leitor (antes: ninguém pontuava). `CalculadoraDePontos`, `Turno`,
  `Partida` e `PartidaUiState.Anuncio` atualizados; 97 testes verdes.
  **Validação final no Z Fold físico (acerto tardio + card queimado):
  PENDENTE.**
- **v11** (2026-07-08) — correções de UI do mesmo teste físico (Z Fold,
  Android 16), detalhadas em `docs/BUGS.md`/`docs/IMPROVEMENTS.md`: validação
  prematura no Setup (novo `motivoDoBloqueioVisivel`), chip "Livre" cortado
  por `Row` sem quebra de linha (trocado por `FlowRow`), botão do Setup sob
  a barra de navegação (`bottomBar` não recebia inset de sistema — novo
  `ui/components/BarraDeAcaoInferior`), e texto "Próxima jogada" no anúncio.
  Só `presentation/ui`, nenhuma mudança de domínio; 103 testes verdes.
  **Validação final no Z Fold físico: PENDENTE.**

## Visão geral

- Party game Android offline: um leitor lê até 10 dicas de um card e os demais
  jogadores tentam adivinhar a resposta; quanto menos dicas, mais pontos.
- Repositório: https://github.com/Fehhhh94/QuemSou
- Stack: Kotlin 2.1.0, Jetpack Compose (Material 3), Hilt (KSP), Room,
  Navigation Compose com rotas tipadas (`@Serializable`), Clean Architecture + MVVM.
- Pacote raiz `com.quemsou.app`. Código Kotlin em `app/src/main/kotlin/`
  (não `src/main/java/`); testes JVM em `app/src/test/kotlin/`.

## Estado do projeto

- **Fase 0 — esqueleto** (Gradle, Hilt, navegação, 4 telas placeholder):
  concluída — commit `f544a09`.
- **Fase 1 — domínio puro** (modelos, seed/embaralhamento determinísticos,
  pontuação, testes JVM): concluída — commit `5227dd8`.
- **Fase 2 — parte técnica concluída** nesta atualização: banco de cards Room
  (`data/local/`), importador de `assets/cards.json` com versionamento via
  DataStore (`data/importer/`), campo `RegrasPartida.descartarCardQueimado` e
  `org.gradle.java.home` no `gradle.properties`. Baralho editorial entregue
  nesta atualização: `cards.json` **version 2** com 60 cards reais
  (30 `PERSONAGEM_FILME` + 30 `MUNDO_DA_MUSICA`), validado pelo
  `BaralhoDeAssetsTest`.
- **Fase 3 — código completo, fechamento pendente de validação física**:
  **3.1** (commit `a3d815b`) — modelos e regras da partida no domínio puro
  (`domain/model` + `CriarPartida` em `domain/usecase`). **3.2**
  (commit `67f4f56`) — navegação tipada (Home, Setup, Partida), `SetupViewModel`
  (validação viva com `podeComecar` + motivo), `PartidaViewModel` (um por
  partida, `StateFlow<PartidaUiState>`, eventos, SavedStateHandle com
  restauração por seed) e `RepositorioDeCards` (interface no domínio,
  implementação Room). **3.3** (commit `2874326`) — telas reais seguindo os
  mockups aprovados, substituindo os placeholders da 3.2: Home (com diálogo
  "Como jogar" e badge "Fase 4" no entrar com código), Setup (chips de
  categoria, segmentado individual/times, jogadores com times, stepper de
  rodadas, toggle leitor pontua) e a tela única da Partida com
  `AnimatedContent` trocando as fases (grid às cegas com revelação por
  pressionar-e-segurar, `ModalBottomSheet` do QuemAcertou, anúncio, placar
  final). Componentes reutilizáveis em `presentation/ui/components/`
  (`ChipTipoDeCard`, `AvatarInicial`, `ChipDeJogador`, `RodapeDePontos`,
  `ConfirmDialog`). Tema Material 3 agora suporta claro/escuro
  (`isSystemInDarkTheme()`).
  **93 testes verdes, push feito.** **PENDENTE: validação de jogo completo no
  Z Fold físico** — não marcar a Fase 3 como encerrada enquanto essa validação
  não constar como concluída.
- **Fase 4 — planejada**: multiplayer via Nearby Connections (é quando a
  biblioteca `play-services-nearby` é instalada); validação exige 2 aparelhos
  físicos.
- **Fase 5 — planejada**: fábrica de cards com Gemini — gera → valida (mesma
  régua do importador) → tela de revisão → Room. A partida em si segue 100%
  offline; só a geração de cards usa rede.
- **Backlog**: salas online à distância (Firebase).

## Arquitetura de multiplayer

- **Revisada** (v3): o multiplayer deixa de ser "baralho sincronizado por seed
  em cada aparelho" e passa a ser rede local real via **Nearby Connections API**
  (`play-services-nearby`), sem depender de internet (Bluetooth/Wi-Fi direto).
- **Modelo estrela**: o anfitrião é a fonte única da verdade. Ele anuncia a
  sala, distribui os cards, e sincroniza turno, dica atual e placar em todos os
  aparelhos conectados.
- A biblioteca só é instalada na Fase 4 — nada de rede nas Fases 2 e 3.
- `SeedDeCodigo` e `EmbaralhadorDeCards` (Fase 1) continuam válidos, mas mudam
  de papel: viram embaralhamento **interno do anfitrião**, não mais mecanismo
  de sincronização de baralho entre aparelhos.

## Decisões de design

- **Leitor pontua** — RESOLVIDA, especificação v3 ("cabo de guerra",
  2026-07-08): configurável em `RegrasPartida.leitorPontua`, padrão SIM;
  quando ativo, o leitor ganha **1 ponto por dica revelada sem acerto**
  (acerto na dica N → leitor N − 1 pontos; card queimado → leitor 10 pontos).
  Todo turno distribui exatamente 10 pontos no total. Ver
  `docs/GAME_RULES.md` para a fórmula completa e `CalculadoraDePontos` no
  domínio.
- **Destino do card queimado** — RESOLVIDA: descartado (não volta ao baralho).
  Campo `RegrasPartida.descartarCardQueimado`, padrão SIM (criado na Fase 2).
- **Categoria "Livre"** — RESOLVIDA: é um **filtro** de baralho, a união de
  todas as categorias — não existem cards exclusivos de `LIVRE`. Implementado
  em `RepositorioDeCardsLocal.buscarPorCategoria`: para `LIVRE` chama
  `CardDao.buscarTodas()`; para as demais, `buscarPorCategoria(categoria.name)`.
- **Dicas às cegas** — RESOLVIDA e implementada (3.1): o grid 1–10 é escolha
  às cegas; `Turno.criar` embaralha as posições das dicas com o PRNG por seed
  da Fase 1 (`EmbaralhadorDeCards`, agora genérico). Seed do grid = **seed da
  partida × 31 + rodada** (`Partida.seedDasDicas`) — grid diferente a cada
  turno, sempre reproduzível. Pontuação inalterada: 11 − quantidade de dicas
  usadas, nunca o número da posição tocada.
- **Rodízio do escolhedor** — RESOLVIDO e implementado (3.1): quem escolhe a
  posição no grid gira **a cada dica revelada**, circular entre os
  adivinhadores (`Turno.indiceDoEscolhedor`, funciona com 1, 2 ou 3); o leitor
  nunca escolhe.
- **Fim de turno sempre revela a resposta** — RESOLVIDO e implementado (3.1):
  tanto `TurnoEncerrado.Acerto` quanto `TurnoEncerrado.Queimado` carregam a
  resposta do card; a UI (`Anuncio`, 3.3) sempre a exibe, independente do
  desfecho.
- **Erros no domínio** — RESOLVIDA: transições e argumentos inválidos lançam
  **exceção** (`IllegalStateException` para estado errado,
  `IllegalArgumentException` para argumento inválido, via `check`/`require`) —
  abordagem única em todo o domínio; não usamos erro tipado de retorno.
  Na camada de UI, o `PartidaViewModel` **ignora eventos fora de fase**
  (toques duplicados não derrubam o app); os guards garantem que só chamadas
  válidas cheguem ao domínio, e as exceções ficam como rede de proteção.
- **Fases do jogo são estados, não rotas** — RESOLVIDA e validada (3.2/3.3):
  o app tem só 3 rotas tipadas (Home, Setup, Partida); VezDeJogar, Grid,
  DicaRevelada, QuemAcertou, Anuncio e PlacarFinal são estados do
  `StateFlow<PartidaUiState>` da rota Partida, trocados com `AnimatedContent`.
  Voltar em qualquer fase (`BackHandler`) abre diálogo de confirmação de
  abandono (`abandonoSolicitado`/`continuarPartida`); confirmar sai para Home.
- **Um ViewModel por partida** — RESOLVIDA (3.2): `PartidaViewModel` único,
  escopado à rota Partida, dirige a partida inteira; nunca duplica regra do
  domínio — só traduz `EstadoDoTurno`/`Placar` em `PartidaUiState` e repassa
  eventos. A rota Partida carrega a `ConfiguracaoDaPartida` em JSON (um único
  argumento serializável; nunca objetos de domínio), e a restauração
  pós-morte de processo reexecuta as revelações salvas sobre a partida
  recriada por seed.
- **Jogadores por partida** — RESOLVIDA: mínimo 2, máximo 4 (1 leitor + 1 a 3
  adivinhadores por rodada).
- **Modo de jogo** — RESOLVIDA: individual ou times, configurável antes da
  partida; ambos os modos entram na v1.
- **Placar** — RESOLVIDA: exibido em todos os aparelhos, sincronizado pelo
  anfitrião via Nearby (ver "Arquitetura de multiplayer" acima). **Empate
  final é declarado, sem desempate**: `Placar.vencedores()`/`vencedoresPorTime`
  retornam todos os empatados na maior pontuação (v1).
- **Evento `reiniciarPartida`** (3.3) — único evento novo autorizado no
  `PartidaViewModel`: no `PlacarFinal`, gera um código novo (seed nova) para os
  mesmos jogadores/modo/regras/categoria e reembaralha o baralho do zero —
  botão "Jogar de novo".
- **Campos aditivos em `PartidaUiState`** (3.3) — nenhum evento ou assinatura
  do `PartidaViewModel` mudou; `Grid` (`rodada`, `nomeDoLeitor`, `tipo`),
  `DicaRevelada` (`tipo`), `QuemAcertou` (`nomeDoLeitor`, `pontosEmJogo`,
  `pontosDoLeitor`, e a lista de adivinhadores agora vem com o escolhedor da
  vez primeiro) e `Anuncio` (`nomeDoLeitor`, `ultimaRodada`) ganharam campos
  que os mockups aprovados exigem e que não eram deriváveis na UI sem duplicar
  regra do domínio — só a tradução ficou mais completa.
- **Nome do app** — EM ABERTO: código, pacote (`com.quemsou.app`) e strings
  usam "QuemSou" provisoriamente; é a única decisão de produto ainda sem
  fechamento.
- **Insets em telas edge-to-edge** — RESOLVIDA (v11, 2026-07-08): o app usa
  `enableEdgeToEdge()` na `MainActivity`, então o `content` do `Scaffold`
  recebe `WindowInsets.systemBars` automaticamente via `innerPadding`, mas um
  `bottomBar` customizado (um `Column` comum, não um componente M3 dedicado
  como `NavigationBar`) **não** recebe esse inset sozinho. Toda barra de ação
  fora do `content` do `Scaffold` deve usar
  `presentation/ui/components/BarraDeAcaoInferior` (aplica
  `Modifier.navigationBarsPadding()`), como no `bottomBar` do Setup.
- **Determinismo é sagrado**: seed e embaralhamento não podem usar `hashCode()`
  da plataforma, `kotlin.random.Random` nem `java.util.Random`. As
  implementações próprias vivem em `domain/rules/` (hash polinomial +
  xorshift64). A mesma seed precisa gerar o mesmo baralho, para sempre — hoje é
  a base do embaralhamento interno do anfitrião (ver "Arquitetura de
  multiplayer").

## Documentação — quem é dono do quê

- `docs/GAME_RULES.md` — dono das regras do jogo.
- `docs/CHANGELOG.md` — mudanças notáveis por fase.
- `docs/BUGS.md` / `docs/IMPROVEMENTS.md` — bugs conhecidos e melhorias futuras.
- `docs/CARDS_GUIDE.md` — guia de criação de cards.

## Fluxo de trabalho

- Domínio (`domain/`) é Kotlin puro: sem Android, Room ou Compose — testável na JVM.
- KDoc e commits em português, formato `tipo: descrição`.
- Rodar `./gradlew test` antes de commitar. O JDK do build (JBR do Android
  Studio, JVM 21) está fixado em `gradle.properties` via `org.gradle.java.home`
  — não é preciso configurar `JAVA_HOME`.
- Cards vivem em `app/src/main/assets/cards.json` (fonte da verdade, com campo
  `version`); o banco Room é só um espelho recarregado pelo `CardsImporter`
  quando a versão do asset avança. Para editar cards: alterar o JSON **e
  incrementar `version`**, senão a mudança não chega ao banco.
- **Push é sempre manual do Felipe** — nunca fazer `git push`.
