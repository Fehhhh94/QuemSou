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
  (Android 16). Baralho editorial: `cards.json` version 2, 60 cards
  (30 `PERSONAGEM_FILME` + 30 `MUNDO_DA_MUSICA`), validado pelo
  `BaralhoDeAssetsTest`. **115 testes verdes.**
- **Fase 5 — EM ANDAMENTO** (Fábrica de Cards com Gemini): sub-fase 5.1
  concluída (commit `775d866`). **Próximo passo: Modo Shot** (spec em
  `docs/IMPROVEMENTS.md`), depois a 5.2.
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
- **Categoria "Livre"** é um filtro: união de todas as categorias, sem cards
  exclusivos. `RepositorioDeCardsLocal.buscarPorCategoria` chama
  `CardDao.buscarTodas()` para `LIVRE`.
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

- **Gemini via REST direto** (`generativelanguage.googleapis.com`), cliente
  HTTP leve; **chave do próprio usuário** no header `x-goog-api-key`, vinda
  do DataStore. Sem Firebase (App Check obrigatório e chave atrelada ao
  desenvolvedor o descartaram). A partida segue 100% offline — só a geração
  de cards usa rede.
- **`ValidadorEditorial`** (`domain/validacao`, feito na 5.1): regras por
  card (resposta não vazia, dica não vazia, dica não contém a resposta
  ignoreCase), retorno `Aprovado` | `Reprovado` com violações acumuladas
  (regra + índice base 0 + mensagem em português numerada 1–10). Regra
  estrutural (10 dicas) fica no construtor de `Card`; regras por baralho no
  `BaralhoDeAssetsTest`; regras de julgamento (autossuficiência, força da
  dica) ficam com o humano na tela de revisão.
- **Lacuna registrada para a 5.2**: o JSON cru do Gemini precisa de validação
  estrutural ANTES de construir `Card` (8 dicas deve virar violação legível
  na tela de revisão, não exceção do construtor).
- **Sub-fases**: 5.2 camada Gemini (interface `GeradorDeCards` no domínio +
  implementação REST na data + tela de configuração da chave) · 5.3 tela de
  revisão (gerados → validados → fila de pendentes → aprovar/rejeitar →
  aprovados entram no Room por categoria) · 5.4 validação ponta a ponta no
  Z Fold.
- **Modo Shot (implementar antes da 5.2; spec em `docs/IMPROVEMENTS.md`)**:
  shot é pedágio, não armadilha — quem escolheu o número bebe, toca "Bebi!"
  e a dica aparece; pontuação intocada. `modoShot: Boolean = false` +
  `quantidadeDeShots: Int = 2` (1–3) em `RegrasPartida`; posições sorteadas
  por seed derivada da fórmula existente com fator próprio; grid não marca a
  posição antes do toque; overlay "🥃 UM SHOT!" com nome de quem bebe, insets
  via `BarraDeAcaoInferior`. Nota: álcool afeta a classificação etária na
  Play Store.

## Backlog

- **Fase 4 — multiplayer local via Nearby Connections** (adiada em
  2026-07-09): modelo estrela, anfitrião como fonte única da verdade
  (anuncia a sala, distribui cards, sincroniza turno/dica/placar);
  `play-services-nearby` só é instalada nessa fase; seed e
  `EmbaralhadorDeCards` viram embaralhamento interno do anfitrião. Retomada
  começa pelo **desenho da arquitetura da camada Nearby** antes de qualquer
  código; validação exige **2+ aparelhos físicos** (emulador não testa Nearby).
- Salas online à distância (Firebase).

## Documentação — quem é dono do quê

- `docs/GAME_RULES.md` — regras do jogo (fórmulas completas).
- `docs/CHANGELOG.md` — histórico: mudanças por fase, decisões substituídas
  e versões deste guia.
- `docs/BUGS.md` / `docs/IMPROVEMENTS.md` — bugs conhecidos e melhorias.
- `docs/CARDS_GUIDE.md` — guia de criação de cards.
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
