# QuemSou — Guia do projeto para o Claude

> **v16 (2026-07-09).** Este arquivo descreve apenas o **estado atual** do
> projeto. Histórico de versões, decisões substituídas e o "porquê" das
> mudanças vivem em `docs/CHANGELOG.md` — não aqui.

## Regras inegociáveis (ler antes de qualquer coisa)

- **Nunca fazer `git push`.** Push é sempre manual do Felipe.
- **Início de todo prompt**: `cd C:\Dev\QuemSou` + `git remote -v`. Só seguir
  se o remoto for `Fehhhh94/QuemSou`.
- **Determinismo é sagrado**: seed e embaralhamento nunca usam `hashCode()`
  da plataforma, `kotlin.random.Random` nem `java.util.Random`. Usar as
  implementações próprias de `domain/rules/` (hash polinomial + xorshift64).
  A mesma seed gera o mesmo baralho, para sempre.
- **Domínio é Kotlin puro**: `domain/` sem Android, Room ou Compose —
  testável na JVM.
- Rodar `./gradlew test` antes de commitar. O JDK do build (JBR, JVM 21) está
  fixado em `gradle.properties` via `org.gradle.java.home`.
- Editar cards = alterar `app/src/main/assets/cards.json` **e incrementar
  `version`**, senão a mudança não chega ao banco Room (espelho recarregado
  pelo `CardsImporter`).

## Visão geral

- Party game Android offline: um leitor lê até 10 dicas de um card e os
  demais tentam adivinhar a resposta; quanto menos dicas, mais pontos.
- Repositório: https://github.com/Fehhhh94/QuemSou · local: `C:\Dev\QuemSou`
- Stack: Kotlin 2.1.0, Jetpack Compose (Material 3), Hilt (KSP), Room,
  DataStore, Navigation Compose com rotas tipadas (`@Serializable`),
  Clean Architecture + MVVM.
- Pacote raiz `com.quemsou.app`. Código em `app/src/main/kotlin/`
  (não `src/main/java/`); testes JVM em `app/src/test/kotlin/`.

## Estado atual

- **Fases 0–3 concluídas** e validadas em jogo completo no Z Fold físico
  (Android 16). Conteúdo editorial: `cards.json` version 3 com **dois
  baralhos embarcados FINALIZADOS** — "Cinema Clássico — Edição 1" (30
  `PERSONAGEM_FILME`) e "Mundo da Música — Edição 1" (30 `MUNDO_DA_MUSICA`)
  —, validados pelo `BaralhoDeAssetsTest`. **156 testes verdes.**
- **Modo Shot entregue** (2026-07-09); validação física no Z Fold pendente
  — incluir na próxima sessão de jogo.
- **Fase 5 — EM ANDAMENTO** (Catálogo de Baralhos): 5.1 concluída
  (`ValidadorEditorial`, régua da fábrica interna); **5A parte 1 concluída**
  (2026-07-09: entidade Baralho + Room v2 + formato do catálogo + seleção
  por baralhos, com o Setup traduzindo os chips de categoria nos baralhos
  embarcados — ponte transitória `BaralhosEmbarcados`). **Próximo passo:
  5A parte 2 (UI do catálogo)** — tela de listar/baixar/atualizar com selos
  de estado e seleção múltipla real no Setup.
- **Fase 4 (Nearby Connections): no backlog**, sem previsão — ver "Backlog".
- Única decisão de produto em aberto: **nome definitivo do app** ("QuemSou"
  é provisório em código, pacote e strings).

## Regras do jogo vigentes

- **Jogadores**: mínimo 2, máximo 4 (1 leitor + 1 a 3 adivinhadores por rodada).
- **Grid às cegas**: posições 1–10 são escolha cega; `Turno.criar` embaralha
  as dicas com seed = **seed da partida × 31 + rodada**
  (`Partida.seedDasDicas`) — grid diferente por turno, sempre reproduzível.
- **Rodízio do escolhedor**: gira a cada dica revelada, circular entre os
  adivinhadores (`Turno.indiceDoEscolhedor`); o leitor nunca escolhe.
- **Pontuação v3 "cabo de guerra"** (`CalculadoraDePontos`; fórmula completa
  em `docs/GAME_RULES.md`): acertador na dica N ganha **11 − N**; leitor
  (se `RegrasPartida.leitorPontua`, padrão SIM) ganha **1 ponto por dica
  revelada sem acerto** (acerto na dica N → N − 1 pontos); **card queimado →
  10 pontos ao leitor**. Todo turno distribui exatamente 10 pontos.
- **Card queimado é descartado**, não volta ao baralho
  (`RegrasPartida.descartarCardQueimado`, padrão SIM).
- **Fim de turno sempre revela a resposta**: `TurnoEncerrado.Acerto` e
  `TurnoEncerrado.Queimado` carregam a resposta; a tela `Anuncio` sempre a exibe.
- **Grupos**: todo jogador pertence a um `Grupo` (id, nome de exibição,
  jogadores, pontos); por padrão cada um nasce em grupo próprio de 1
  (`Grupo.individuais`) — "times" é apenas mesclar 2+ jogadores. Grupos
  mistos são permitidos; não há máximo de grupos (teto natural = nº de
  jogadores). Pontos de acertador/leitor vão para o grupo de quem pontuou
  (`Partida.grupoDe`). Rodízios de leitor/escolhedor continuam **por jogador
  individual**. **Colega de grupo do leitor pode adivinhar na mesma rodada**
  — decisão deliberada, por simplicidade. Nome de exibição: nome do jogador
  se solo, concatenado ("Ana & Bruno") se 2+.
- **Placar agregado por grupo** (`Partida.ranking()`, pontos em
  `Grupo.pontos`). **Empate final é declarado, sem desempate**
  (`Partida.vencedores()` retorna todos os empatados na maior pontuação).
- **Modo Shot** (`RegrasPartida.modoShot`, padrão NÃO; `quantidadeDeShots`
  1–3, padrão 2): shot é pedágio, não armadilha — quem escolheu o número
  bebe, toca "Bebi!" e a dica é revelada normalmente; **pontuação intocada**.
  Posições sorteadas por turno, sem repetição, com seed derivada da seed das
  dicas com fator próprio (`Partida.seedDosShots` = seed das dicas × 31 + 7).
  O grid nunca marca a posição antes do toque; o overlay não é dispensável
  por toque no scrim — só o "Bebi!" avança. A posição pendente sobrevive à
  morte de processo (`SavedStateHandle`). Paleta âmbar/dourada é EXCLUSIVA
  do modo (overlay e card do Setup — nada de âmbar no grid). Nota: álcool
  afeta a classificação etária na Play Store.
- **Seleção por baralhos**: a partida usa 1+ baralhos
  (`ConfiguracaoDaPartida.baralhos`, lista de ids); o monte é a união dos
  cards deles. Categoria é **metadado do baralho** (etiqueta/filtro visual),
  herdada pelos cards; o espírito da "Livre" sobrevive como "selecionar
  todos os baralhos" (`CardCategory.LIVRE` segue no enum só para a UI
  transitória do Setup — nenhum baralho a usa).
- **União determinística** (`Baralho.uniaoDeterministica`): os cards dos
  baralhos selecionados são ordenados por chave estável — id do baralho, id
  do card — ANTES do `EmbaralhadorDeCards`. Mesma seleção + mesma seed →
  mesmo monte, independente da ordem de download/inserção no Room. Ids de
  baralho e de card são imutáveis para sempre (são a chave).
- **`reiniciarPartida`** (no `PlacarFinal`): gera seed nova para os mesmos
  jogadores/regras/categoria e reembaralha do zero — botão "Jogar de novo".

## Arquitetura da UI

- **3 rotas tipadas** (Home, Setup, Partida). Fases do jogo (VezDeJogar,
  Grid, DicaRevelada, QuemAcertou, Anuncio, PlacarFinal) são **estados** do
  `StateFlow<PartidaUiState>` — nunca rotas — trocados com `AnimatedContent`.
- **Um `PartidaViewModel` por partida**, escopado à rota Partida. Nunca
  duplica regra do domínio: só traduz `EstadoDoTurno`/`Placar` em
  `PartidaUiState` e repassa eventos. A rota recebe `ConfiguracaoDaPartida`
  em JSON (um único argumento serializável; nunca objetos de domínio).
- **Restauração pós-morte de processo**: `SavedStateHandle` mínimo +
  recriação determinística por seed, reexecutando as revelações salvas.
- **Erros**: domínio lança exceção (`check`/`require` →
  `IllegalStateException`/`IllegalArgumentException`); o ViewModel **ignora
  eventos fora de fase** (toques duplicados não derrubam o app) — exceções
  são rede de proteção, não fluxo.
- **Edge-to-edge**: o app usa `enableEdgeToEdge()`. Toda barra de ação fora
  do `content` do `Scaffold` deve usar
  `presentation/ui/components/BarraDeAcaoInferior`
  (aplica `navigationBarsPadding()`), como no `bottomBar` do Setup.
- Componentes reutilizáveis em `presentation/ui/components/` (`ChipTipoDeCard`,
  `AvatarInicial`, `ChipDeJogador`, `RodapeDePontos`, `ConfirmDialog`,
  `BarraDeAcaoInferior`). Tema Material 3 claro/escuro.
- Voltar em qualquer fase (`BackHandler`) abre confirmação de abandono;
  confirmar sai para Home.
- Setup: agrupamento opcional via toggle "Jogar em times" + chip cíclico por
  jogador (Sem grupo → Grupo 1–3 → Sem grupo; ciclo só de exibição);
  validação viva com `podeComecar` + `motivoDoBloqueioVisivel`.

## Fase 5 — arquitetura

- **Catálogo estático de baralhos**: o app consome baralhos curados de um
  catálogo hospedado estaticamente (ex.: GitHub raw/releases) — um arquivo
  índice JSON + um JSON por baralho, formato em `docs/CATALOG_FORMAT.md`,
  parse e validação estrutural com violações legíveis no `ParserDoCatalogo`
  (`data/catalogo/`). Sem servidor, sem Firebase, sem chave de API no app.
  A tela de catálogo lista, baixa e atualiza baralhos; o import usa o
  mecanismo de versionamento do `CardsImporter`. A partida segue 100%
  offline — rede só na tela de catálogo.
- **Baralho** (modelo de conteúdo): pertence a uma categoria existente e
  agrupa cards tematicamente (ex.: `PERSONAGEM_FILME` → "Harry Potter").
  Máximo de **100 cards por baralho**; crescimento além disso vira baralho
  novo ("Harry Potter 2"), preferindo subtítulos temáticos quando fizer
  sentido.
- **Ciclo de vida do baralho**: `EM_DESENVOLVIMENTO` (versões novas podem
  adicionar, remover ou melhorar cards; o app atualiza por versionamento) →
  `FINALIZADO` (imutável para sempre; evolução só via novo baralho ou
  extensão). O catálogo e a UI sinalizam o estado (selo "em evolução" vs
  "edição final").
- **Seleção múltipla no Setup**: a partida pode usar a união dos cards de
  1+ baralhos — mesma filosofia da categoria LIVRE (filtro-união
  determinístico, sem entidade "baralho mesclado" persistida).
- **Fábrica interna (ferramenta do desenvolvedor, fora do app)**: pipeline
  Gemini gera → `ValidadorEditorial` valida → revisão humana → publicação no
  catálogo. As decisões da camada Gemini (modelo nunca hardcoded, saída
  estruturada, thinking × `maxOutputTokens`, mapa de erros, timeout 60 s,
  sanitização, sem fallback) estão em `docs/IMPROVEMENTS.md` e valem para a
  ferramenta interna.
- **`ValidadorEditorial`** (`domain/validacao`, feito na 5.1): regras por
  card (resposta não vazia, dica não vazia, dica não contém a resposta
  ignoreCase), retorno `Aprovado` | `Reprovado` com violações acumuladas
  (regra + índice base 0 + mensagem em português numerada 1–10). Regra
  estrutural (10 dicas) fica no construtor de `Card`; regras por baralho no
  `BaralhoDeAssetsTest`; regras de julgamento (autossuficiência, força da
  dica) ficam com o humano na revisão da fábrica interna.
- **Validação estrutural pré-`Card`**: o JSON cru do Gemini precisa de
  validação estrutural ANTES de construir `Card` (8 dicas deve virar
  violação legível na revisão, não exceção do construtor).
- **Sub-fases**: **5A — Catálogo** (entidade Baralho no domínio + Room com
  migração, índice e formato JSON do baralho, tela de catálogo com
  listar/baixar/atualizar e selos de estado, seleção múltipla de baralhos no
  Setup, união determinística na partida; validação no Z Fold) ·
  **5B — Fábrica interna** (pipeline Gemini → validação → revisão como
  ferramenta do desenvolvedor; o formato — script/CLI ou app-side oculto —
  é **decisão em aberto**, a fechar no início da 5B) · **5C — visão
  comercial** (backlog — ver "Backlog").

## Backlog

- **Fase 4 — multiplayer local via Nearby Connections** (adiada em
  2026-07-09): modelo estrela, anfitrião como fonte única da verdade
  (anuncia a sala, distribui cards, sincroniza turno/dica/placar);
  `play-services-nearby` só é instalada nessa fase; seed e
  `EmbaralhadorDeCards` viram embaralhamento interno do anfitrião. Retomada
  começa pelo **desenho da arquitetura da camada Nearby** antes de qualquer
  código; validação exige **2+ aparelhos físicos** (emulador não testa Nearby).
- **Visão comercial de baralhos (5C)**: baralhos customizados pagos sob
  demanda. Restrição registrada: conteúdo com marca registrada (Harry
  Potter etc.) **não pode ser vendido** — customizado pago só para temas
  próprios do cliente. MVP pode ser artesanal (pedido → fábrica interna →
  baralho no catálogo), sem Play Billing. **Alerta de trademark**: também os
  baralhos gratuitos com nome de marca são risco a avaliar antes do
  lançamento na Play Store.
- Salas online à distância (Firebase).

## Documentação — quem é dono do quê

- `docs/GAME_RULES.md` — regras do jogo (fórmulas completas).
- `docs/CHANGELOG.md` — histórico: mudanças por fase, decisões substituídas
  e versões deste guia.
- `docs/BUGS.md` / `docs/IMPROVEMENTS.md` — bugs conhecidos e melhorias.
- `docs/CARDS_GUIDE.md` — guia de criação de cards.
- `docs/CATALOG_FORMAT.md` — formato dos JSONs do catálogo de baralhos
  (índice, baralho e envelope de `assets/cards.json`).
- **Regra de manutenção deste arquivo**: só estado atual, em voz imperativa.
  Ao substituir uma decisão, a antiga é **apagada daqui** e o "antes/depois"
  vai para o `CHANGELOG.md`. Nada de "RESOLVIDA (substitui X)" — a regra
  vigente é escrita como se sempre tivesse sido assim.

## Fluxo de trabalho

- Planejar/decidir no Claude.ai → gerar prompt → executar no Claude Code →
  validar no aparelho → voltar ao Claude.ai para o veredito.
- Mockups visuais (HTML/SVG) aprovados **antes** de qualquer código de tela.
- KDoc e commits em português, formato `tipo: descrição`.
- Antes de sugerir push manual: conferir `git log origin/main..HEAD`.
