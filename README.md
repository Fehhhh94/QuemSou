# QuemSou

Jogo Android presencial de adivinhação por dicas. Um jogador lê até dez dicas;
os demais tentam descobrir a resposta. O jogo suporta grupos, baralhos locais,
Modo Shot opcional e um espelho de leitura acessível pelo navegador na rede
local.

## Estado do projeto

- Partida principal offline e jogável em um único aparelho.
- Catálogo Firebase com cache: primeira instalação exige baixar baralhos;
  depois, as partidas funcionam offline.
- Conteúdo editorial e backups fora do Git. O APK não embarca respostas/dicas.
- Fase 4A parte 1 implementada: pareamento do espelho por QR/URL local.
- Partes 2 e 3 do espelho ainda pendentes.

Estado e próxima ação: [Handoff ativo](docs/HANDOFF_ACTIVE.md).

## Começar rápido

Pré-requisitos: Android Studio/JBR configurado pelo projeto e Android SDK 35.

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Administrar baralhos nesta máquina

Dê dois cliques em `Abrir-Administrador.cmd`. A Central de Baralhos abre no
navegador local para consultar, editar rascunhos e validar com a régua do app.
Ela também mostra automaticamente sob cada dica os feedbacks sincronizados pelo
app via Firestore, mantém a importação manual como contingência, prepara pedidos
de revisão para o Codex e permite remover uma origem local com backup. Depois
de salvar e validar o conteúdo exato, uma ação separada pode publicar ou retirar
o baralho do catálogo no Firestore, sempre com confirmação. O painel não faz
push, não gera APK e não reativa a fábrica automática.

Instruções: [administrador/README.md](administrador/README.md).

## Stack

- Kotlin + Jetpack Compose Material 3
- Clean Architecture + MVVM
- Hilt/KSP, Room e DataStore
- Navigation Compose com rotas tipadas
- Firebase Auth/Firestore para catálogo e feedback; Room mantém o jogo offline
- Ktor CIO para servidor HTTP local
- ZXing core para QR gerado no aparelho

## Documentação

Use o [índice da documentação](docs/DOCS_INDEX.md) para carregar somente o que
precisa:

- [Contexto e arquitetura atuais](docs/PROJECT_CONTEXT.md)
- [Regras do jogo](docs/GAME_RULES.md)
- [Runbook de suporte](docs/SUPPORT_RUNBOOK.md)
- [Bugs e armadilhas](docs/BUGS.md)
- [Formato do catálogo](docs/CATALOG_FORMAT.md)
- [Criação de cards](docs/CARDS_GUIDE.md)
- [Histórico](docs/CHANGELOG.md)

Agentes devem começar por `AGENTS.md` ou `CLAUDE.md`. **Nunca fazer push por
agente**; o push é manual do Felipe.
