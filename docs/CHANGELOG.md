# Changelog

Todas as mudanças notáveis do projeto QuemSou serão documentadas neste arquivo.

## Especificação v3: pontuação do leitor por dica sem acerto (cabo de guerra) (2026-07-08)

Mudança de especificação motivada pelo teste de jogo completo no Z Fold
físico: a regra antiga (leitor ganha os mesmos pontos do acertador) foi
substituída por uma dinâmica de "cabo de guerra" entre leitor e adivinhadores.

- `CalculadoraDePontos.calcular`: o acertador continua ganhando `11 − N`
  pontos (inalterado); o leitor agora ganha **1 ponto por dica revelada sem
  acerto** (`N − 1`) quando `RegrasPartida.leitorPontua`. Acerto na dica 1 →
  leitor 0; acerto na dica 10 → leitor 9.
- `CalculadoraDePontos.ninguemAcertou` passa a receber `RegrasPartida`: o card
  queimado agora dá **10 pontos ao leitor** (antes: ninguém pontuava).
  `EstadoDoTurno.TurnoEncerrado.Queimado` e `Partida.encerrarTurno` foram
  atualizados para propagar esses pontos ao placar.
- Novo teste de invariante: com o leitor pontuando, todo turno distribui
  exatamente 10 pontos no total (acertador + leitor num acerto; os 10 inteiros
  para o leitor num card queimado).
- `PartidaUiState.Anuncio` ganhou `nomeDoLeitor`/`pontosDoLeitor` no nível do
  sealed interface (antes só em `Acerto`), então o anúncio de card queimado
  também exibe o chip de pontos do leitor — sem lógica de pontuação na UI, que
  só exibe o que o domínio calculou.
- `docs/GAME_RULES.md` atualizado com a nova fórmula e exemplos numéricos.
- **Validação final no Z Fold físico (acerto tardio + card queimado):
  PENDENTE** — Felipe valida na próxima sessão de jogo.

## Fechamento documental da Fase 3 (2026-07-04)

Sem mudança de código — apenas documentação (`docs/CLAUDE.md`,
`docs/GAME_RULES.md`).

**Status**: código completo, 93 testes verdes, push feito (commits `a3d815b`,
`67f4f56`, `2874326` — sub-etapas 3.1, 3.2 e 3.3). **Validação de jogo
completo no Z Fold físico: PENDENTE — a ser realizada na próxima sessão.** A
Fase 3 não está marcada como encerrada em `docs/CLAUDE.md` até essa validação
constar como concluída.

- Precisão da seed do grid de dicas em `docs/CLAUDE.md`: seed da partida × 31
  + rodada (`Partida.seedDasDicas`).
- Decisões registradas explicitamente em `docs/CLAUDE.md`: rodízio do
  escolhedor (gira a cada dica revelada, circular entre adivinhadores); fim de
  turno sempre revela a resposta (acerto ou queimado); empate final declarado
  no placar, sem desempate; "Livre" como filtro implementado via
  `RepositorioDeCardsLocal.buscarPorCategoria`/`CardDao.buscarTodas()`.
- `docs/GAME_RULES.md`: adicionada a regra de categoria como filtro de
  baralho (Personagem de filme / Mundo da música / Livre) — estava só em
  `docs/CARDS_GUIDE.md`, que já estava correto e não precisou de mudança.
- Novo item em aberto registrado: nome do app ("QuemSou" é provisório) — a
  única decisão de produto ainda sem fechamento.

## Fase 3.3 — Telas reais da partida (2026-07-04)

Substitui os placeholders da 3.2 pelas telas Compose reais, seguindo os
mockups aprovados. Validação visual no aparelho ainda é manual (Felipe).

- **Home**: título/subtítulo, botão primário "Nova partida", "Entrar em
  partida" visível porém desabilitado com selo "Fase 4", botão texto
  "Como jogar" com diálogo resumindo `docs/GAME_RULES.md`.
- **Setup**: chips de categoria, segmentado Individual/Times, lista de
  jogadores (2–4) com nome/remover/adicionar, seletor de time por jogador
  (Time A/Time B) no modo Times, stepper de rodadas (mínimo 1), toggle
  "Leitor também pontua", botão "Começar partida" com o motivo do bloqueio
  como texto de apoio. Botão de teste da 3.2 removido.
- **Partida**: uma tela só, fases trocadas com `AnimatedContent` sobre o
  `PartidaUiState` — Carregando, VezDeJogar (avatar do leitor, chips dos
  adivinhadores), Grid (resposta visível só enquanto pressionada — nunca fixa
  —, banner do escolhedor da vez, grade 2×5 com posições reveladas em check,
  rodapé fixo de pontos), DicaRevelada (dica em destaque, ≥ 22sp, "queimar
  card" com confirmação), QuemAcertou (`ModalBottomSheet`, escolhedor primeiro
  na lista, nota de pontos do leitor), Anúncio (acerto ou queimado, resposta
  sempre revelada, botão "Próximo turno"/"Ver placar" na última rodada) e
  PlacarFinal (vencedor ou empate declarado, ranking com o(s) 1º(s) lugar(es)
  destacado(s), "Jogar de novo"/"Voltar ao início"). Voltar (em qualquer fase)
  abre diálogo de abandono ligado a `abandonoSolicitado`/`continuarPartida`.
- **Evento novo no `PartidaViewModel`**: `reiniciarPartida()` — mesma
  configuração, código novo sorteado (nova seed), baralho reembaralhado do
  zero; só age na fase `PlacarFinal`.
- **Campos aditivos em `PartidaUiState`** (sem mudar nenhum evento/assinatura):
  `Grid` ganhou `rodada`/`nomeDoLeitor`/`tipo`; `DicaRevelada` ganhou `tipo`;
  `QuemAcertou` ganhou `nomeDoLeitor`/`pontosEmJogo`/`pontosDoLeitor` e agora
  ordena o escolhedor da vez primeiro; `Anuncio` ganhou `nomeDoLeitor`/
  `ultimaRodada`. Necessários para os mockups e não deriváveis na UI sem
  duplicar regra do domínio — só a tradução ficou mais completa.
- **Componentes reutilizáveis** em `presentation/ui/components/`:
  `ChipTipoDeCard`, `AvatarInicial`, `ChipDeJogador`, `RodapeDePontos`,
  `ConfirmDialog` (toda ação destrutiva — queimar card, abandonar partida —
  passa por ele).
- **Tema**: Material 3 agora segue o tema claro/escuro do aparelho
  (`isSystemInDarkTheme()`), com paleta clara provisória equivalente à escura
  da Fase 0.
- **Testes**: 2 novos (`reiniciarPartida` reseta para a rodada 1 com placar
  zerado; ignorado fora do `PlacarFinal`), mais os testes existentes
  atualizados para os campos aditivos e a nova ordem do `QuemAcertou` —
  **93 no total, todos verdes**; `assembleDebug` compila.

## Fase 3.2 — ViewModels e navegação tipada (2026-07-04)

Sem telas reais (ficam para a 3.3) — placeholders mínimos navegáveis por fase.

- **Navegação**: 3 rotas tipadas (`HomeRoute`, `SetupRoute`, `PartidaRoute`);
  as fases do jogo **não são rotas** — são estados da rota Partida. A rota
  Partida carrega só a `ConfiguracaoDaPartida` (JSON serializável em um único
  argumento; nunca objetos de domínio). Rotas antigas Game/Score removidas.
- **SetupViewModel**: categoria (3 opções), modo individual/times, jogadores
  2–4 (adicionar/remover com clamp, renomear, atribuir time), rodadas e
  leitorPontua; validação viva com `podeComecar` + `MotivoDoBloqueio`; ao
  confirmar, sorteia o código (4 letras) e publica a configuração para a
  navegação.
- **PartidaViewModel** (um por partida, escopado à rota): cria a partida via
  `CriarPartida` + `RepositorioDeCards` (interface no domínio, implementação
  Room em `data/`), expõe `StateFlow<PartidaUiState>` (Carregando, VezDeJogar,
  Grid, DicaRevelada, QuemAcertou — pulado com 1 adivinhador —, Anúncio de
  acerto/queimado com a resposta revelada, PlacarFinal com empate declarado) e
  os eventos da partida. Nunca duplica regra do domínio. Eventos fora de fase
  são **ignorados** (guards); exceções do domínio ficam como rede de proteção.
- **Morte de processo**: SavedStateHandle guarda só o não-derivável (rodada,
  placar, fase, posições reveladas na ordem, acertador do anúncio); o resto é
  reconstruído por determinismo — baralho e grid derivam da seed, e reexecutar
  as revelações salvas devolve o turno ao mesmo estado.
- **Voltar** na rota Partida: interceptado (`BackHandler`) e exposto como
  pedido de abandono no ViewModel (UI de confirmação na 3.3).
- **Testes**: 13 novos com `MainDispatcherRule` (validações do Setup, partida
  completa por eventos até o placar final, queimado por 10 dicas, pulo do
  QuemAcertou, leitor sem pontuar, restauração pós-morte de processo, eventos
  inválidos ignorados, pedido de abandono) — **91 no total, todos verdes**;
  `assembleDebug` compila.

## Fase 3.1 — Modelos e regras da partida (2026-07-04)

Domínio puro, sem nenhuma UI (telas ficam para a 3.2).

- **Modelos** em `domain/model` (imutáveis; transições retornam cópias):
  `Jogador` (id, nome, timeId opcional), `ModoDeJogo` (INDIVIDUAL/TIMES),
  `Partida` (2–4 jogadores validados, baralho embaralhado, rodada atual,
  rodízio circular de leitor), `Turno` (card da vez, leitor, 1–3
  adivinhadores, escolhedor da vez, posições reveladas) e `Placar` (pontos por
  jogadorId, ranking, vencedores com empate declarado, soma por time).
- **Máquina de estados do turno** (`EstadoDoTurno`): EscolhendoDica →
  `revelarDica(posicao)` → DicaRevelada → `outraDica()` (avança o escolhedor;
  posição repetida rejeitada) | `registrarAcerto(id)` → TurnoEncerrado.Acerto |
  `queimarCard()` ou 10ª dica sem acerto → TurnoEncerrado.Queimado. O fim de
  turno carrega resposta revelada, dicas usadas e pontos (acertador + leitor
  conforme `leitorPontua`; queimado = 0). Transições inválidas lançam
  **exceção** (abordagem única do domínio, documentada).
- **Grid de dicas às cegas implementado**: `Turno.criar` embaralha as 10 dicas
  nas posições 1–10 com o PRNG xorshift64 da Fase 1 — `EmbaralhadorDeCards`
  generalizado para `<T>` (mesmo algoritmo; nome mantido por histórico) — com
  seed derivada de `seed da partida × 31 + rodada`. Pontuação segue 11 − dicas
  usadas, nunca o número da posição.
- **Avanço da partida**: `encerrarTurno` soma o placar, gira o leitor e avança
  a rodada; após `totalDeRodadas` a partida encerra com placar final;
  `vencedores()` respeita o modo (jogadores ou times) com empate declarado.
- **Use case** `CriarPartida` em `domain/usecase`: código → seed
  (`SeedDeCodigo`) → baralho embaralhado → `Partida` validada.
- **Testes**: 37 novos (rodízios com 1/2/3 adivinhadores, embaralhamento
  determinístico e permutação exata do grid, pontuação nos extremos, máquina
  de estados completa incluindo transições inválidas, giro de leitor,
  encerramento, empate, validações 2–4 e de times) — **78 no total, todos
  verdes** com `./gradlew test`.

## Baralho v2 — 60 cards editoriais (2026-07-04)

- `assets/cards.json` atualizado para `"version": 2` com o baralho editorial
  definitivo: **60 cards** (30 `PERSONAGEM_FILME` + 30 `MUNDO_DA_MUSICA`),
  substituindo os 4 dummies da Fase 2.
- Novo `BaralhoDeAssetsTest`: valida o baralho real com as regras do
  importador (10 dicas, answer não vazio), confere ids únicos e que nenhuma
  dica contém a resposta — suíte com 41 testes, todos verdes.
- **Regras revisadas** (`docs/GAME_RULES.md`): as dicas não têm mais curva de
  dificuldade; o grid 1–10 é escolha às cegas — o app embaralha a posição das
  dicas a cada partida (implementação na Fase 3, no domínio, com
  `clues.shuffled(random)`). Pontuação inalterada: 11 − dicas usadas.
- **Régua editorial** registrada em `docs/CARDS_GUIDE.md`: 10 dicas com mix de
  dificuldades, todas autossuficientes, nenhuma nomeia a resposta (a mais
  forte aponta, não entrega), sem trechos de letras de música.
- **Decisões**: categoria "Livre" resolvida como filtro (união de todas as
  categorias, sem cards exclusivos); nova **Fase 5** no plano — fábrica de
  cards com Gemini (gera → valida → tela de revisão → Room; a partida segue
  offline).

## Fase 2 — Banco de cards (parte técnica) (2026-07-04)

Parte técnica concluída. **Pendente**: cards definitivos (trabalho editorial —
o `cards.json` atual tem só 4 cards DUMMY) e a decisão sobre a categoria
"Livre" (em aberto).

- **Ambiente**: `org.gradle.java.home` fixado no `gradle.properties` apontando
  para o JBR do Android Studio (JVM 21) — o build não depende mais do
  `JAVA_HOME` da máquina.
- **Domínio**: novo campo `RegrasPartida.descartarCardQueimado` (padrão `true`),
  materializando a decisão fechada na revisão v3.
- **Dependências**: DataStore Preferences 1.1.1 e kotlinx-coroutines-test via
  version catalog (Room, KSP e kotlinx-serialization já estavam no projeto
  desde a Fase 0).
- **Camada data** (`data/local/`): `CardEntity` (tabela `cards`, dicas
  serializadas como JSON string via `Converters`), `CardDao` (inserirTodos,
  limparTabela, buscarPorCategoria, buscarTodas, contar), `AppDatabase`
  (version 1, schema exportado em `app/schemas/`), mapper
  `CardEntity` ⇄ `Card` e módulo Hilt `DataModule` (banco, DAO, DataStore).
- **Importador** (`data/importer/`): `assets/cards.json` com `"version": 1` e
  4 cards DUMMY (`TESTE_01`..`TESTE_04`, 2 PERSONAGEM_FILME + 2
  MUNDO_DA_MUSICA, types variados); `CardsImporter` compara a versão do asset
  com a salva no DataStore (chave `cards_db_version`, default 0) e só recarrega
  a tabela quando o asset é mais novo. Validação ruidosa: card com answer
  vazio, dicas faltando ou dica vazia lança exceção com o id do card. Disparado
  no `QuemSouApp` fora da main thread, com log na tag `CardsImporter`.
- **Testes**: 17 novos (parsing e validação do JSON, mapper ida e volta,
  conversores, decisão de versionamento com fakes em memória — sem Robolectric,
  e defaults de `RegrasPartida`) — 37 no total, todos verdes com
  `./gradlew test`, incluindo os 20 da Fase 1.

## docs: revisão de arquitetura e decisões de produto (2026-07-04)

Sem mudança de código — apenas documentação (`docs/CLAUDE.md`,
`docs/GAME_RULES.md`).

- **Arquitetura revisada**: multiplayer deixa de ser "baralho sincronizado por
  seed em cada aparelho" e passa a ser rede local real via Nearby Connections
  API (`play-services-nearby`), modelo estrela (anfitrião = fonte única da
  verdade, distribui cards e sincroniza turno/dica/placar), sem internet
  (Bluetooth/Wi-Fi direto). Biblioteca só instalada na Fase 4. `SeedDeCodigo` e
  `EmbaralhadorDeCards` continuam válidos, agora como embaralhamento interno
  do anfitrião.
- **Jogadores**: mínimo 2, máximo 4 (1 leitor + 1 a 3 adivinhadores por
  rodada).
- **Modo de jogo**: individual ou times, configurável antes da partida (ambos
  na v1).
- **Placar**: exibido em todos os aparelhos, sincronizado pelo anfitrião via
  Nearby.
- **Card queimado**: decisão fechada — descartado (não volta ao baralho);
  campo entra em `RegrasPartida` na Fase 2 (não criado agora).
- **Roadmap atualizado**: Fase 2 = banco de cards + importador Room + campo
  card queimado + `gradle.properties` (`org.gradle.java.home`) · Fase 3 =
  telas (1 partida em 1 celular, sem rede) + modelos `Partida`/`Turno`/`Placar`
  · Fase 4 = multiplayer via Nearby (validação exige 2 aparelhos físicos).
  Backlog: Gemini para cards por IA · Firebase para salas online à distância.

## Fase 1 — Domínio puro

- Modelos de domínio em `domain/model` (Kotlin puro, sem Android): `Card` (exige
  exatamente 10 dicas), `CardType` (PESSOA/LUGAR/COISA), `CardCategory`
  (PERSONAGEM_FILME/MUNDO_DA_MUSICA/LIVRE), `RegrasPartida` (`leitorPontua`
  padrão `true`, `numeroDeRodadas` padrão 5) e `ResultadoTurno`.
- Regras puras em `domain/rules`:
  - `SeedDeCodigo` — código da partida → seed `Long` via hash polinomial
    implementado à mão (normaliza com trim + uppercase; estável entre versões
    de Kotlin/JVM).
  - `EmbaralhadorDeCards` — Fisher–Yates com PRNG xorshift64 próprio; a mesma
    seed gera sempre a mesma ordem em qualquer aparelho (base do multiplayer
    offline). Não usa `kotlin.random.Random` nem `java.util.Random`.
  - `CalculadoraDePontos` — acerto com N dicas vale 11 − N pontos; leitor ganha
    os mesmos pontos se `leitorPontua`; "ninguém acertou" = 0/0.
- 20 testes unitários JVM (`app/src/test/kotlin`) cobrindo seed, embaralhamento
  (determinismo e permutação exata), pontuação e validação do `Card` — todos
  passando via `./gradlew test`.
- Documentação sincronizada: regras de pontuação registradas em
  `docs/GAME_RULES.md`; criado `docs/CLAUDE.md` (guia do projeto, decisões e
  histórico de versões).

## Fase 0 — Esqueleto do projeto

- Estrutura Gradle (Kotlin DSL + version catalog) com Kotlin 2.1.0, Jetpack Compose BOM
  2025.02.00, Material 3, Hilt 2.53.1, Room 2.6.1, Navigation Compose 2.8.9 e
  kotlinx-serialization.
- `minSdk` 26, `targetSdk`/`compileSdk` 35. Pacote `com.quemsou.app`.
- Estrutura em Clean Architecture + MVVM: `domain/`, `data/`, `presentation/ui/`,
  `navigation/`, `di/`.
- Telas placeholder: Home (com botões "Criar partida" e "Entrar com código"),
  Setup, Game e Score — sem nenhuma regra de jogo implementada ainda.
- Navegação com rotas tipadas (`@Serializable`) via Navigation Compose.
- Tema Material 3 escuro padrão (cores definitivas virão em uma fase futura).
- `QuemSouApp` (`@HiltAndroidApp`) e módulo `AppModule` do Hilt preparado, ainda vazio.
- Infra do repositório: `.gitignore`, `docs/BUGS.md`, `docs/IMPROVEMENTS.md`,
  `docs/GAME_RULES.md`, `docs/CARDS_GUIDE.md` e `README.md`.
