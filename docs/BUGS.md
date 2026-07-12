# Bugs

Bugs encontrados em teste de jogo físico (Samsung Galaxy Z Fold, Android 16)
após o fechamento documental da Fase 3. Corrigidos em código e **validados
no Z Fold físico no fechamento da Fase 3 (2026-07-09)**.

## Checklist de validação física — Modo Shot + Fase 5A + 5B parte 2 (CONCLUÍDA em 2026-07-12)

Validada no Z Fold físico (Android 16), **por update em cima do app
instalado (sem desinstalar)** — é isso que exercita as migrações Room de
verdade. **Fechamento por Felipe em 2026-07-12**: catálogo com rede real
(download, bump v2→v3, offline), migração Room 3→4 por update, Modo Shot
(overlay, invariante, pedágio) e feedback dev (fluxo, export, limpeza,
invisibilidade com o modo desligado) validados nesta data; o restante já
havia rodado nas sessões anteriores (achados nas seções abaixo). Única
ressalva: a restauração pós-morte de processo (parte do item 8) é
**não-bug** com revalidação ritual pendente — ver seção 7.

1. ✓ **Migração Room 1→2→3 + importação**: instalar o APK novo por cima,
   abrir o app e jogar uma rodada completa — sem crash na abertura, cards
   presentes (importador v4 recarrega os dois baralhos embarcados).
2. ✓ **Catálogo**: Home → Baralhos; coleções listadas com contagens; filtro
   por categoria; nível 2 com selos ("✓ EDIÇÃO FINAL" / "🧪 EM EVOLUÇÃO");
   nenhum card de baralho listado em lugar nenhum.
3. ✓ **Download real do baralho de teste**: baixar "Baralho de Teste —
   Edição 1"; ver progresso; baralho aparece no Setup depois. Bump da
   `versao` v2→v3 no repositório `QuemSou-Baralhos` acendeu o botão
   "Atualizar" (âmbar) + pontinho de novidade na coleção (2026-07-12).
   Achado colateral do bump: o `tamanhoEmBytes` do índice fica defasado
   sem ninguém medir — melhoria registrada em `docs/IMPROVEMENTS.md`
   (validarCatalogo conferir declarado vs real).
4. ✓ **Modo avião na tela de catálogo**: banner offline, baralhos baixados
   funcionais (dá para jogar), não-baixados esmaecidos com "Requer
   conexão"; catálogo aberto sem cache nenhum → estado indisponível com
   "Tentar de novo".
5. ✓ **Dobrar/desdobrar no catálogo** e **durante um download**: a tela
   redesenha e o download continua (ViewModel sobrevive à mudança de
   configuração).
6. ✓ **Setup por baralhos**: todos nascem marcados; desmarcar tudo → bloqueio
   "Selecione pelo menos um baralho"; rodadas > cards da união → bloqueio
   de cards insuficientes; "Selecionar todos" e "Catálogo →" funcionam;
   voltar do catálogo atualiza a lista. (Fecha também a ação pendente da
   seção 1 — revalidado com o APK atual.)
7. ✓ **Partida com união de 2+ baralhos**: jogar partida completa com os dois
   baralhos embarcados + o de teste, conferindo o **invariante dos 10
   pontos por turno** no placar e o "Jogar de novo" preservando a seleção.
8. ✓ **Modo Shot** (validado 2026-07-12): overlay abre na posição
   sorteada, scrim não dispensa, "Bebi!" revela normalmente; **dobrar/
   desdobrar com o overlay aberto** mantém o overlay; pontuação idêntica
   com o modo ligado. A parte "morte de processo restaura o overlay" é o
   não-bug da seção 7 — revalidação ritual pendente (am kill +
   relançamento pelo ícone).
9. ✓ **Pedir um baralho**: preencher tema e enviar — o Sharesheet abre com o
   texto estruturado.
10. ✓ **Migração Room 3→4 por update** (5B parte 2, validado 2026-07-12):
    instalar o APK novo por cima, abrir o app e jogar — sem crash na
    abertura; baralhos baixados e o histórico de feedback anterior (se
    houver) preservados.
11. ✓ **Fluxo completo de feedback em partida real** (5B parte 2, validado
    2026-07-12 com o Switch): o Switch "Modo dev" no rodapé da Home liga o
    modo (Snackbar); no Anúncio (acerto E queimado) o widget violeta
    tracejado aparece; votar/desmarcar; comentar com o teclado aberto
    mantém a resposta visível; "Continuar" grava; pular (sem voto) não
    grava; com o modo desligado o widget e o export somem e a partida fica
    idêntica à de sempre (só o Switch permanece na Home).
12. ✓ **Export do feedback** (5B parte 2, validado 2026-07-12): "Exportar
    feedback (N)" na Home (N bate com os votos dados) abre o Sharesheet
    com JSON `quemsou-feedback` válido (respostas presentes via join);
    "Limpar feedback" pede confirmação, zera e esconde o item.

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
- **Fechado (2026-07-12)**: item 6 do checklist revalidado no Z Fold com o
  APK atual, no fechamento da validação física — comportamento conforme o
  esperado.

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

## 5. Wrapper do Gradle customizado (UTF-8) — armadilha ao regenerar

- **Contexto**: `gradlew` e `gradlew.bat` estão **customizados** (commits
  `53ceaaf` e `830158e`, 2026-07-11) para matar o mojibake das ferramentas
  de validação no console do Windows, em duas camadas: flags UTF-8 no
  `DEFAULT_JVM_OPTS` (`-Dfile.encoding`/`-Dstdout.encoding`/
  `-Dstderr.encoding` — garantem os bytes corretos com qualquer `java`
  resolvido pelo PATH) e, só no `gradlew.bat`, `chcp 65001` automático em
  volta da execução do java, com restauração do codepage anterior e
  preservação do exit code do Gradle via `cmd /c exit`.
- **Armadilha**: regenerar o wrapper (`./gradlew wrapper ...`) sobrescreve
  os dois scripts e as customizações **somem silenciosamente** — o mojibake
  ("Γ£ô v├ílido" no lugar de "✓ válido") volta ao rodar as ferramentas num
  console recém-aberto.
- **Ação ao regenerar**: reaplicar as customizações (diff contra o commit
  `830158e`) e revalidar com `./gradlew validarCatalogo` num PowerShell
  recém-aberto (sem `chcp` manual), conferindo acentos e ✓/✗ íntegros na
  saída.

## 6. Easter egg de 7 toques não ativava o modo dev no Z Fold

- **Sintoma**: na validação física do fluxo de feedback (item 11 do
  checklist acima), 7 toques no título da Home não ligavam o modo dev de
  feedback no Z Fold — nenhum Snackbar, nenhuma reação.
- **Investigação**: a causa raiz do contador não foi depurada a fundo — a
  implementação usava `detectTapGestures` com contador e janela de 2 s
  entre toques, e ferramenta de dev não justifica sessão de depuração de
  gesto no aparelho. Decisão pragmática: **trocar o mecanismo** por um
  gesto único e sem estado.
- **Correção (1ª tentativa)**: toque longo no título (`combinedClickable`
  com `onLongClick`). Contador, janela de tempo e constantes dos 7 toques
  removidos por inteiro. **Nunca chegou a ser validado fisicamente**: antes
  da validação, a decisão de produto tornou o feedback candidato a feature
  pública e o easter egg deixou de fazer sentido.
- **Correção (vigente)**: Switch M3 **"Modo dev"** visível no rodapé da
  Home (`CLAUDE.md` v20) — o toque longo foi removido junto com o que o
  suportava; o título voltou a texto simples. Snackbar de confirmação
  inalterado nas duas trocas.
- **Validado no Z Fold físico**: SIM (2026-07-12) — item 11 do checklist
  revalidado com o Switch, no fechamento da validação física.

## 7. "Morte de processo não restaura a partida" — não é bug (semântica do swipe)

- **Sintoma relatado** (validação física, Z Fold, Android 16): morte de
  processo durante a partida não restaurava — o app reabria na Home,
  partida perdida, sem crash. Reproduzido em fase comum (dica revelada) e
  com o overlay de shot aberto. Método usado: recentes → deslizar →
  reabrir.
- **Diagnóstico (2026-07-12, emulador Pixel API 35, build desta árvore)**:
  a restauração funciona nos dois andares — navegação e ViewModel:
  - **"Don't keep activities"** (activity recriada, ViewModel morto,
    processo vivo): restaura a mesma fase com a mesma dica. ✓
  - **`adb shell am kill` com o app em background** (morte de processo
    real, PID novo confirmado): restaura a mesma fase com a mesma dica. ✓
  - **Swipe nos recentes**: o sistema **remove a task inteira e descarta o
    estado salvo** (confirmado via `dumpsys activity activities` — a task
    some) antes de qualquer relançamento. É o comportamento padrão do
    Android para dispensa intencional pelo usuário; nenhum mecanismo de
    instance state sobrevive a isso, por design da plataforma — **não é
    bug do app**.
- **Armadilha de método (vale para qualquer revalidação futura)**:
  relançar com `adb shell am start -n com.quemsou.app/.MainActivity`
  **empilha uma MainActivity nova** na mesma task (launchMode standard) e
  a instância nova nasce sem estado — parece "caiu na Home" e produz
  **falso negativo**. Relançar sempre pelo ícone do launcher (ou
  `adb shell monkey -p com.quemsou.app -c android.intent.category.LAUNCHER 1`);
  em caso de dúvida, conferir `sz=1` na task via
  `dumpsys activity activities`.
- **Revalidação ritual no Z Fold: PENDENTE** — HOME → `adb shell am kill
  com.quemsou.app` → reabrir tocando o ícone (nunca `am start -n`); a
  partida deve voltar na mesma fase, com a mesma dica.
- **Decisão de produto derivada**: sobreviver também ao swipe exige
  persistência em disco — backlog "Retomar partida" no `docs/CLAUDE.md`
  (escopo aprovado, mockup pendente).
