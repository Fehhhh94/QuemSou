# Bugs

Bugs encontrados em teste de jogo físico (Samsung Galaxy Z Fold, Android 16)
após o fechamento documental da Fase 3. Corrigidos em código e **validados
no Z Fold físico no fechamento da Fase 3 (2026-07-09)**.

## Checklist de validação física — Modo Shot + Fase 5A (pendente)

Validar no Z Fold físico, **por update em cima do app instalado (sem
desinstalar)** — é isso que exercita as migrações Room de verdade:

1. **Migração Room 1→2→3 + importação**: instalar o APK novo por cima,
   abrir o app e jogar uma rodada completa — sem crash na abertura, cards
   presentes (importador v4 recarrega os dois baralhos embarcados).
2. **Catálogo**: Home → Baralhos; coleções listadas com contagens; filtro
   por categoria; nível 2 com selos ("✓ EDIÇÃO FINAL" / "🧪 EM EVOLUÇÃO");
   nenhum card de baralho listado em lugar nenhum.
3. **Download real do baralho de teste**: baixar "Baralho de Teste —
   Edição 1" (requer o repositório `QuemSou-Baralhos` publicado e a URL
   real no lugar do `TODO_URL_CATALOGO`); ver progresso; baralho aparece no
   Setup depois. Bumpar a `versao` dele no repositório e conferir o botão
   "Atualizar" (âmbar) + pontinho de novidade na coleção.
4. **Modo avião na tela de catálogo**: banner offline, baralhos baixados
   funcionais (dá para jogar), não-baixados esmaecidos com "Requer
   conexão"; catálogo aberto sem cache nenhum → estado indisponível com
   "Tentar de novo".
5. **Dobrar/desdobrar no catálogo** e **durante um download**: a tela
   redesenha e o download continua (ViewModel sobrevive à mudança de
   configuração).
6. **Setup por baralhos**: todos nascem marcados; desmarcar tudo → bloqueio
   "Selecione pelo menos um baralho"; rodadas > cards da união → bloqueio
   de cards insuficientes; "Selecionar todos" e "Catálogo →" funcionam;
   voltar do catálogo atualiza a lista.
7. **Partida com união de 2+ baralhos**: jogar partida completa com os dois
   baralhos embarcados + o de teste, conferindo o **invariante dos 10
   pontos por turno** no placar e o "Jogar de novo" preservando a seleção.
8. **Modo Shot** (pendente da entrega anterior): overlay abre na posição
   sorteada, scrim não dispensa, "Bebi!" revela normalmente; **dobrar/
   desdobrar com o overlay aberto** mantém o overlay; morte de processo
   restaura o overlay; pontuação idêntica com o modo ligado.
9. **Pedir um baralho**: preencher tema e enviar — o Sharesheet abre com o
   texto estruturado.

## 1. Motivo de bloqueio "cards insuficientes" — achado da validação física da 5A (revisão)

- **Sintoma relatado**: no Z Fold, selecionando apenas o baralho de teste (6
  cards) no Setup, o botão "Começar partida" ficava corretamente
  desabilitado, mas nenhum motivo visível aparecia — o usuário não sabia por
  que não podia seguir.
- **Investigação**: `SetupViewModel.kt` e `SetupScreen.kt` já implementam
  exatamente esse comportamento. `motivoDoBloqueio`/`motivoDoBloqueioVisivel`
  cobrem `NENHUM_BARALHO` e `CARDS_INSUFICIENTES` com exibição **imediata**
  (sem o gate de interação que existe só para `NOMES_VAZIOS`), exibidas como
  `Text` de erro no `bottomBar` via `BarraDeAcaoInferior`, com as strings
  `setup_bloqueio_nenhum_baralho` e `setup_bloqueio_cards_insuficientes` já
  cadastradas. O teste `` `uniao com menos cards que rodadas bloqueia` ``
  (`SetupViewModelTest.kt`) já cobre o cenário e passa.
- **Conclusão**: nenhuma mudança de código foi necessária — é exatamente o
  comportamento que o item 6 do checklist de validação física já descreve
  como esperado. O achado deve ter vindo de um APK instalado antes deste
  código existir (build anterior ao commit que trouxe o Setup por
  baralhos).
- **Ação pendente**: revalidar no Z Fold físico com o **APK atual** antes de
  marcar o item 6 do checklist como concluído.

## 2. Validação prematura na tela de Configuração

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
- **Validado no Z Fold físico**: SIM (fechamento da Fase 3, 2026-07-09).

## 3. Categoria "Livre" ausente na tela de Configuração

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
- **Validado no Z Fold físico**: SIM (fechamento da Fase 3, 2026-07-09).

## 4. Botão "Começar partida" cortado pela barra de navegação do sistema

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
- **Validado no Z Fold físico**: SIM (fechamento da Fase 3, 2026-07-09).
