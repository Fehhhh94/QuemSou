# Contexto atual do QuemSou

> Atualizado em 2026-08-20. Este é o dono da visão atual de produto,
> arquitetura e estágio. Regras completas e história ficam nos documentos
> apontados.

## Produto

QuemSou é um jogo Android presencial de adivinhação por dicas. Em cada turno,
um jogador lê e os demais tentam descobrir a resposta usando até dez dicas. O
app suporta jogo individual ou grupos, Modo Shot opcional e baralhos locais.

A partida principal é offline. A internet é usada apenas para consultar e
baixar baralhos no catálogo. O espelho de leitura usa HTTP somente dentro da
rede local do anfitrião.

## Estado atual

- Fases 0–3 concluídas e validadas em partida completa no Samsung Z Fold,
  Android 16.
- Modo Shot concluído e validado fisicamente.
- Fase 5A (catálogo) e 5B (fábrica/feedback) concluídas e validadas conforme o
  histórico.
- Fase 4A, parte 1 (pareamento do espelho) implementada no commit local
  `2c09cdc`, coberta por 230 testes JVM e APK debug montado; validação física
  ainda pendente e nenhum push realizado.
- Fase 4A partes 2 e 3 pendentes. Fase 4 Nearby continua no backlog e é outra
  iniciativa.
- Nome definitivo do produto ainda está em aberto; `QuemSou` é provisório.

O estado operacional e a próxima ação estão em `docs/HANDOFF_ACTIVE.md`.

## Stack e estrutura

- Kotlin 2.1.0, Java 17 no bytecode e JBR/JVM 21 no build.
- Jetpack Compose Material 3, Navigation Compose com rotas tipadas.
- Hilt + KSP, Room, DataStore, kotlinx.serialization e OkHttp.
- Ktor server CIO 3.2.4 no espelho local; ZXing `core` para QR em memória.
- Um módulo Android `:app`, pacote `com.quemsou.app`.

```text
app/src/main/kotlin/com/quemsou/app/
├── domain/          regras e modelos puros
├── data/            Room, catálogo, feedback, importação e espelho
├── presentation/    telas, ViewModels, estado de UI e componentes
├── navigation/      rotas tipadas e NavGraph
└── di/              bindings e providers Hilt
```

Fluxo padrão:

```text
Compose → ViewModel → repositório/serviço → fonte local ou rede
                     ↓
                  domain/ para regras do jogo
```

## Fluxo principal

1. Home abre uma nova partida ou o catálogo.
2. Setup seleciona baralhos, jogadores, grupos e regras opcionais.
3. `ConfiguracaoDaPartida` atravessa uma rota tipada até a partida.
4. Um único `PartidaViewModel` traduz o domínio em fases de UI.
5. O jogo alterna leitor, grid, dica, acerto/queima e anúncio.
6. O placar final agrega pontos por grupo e permite jogar novamente.

As fases da partida são estados de `PartidaUiState`, não rotas de navegação.

## Regras críticas

A fonte completa é `docs/GAME_RULES.md`. Contratos que afetam arquitetura:

- 2–4 jogadores; rodízios continuam por jogador mesmo em grupos.
- Grid de dez dicas em ordem determinística e escolha às cegas.
- Acertador recebe `11 - N`; leitor recebe `N - 1` quando habilitado; card
  queimado dá 10 ao leitor. Todo turno distribui 10 pontos.
- Card queimado é descartado; empate final é declarado sem desempate.
- Modo Shot não altera pontuação nem ordem das dicas.
- Seleção múltipla usa união determinística antes do embaralhamento.

## Persistência e conteúdo

- Room guarda baralhos, cards e histórico de feedback.
- `CardsImporter` atualiza somente conteúdo embarcado quando a versão de
  `assets/cards.json` aumenta; downloads não são apagados.
- O catálogo cruza índice remoto/cache com Room e nunca persiste baralho que
  falhe no parser/validador.
- Dois baralhos finais embarcados possuem 30 cards cada: Cinema Clássico e
  Mundo da Música.
- Cards e ids de baralho são chaves estáveis. Regras editoriais:
  `docs/CARDS_GUIDE.md`; JSON: `docs/CATALOG_FORMAT.md`.

## Catálogo

O catálogo é estático, sem Firebase nem backend próprio. A única URL remota do
app fica em `HttpFonteDoCatalogo.URL_DO_INDICE`. O último índice válido fica em
cache local; baralhos baixados continuam jogáveis sem rede.

O catálogo mostra coleções e baralhos, nunca as respostas dos cards. Download
inválido é recusado antes do Room. A régua de publicação é executada por:

```powershell
.\gradlew.bat validarBaralho -Parquivo=<json>
.\gradlew.bat validarCatalogo -Ppasta=<raiz-do-QuemSou-Baralhos>
```

## Espelho de leitura — Fase 4A

O anfitrião liga um servidor HTTP local no Setup. Outros jogadores abrem o QR
ou endereço no próprio navegador, escolhem o nome e aguardam. O espelho é
apresentação; o domínio continua sendo a única fonte das regras.

### Parte 1 implementada

- `RegistroDeSessoes`: elenco, reivindicação por token, reconexão, marcador
  “este aparelho” e liberação manual de sessão morta.
- `EnderecoLocal`: procura IPv4 alcançável por Wi-Fi/hotspot/Ethernet.
- `KtorServidorDoEspelho`: CIO, portas 8080–8089, cliente estático e SSE.
- `/estado` exige `jogador` + token pertencente à mesma sessão.
- `assets/espelho/`: HTML/JS offline, sem framework e sem CDN.
- Setup: switch opt-in, QR, URL, estado dos jogadores e confirmação de
  liberação. Começar a partida não depende de ninguém conectado.
- Servidor pertence ao `SetupViewModel` nesta parte e cai ao sair da tela.

### Pendente

- Parte 2: transportar dica/resposta da vez e prolongar o servidor durante a
  partida.
- Parte 3: escopo ainda não detalhado no documento atual; definir antes de
  implementar.
- Validação em aparelho/rede física do pareamento da parte 1.

Ktor está fixado em 3.2.4 por compatibilidade com Kotlin 2.1.x. Consulte
`docs/BUGS.md`, seção 8, antes de atualizar.

## Restauração e limites Android

`PartidaViewModel` persiste chaves mínimas no `SavedStateHandle` e recria a
partida deterministicamente. Isso cobre recriação e morte de processo com a
task preservada; não cobre swipe nos recentes, que remove a task por semântica
do Android.

Teste correto: colocar o app em background, executar `am kill` e reabrir pelo
ícone. `am start -n` pode empilhar uma Activity e produzir falso negativo.
Detalhes: `docs/BUGS.md`, seção 7.

## Evidência vigente

- Baseline automatizada atual: 230 testes JVM, zero falhas/ignorados.
- `assembleDebug` concluído após o endurecimento do servidor local.
- Sintaxe do cliente web validada com `node --check`.
- Pareamento 4A ainda não possui evidência física registrada.

Não extrapolar essa evidência para Wi-Fi real, QR, visual, TalkBack, tamanho de
fonte ou diferentes fabricantes.

## Roadmap resumido

- Fase 4A partes 2 e 3.
- Primeiro ciclo de geração/publicação de conteúdo na fábrica de baralhos.
- Revalidação ritual de restauração por morte de processo no Z Fold.
- Backlog: Nearby Connections, retomar partida após swipe, visão comercial de
  baralhos e salas online.

Detalhes e critérios futuros ficam em `docs/IMPROVEMENTS.md`.
