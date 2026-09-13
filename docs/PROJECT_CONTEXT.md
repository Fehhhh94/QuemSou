# Contexto atual — central de jogos e QuemSou

> Atualizado em 2026-09-12. Este é o dono da visão atual de produto,
> arquitetura e estágio. Regras completas e história ficam nos documentos
> apontados.

## Produto

O app passa a ser uma central de party games presenciais, com **Bora Jogar**
como nome provisório e **QuemSou** como primeiro jogo disponível. Esta versão
é pessoal, para encontros na casa dos amigos, distribuída por APK. Não há
publicação na Google Play nesta etapa. Outros jogos serão definidos depois;
a Home oferece somente ações que já existem.

No QuemSou, um jogador lê e os demais tentam descobrir a resposta usando até
dez dicas. O jogo suporta 2–4 jogadores, grupos, Modo Shot opcional e baralhos
locais. Tema, resposta, baralho e carta são conceitos do QuemSou, não contratos
obrigatórios dos futuros jogos.

A partida principal é offline. A internet atende ao catálogo e, futuramente,
à geração automática solicitada dentro do app. A fábrica permanece sem
ativação nesta unidade. O pareamento do espelho usa a rede local; transmitir
as dicas durante a partida continua pendente.

### Identidade e navegação

- Home é a central; o bloco QuemSou leva à sua preparação, aos seus baralhos
  e às suas instruções. Próximos jogos deverão ter entrada e fluxo próprios,
  reaproveitando tema e navegação sem compartilhar o histórico por acidente.
- Paleta azul, verde-lima, papel quente e tinta escura, com variante noturna.
  Tipografia forte, formas arredondadas e ilustração vetorial de cartas.
  Dentro da partida, âmbar continua reservado ao Modo Shot.
- Setup começa pelos nomes e grupos, seguido de rodadas e baralhos. Leitor
  pontua, Shot e pareamento ficam em Mais opções. Recolher preserva valores
  e resume as regras secundárias que já estão valendo.
- Falta de conteúdo tem três estados distintos, porque levam a saídas
  diferentes: sem nenhuma resposta elegível no aparelho (baixar baralho),
  nenhum baralho marcado (marcar) e seleção só com baralhos esgotados (trocar
  a seleção). A disponibilidade olha respostas elegíveis, não a lista de
  baralhos: o repositório mantém o baralho e filtra as cartas dentro dele.
  Baralho esgotado continua listado com "0 respostas disponíveis".
- Avaliar cartas continua acessível na Home; exportar/limpar mantêm seus
  controles e confirmação. Criação de baralhos fica recolhida; a tela da fábrica
  apresenta a conexão quando necessária. Identificação do build permanece no rodapé.
- Home e Setup têm conteúdo limitado em largura; conteúdo longo pode rolar.
  As dicas ganham apresentação de carta, sem alterar a escolha às cegas.
  A arte decorativa da Home sai em janela baixa para não empurrar a chamada
  de jogar; o grid tem teto de largura e fica na zona do polegar; ações usam
  altura mínima, não fixa, para não cortar rótulo em fonte ampliada.
- A tela da dica se reorganiza abaixo de 560 dp de altura disponível (como em
  paisagem de celular): ações principais lado a lado
  e corpo de texto proporcional, para que a área de leitura continue sendo uma
  janela e não uma faixa. Acima do limiar, o layout de retrato é o mesmo.
- Partida sem conteúdo elegível apresenta uma tela de estado com saída para
  o Setup — é lá que se reduz rodadas ou se escolhem mais baralhos.
- Nome exibido e ícone podem evoluir, mas `applicationId = com.quemsou.app`
  permanece para preservar a instalação, o acervo e os históricos locais.

## Estado atual

- Conceito de partidas implementado localmente: baralhos como coleções de
  respostas, acervo compartilhado entre temas/edições, cartas de dez dicas,
  consumo só das reveladas e rotação por última aparição. Regras e exemplos:
  `docs/GAME_RULES.md`. Evidência atual e pendências: `docs/HANDOFF_ACTIVE.md`.
- Fábrica automática: Felipe autorizou sua implementação e escolheu este computador ligado como
  host. Pedidos persistentes, recuperação, instalação e ampliação foram
  implementados; HTTPS e geração real seguem sem ativação. Operação:
  `docs/DECK_STUDIO.md`.

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
- Nome definitivo da central em aberto; `Bora Jogar` é provisório. QuemSou
  identifica o primeiro jogo.

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

1. A central apresenta QuemSou: jogar, baralhos e como jogar.
2. Preparar QuemSou reúne jogadores, grupos, rodadas, baralhos e opções.
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

- Room v6 separa conteúdo editorial, reservas, dicas utilizadas, últimas
  aparições de respostas e checkpoints de sessões/rodadas. Migrações 4→5→6
  aditivas; o histórico conservador da v5 é preservado.
- `CardsImporter` atualiza somente conteúdo embarcado quando a versão de
  `assets/cards.json` aumenta; downloads não são apagados.
- O catálogo cruza índice remoto/cache com Room e nunca persiste baralho que
  falhe no parser/validador.
- Dois baralhos finais embarcados possuem 30 cards cada: Cinema Clássico e
  Mundo da Música.
- Cards e ids de baralho são chaves estáveis. Regras editoriais:
  `docs/CARDS_GUIDE.md`; JSON: `docs/CATALOG_FORMAT.md`.

## Catálogo

A criação sob pedido é um serviço separado do catálogo estático, com HTTPS
autenticado. O app recebe o resultado e instala o conteúdo validado numa
transação. O servidor ainda não está ativado; ver `DECK_STUDIO.md`.

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

`PartidaViewModel` persiste chaves mínimas e id da sessão no `SavedStateHandle`;
Room preserva o conteúdo da sessão e as cartas preparadas. A restauração
recupera as mesmas dicas sem novo consumo. Isso cobre morte de processo com a
task preservada; não cobre swipe nos recentes, que remove a task por semântica
do Android.

Teste correto: colocar o app em background, executar `am kill` e reabrir pelo
ícone. `am start -n` pode empilhar uma Activity e produzir falso negativo.
Detalhes: `docs/BUGS.md`, seção 7.

## Evidência vigente

A base atual tem 270 testes JVM por variante, 23 instrumentados no emulador
Pixel_1/API 35 e 17 testes Python (também em modo otimizado), além de build e
lint sem erros. A fábrica tem testes de reabertura do pedido persistido,
instalação/ampliação com partida ativa e HTTP local com geração simulada.
O resultado, o APK e os limites visuais estão em `docs/HANDOFF_ACTIVE.md`.
Não confundir esses números com validação física no aparelho, TalkBack ou
operação real da fábrica — todos seguem pendentes.

## Roadmap resumido

- Validar a versão pessoal com a turma e escolher o próximo jogo.
- Evoluir a fábrica conforme o conceito aprovado, com geração dentro do app.

- Fase 4A partes 2 e 3.
- Primeiro ciclo de geração/publicação de conteúdo na fábrica de baralhos.
- Revalidação ritual de restauração por morte de processo no Z Fold.
- Backlog: Nearby Connections, retomar partida após swipe, visão comercial de
  baralhos e salas online.

Detalhes e critérios futuros ficam em `docs/IMPROVEMENTS.md`.
