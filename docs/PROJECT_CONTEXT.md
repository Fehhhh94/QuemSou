# Contexto atual — central de jogos e QuemSou

> Atualizado em 2026-09-20. Este é o dono da visão atual de produto,
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

A partida principal é offline após baixar os primeiros baralhos. A internet atende ao catálogo. Por decisão de
Felipe, a fábrica está **em hold**, sem ativação ou desenvolvimento nesta
etapa; novos conteúdos são pedidos ao assistente por tema e integrados
pontualmente. O código existente é preservado. O espelho usa a rede local e acompanha a partida:
cada navegador recebe a fase atual, mas dica e resposta secreta só são
enviadas à sessão do leitor.

### Identidade e navegação

- Home é a central; o bloco QuemSou leva à sua preparação, aos seus baralhos
  e às suas instruções. Próximos jogos deverão ter entrada e fluxo próprios,
  reaproveitando tema e navegação sem compartilhar o histórico por acidente.
- Paleta azul, verde-lima, papel quente e tinta escura, com variante noturna.
  Tipografia forte, formas arredondadas e ilustração vetorial de cartas.
  Dentro da partida, âmbar continua reservado ao Modo Shot.
- Setup começa pelos nomes e grupos, seguido de rodadas e baralhos. Uma nova
  partida começa sem baralho marcado, para a pessoa escolher conscientemente
  o conteúdo; o atalho "Selecionar todos" continua disponível. Leitor pontua,
  Shot e pareamento ficam em Mais opções. Recolher preserva valores e resume
  as regras secundárias que já estão valendo.
- Rodadas são configuradas em ciclos completos: o total acompanha a quantidade
  de jogadores para que todos sejam leitores e adivinhadores o mesmo número de vezes.
- A seleção tem o título "Temas da partida". Conteúdos personalizados ficam
  em **Especiais**, depois dos grupos comuns. Kimberly-Clark — Finanças é o
  primeiro, com 30 respostas e escolha individual, sem seleção automática.
  A implementação reutiliza coleções e ids existentes; versões permanecem
  técnicas. Outros nomes/coleções e as regras de união não foram migrados.
- Falta de conteúdo tem três estados distintos, porque levam a saídas
  diferentes: sem nenhuma resposta elegível no aparelho (baixar baralho),
  nenhum baralho marcado (marcar) e seleção só com baralhos esgotados (trocar
  a seleção). A disponibilidade olha respostas elegíveis, não a lista de
  baralhos: o repositório mantém o baralho e filtra as cartas dentro dele.
  Baralho esgotado continua listado com "0 respostas disponíveis".
- Após cada dica revelada há um convite para avaliar, sem diálogo automático.
  Voto e comentário são opcionais, salvos localmente naquele momento e
  recuperados ao restaurar a dica. Falha no feedback não bloqueia o jogo.
  O registro identifica sessão, rodada, posição, resposta e fato editorial;
  não exporta as dicas ocultas nessa avaliação. Editar o voto não o duplica.
- Avaliar a carta ao fim da rodada continua como opção adicional na Home;
  exportar/limpar funcionam também com essa opção desligada e mantêm seus
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

- Administrador local no navegador disponível pelo atalho
  `Abrir-Administrador.cmd`. Consolida asset, cópia local do catálogo e
  rascunhos privados; valida pelo Gradle real e só aplica após confirmação,
  com conflito de revisão e backup. Uma ação explícita adicional publica a
  versão validada no Firestore ou a retira do índice sem apagar a versão. Não
  faz push, não gera APK e não reativa a fábrica. Evidência:
  `ADMINISTRADOR_LOCAL.md`.

- Modelo editorial de resposta + banco de dicas ativo em
  `acervoEditorial/{respostaId}` (admin-only, upstream de `catalogo/**`):
  `docs/CATALOG_FORMAT.md`. A Central ganhou uma prévia local de migração e um
  editor de banco de dicas (acrescentar, corrigir preservando o id, escopo
  público/privado, desativar/reativar), transporte remoto atômico para até
  500 dicas e projeção controlada para os baralhos. Código validado localmente
  e no emulador; **Rules publicadas e acervo migrado em produção em 2026-09-19**.
  O editor usa a base remota, preserva IDs/histórico, detecta
  conflitos e impede dicas privadas em baralho público. Estado e evidências:
  `docs/ADMINISTRADOR_LOCAL.md`, `docs/HANDOFF_ACTIVE.md`.

- Conceito de partidas implementado localmente: baralhos como coleções de
  respostas, acervo compartilhado entre temas/edições, cartas de dez dicas,
  consumo só das reveladas e rotação por última aparição. Regras e exemplos:
  `docs/GAME_RULES.md`. Evidência atual e pendências: `docs/HANDOFF_ACTIVE.md`.
- Fábrica automática em hold por decisão de Felipe. A implementação anterior
  de pedidos, recuperação, instalação e ampliação foi preservada, sem ativar
  HTTPS ou geração real. Referência técnica para eventual retomada:
  `docs/DECK_STUDIO.md`.

- Fases 0–3 concluídas e validadas em partida completa no Samsung Z Fold,
  Android 16.
- Modo Shot concluído e validado fisicamente.
- Fase 5A (catálogo) e 5B (fábrica/feedback) concluídas e validadas conforme o
  histórico.
- Fase 4A, partes 1 e 2 implementadas: pareamento, continuidade do servidor,
  fases da partida, dica/resposta por leitor, anúncio e placar final. A
  validação física da parte 2 ainda está pendente.
- Fase 4A parte 3 (presença automática e endurecimentos de operação) continua
  pendente. Fase 4 Nearby permanece no backlog e é outra iniciativa.
- Nome definitivo da central em aberto; `Bora Jogar` é provisório. QuemSou
  identifica o primeiro jogo.

O estado operacional e a próxima ação estão em `docs/HANDOFF_ACTIVE.md`.

## Stack e estrutura

- Kotlin 2.1.0, Java 17 no bytecode e JBR/JVM 21 no build.
- Jetpack Compose Material 3, Navigation Compose com rotas tipadas.
- Hilt + KSP, Room, DataStore, kotlinx.serialization, Firebase Auth/Firestore
  e OkHttp.
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

- Room v7 separa conteúdo editorial, reservas, dicas utilizadas, últimas
  aparições de respostas, checkpoints de sessões/rodadas e o estado da fila de
  feedback. Migrações 4→5→6→7 aditivas; o histórico conservador é preservado.
- `CardsImporter` mantém o protocolo do envelope legado; na v8 vazia
  avança apenas o marcador, sem alterar conteúdo instalado ou downloads.
- O catálogo cruza índice remoto/cache com Room e nunca persiste baralho que
  falhe no parser/validador.
- Os baralhos não têm mais estado editorial final: todos podem receber
  correções e novos cards até o teto atual de 500. `versao` e os valores
  antigos de `estado` permanecem apenas como compatibilidade técnica e não
  aparecem na interface.
- GitHub guarda código/regras e fixtures fictícias, não o conteúdo editorial.
  Firebase guarda os baralhos publicados e o acervo de dicas. A Central usa
  cópias privadas fora do Git; caminhos e backup em `ADMINISTRADOR_LOCAL.md`.
  O catálogo GitHub antigo foi descontinuado, sem limpeza do histórico.
- O APK leva somente `{"version":8,"baralhos":[]}`. Na atualização, o
  importador preserva todos os baralhos já instalados, históricos e feedbacks.
  Uma instalação nova precisa baixar conteúdo com internet antes de jogar;
  após o download, a partida funciona offline. Não há fallback GitHub.
- Avaliações de dica reutilizam a tabela Room, com discriminador
  `DICA_REVELADA` e snapshot próprio. Export `quemsou-feedback` v3 mantém
  os campos anteriores e acrescenta esse tipo; avaliação de carta preservada.
  A migração 6→7 adiciona revisão local e confirmação de sincronização. Quando
  há configuração Firebase, WorkManager envia apenas avaliações de dicas e o
  painel as consulta no Firestore; sem rede ou configuração, o jogo continua e
  a fila permanece no aparelho. Não há treinamento nem geração automática.
- Cards e ids de baralho são chaves estáveis. Regras editoriais:
  `docs/CARDS_GUIDE.md`; JSON: `docs/CATALOG_FORMAT.md`.

## Catálogo

A criação sob pedido continua separada e em hold; não existe geração automática
de cards nesta migração. O JSON do catálogo permanece como formato canônico de
autoria, validação e contingência, conforme `CATALOG_FORMAT.md`.

O app passou a consultar o catálogo publicado no Firestore. Cada manifesto
aponta para uma versão imutável dividida em blocos de até 25 cards; hashes
SHA-256 do bloco e do baralho completo são conferidos antes do parser. O último
índice válido fica em cache local e o conteúdo aprovado é instalado no Room,
que continua como fonte de verdade da partida e mantém baralhos baixados
jogáveis sem rede.

Baralhos comuns usam visibilidade `PUBLICO`. A categoria `ESPECIAIS` usa
`PRIVADO` por padrão e exige `leitoresCatalogo/{uid}.ativo == true`, além da
autenticação anônima. O administrador publica somente após salvar e validar o
conteúdo exato. Retirar um baralho apenas muda `publicado` para `false`; versões
e blocos não são apagados.

O catálogo mostra coleções e baralhos, nunca as respostas dos cards. Download
inválido é recusado antes do Room. A régua de publicação é executada por:

```powershell
.\gradlew.bat validarBaralho -Parquivo=<json>
.\gradlew.bat validarCatalogo -Ppasta=<pasta-privada-do-catalogo>
```

## Espelho de leitura — Fase 4A

O anfitrião liga um servidor HTTP local no Setup. Outros jogadores abrem o QR
ou endereço no próprio navegador, escolhem o nome e aguardam. O espelho é
apresentação; o domínio continua sendo a única fonte das regras.

### Partes 1 e 2 implementadas

- `RegistroDeSessoes`: elenco, reivindicação por token, reconexão, marcador
  “este aparelho” e liberação manual de sessão morta.
- `EnderecoLocal`: procura IPv4 alcançável por Wi-Fi/hotspot/Ethernet.
- `KtorServidorDoEspelho`: CIO, portas 8080–8089, cliente estático e SSE.
- `/estado` exige `jogador` + token pertencente à mesma sessão.
- `assets/espelho/`: HTML/JS offline, sem framework e sem CDN.
- Setup: switch opt-in, QR, URL, estado dos jogadores e confirmação de
  liberação. Começar a partida não depende de ninguém conectado.
- O servidor passa do `SetupViewModel` ao `PartidaViewModel`, publica todas as
  fases e cai ao sair da partida. Reconexões recebem o estado mais recente.
- Dica e resposta são filtradas por sessão antes da serialização; durante o
  turno, só o leitor as recebe. Anúncio e placar são públicos para a mesa.

### Pendente

- Parte 3: presença automática de sessões e demais endurecimentos; detalhar o
  escopo antes de implementar.
- Validação em aparelho/rede física de uma partida completa com o espelho.

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

A base anterior passou em 286 testes JVM por variante (Debug/Release), build do
app e do APK de testes, além de 11 instrumentados no emulator-5580. Em turno
posterior, confirmado pelo Codex: as regras ampliadas do catálogo, o índice
composto e sete baralhos de jogo foram publicados no projeto `borajogar-app`
(seis `PUBLICO`, Kimberly-Clark — Finanças `PRIVADO`); o APK
`0.5.0-dev-0919.1436` foi instalado inicialmente no Samsung SM-F966B; e
uma avaliação real na dica 10 do card `mm_028` (Shakira) sincronizou pelo
WorkManager e apareceu na Central. Evidência completa:
`docs/ADMINISTRADOR_LOCAL.md`, seção "Evidências de conclusão". Não houve
comprovação de novo download do especial privado pela interface Android nem lint.
Em 2026-09-20, 292 testes JVM e assembleDebug passaram; a atualização
`0.5.0-dev-0920.0045` foi instalada preservando dados. No Fold/API 36,
Mundo dos Bruxos e Mundo Pop foram atualizados pela UI Firebase, o especial
privado apareceu no catálogo e completou quatro rodadas. Consumo de somente
uma dica e exclusão da resposta com nove restantes foram conferidos em
Instrumentos. Feedbacks e históricos anteriores foram preservados.
O download privado v1 não foi forçado porque a mesma versão já estava instalada.
O acervo editorial (`acervoEditorial/**`) é uma coleção separada, agora ativa;
contagem e evidência da migração estão em `docs/ADMINISTRADOR_LOCAL.md`.
Fábrica segue em hold.
APK e limites da evidência: `docs/HANDOFF_ACTIVE.md`. Saída pelo botão do
placar deixa uma flag de sessão aberta, sem reservas: `docs/BUGS.md`, seção 9.
Sorteio físico da mesma resposta com banco ampliado, TalkBack e revisão humana
do novo conteúdo permanecem pendentes; não equivalem aos cenários acima.

## Roadmap resumido

- Validar a versão pessoal com a turma e escolher o próximo jogo.
- Manter a fábrica em hold e criar conteúdo pontualmente a partir dos temas
  solicitados por Felipe; retomada automática exige nova decisão.

- Validar fisicamente a Fase 4A parte 2 e definir a presença automática da
  parte 3.
- Geração/publicação pela fábrica fica suspensa enquanto estiver em hold.
- Revalidação ritual de restauração por morte de processo no Z Fold.
- Backlog: Nearby Connections, retomar partida após swipe, visão comercial de
  baralhos e salas online.

Detalhes e critérios futuros ficam em `docs/IMPROVEMENTS.md`.
