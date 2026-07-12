# Changelog

Todas as mudanças notáveis do projeto QuemSou serão documentadas neste arquivo.

## Fechamento da validação física — Modo Shot + 5A + 5B parte 2 (2026-07-12)

Sem mudança de código — apenas documentação (`docs/CLAUDE.md` v22).

- **Checklist de validação física no Z Fold (Android 16): CONCLUÍDO** por
  Felipe em 2026-07-12 (`docs/BUGS.md`): catálogo com rede real (download
  do baralho de teste, bump v2→v3 acendendo o "Atualizar" âmbar + pontinho
  de novidade, comportamento offline), **migração Room 3→4 por update em
  cima do app instalado**, **Modo Shot** (overlay que só avança no "Bebi!",
  pedágio sem mudança de pontuação, invariante dos 10 pontos) e **feedback
  dev** (fluxo completo no Anúncio, export via Sharesheet, limpeza com
  confirmação, invisibilidade total com o modo desligado).
- Com isso, **Modo Shot, 5A e 5B parte 2 (lado app) passam a "concluída e
  validada fisicamente"** — as pendências de validação física dessas
  entregas saem do `docs/CLAUDE.md`; as pendências das seções 1 e 6 do
  `docs/BUGS.md` (revalidar itens 6 e 11 do checklist) fecham juntas.
- **Morte de processo é não-bug** (`docs/BUGS.md` seção 7): diagnóstico de
  2026-07-12 no emulador (API 35) — `am kill` em background e "Don't keep
  activities" restauram a partida perfeitamente; o "reabre na Home" visto
  no Z Fold veio do **swipe nos recentes**, que remove a task e descarta o
  estado salvo por semântica do Android (dispensa intencional). Fica
  pendente só a **revalidação ritual no Z Fold** (am kill + relançamento
  pelo ícone; nunca `am start -n`, que empilha uma activity nova e produz
  falso negativo).
- **"Retomar partida" entra no Backlog** do `docs/CLAUDE.md` (escopo
  aprovado: persistência em disco para a partida sobreviver também ao
  swipe; **mockup pendente — não iniciar sem desenho aprovado**).
- Achado colateral do bump v2→v3: `tamanhoEmBytes` do índice fica defasado
  sem ninguém medir — melhoria registrada em `docs/IMPROVEMENTS.md`
  (`validarCatalogo` conferir declarado vs real).

### 2026-07-12 — CLAUDE.md v21
- **Versão visível no rodapé da Home** + regra **"visível-primeiro"** no
  fluxo de trabalho. Motivo: na validação física da 5B parte 2, um build
  defasado no aparelho passou despercebido porque a feature nova (ativação
  do modo dev) era invisível — olhando a tela, não dava para distinguir
  "não implementado" de "build antigo". Agora: `versionName` no rodapé
  (`0.5.0-dev`, fonte única no `build.gradle.kts`; debug ganha sufixo
  `MMdd.HHmm` que muda a cada build, barato porque o projeto não usa
  configuration cache), e toda feature nova nasce com confirmação visual
  imediata — gesto/easter egg só como refinamento de algo já validado
  visível. `buildConfig = true` ligado para o `BuildConfig.VERSION_NAME`.

### 2026-07-12 — CLAUDE.md v20
- **Ativação do modo dev de feedback**: **antes**, toque longo no título da
  Home (v19 — nunca chegou a ser validado fisicamente); **depois**, Switch
  M3 **"Modo dev"** visível no rodapé da Home, agrupado com o "Exportar
  feedback (N)"/"Limpar feedback" quando o modo está ligado. Decisão de
  produto: o feedback de cards vai virar **feature pública** — controle
  visível é deliberado, não andaime; a transição (renomear, tirar a
  identidade violeta de andaime, padrão ligado/desligado, canal de envio
  que escale) está registrada em `docs/IMPROVEMENTS.md`. Snackbar de
  confirmação permanece; `HomeViewModel.alternarModoDev` inalterado.

### 2026-07-11 — CLAUDE.md v19
- **Ativação do modo dev de feedback**: **antes**, 7 toques no título da
  Home (easter egg padrão Android, contador com janela de 2 s no
  `detectTapGestures`); **depois**, **toque longo** no título
  (`combinedClickable` com `onLongClick`, sem ripple, área de toque
  inflada com padding). Motivo: achado da validação física — os 7 toques
  não ativavam o modo no Z Fold (registro em `docs/BUGS.md` seção 6);
  toque longo é gesto único, sem contador nem janela de tempo. O Snackbar
  de confirmação permanece o mesmo.

## Fase 5B parte 2 (lado app) — feedback de cards dev-only na partida (2026-07-11)

Ferramenta de desenvolvedor, não feature: avaliar cards durante partidas
reais e exportar o resultado para a fábrica interna. Nada toca `domain/`
(segue Kotlin puro e intocado) — tudo vive em `data/feedback/`,
`data/local/` e `presentation/`. Jogador comum nunca vê nada: com o modo
desligado (padrão), nenhum composable do feedback entra na composição.
Mockup aprovado: `mockup-feedback-anuncio-v1.html`.

- **Modo dev (DataStore)**: preferência `modoDevFeedback` (padrão false)
  atrás de `ModoDevFeedbackStore`; alternância exclusivamente por 7 toques
  no título da Home (easter egg padrão Android), Snackbar confirma
  "ativado"/"desativado". Nenhuma outra entrada de UI.
- **Room migração 3→4**: nova tabela `feedback_de_cards`
  (`FeedbackDeCardEntity`: baralhoId, cardId, voto BOM|FRACO, comentário
  opcional, rodada, resultado ACERTO|QUEIMADO, número da dica do acerto,
  criadoEm). Histórico completo por inserção — nunca sobrescreve; **sem FK
  de propósito**: reimportação/remoção de baralho não apaga o histórico da
  fábrica. Migração puramente aditiva com SQL idêntico ao schema exportado
  (`app/schemas/.../4.json`). Nota: como as migrações anteriores, sem teste
  automatizado — o projeto não usa Robolectric/instrumentação; a validação
  é física, por update no Z Fold (checklist em `docs/BUGS.md`).
- **Widget no Anúncio** (Acerto e Queimado, igual): entre o bloco da
  resposta e o botão de avançar, identidade "andaime" — borda tracejada
  violeta (tokens novos `DevVioleta`/`DevVioletaEscuro`; nunca âmbar, que é
  exclusivo do Modo Shot), selo "DEV", chips 👍/👎 com toggle e comentário
  inline que só existe após um voto. Sem botão salvar: "Continuar" grava
  (voto + comentário + dados do turno) e avança; sem voto nada é gravado —
  avaliar é opcional e nunca bloqueia a mesa. Estado do widget em
  `StateFlow` próprio (`PartidaViewModel.feedbackDev`), fora do
  `PartidaUiState`, para voto/tecla não reanimar o `AnimatedContent`; voto
  pendente não sobrevive à morte de processo (limitação aceita, em KDoc).
  `AnuncioContent` ganhou `imePadding` + scroll: com o teclado aberto o
  bloco da resposta encolhe mas a resposta continua visível.
- **Export via Sharesheet** (padrão "Pedir um baralho"): item discreto
  "Exportar feedback (N)" na Home (N vivo via Flow; oculto com N == 0 ou
  modo desligado) monta o JSON `quemsou-feedback` versão 1 — `resposta` de
  cada card via LEFT JOIN com `cards` (nula se o card já não existe no
  aparelho), timestamps ISO-8601 — e dispara ACTION_SEND text/plain.
  Exportar NÃO apaga; "Limpar feedback" é ação separada com ConfirmDialog.
  `HomeViewModel` novo (a Home não tinha ViewModel).
- **174→191 testes verdes** (17 novos): mapeamento entidade↔export com JSON
  estável (`ExportadorDeFeedbackTest`), lógica do `PartidaViewModel`
  (votar/desmarcar/comentar, gravação no continuar com dados do turno,
  nada gravado sem voto, queimado sem número de dica, comentário em branco
  vira null, widget zerado após morte de processo) e `HomeViewModelTest`
  (toggle + aviso, visibilidade do export condicionada ao modo dev e a
  N > 0, limpeza).

### 2026-07-11 — CLAUDE.md v17
- Novas regras inegociáveis de **memória persistente**: em divergência entre
  a memória do Claude Code e os docs versionados, os docs mandam (a memória
  é corrigida imediatamente, nunca defendida); a memória guarda só estado
  operacional (fila de push, pendências entre sessões, armadilhas de
  ambiente) — regras do jogo, arquitetura e decisões de produto têm dono nos
  docs, e a memória no máximo aponta para eles, nunca duplica o conteúdo.

## Fase 5B parte 1 — validador de catálogo como ferramenta de linha de comando (2026-07-11)

Primeira entrega da fábrica interna: uma régua executável para o agente
(Claude Code, operando no repositório `QuemSou-Baralhos`) validar o
trabalho antes da revisão humana — fecha a decisão em aberto de formato da
5B (não é mais pipeline Gemini no app; é uma ferramenta de linha de comando
que roda no repositório de conteúdo).

- **`./gradlew validarBaralho -Parquivo=<caminho>`**: valida um único JSON
  de baralho — estrutura + regras de conjunto (`ParserDoCatalogo`, que já
  embute o `ValidadorDeBaralho`: teto de 100 cards, ids únicos, categoria
  herdada) e, só se a estrutura for válida, as regras editoriais por card
  (`ValidadorEditorial` — a única regra que o parser não cobre, já que
  resposta/dica vazias já são violação estrutural). Saída legível em
  português com origem de cada violação; exit code ≠ 0 se houver qualquer
  violação.
- **`./gradlew validarCatalogo -Ppasta=<raiz>`**: valida o `indice.json` +
  todos os baralhos de `baralhos/` (mesma disciplina do comando acima) +
  **consistência cruzada** índice↔baralho: versão declarada == versão do
  arquivo, quantidade declarada == quantidade real, todo id do índice tem
  arquivo `<id>.json` e vice-versa (nenhum órfão). Este último check é
  exatamente o que teria pego o índice bumpado sem o arquivo do baralho
  acompanhar — achado real da validação física da 5A (`docs/BUGS.md`).
- **Sem módulo novo**: o projeto continua de módulo único (`:app`); os
  pontos de entrada (`fun main`) vivem em
  `app/src/test/kotlin/.../ferramentas/catalogo/` de propósito — reusam o
  classpath já resolvido de `testDebugUnitTest` (JVM pura: o domínio +
  kotlinx.serialization, sem Android em tempo de execução) sem nunca ir
  parar no APK. As duas tasks Gradle são `JavaExec` com esse mesmo
  classpath.
- **Zero regra duplicada**: tudo delega a `ParserDoCatalogo`,
  `ValidadorDeBaralho` e `ValidadorEditorial` já existentes — mesmas regras
  usadas pelo app e pelo importador de assets.
- **164→174 testes verdes** (10 novos, `NucleoDeValidacaoCliTest`: baralho
  válido, arquivo inexistente, violação estrutural, dica que nomeia a
  resposta, catálogo consistente, versão/quantidade divergente, entrada sem
  arquivo, arquivo órfão, índice ausente).
- **Doc**: `docs/CLAUDE.md` (Fase 5 — estado atual e arquitetura) registra
  os dois comandos e fecha a decisão de formato da 5B; aproveitado para
  corrigir uma referência desatualizada ao `TODO_URL_CATALOGO` (já
  resolvido desde a publicação do repositório `QuemSou-Baralhos`).

## Fase 5A parte 2 — UI do catálogo, download e Setup por baralhos (2026-07-09)

Segunda metade da 5A: o catálogo vira produto visível. **Rede existe SOMENTE
na tela de catálogo** — a partida segue 100% offline.

- **Coleção** (complemento do formato da parte 1): `{ id, nome, icone }`
  como metadado de agrupamento no índice e no baralho (`Colecao` no domínio,
  sem regras próprias); Room migra 2→3 (colunas achatadas em `baralhos`);
  `cards.json` v4 com "Cinema Clássico" 🎬 e "Mundo da Música" 🎸. O índice
  também ganhou `tamanhoEmBytes` opcional (meta do card).
- **Camada de rede**: OkHttp puro (única dependência nova; permissão
  INTERNET nova) em `HttpFonteDoCatalogo`, com progresso por bytes e
  cancelamento propagado; URL do índice em constante única
  (`TODO_URL_CATALOGO` — aguarda o repositório `QuemSou-Baralhos`).
  `RepositorioDoCatalogo` deriva NÃO BAIXADO / BAIXADO / ATUALIZAÇÃO
  DISPONÍVEL cruzando índice × Room; **baralho inválido nunca entra no
  Room**; o último índice bem-sucedido fica em cache em disco (offline =
  último estado conhecido). `CardsImporter` virou cirúrgico: update do app
  não apaga downloads. Testes só com fakes — nenhum bate na rede.
- **UI em dois níveis**: rotas novas Catalogo e Colecao + entrada "Baralhos"
  na Home. Coleções com filtro por categoria, pontinho âmbar de novidade e
  card "Pedir um baralho"; baralhos com selo de ciclo de vida (verde/ciano),
  meta e botão de 4 estados com progresso. Cards de baralho nunca são
  listados. Offline: banner + baixados funcionais + não-baixados esmaecidos.
- **"Pedir um baralho"**: formulário → texto estruturado → Sharesheet
  (ACTION_SEND); nada é transmitido pelo app.
- **Setup por baralhos**: seção "Baralhos da partida" (agrupada por coleção,
  checkbox + mini-selo, "Selecionar todos", "Catálogo →", contador vivo da
  união) substitui os chips de categoria; validação nova (nenhum baralho /
  cards insuficientes). Todos os baralhos nascem selecionados — o antigo
  "Livre" como padrão. **Antes/depois**: `CardCategory.LIVRE` e a ponte
  `BaralhosEmbarcados` (parte 1) foram REMOVIDAS — categoria agora é só
  metadado/filtro; a regra do validador "categoria nunca LIVRE" virou
  "categoria desconhecida" no parser.
- **catalogo-seed/** (fora do versionamento): índice + JSONs dos dois
  baralhos embarcados + baralho de teste (6 cards, coleção 🧪,
  EM_DESENVOLVIMENTO) + README de publicação, prontos para o repositório
  `QuemSou-Baralhos`.
- **164 testes verdes** (rede/cache/estados com fakes, Setup por baralhos,
  coleção no parser; antigos adaptados). **Validação física no Z Fold:
  PENDENTE — checklist em `docs/BUGS.md`** (migração 1→2→3 por update,
  download real, modo avião, dobra durante download, união de 2+ baralhos).

## Fase 5A parte 1 — entidade Baralho e seleção por baralhos (2026-07-09)

Primeira metade da 5A: domínio, Room e formato do catálogo. Sem UI nova —
comportamento visível idêntico ao anterior; a tela de catálogo e a seleção
múltipla real ficam para a parte 2.

- **Antes**: a partida era montada por **categoria**
  (`ConfiguracaoDaPartida.categoria`; LIVRE = todas via
  `CardDao.buscarTodas()`), com os cards numa lista plana de
  `assets/cards.json` version 2.
- **Depois**: a partida é montada por **baralhos** (1+):
  `ConfiguracaoDaPartida.baralhos` (lista de ids),
  `RepositorioDeCards.buscarPorIds` e monte =
  `Baralho.uniaoDeterministica` — a união dos cards ordenada por chave
  estável (id do baralho, id do card) ANTES do embaralhamento por seed, com
  teste explícito de que ordens de inserção diferentes geram o mesmo monte.
  Categoria virou **metadado do baralho** (herdada pelos cards); o espírito
  da "Livre" sobrevive como "selecionar todos os baralhos".
- **Domínio**: novo `Baralho` (id estável, nome, categoria, versão, estado
  `EM_DESENVOLVIMENTO`/`FINALIZADO`, cards; contagem derivada) e
  `ValidadorDeBaralho` (violação legível acumulada, filosofia do
  `ValidadorEditorial`): não vazio, **teto de 100 cards**, ids únicos,
  categoria real (nunca LIVRE), card com a categoria do baralho.
  `Partida.baralho` renomeada para `Partida.monte` (colisão com a entidade).
- **Formato do catálogo** (`docs/CATALOG_FORMAT.md`): índice JSON (id, nome,
  categoria, versão, estado, contagem, url, descrição) + JSON por baralho
  (metadados + cards **sem** `category` — herdada). `ParserDoCatalogo` com
  validação estrutural ANTES de construir domínio (8 dicas → violação
  legível com caminho, não exceção) — resolve a lacuna registrada para o
  JSON do Gemini.
- **Room v2**: tabela `baralhos` + `baralhoId` (FK com cascade, índice) em
  `cards`; migração 1→2 recria `cards` preservando os 60 cards da v1
  atribuídos aos dois baralhos pela categoria; SQL conferido contra o schema
  exportado `2.json`.
- **Assets**: `cards.json` version 3 — os 60 cards viram dois baralhos
  embarcados **FINALIZADOS**: "Cinema Clássico — Edição 1"
  (`cinema-classico-1`, 30 `PERSONAGEM_FILME`) e "Mundo da Música —
  Edição 1" (`mundo-da-musica-1`, 30 `MUNDO_DA_MUSICA`). `CardsImporter`
  valida cada baralho pelo parser (falha ruidosa com violações legíveis) e
  recarrega as duas tabelas; `BaralhoDeAssetsTest` valida por baralho,
  incluindo o teto de 100.
- **Ponte transitória**: o Setup mantém os chips de categoria e os traduz
  nos ids embarcados (`BaralhosEmbarcados`; LIVRE = os dois) — some na
  parte 2, quando entra a seleção múltipla real.
- **156 testes verdes** (eram 130) — novos de domínio (Baralho e união),
  validador, parser e importador; os antigos adaptados ao modelo de
  baralhos (o determinismo por seed e todas as regras de jogo intactos).

## Reestruturação da Fase 5 — fábrica no app → Catálogo de Baralhos (2026-07-09)

Decisão de produto, sem mudança de código — apenas documentação
(`docs/CLAUDE.md`, `docs/IMPROVEMENTS.md`).

- **Antes** (sub-fases 5.2–5.4 antigas): geração de cards por IA dentro do
  app, com a chave Gemini do próprio usuário (REST direto, chave no
  DataStore), tela de revisão no app (gerados → validados → fila →
  aprovar/rejeitar → Room) e validação ponta a ponta no Z Fold.
- **Depois** (sub-fases 5A–5C): o app consome um **catálogo estático de
  baralhos curados** — índice JSON + um JSON por baralho, hospedagem
  estática (ex.: GitHub raw/releases), sem servidor, sem Firebase, sem chave
  de API no app; tela de catálogo lista/baixa/atualiza via o versionamento
  do `CardsImporter`. A geração por IA vira **fábrica interna do
  desenvolvedor** (Gemini → `ValidadorEditorial` → revisão humana →
  publicação no catálogo). 5C é a visão comercial em backlog (baralhos
  customizados pagos, com restrição de trademark).
- **Porquê**: exigir chave de API do usuário final é fricção demais para um
  party game; billing na chave do usuário é custo/risco (cota pré-paga do
  Google AI Studio, diagnóstico nas mãos do usuário); e o catálogo estático
  mantém a partida 100% offline — rede só na tela de catálogo.
- Novo modelo de conteúdo: entidade **Baralho** por categoria, teto de
  **100 cards por baralho**, ciclo de vida `EM_DESENVOLVIMENTO` →
  `FINALIZADO` (imutável; selos "em evolução"/"edição final" na UI) e
  **seleção múltipla de baralhos no Setup** (união dos cards, filosofia da
  categoria LIVRE).
- As **7 decisões da camada Gemini** (2026-07-09) continuam valendo,
  aplicadas à fábrica interna — seção renomeada no `docs/IMPROVEMENTS.md`
  ("Fábrica interna — Decisões da camada Gemini").
- A sub-fase **5.1 permanece válida e entregue**: o `ValidadorEditorial` é a
  régua editorial da fábrica interna.

## Modo Shot — shot como pedágio no grid (2026-07-09)

Melhoria pré-5.2, implementada a partir da spec registrada em
`docs/IMPROVEMENTS.md` (agora marcada como entregue). Antes: nenhuma regra de
bebida no jogo. Depois: modo opcional em que algumas posições do grid pedem um
shot antes de revelar a dica — pedágio, não armadilha.

- **Domínio**: `RegrasPartida.modoShot` (padrão NÃO) +
  `quantidadeDeShots` (1–3, padrão 2, validado no `init`). `Turno.criar`
  sorteia as posições com shot (sem repetição, vazio com o modo desligado)
  com o `EmbaralhadorDeCards` e expõe `temShot(posicao)`; a seed dos shots
  continua o polinômio existente com fator próprio (`Partida.seedDosShots` =
  seed das dicas × 31 + 7) — determinística, independente do grid de dicas e
  diferente a cada rodada. A máquina de estados do turno e a pontuação não
  mudaram em nada (invariante dos 10 pontos coberta por teste com o modo
  ligado).
- **UI**: nova fase `SHOT` como estado do `PartidaUiState` (nunca rota) —
  overlay "🥃 UM SHOT!" com o nome de quem bebe (sempre o escolhedor da vez)
  sobre scrim escuro que não dispensa por toque; só o "Bebi!" revela a dica,
  normalmente. Voltar segue a confirmação de abandono da partida. Setup
  ganhou o card do Modo Shot com borda âmbar sutil (única regra 18+):
  toggle, stepper 1–3 e nota de responsabilidade. Paleta âmbar/dourada
  exclusiva do modo; insets via `BarraDeAcaoInferior`.
- **Morte de processo**: a posição pendente do shot é salva no
  `SavedStateHandle` (`estado_shot_pendente`) e a restauração reabre o
  overlay exatamente onde estava (coberto por teste de ViewModel).
- **Navegação**: `ConfiguracaoDaPartida` leva `modoShot`/`quantidadeDeShots`
  com defaults — JSONs antigos seguem decodificáveis.
- `docs/GAME_RULES.md` ganhou a seção "Modo Shot (regra opcional)".
- **130 testes verdes** (eram 115; +9 de domínio e +6 de ViewModel).
- **Validação física no Z Fold: PENDENTE** — incluir na próxima sessão de
  jogo (caso crítico: dobrar/desdobrar com o overlay do shot aberto).

### 2026-07-09 — CLAUDE.md v16
- Guia refatorado: passa a conter apenas o estado atual, em voz imperativa.
- Histórico de versões (v1–v15) e decisões substituídas movidos para a seção
  "Histórico do guia CLAUDE.md" deste arquivo.
- Nova regra de manutenção: decisão substituída é apagada do guia e registrada
  aqui como antes/depois.

## Sub-fase 5.1 — régua editorial extraída para o domínio (2026-07-09)

Refatoração sem mudança de comportamento, preparando a Fase 5: as regras
mecânicas que viviam só dentro do `BaralhoDeAssetsTest` agora são código de
produção no domínio puro, prontas para validar cards gerados por IA antes da
revisão humana.

- Novo pacote `domain/validacao/`: `ValidadorEditorial` (classe pura,
  `validar(card): ResultadoValidacao`), `ResultadoValidacao` (sealed:
  `Aprovado` | `Reprovado(violacoes)`) e `ViolacaoEditorial` (regra violada
  — enum `RegraEditorial` —, índice da dica quando aplicável e mensagem em
  português para a futura tela de revisão). Todas as violações são
  acumuladas, não só a primeira.
- Regras por card extraídas do teste, sem inventar nem remover nenhuma:
  resposta não vazia, nenhuma dica vazia, nenhuma dica contém a resposta
  (sem diferenciar maiúsculas). A regra estrutural de exatamente 10 dicas
  continua no construtor de `Card`/`paraDominio()` (um `Card` construído não
  consegue violá-la); as regras de baralho inteiro (ids únicos, não vazio)
  continuam como testes do `BaralhoDeAssetsTest`.
- `BaralhoDeAssetsTest` refatorado para delegar ao `ValidadorEditorial`,
  falhando com id do card + violações; os 60 cards reais continuam passando.
- Novo `ValidadorEditorialTest`: card válido aprovado, um caso sintético por
  regra (conferindo regra + índice da dica) e acúmulo de múltiplas violações.
- Especificação do **Modo Shot** registrada em `docs/IMPROVEMENTS.md` como
  backlog (implementar após a 5.1; não implementado).
- **115 testes verdes** (eram 110; +5 do `ValidadorEditorialTest`).

## Reprioritização do roadmap — Fase 4 adiada, Fase 5 é o próximo passo (2026-07-09)

Sem mudança de código — apenas documentação (`docs/CLAUDE.md` v14).
Reprioritização decidida pelo Felipe.

- **Fase 4 (Nearby Connections / multiplayer local): adiada para o
  backlog**, sem previsão de retomada por agora. Quando for retomada,
  continua valendo o combinado do fechamento da Fase 3: começar pelo desenho
  da arquitetura da camada Nearby antes de qualquer código.
- **Fase 5 (Fábrica de Cards com Gemini): passa a ser o próximo passo em
  execução**, pulando a ordem original e sem depender da Fase 4 — gera →
  valida (mesma régua do importador) → tela de revisão → Room; a partida
  segue 100% offline, só a geração de cards usa rede.
- A **numeração original das fases é mantida** (não há renumeração).

## Fechamento da Fase 3 — validação física concluída (2026-07-09)

Sem mudança de código — apenas documentação (`docs/CLAUDE.md` v13).

- **Validação de jogo completo no Z Fold físico: CONCLUÍDA** por Felipe,
  cobrindo as três entregas que estavam pendentes: correções de UI
  pós-teste (commit `2273342`), pontuação v3 "cabo de guerra" (commit
  `62342af`) e modelo unificado de Grupos v4 (commit `88735fd`). **Fase 3
  encerrada** — 110 testes verdes, tudo pushado.
- Decisão de design registrada como **deliberada**: colega de grupo do
  leitor **pode** adivinhar na mesma rodada — mantido de propósito, por
  simplicidade (a regra já constava em `docs/GAME_RULES.md`; a intenção
  agora está registrada em `docs/CLAUDE.md`).
- Novo item no fluxo de trabalho, após incidente de sessão (prompt executado
  por engano em outro repositório): todo prompt começa conferindo
  `git remote -v` — só seguir se for `Fehhhh94/QuemSou`.
- Decisão de produto em aberto: nome definitivo do app.
- Próximo passo: **Fase 4 — multiplayer local via Nearby Connections**
  (modelo estrela, anfitrião como fonte da verdade), começando pelo desenho
  da arquitetura da camada Nearby (anúncio de sala, descoberta,
  sincronização de estado) antes de qualquer prompt de código; a validação
  passa a exigir 2+ aparelhos físicos.

## Especificação v4: modelo unificado de Grupos (fim da distinção Individual/Times) (2026-07-09)

Mudança de especificação: os modos "Individual" e "Times" deixam de existir
como bifurcação de código e viram um único conceito de domínio, o **Grupo**.

- **Domínio**: novo modelo `Grupo` (id, nome de exibição, jogadores, pontos).
  Todo jogador pertence a exatamente um grupo; por padrão, cada um nasce em
  um grupo próprio de tamanho 1 (`Grupo.individuais`) — o "individual" é o
  estado padrão do modelo, não um caso especial, e "times" é só mesclar 2+
  jogadores num grupo. `ModoDeJogo`, `Jogador.timeId` e `Placar` (pontos por
  jogador) foram **removidos**: `Partida` agora carrega `List<Grupo>` e
  credita os pontos de acertador e leitor ao grupo do jogador que pontuou
  (`Partida.grupoDe`); `ranking()`/`vencedores()` operam por grupo, com
  empate declarado. Grupos mistos (tamanho 1 e 2+) na mesma partida são
  permitidos sem validação especial e não há constante de máximo de grupos.
  A fórmula v3 de pontuação **não mudou** (acertador 11 − N, leitor N − 1,
  queima = 10 pro leitor; a invariante dos 10 pontos por turno agora soma por
  grupo), nem o rodízio de leitor/escolhedor (continua por jogador).
- **Navegação**: `ConfiguracaoDaPartida` perde `modoDeJogo`;
  `JogadorConfigurado` troca `timeId` por `grupoId` opcional (`null` = grupo
  próprio de 1). JSONs antigos seguem decodificáveis (`ignoreUnknownKeys`).
- **Setup**: o segmentado "Individual | Times" foi removido. Novo toggle
  "Jogar em times" (desligado por padrão — nenhuma UI extra); ligado, cada
  jogador ganha um chip que cicla Sem grupo → Grupo 1 → Grupo 2 → Grupo 3 →
  Sem grupo (ciclo só de exibição — com o teto de 4 jogadores, 3 grupos
  nomeados cobrem qualquer agrupamento). Os motivos de bloqueio
  `TIMES_INCOMPLETOS`/`TIMES_INSUFICIENTES` sumiram: nenhum agrupamento é
  inválido.
- **Placar final**: agregado por grupo, com o nome de exibição (nome do
  jogador se solo, "Ana & Bia" se time). As telas do turno seguem nomeando o
  jogador que agiu (leitor/acertador) — o turno continua por jogador. O
  `SavedStateHandle` agora persiste os pontos por grupo.
- Texto do diálogo "Como jogar" da Home atualizado: ainda descrevia a
  pontuação pré-v3 (leitor ganhando os mesmos pontos do acertador, card
  queimado sem pontos) e foi reescrito com a regra v3 e o modelo de grupos.
- `docs/GAME_RULES.md` reescrito: seção "Jogadores e grupos" no lugar de
  "Modo de jogo", destino dos pontos por grupo e exemplo de partida mista.
- **Testes: 110 verdes** — novos casos de regressão (grupo de 1 se comporta
  exatamente como o "individual" de antes), soma por grupo quando qualquer
  membro pontua, partida mista (grupo de 2 + dois solos, incluindo
  companheiro do leitor acertando) e a invariante dos 10 pontos por turno
  somando por grupo; `PlacarTest` substituído por `GrupoTest`.
- **Validação final no Z Fold físico (partida com 2 solos + 1 grupo de 2):
  PENDENTE** — Felipe valida na próxima sessão de jogo.

## Correções de UI pós-teste físico da Fase 3 (2026-07-08)

3 bugs e 1 melhoria encontrados no teste de jogo físico (Samsung Galaxy Z
Fold, Android 16), registrados em detalhe em `docs/BUGS.md` e
`docs/IMPROVEMENTS.md`. Escopo só de `presentation/ui` — nenhuma mudança de
regra de jogo ou de domínio.

- **Bug 1 — validação prematura no Setup**: `SetupUiState` ganhou
  `jogadoresTocados`/`tentouComecar` e a propriedade `motivoDoBloqueioVisivel`;
  a mensagem "Dê um nome a todos os jogadores" não aparece mais assim que a
  tela abre, só depois de um campo vazio ser tocado ou de uma tentativa de
  confirmar com a configuração inválida.
- **Bug 2 — chip "Livre" ausente**: os 3 chips de categoria já existiam no
  código; o `Row` sem quebra de linha cortava o terceiro chip em telas
  estreitas (Z Fold). Trocado por `FlowRow`.
- **Bug 3 — botão cortado pela barra de navegação**: o `bottomBar` do Setup
  não recebia o inset de `WindowInsets.systemBars` como o `content` do
  `Scaffold` recebe. Novo componente `ui/components/BarraDeAcaoInferior`
  (com `Modifier.navigationBarsPadding()`) resolve e fica pronto para reuso;
  as demais telas do fluxo (Home, Partida) já usavam só `content` e não
  tinham o problema.
- **Melhoria — "Próxima jogada"**: texto do botão da tela Anuncio trocado de
  "Próximo turno" para a linguagem usada à mesa; só o texto exibido mudou.
- **103 testes verdes** (6 novos cobrindo o Bug 1). **Validação final no Z
  Fold físico: PENDENTE** — Felipe valida na próxima sessão de jogo.

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

## Histórico do guia CLAUDE.md (v1–v15)

Bloco de histórico de versões movido verbatim do `docs/CLAUDE.md` na
refatoração para a v16 (2026-07-09).

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
- **v12** (2026-07-09) — especificação v4: modelo unificado de **Grupos**,
  fim da distinção Individual/Times. Novo `domain/model/Grupo` (id, nome de
  exibição, jogadores, pontos); `ModoDeJogo`, `Jogador.timeId` e `Placar`
  removidos; `Partida` carrega `List<Grupo>` (padrão: cada jogador em grupo
  próprio de 1) e credita os pontos ao grupo de quem pontuou; fórmula v3 e
  rodízios inalterados. Setup: toggle "Jogar em times" + chip cíclico de
  grupo no lugar do segmentado; placar final agregado por grupo. 110 testes
  verdes. **Validação final no Z Fold físico (2 solos + 1 grupo de 2):
  PENDENTE.**
- **v13** (2026-07-09) — **Fase 3 encerrada**: validação física no Z Fold
  concluída por Felipe, cobrindo as três entregas pós-v9 (correções de UI
  `2273342`, pontuação v3 "cabo de guerra" `62342af` e modelo unificado de
  Grupos v4 `88735fd`). 110 testes verdes, tudo pushado. Decisão registrada
  como deliberada: **colega de grupo do leitor pode adivinhar na mesma
  rodada** (mantido de propósito, por simplicidade). Novo item no fluxo de
  trabalho: conferir `git remote -v` no início de todo prompt (incidente de
  repositório errado nesta data). Única decisão de produto em aberto: nome
  definitivo do app. Próxima: **Fase 4 — Nearby Connections**, começando
  pelo desenho da arquitetura da camada Nearby (anúncio de sala, descoberta,
  sincronização de estado) antes de qualquer código; validação passa a
  exigir 2+ aparelhos físicos (emulador não testa Nearby).
- **v14** (2026-07-09) — reprioritização do roadmap (decisão do Felipe):
  **Fase 4 (Nearby Connections) adiada para o backlog**, sem previsão de
  retomada por agora; **Fase 5 (Fábrica de Cards com Gemini) passa a ser o
  próximo passo**, sem depender da Fase 4. Numeração original das fases
  mantida (sem renumeração). Nenhuma mudança de código.
- **v15** (2026-07-09) — sub-fase 5.1 concluída: régua editorial mecânica
  extraída do `BaralhoDeAssetsTest` para o domínio puro
  (`domain/validacao/`: `ValidadorEditorial`, `ResultadoValidacao`,
  `ViolacaoEditorial`/`RegraEditorial`), refatoração sem mudança de
  comportamento — o teste do baralho real delega ao validador e os 60 cards
  continuam passando. Especificação do **Modo Shot** registrada em
  `docs/IMPROVEMENTS.md` como backlog (após a 5.1). 115 testes verdes.

### Decisões substituídas (antes/depois)

- **Arquitetura de multiplayer** (revisada na v3 do guia): antes, baralho
  sincronizado por seed em cada aparelho; depois, rede local real via Nearby
  Connections (modelo estrela, anfitrião como fonte única da verdade).
- **Dicas do card** (v5 do guia): antes, dicas com curva de dificuldade;
  depois, grid 1–10 às cegas com posições embaralhadas por seed.
- **Pontuação do leitor** (especificação v3 "cabo de guerra", v10 do guia):
  antes, leitor ganhava os mesmos pontos do acertador e card queimado não
  pontuava; depois, leitor ganha 1 ponto por dica revelada sem acerto
  (acerto na dica N → N − 1 pontos) e card queimado dá 10 pontos ao leitor.
- **Modo de jogo → Grupos** (especificação v4, v12 do guia): antes,
  bifurcação Individual/Times (`ModoDeJogo`, `Jogador.timeId`, `Placar` por
  jogador); depois, modelo unificado de `Grupo` — todo jogador nasce em
  grupo próprio de 1 e "times" é apenas mesclar 2+ jogadores.

Commits de referência das fases iniciais, que constavam no guia antigo:
Fase 0 `f544a09` · Fase 1 `5227dd8` (os commits das sub-etapas da Fase 3 já
constam nas entradas deste changelog).
