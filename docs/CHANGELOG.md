# Changelog

Todas as mudanças notáveis do projeto QuemSou serão documentadas neste arquivo.

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
