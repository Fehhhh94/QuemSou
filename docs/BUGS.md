# Bugs

Bugs encontrados em teste de jogo físico (Samsung Galaxy Z Fold, Android 16)
após o fechamento documental da Fase 3. Corrigidos em código; **validação
final no aparelho físico: PENDENTE** (Felipe).

## 1. Validação prematura na tela de Configuração

- **Sintoma**: a mensagem "Dê um nome a todos os jogadores" aparecia assim
  que a tela de Configuração abria, antes de qualquer interação do usuário —
  os 2 jogadores em branco do estado inicial já disparavam
  `MotivoDoBloqueio.NOMES_VAZIOS`.
- **Causa raiz**: `SetupScreen.kt` exibia `uiState.motivoDoBloqueio`
  diretamente no `bottomBar`, sem nenhum controle de interação. Como
  `SetupUiState` nasce com 2 `JogadorEmEdicao()` de nome vazio, o motivo de
  bloqueio já existia no primeiro frame.
- **Correção**: `SetupUiState` (`SetupViewModel.kt`) ganhou
  `jogadoresTocados: Set<Int>` e `tentouComecar: Boolean`, e a propriedade
  derivada `motivoDoBloqueioVisivel` — `NOMES_VAZIOS` só aparece quando um
  campo de nome vazio já foi tocado (perdeu o foco ao menos uma vez) ou
  quando o usuário tentou confirmar com a configuração inválida. Os demais
  motivos (times incompletos/insuficientes, poucos jogadores) continuam
  aparecendo imediatamente, pois só surgem como consequência direta de uma
  interação real (ex.: trocar para o modo Times). `SetupScreen.kt` passou a
  usar `motivoDoBloqueioVisivel` no `bottomBar` e marca o campo como tocado
  via `Modifier.onFocusChanged` na perda de foco (com uma guarda local para
  não contar o evento de foco inicial da composição como "toque").
- **Validado no Z Fold físico**: PENDENTE.

## 2. Categoria "Livre" ausente na tela de Configuração

- **Sintoma**: só apareciam os chips "Personagem de filme" e "Mundo da
  música"; havia espaço vazio abaixo deles. A especificação define 3 opções
  (`docs/GAME_RULES.md`): Personagem de filme, Mundo da música e Livre.
- **Causa raiz**: `SecaoCategoria` em `SetupScreen.kt` já montava os 3 chips
  corretamente (`CardCategory.PERSONAGEM_FILME`, `MUNDO_DA_MUSICA`,
  `LIVRE`) — não era um problema de dados ou de categoria ausente no
  domínio. O problema era de layout: os chips ficavam num `Row` comum, que
  não quebra linha. Com os textos em português ("Personagem de filme" é
  longo) mais o padding do `FilterChip`, os 3 chips não cabem numa única
  linha em telas estreitas — no caso, a tela do Z Fold — e o terceiro chip
  ("Livre") ficava fora da área visível, sem nenhum indício visual do corte.
- **Correção**: trocado `Row` por `FlowRow` (`androidx.compose.foundation.layout`,
  `@OptIn(ExperimentalLayoutApi::class)`) em `SecaoCategoria`, que quebra os
  chips para uma nova linha quando não cabem na largura disponível — daí o
  "espaço vazio abaixo" relatado ser exatamente onde o chip "Livre" passa a
  aparecer. Nenhuma mudança no domínio: selecionar "Livre" continua usando
  `RepositorioDeCardsLocal.buscarPorCategoria` → `CardDao.buscarTodas()`, já
  existente.
- **Validado no Z Fold físico**: PENDENTE.

## 3. Botão "Começar partida" cortado pela barra de navegação do sistema

- **Sintoma**: no Z Fold (Android 16, edge-to-edge via `enableEdgeToEdge()`
  em `MainActivity`), o botão "Começar partida" da tela de Configuração
  ficava parcialmente sob a barra de navegação do sistema.
- **Causa raiz**: o `bottomBar` do `Scaffold` em `SetupScreen.kt` era um
  `Column` comum com `Modifier.padding(24.dp)`. Diferente do slot `content`
  do `Scaffold` (que recebe `PaddingValues` já somando
  `WindowInsets.systemBars`), o slot `bottomBar` **não** aplica esse inset
  automaticamente a um `Column` simples — só componentes M3 dedicados
  (`NavigationBar`, `BottomAppBar`) já vêm com esse tratamento embutido.
  Investigação nas outras telas do fluxo (Home, Partida — Grid,
  DicaRevelada, QuemAcertou, Anuncio, PlacarFinal): nenhuma define
  `bottomBar` customizado; todas usam só `content` do `Scaffold`, que já
  recebe o inset correto via `innerPadding`. `SetupScreen` era a única tela
  afetada.
- **Correção**: novo componente reutilizável
  `presentation/ui/components/BarraDeAcaoInferior.kt` — um `Column` que
  aplica `Modifier.navigationBarsPadding()` antes do padding visual de
  24dp. `SetupScreen.kt` passou a usar esse componente no `bottomBar` no
  lugar do `Column` avulso. Fica pronto para reuso caso alguma tela futura
  precise de uma barra de ação inferior fora do `content` do `Scaffold`.
- **Validado no Z Fold físico**: PENDENTE.
